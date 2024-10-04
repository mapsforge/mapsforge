/*
 * Copyright 2017-2022 usrusr
 * Copyright 2019 devemux86
 * Copyright 2024 Sublimis
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.mapsforge.map.layer.hills;

import org.mapsforge.core.graphics.Canvas;
import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.graphics.HillshadingBitmap;
import org.mapsforge.core.model.BoundingBox;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * immutably configured, does the work for {@link MemoryCachingHgtReaderTileSource}
 */
public class HgtCache {
    private static final Logger LOGGER = Logger.getLogger(HgtCache.class.getName());

    // Should be lower-case
    public static final String ZipFileExtension = "zip";
    public static final String HgtFileExtension = "hgt";
    public static final String DotZipFileExtension = "." + ZipFileExtension;
    public static final String DotHgtFileExtension = "." + HgtFileExtension;

    final DemFolder demFolder;
    final boolean interpolatorOverlap;
    final ShadingAlgorithm algorithm;
    final int mainCacheSize;
    final int neighborCacheSize;

    private final GraphicFactory graphicsFactory;

    final private Lru secondaryLru;
    final private Lru mainLru;

    private final LazyFuture<Map<TileKey, HgtFileInfo>> hgtFiles;


    protected static final class TileKey {
        final int north;
        final int east;

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;

            TileKey tileKey = (TileKey) o;

            return north == tileKey.north && east == tileKey.east;
        }

        @Override
        public int hashCode() {
            int result = north;
            result = 31 * result + east;
            return result;
        }

