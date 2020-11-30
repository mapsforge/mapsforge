/*
 * Copyright 2017 usrusr
 * Copyright 2019 devemux86
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
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * immutably configured, does the work for {@link MemoryCachingHgtReaderTileSource}
 */
class HgtCache {
    final File demFolder;
    final boolean interpolatorOverlap;
    final ShadingAlgorithm algorithm;
    final int mainCacheSize;
    final int neighborCacheSize;

    private final GraphicFactory graphicsFactory;

    final private Lru secondaryLru;
    final private Lru mainLru;

    private LazyFuture<Map<TileKey, HgtFileInfo>> hgtFiles;


    protected final static class TileKey {
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

    private List<String> problems = new ArrayList<>();

    HgtCache(File demFolder, boolean interpolationOverlap, GraphicFactory graphicsFactory, ShadingAlgorithm algorithm, int mainCacheSize, int neighborCacheSize) {
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
                Map<TileKey, HgtFileInfo> map = new HashMap<>();
                Matcher matcher = Pattern.compile("([ns])(\\d{1,2})([ew])(\\d{1,3})\\.(?:(hgt)|(zip))", Pattern.CASE_INSENSITIVE).matcher("");
                crawl(HgtCache.this.demFolder, matcher, map, problems);
                return map;
            }