        TileKey(int north, int east) {
            this.east = east;
            this.north = north;
        }
    }

    private static class Lru {
        public int getSize() {
            return size;
        }

        public void setSize(int size) {

            this.size = Math.max(0, size);

            if (size < lru.size()) synchronized (lru) {
                Iterator<Future<HillshadingBitmap>> iterator = lru.iterator();
                while (lru.size() > size) {
                    iterator.remove();
                }
            }
        }

        private int size;
        final private LinkedHashSet<Future<HillshadingBitmap>> lru;

        Lru(int size) {
            this.size = size;
            lru = size > 0 ? new LinkedHashSet<Future<HillshadingBitmap>>() : null;
        }

        /**
         * @param freshlyUsed the entry that should be marked as freshly used
         * @return the evicted entry, which is freshlyUsed if size is 0
         */
        Future<HillshadingBitmap> markUsed(Future<HillshadingBitmap> freshlyUsed) {
            if (size > 0 && freshlyUsed != null) {
                synchronized (lru) {
                    lru.remove(freshlyUsed);
                    lru.add(freshlyUsed);
                    if (lru.size() > size) {
                        Iterator<Future<HillshadingBitmap>> iterator = lru.iterator();
                        Future<HillshadingBitmap> evicted = iterator.next();
                        iterator.remove();
                        return evicted;
                    }
                    return null;
                }
            }
            return freshlyUsed;
        }

        void evict(Future<HillshadingBitmap> loadingFuture) {
            if (size > 0) {
                synchronized (lru) {
                    lru.add(loadingFuture);
                }
            }
        }
    }

    protected final List<String> problems = new ArrayList<>();

    HgtCache(DemFolder demFolder, boolean interpolationOverlap, GraphicFactory graphicsFactory, ShadingAlgorithm algorithm, int mainCacheSize, int neighborCacheSize) {
        this.demFolder = demFolder;
        this.interpolatorOverlap = interpolationOverlap;
        this.graphicsFactory = graphicsFactory;
        this.algorithm = algorithm;
        this.mainCacheSize = mainCacheSize;
        this.neighborCacheSize = neighborCacheSize;

        mainLru = new Lru(this.mainCacheSize);
        secondaryLru = (interpolatorOverlap ? new Lru(neighborCacheSize) : null);

        hgtFiles = new LazyFuture<Map<TileKey, HgtFileInfo>>() {
            @Override
            protected Map<TileKey, HgtFileInfo> calculate() {
                final Map<TileKey, HgtFileInfo> map = new HashMap<>();
                final String regex = ".*([ns])(\\d{1,2})([ew])(\\d{1,3})\\.(?:(" + HgtFileExtension + ")|(" + ZipFileExtension + "))";
                final Matcher matcher = Pattern
                        .compile(regex, Pattern.CASE_INSENSITIVE)
                        .matcher("");
                indexFolder(HgtCache.this.demFolder, matcher, map, problems);
                return map;
            }

            void indexFile(DemFile file, Matcher matcher, Map<TileKey, HgtFileInfo> map, List<String> problems) {
                final String name = file.getName();
                if (matcher
                        .reset(name)
                        .matches()) {
                    final int northsouth = Integer.parseInt(matcher.group(2));
                    final int eastwest = Integer.parseInt(matcher.group(4));

                    final int north = "n".equalsIgnoreCase(matcher.group(1)) ? northsouth : -northsouth;
                    final int east = "e".equalsIgnoreCase(matcher.group(3)) ? eastwest : -eastwest;

                    final long length = file.getSize();
                    final long heights = length / 2;
                    final long sqrt = (long) Math.sqrt(heights);
                    if (heights == 0 || sqrt * sqrt != heights) {
                        if (problems != null)
                            problems.add(file + " length in shorts (" + heights + ") is not a square number");
                        return;
                    }

                    final TileKey tileKey = new TileKey(north, east);
                    final HgtFileInfo existing = map.get(tileKey);
                    if (existing == null || existing.size < length) {
                        map.put(tileKey, new HgtFileInfo(file, north - 1, east, north, east + 1, length));
                    }
                }
            }

            void indexFolder(DemFolder folder, Matcher matcher, Map<TileKey, HgtFileInfo> map, List<String> problems) {
                for (DemFile demFile : folder.files()) {
                    indexFile(demFile, matcher, map, problems);
                }
                for (DemFolder sub : folder.subs()) {
                    indexFolder(sub, matcher, map, problems);
                }
            }
        };

    }

    void indexOnThread() {
        hgtFiles.withRunningThread();
    }


    class LoadUnmergedFuture extends LazyFuture<HillshadingBitmap> {
        private final HgtFileInfo hgtFileInfo;

        LoadUnmergedFuture(HgtFileInfo hgtFileInfo) {
            this.hgtFileInfo = hgtFileInfo;
        }

        public HillshadingBitmap calculate() {
            ShadingAlgorithm.RawShadingResult raw = algorithm.transformToByteBuffer(hgtFileInfo, HgtCache.this.interpolatorOverlap ? 1 : 0);

            // is this really necessary? Maybe, if some downscaling is filtered and rounding is not as expected
            raw.fillPadding();

            return graphicsFactory.createMonoBitmap(raw.width, raw.height, raw.bytes, raw.padding, hgtFileInfo);
        }
    }

    /* */
    class MergeOverlapFuture extends LazyFuture<HillshadingBitmap> {
        final LoadUnmergedFuture loadFuture;
        private final HgtFileInfo hgtFileInfo;

        MergeOverlapFuture(HgtFileInfo hgtFileInfo, LoadUnmergedFuture loadFuture) {

            this.hgtFileInfo = hgtFileInfo;
            this.loadFuture = loadFuture;
        }

        MergeOverlapFuture(HgtFileInfo hgtFileInfo) {
            this(hgtFileInfo, new LoadUnmergedFuture(hgtFileInfo));
        }

        public HillshadingBitmap calculate() throws ExecutionException, InterruptedException {
            HillshadingBitmap monoBitmap = loadFuture.get();

            for (HillshadingBitmap.Border border : HillshadingBitmap.Border.values()) {
                HgtFileInfo neighbor = hgtFileInfo.getNeighbor(border);
                mergePaddingOnBitmap(monoBitmap, neighbor, border);
            }

            return monoBitmap;
        }

        private void mergePaddingOnBitmap(HillshadingBitmap fresh, HgtFileInfo neighbor, HillshadingBitmap.Border border) {
            final int padding = fresh.getPadding();

            if (padding < 1) return;

            if (neighbor != null) {
                final Future<HillshadingBitmap> neighborUnmergedFuture = neighbor.getUnmergedAsMergePartner();
                if (neighborUnmergedFuture != null) {
                    try {
                        HillshadingBitmap other = neighborUnmergedFuture.get();
                        Canvas copyCanvas = graphicsFactory.createCanvas();

                        mergeSameSized(fresh, other, border, padding, copyCanvas);

                    } catch (InterruptedException | ExecutionException e) {
//                        e.printStackTrace();
                        LOGGER.log(Level.WARNING, e.toString());
                    }
                }
            }
        }
    }


    public class HgtFileInfo extends BoundingBox implements ShadingAlgorithm.RawHillTileSource {
        final DemFile file;
        final Object mWeakRefSync = new Object();
        WeakReference<Future<HillshadingBitmap>> weakRef = null;

        final long size;

        HgtFileInfo(DemFile file, double minLatitude, double minLongitude, double maxLatitude, double maxLongitude, long size) {
            super(minLatitude, minLongitude, maxLatitude, maxLongitude);
            this.file = file;
            this.size = size;
        }

        Future<HillshadingBitmap> getBitmapFuture(double pxPerLat, double pxPerLng) {
            if (HgtCache.this.interpolatorOverlap) {

                int axisLen = algorithm.getOutputAxisLen(this);
                if (pxPerLat > axisLen || pxPerLng > axisLen) {
                    return getForHires();
                } else {
                    return getForLores();
                }
            } else {
                return getForLores();
            }
        }


        /**
         * for zoomed in view (if padding): merged or unmerged padding for padding merge of a neighbor
         *
         * @return MergeOverlapFuture or LoadUnmergedFuture as available
         */
        private MergeOverlapFuture getForHires() {
            synchronized (mWeakRefSync) {
                final MergeOverlapFuture ret;

                final WeakReference<Future<HillshadingBitmap>> weak = this.weakRef;
                final Future<HillshadingBitmap> candidate = weak == null ? null : weak.get();

                if (candidate instanceof MergeOverlapFuture) {
                    ret = ((MergeOverlapFuture) candidate);
                } else if (candidate instanceof LoadUnmergedFuture) {
                    LoadUnmergedFuture loadFuture = (LoadUnmergedFuture) candidate;
                    ret = new MergeOverlapFuture(this, loadFuture);
                    this.weakRef = new WeakReference<>(ret);
                    secondaryLru.evict(loadFuture);  // candidate will henceforth be referenced via created (until created is gone)
                } else {
                    ret = new MergeOverlapFuture(this);
                    //logLru("new merged", mainLru, ret);
                    weakRef = new WeakReference<>(ret);
                }
                mainLru.markUsed(ret);

                //logLru("merged", mainLru, ret);
                return ret;
            }
        }

        /**
         * for zoomed in view (if padding): merged or unmerged padding for padding merge of a neighbor
         *
         * @return MergeOverlapFuture or LoadUnmergedFuture as available
         */
        private LoadUnmergedFuture getUnmergedAsMergePartner() {
            synchronized (mWeakRefSync) {
                final WeakReference<Future<HillshadingBitmap>> weak = this.weakRef;
                Future<HillshadingBitmap> candidate = weak == null ? null : weak.get();


                final LoadUnmergedFuture ret;
                if (candidate instanceof LoadUnmergedFuture) {
                    secondaryLru.markUsed(candidate);
                    ret = (LoadUnmergedFuture) candidate;
                } else if (candidate instanceof MergeOverlapFuture) {
                    mainLru.markUsed(candidate);
                    ret = ((MergeOverlapFuture) candidate).loadFuture;
                } else {
                    final LoadUnmergedFuture created = new LoadUnmergedFuture(this);
                    this.weakRef = new WeakReference<Future<HillshadingBitmap>>(created);
                    secondaryLru.markUsed(created);
                    ret = created;
                }
                return ret;
            }
        }

        /**
         * for zoomed out view (or all resolutions, if no padding): merged or unmerged padding, primary LRU spilling over to secondary (if available)
         *
         * @return MergeOverlapFuture or LoadUnmergedFuture as available
         */
        private Future<HillshadingBitmap> getForLores() {
            synchronized (mWeakRefSync) {
                final WeakReference<Future<HillshadingBitmap>> weak = this.weakRef;
                Future<HillshadingBitmap> candidate = weak == null ? null : weak.get();

                if (candidate == null) {
                    candidate = new LoadUnmergedFuture(this);
                    this.weakRef = new WeakReference<>(candidate);
                }
                final Future<HillshadingBitmap> evicted = mainLru.markUsed(candidate);
                if (secondaryLru != null) secondaryLru.markUsed(evicted);
                return candidate;
            }
        }

        @Override
        public HillshadingBitmap getFinishedConverted() {
            synchronized (mWeakRefSync) {
                WeakReference<Future<HillshadingBitmap>> weak = this.weakRef;
                if (weak != null) {
                    Future<HillshadingBitmap> hillshadingBitmapFuture = weak.get();
                    if (hillshadingBitmapFuture != null && hillshadingBitmapFuture.isDone()) {
                        try {
                            return hillshadingBitmapFuture.get();
                        } catch (InterruptedException | ExecutionException e) {
//                        e.printStackTrace();
                            LOGGER.log(Level.WARNING, e.toString());
                        }
                    }
                }
                return null;
            }
        }

        @Override
        public long getSize() {
            return size;
        }

        @Override
        public DemFile getFile() {
            return file;
        }

        @Override
        public double northLat() {
            return maxLatitude;
        }

        @Override
        public double southLat() {
            return minLatitude;
        }

        @Override
        public double westLng() {
            return minLongitude;
        }

        @Override
        public double eastLng() {
            return maxLongitude;
        }

        private HgtFileInfo getNeighbor(HillshadingBitmap.Border border) throws ExecutionException, InterruptedException {

            Map<TileKey, HgtFileInfo> map = hgtFiles.get();
            switch (border) {
                case NORTH:
                    return map.get(new TileKey((int) maxLatitude + 1, (int) minLongitude));
                case SOUTH:
                    return map.get(new TileKey((int) maxLatitude - 1, (int) minLongitude));
                case EAST:
                    return map.get(new TileKey((int) maxLatitude, (int) minLongitude + 1));
                case WEST:
                    return map.get(new TileKey((int) maxLatitude, (int) minLongitude - 1));
            }
            return null;
        }

        @Override
        public String toString() {
            Future<HillshadingBitmap> future = weakRef == null ? null : weakRef.get();
            return "[lt:" + minLatitude + "-" + maxLatitude + " ln:" + minLongitude + "-" + maxLongitude + (future == null ? "" : future.isDone() ? "done" : "wip") + "]";
        }
    }


    HillshadingBitmap getHillshadingBitmap(int northInt, int eastInt, double pxPerLat, double pxPerLng) throws InterruptedException, ExecutionException {
        final HgtFileInfo hgtFileInfo = hgtFiles
                .get()
                .get(new TileKey(northInt, eastInt));

        if (hgtFileInfo == null) {
            return null;
        }

        Future<HillshadingBitmap> future = hgtFileInfo.getBitmapFuture(pxPerLat, pxPerLng);
        return future.get();
    }

    static void mergeSameSized(HillshadingBitmap center, HillshadingBitmap neighbor, HillshadingBitmap.Border border, int padding, Canvas copyCanvas) {
        final HillshadingBitmap sink;
        final HillshadingBitmap source;

        if (border == HillshadingBitmap.Border.EAST) {
            sink = center;
            source = neighbor;
            copyCanvas.setBitmap(sink);
            copyCanvas.setClip(sink.getWidth() - padding, padding, padding, sink.getHeight() - 2 * padding, true);
            copyCanvas.drawBitmap(source, (source.getWidth() - 2 * padding), 0);
        } else if (border == HillshadingBitmap.Border.WEST) {
            sink = center;
            source = neighbor;
            copyCanvas.setBitmap(sink);
            copyCanvas.setClip(0, padding, padding, sink.getHeight() - 2 * padding, true);
            copyCanvas.drawBitmap(source, 2 * padding - (source.getWidth()), 0);
        } else if (border == HillshadingBitmap.Border.NORTH) {
            sink = center;
            source = neighbor;
            copyCanvas.setBitmap(sink);
            copyCanvas.setClip(padding, 0, sink.getWidth() - 2 * padding, padding, true);
            copyCanvas.drawBitmap(source, 0, 2 * padding - (source.getHeight()));
        } else if (border == HillshadingBitmap.Border.SOUTH) {
            sink = center;
            source = neighbor;
            copyCanvas.setBitmap(sink);
            copyCanvas.setClip(padding, sink.getHeight() - padding, sink.getWidth() - 2 * padding, padding, true);
            copyCanvas.drawBitmap(source, 0, (source.getHeight() - 2 * padding));
        }
    }

    public static boolean isFileNameZip(final String fileName) {
        boolean retVal = false;

        if (fileName != null) {
            retVal = fileName
                    .toLowerCase()
                    .endsWith(HgtCache.DotZipFileExtension);
        }

        return retVal;
    }

    public static boolean isFileNameHgt(final String fileName) {
        boolean retVal = false;

        if (fileName != null) {
            retVal = fileName
                    .toLowerCase()
                    .endsWith(HgtCache.DotHgtFileExtension);
        }

        return retVal;
    }

    public static boolean isFileZip(final File file) {
        return isFileNameZip(file.getName());
    }

    public static boolean isFileHgt(final File file) {
        return isFileNameHgt(file.getName());
    }

    public static boolean isFileZip(final DemFile file) {
        return isFileNameZip(file.getName());
    }

//    private void logLru(String merged, Lru lru, Future<HillshadingBitmap> ret) {
//        try {
//            StringBuilder sb = new StringBuilder();
//            sb.append(merged).append("\n  LRU: ");
//            synchronized (lru.lru) {
//                for (Future<HillshadingBitmap> f : lru.lru){
//                    sb.append("   E#"+System.identityHashCode(f));
//                }
//                sb.append("\n  ");
//                for(HgtFileInfo hgt : hgtFiles.get().values()){
//                    WeakReference<Future<HillshadingBitmap>> weakRef = hgt.weakRef;
//                    if(weakRef!=null){
//                        Future<HillshadingBitmap> f = weakRef.get();
//                        if(f!=null){
//                            sb.append("  ").append(f.getClass().getSimpleName().substring(0,3));
//                            sb.append("#").append(System.identityHashCode(f));
//                        }
//                    }
//                }
//            }
//            System.out.println("\n"+sb+"\n");
//        }  catch(RuntimeException e){
//            e.printStackTrace();
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        } catch (ExecutionException e) {
//            e.printStackTrace();
//        }
//    }


//    private static void logbytes(HillshadingBitmap fresh, String msg) {
//        try {
//            Class<?> superclass = fresh.getClass().getSuperclass();
//            Field bufferedImage = superclass.getDeclaredField("bufferedImage");
//            bufferedImage.setAccessible(true);
//            BufferedImage bi = (BufferedImage) bufferedImage.get(fresh);
//            Raster data = bi.getData();
//            StringBuilder sb = new StringBuilder();
//            for(int y=0;y<data.getHeight();y+=(y==4?data.getHeight()-8:1)) {
//                if(y==4) {
//                    sb.append("\n");
//                    continue;
//                }
//                sb.append("\n").append(String.format(" %5d", y)).append(":     ");
//                for(int x=0;x<data.getWidth();x+=(x==4?data.getWidth()-8:1)) {
//                    if(x==4) {
//                        sb.append("   ");
//                        continue;
//                    }
//                    int sample = data.getSample(x, y, 0);
//                    sb.append(String.format(" %3d", sample));
//                }
//
//            }
//            System.out.println(msg+" sample: "+fresh.getAreaRect()+sb);
//
//        } catch (NoSuchFieldException e) {
//            e.printStackTrace();
//        } catch (IllegalAccessException e) {
//            e.printStackTrace();
//        }
//
//    }
}