            void crawl(File file, Matcher matcher, Map<TileKey, HgtFileInfo> map, List<String> problems) {
                if (file.exists()) {
                    if (file.isFile()) {
                        String name = file.getName();
                        if (matcher.reset(name).matches()) {
                            int northsouth = Integer.parseInt(matcher.group(2));
                            int eastwest = Integer.parseInt(matcher.group(4));

                            int north = "n".equals(matcher.group(1).toLowerCase()) ? northsouth : -northsouth;
                            int east = "e".equals(matcher.group(3).toLowerCase()) ? eastwest : -eastwest;

                            long length = 0;

                            if (matcher.group(6) == null) {
                                length = file.length();
                            } else {
                                // zip
                                ZipInputStream zipInputStream = null;
                                try {
                                    zipInputStream = new ZipInputStream(new FileInputStream(file));
                                    String expectedHgt = name.toLowerCase().substring(0, name.length() - 4) + ".hgt";
                                    ZipEntry entry;
                                    while (null != (entry = zipInputStream.getNextEntry())) {
                                        if (expectedHgt.equals(entry.getName().toLowerCase())) {
                                            length = entry.getSize();
                                            break;
                                        }
                                    }
                                } catch (IOException e) {
                                    problems.add("could not read zip file " + file.getName());
                                }
                                if (zipInputStream != null) {
                                    try {
                                        zipInputStream.close();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                            long heights = length / 2;
                            long sqrt = (long) Math.sqrt(heights);
                            if (heights == 0 || sqrt * sqrt != heights) {
                                if (problems != null)
                                    problems.add(file + " length in shorts (" + heights + ") is not a square number");
                                return;
                            }

                            TileKey tileKey = new TileKey(north, east);
                            HgtFileInfo existing = map.get(tileKey);
                            if (existing == null || existing.size < length) {
                                map.put(tileKey, new HgtFileInfo(file, north - 1, east, north, east + 1, length));
                            }
                        }
                    } else if (file.isDirectory()) {
                        File[] files = file.listFiles();
                        if (files != null) {
                            for (File sub : files) {
                                crawl(sub, matcher, map, problems);
                            }
                        }
                    }
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
        private HgtFileInfo hgtFileInfo;

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
            int padding = fresh.getPadding();

            if (padding < 1) return;
            if (neighbor != null) {
                Future<HillshadingBitmap> neighborUnmergedFuture = neighbor.getUnmergedAsMergePartner();
                if (neighborUnmergedFuture != null) {
                    try {
                        HillshadingBitmap other = neighborUnmergedFuture.get();
                        Canvas copyCanvas = graphicsFactory.createCanvas();

                        mergeSameSized(fresh, other, border, padding, copyCanvas);

                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }


    class HgtFileInfo extends BoundingBox implements ShadingAlgorithm.RawHillTileSource {
        final File file;
        WeakReference<Future<HillshadingBitmap>> weakRef = null;

        final long size;

        HgtFileInfo(File file, double minLatitude, double minLongitude, double maxLatitude, double maxLongitude, long size) {
            super(minLatitude, minLongitude, maxLatitude, maxLongitude);
            this.file = file;
            this.size = size;
        }

        Future<HillshadingBitmap> getBitmapFuture(double pxPerLat, double pxPerLng) {
            if (HgtCache.this.interpolatorOverlap) {

                int axisLen = algorithm.getAxisLenght(this);
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
            final WeakReference<Future<HillshadingBitmap>> weak = this.weakRef;
            Future<HillshadingBitmap> candidate = weak == null ? null : weak.get();

            final MergeOverlapFuture ret;
            if (candidate instanceof MergeOverlapFuture) {
                ret = ((MergeOverlapFuture) candidate);
            } else if (candidate instanceof LoadUnmergedFuture) {
                LoadUnmergedFuture loadFuture = (LoadUnmergedFuture) candidate;
                ret = new MergeOverlapFuture(this, loadFuture);
                this.weakRef = new WeakReference<Future<HillshadingBitmap>>(ret);
                secondaryLru.evict(loadFuture);  // candidate will henceforth be referenced via created (until created is gone)
            } else {
                ret = new MergeOverlapFuture(this);
//logLru("new merged", mainLru, ret);
                weakRef = new WeakReference<Future<HillshadingBitmap>>(ret);
            }
            mainLru.markUsed(ret);

//logLru("merged", mainLru, ret);
            return ret;
        }

        /**
         * for zoomed in view (if padding): merged or unmerged padding for padding merge of a neighbor
         *
         * @return MergeOverlapFuture or LoadUnmergedFuture as available
         */
        private LoadUnmergedFuture getUnmergedAsMergePartner() {
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

        /**
         * for zoomed out view (or all resolutions, if no padding): merged or unmerged padding, primary LRU spilling over to secondary (if available)
         *
         * @return MergeOverlapFuture or LoadUnmergedFuture as available
         */
        private Future<HillshadingBitmap> getForLores() {
            final WeakReference<Future<HillshadingBitmap>> weak = this.weakRef;
            Future<HillshadingBitmap> candidate = weak == null ? null : weak.get();

            if (candidate == null) {
                candidate = new LoadUnmergedFuture(this);
                this.weakRef = new WeakReference<>(candidate);
            }
            Future<HillshadingBitmap> evicted = mainLru.markUsed(candidate);
            if (secondaryLru != null) secondaryLru.markUsed(evicted);
            return candidate;
        }

        @Override
        public HillshadingBitmap getFinishedConverted() {
            WeakReference<Future<HillshadingBitmap>> weak = this.weakRef;
            if (weak != null) {
                Future<HillshadingBitmap> hillshadingBitmapFuture = weak.get();
                if (hillshadingBitmapFuture != null && hillshadingBitmapFuture.isDone()) {
                    try {
                        return hillshadingBitmapFuture.get();
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                    }
                }
            }
            return null;
        }

        @Override
        public long getSize() {
            return size;
        }

        @Override
        public File getFile() {
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
        HgtFileInfo hgtFileInfo = hgtFiles.get().get(new TileKey(northInt, eastInt));
        if (hgtFileInfo == null)
            return null;

        Future<HillshadingBitmap> future = hgtFileInfo.getBitmapFuture(pxPerLat, pxPerLng);
        return future.get();
    }

    static void mergeSameSized(HillshadingBitmap center, HillshadingBitmap neighbor, HillshadingBitmap.Border border, int padding, Canvas copyCanvas) {
        HillshadingBitmap sink;
        HillshadingBitmap source;

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
