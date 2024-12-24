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
import org.mapsforge.map.layer.hills.HillShadingUtils.BlockingSumLimiter;
import org.mapsforge.map.layer.hills.HillShadingUtils.HillShadingThreadPool;
import org.mapsforge.map.layer.hills.HillShadingUtils.SilentFutureTask;

import java.io.File;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Immutably configured, does the work for {@link MemoryCachingHgtReaderTileSource}
 * <p>
 * The file indexing implementation is such that it processes files and folders/ZIPs in parallel (ZIP file is treated as a folder).
 * Packing each HGT file into a separate ZIP and distributing those ZIP files across multiple folders is considered a fairly balanced
 * tactic for saving storage space.
 */
public class HgtCache {

    // Should be lower-case
    public static final String ZipFileExtension = "zip";
    public static final String HgtFileExtension = "hgt";
    public static final String DotZipFileExtension = "." + ZipFileExtension;
    public static final String DotHgtFileExtension = "." + HgtFileExtension;

    /**
     * Default name prefix for additional reading threads created and used by this. A numbered suffix will be appended.
     */
    public static final String ThreadPoolName = "MapsforgeHgtCache";

    protected final DemFolder demFolder;
    protected final ShadingAlgorithm shadingAlgorithm;
    protected final int padding;

    protected final GraphicFactory graphicsFactory;
    protected final Lru lruCache;
    protected final LazyFuture<Map<TileKey, HgtFileInfo>> hgtFiles;
    protected final BlockingSumLimiter blockingSumLimiter = new BlockingSumLimiter();

    protected final List<String> problems = new ArrayList<>();

    protected final AtomicReference<HillShadingThreadPool> ThreadPool = new AtomicReference<>(null);

    public HgtCache(DemFolder demFolder, GraphicFactory graphicsFactory, int padding, ShadingAlgorithm algorithm, int cacheMinCount, int cacheMaxCount, long cacheMaxBytes) {
        this.demFolder = demFolder;
        this.graphicsFactory = graphicsFactory;
        this.shadingAlgorithm = algorithm;
        this.padding = padding;

        this.lruCache = new Lru(cacheMinCount, cacheMaxCount, cacheMaxBytes);

        this.hgtFiles = new LazyFuture<Map<TileKey, HgtFileInfo>>() {
            final Deque<SilentFutureTask> myTasks = new ConcurrentLinkedDeque<>();
            final Map<TileKey, HgtFileInfo> myMap = new HashMap<>();

            @Override
            protected Map<TileKey, HgtFileInfo> calculate() {
                final String regex = ".*([ns])(\\d{1,2})([ew])(\\d{1,3})\\.(?:(" + HgtFileExtension + ")|(" + ZipFileExtension + "))";
                final Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);

                // Create our short-lived thread pool for fast file indexing purposes
                createThreadPoolMaybe();

                indexFolder(demFolder, pattern, problems);

                while (false == myTasks.isEmpty()) {
                    myTasks.pollFirst().get();
                }

                // Our thread pool won't be needed any more
                shutdownThreadPool();

                return myMap;
            }

            void indexFolder(DemFolder folder, Pattern pattern, List<String> problems) {
                for (DemFile demFile : folder.files()) {
                    // Process files concurrently
                    final SilentFutureTask task = new SilentFutureTask(new Callable<Boolean>() {
                        public Boolean call() {
                            indexFile(demFile, pattern, problems);
                            return true;
                        }
                    });

                    postToThreadPoolOrRun(task);
                    myTasks.add(task);
                }

                for (final DemFolder sub : folder.subs()) {
                    // Process folders concurrently
                    final SilentFutureTask task = new SilentFutureTask(new Callable<Boolean>() {
                        public Boolean call() {
                            indexFolder(sub, pattern, problems);
                            return true;
                        }
                    });

                    postToThreadPoolOrRun(task);
                    myTasks.add(task);
                }
            }

            void indexFile(DemFile file, Pattern pattern, List<String> problems) {
                final String name = file.getName();
                final Matcher matcher = pattern.matcher(name);

                if (matcher.matches()) {
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
                    synchronized (myMap) {
                        final HgtFileInfo existing = myMap.get(tileKey);
                        if (existing == null || existing.getSize() < length) {
                            myMap.put(tileKey, new HgtFileInfo(file, north, east, north + 1, east + 1, length));
                        }
                    }
                }
            }
        };
    }

    public HillshadingBitmap getHillshadingBitmap(int northInt, int eastInt, int zoomLevel, double pxPerLat, double pxPerLon, int color) throws InterruptedException, ExecutionException {
        HillshadingBitmap output = null;

        final HgtFileInfo hgtFileInfo = hgtFiles.get().get(new TileKey(northInt, eastInt));

        if (hgtFileInfo != null) {
            final long outputSizeEstimate = shadingAlgorithm.getOutputSizeBytes(hgtFileInfo, padding, zoomLevel, pxPerLat, pxPerLon);

            // Blocking sum limiter is used to prevent cache hammering, and resulting excess memory usage and possible OOM exceptions in extreme situations.
            // This can happen when many future.get() calls (like the one below) are made concurrently without any limits.
            blockingSumLimiter.add(outputSizeEstimate, lruCache.maxBytes);
            try {
                final HgtFileLoadFuture future = hgtFileInfo.getBitmapFuture(HgtCache.this, shadingAlgorithm, padding, zoomLevel, pxPerLat, pxPerLon, color);

                if (false == future.isDone()) {
                    lruCache.ensureEnoughSpace(outputSizeEstimate);
                }

                // This must be called before...
                output = future.get();

                // ...before this.
                lruCache.markUsed(future);
            } finally {
                blockingSumLimiter.subtract(outputSizeEstimate);
            }
        }

        return output;
    }

    /**
     * @return Whether the zoom level is supported on the lat/lon coordinates.
     */
    public boolean isZoomLevelSupported(int zoomLevel, int lat, int lon) {
        boolean retVal = true;

        try {
            if (shadingAlgorithm instanceof AThreadedHillShading) {
                final HgtFileInfo hgtFileInfo = hgtFiles.get().get(new TileKey(lat, lon));

                if (hgtFileInfo != null) {
                    retVal = ((AThreadedHillShading) shadingAlgorithm).isZoomLevelSupported(zoomLevel, hgtFileInfo);
                }
            }
        } catch (Exception ignored) {
        }

        return retVal;
    }

    protected HgtFileLoadFuture createHgtFileLoadFuture(HgtFileInfo hgtFileInfo, int padding, int zoomLevel, double pxPerLat, double pxPerLon, int color) {
        return new HgtFileLoadFuture(hgtFileInfo, padding, zoomLevel, pxPerLat, pxPerLon, color);
    }

    public void indexOnThread() {
        hgtFiles.withRunningThread();
    }

    protected void postToThreadPoolOrRun(final Runnable code) {
        final HillShadingThreadPool threadPool = ThreadPool.get();

        if (threadPool != null) {
            threadPool.executeOrRun(code);
        } else {
            if (code != null) {
                code.run();
            }
        }
    }

    protected void createThreadPoolMaybe() {
        final AtomicReference<HillShadingThreadPool> threadPoolReference = ThreadPool;

        if (threadPoolReference.get() == null) {
            synchronized (threadPoolReference) {
                if (threadPoolReference.get() == null) {
                    threadPoolReference.set(createThreadPool());
                }
            }
        }
    }

    protected HillShadingThreadPool createThreadPool() {
        final int threadCount = AThreadedHillShading.ReadingThreadsCountDefault;
        final int queueSize = Integer.MAX_VALUE;
        return new HillShadingThreadPool(threadCount, threadCount, queueSize, 1, ThreadPoolName).start();
    }

    protected void shutdownThreadPool() {
        final AtomicReference<HillShadingThreadPool> threadPoolReference = ThreadPool;

        synchronized (threadPoolReference) {
            final HillShadingThreadPool threadPool = threadPoolReference.getAndSet(null);

            if (threadPool != null) {
                threadPool.shutdown();
            }
        }
    }

    public void interruptAndDestroy() {
        if (shadingAlgorithm instanceof AThreadedHillShading) {
            ((AThreadedHillShading) shadingAlgorithm).interruptAndDestroy();
        }

        final AtomicReference<HillShadingThreadPool> threadPoolReference = ThreadPool;

        synchronized (threadPoolReference) {
            final HillShadingThreadPool threadPool = threadPoolReference.getAndSet(null);

            if (threadPool != null) {
                threadPool.shutdownNow();
            }
        }
    }

    public static void mergeSameSized(HillshadingBitmap sink, HillshadingBitmap source, HillshadingBitmap.Border border, int padding, Canvas copyCanvas) {

        final Object mutex1, mutex2;
        {
            // Mutexes must be ordered to prevent deadlocks (it doesn't matter how, it just has to be consistent)
            if (source.getMutex().hashCode() < sink.getMutex().hashCode()) {
                mutex1 = source.getMutex();
                mutex2 = sink.getMutex();
            } else {
                mutex1 = sink.getMutex();
                mutex2 = source.getMutex();
            }
        }

        // Synchronized to prevent visual artifacts when using the bitmaps concurrently (see CanvasRasterer)
        synchronized (mutex1) {
            synchronized (mutex2) {
                copyCanvas.setBitmap(sink);

                switch (border) {
                    case WEST:
                        copyCanvas.setClip(0, padding, padding, sink.getHeight() - 2 * padding, true);
                        copyCanvas.drawBitmap(source, -sink.getWidth() + 2 * padding, 0);
                        break;
                    case EAST:
                        copyCanvas.setClip(sink.getWidth() - padding, padding, padding, sink.getHeight() - 2 * padding, true);
                        copyCanvas.drawBitmap(source, sink.getWidth() - 2 * padding, 0);
                        break;
                    case NORTH:
                        copyCanvas.setClip(padding, 0, sink.getWidth() - 2 * padding, padding, true);
                        copyCanvas.drawBitmap(source, 0, -sink.getHeight() + 2 * padding);
                        break;
                    case SOUTH:
                        copyCanvas.setClip(padding, sink.getHeight() - padding, sink.getWidth() - 2 * padding, padding, true);
                        copyCanvas.drawBitmap(source, 0, sink.getHeight() - 2 * padding);
                        break;
                }
            }
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

    protected class HgtFileLoadFuture extends LazyFuture<HillshadingBitmap> {
        protected final HgtFileInfo hgtFileInfo;
        protected final int padding;
        protected final int zoomLevel;
        protected final double pxPerLat, pxPerLon;
        protected final int color;
        protected volatile long sizeBytes = 0;

        HgtFileLoadFuture(HgtFileInfo hgtFileInfo, int padding, int zoomLevel, double pxPerLat, double pxPerLon, int color) {
            this.hgtFileInfo = hgtFileInfo;
            this.padding = padding;
            this.zoomLevel = zoomLevel;
            this.pxPerLat = pxPerLat;
            this.pxPerLon = pxPerLon;
            this.color = color;
        }

        public HillshadingBitmap calculate() {
            HillshadingBitmap output = null;

            final ShadingAlgorithm.RawShadingResult raw = shadingAlgorithm.transformToByteBuffer(this.hgtFileInfo, this.padding, this.zoomLevel, this.pxPerLat, this.pxPerLon);

            if (raw != null) {
                output = graphicsFactory.createMonoBitmap(raw.width, raw.height, raw.bytes, raw.padding, this.hgtFileInfo, this.color);

                if (output != null) {
                    this.sizeBytes = output.getSizeBytes();
                } else {
                    this.sizeBytes = 0;
                }
            } else {
                this.sizeBytes = 0;
            }

            return output;
        }

        public long getCacheTag() {
            return shadingAlgorithm.getCacheTag(this.hgtFileInfo, this.padding, this.zoomLevel, this.pxPerLat, this.pxPerLon);
        }

        public long getSizeBytes() {
            return this.sizeBytes;
        }
    }

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

    protected static class Lru {
        protected final int minCount, maxCount;
        protected final long maxBytes;
        protected final Deque<HgtFileLoadFuture> lruSet = new ArrayDeque<>();
        protected final AtomicLong sizeBytes = new AtomicLong(0);

        protected Lru(int minCount, int maxCount, long maxBytes) {
            this.minCount = minCount;
            this.maxCount = maxCount;
            this.maxBytes = maxBytes;
        }

        /**
         * Note: The Future should be completed by the time this is called.
         * This can be ensured by calling this method AFTER at least one call to future.get() elsewhere in the same thread.
         *
         * @param freshlyUsed the entry that should be marked as freshly used
         */
        public void markUsed(HgtFileLoadFuture freshlyUsed) {
            if (maxBytes > 0 && freshlyUsed != null) {

                final long sizeBytes = freshlyUsed.getSizeBytes();

                synchronized (lruSet) {
                    if (lruSet.remove(freshlyUsed)) {
                        this.sizeBytes.addAndGet(-sizeBytes);
                    }

                    if (lruSet.add(freshlyUsed)) {
                        this.sizeBytes.addAndGet(sizeBytes);
                    }
                }

                manageSize();
            }
        }

        protected void manageSize() {
            synchronized (lruSet) {
                while (lruSet.size() > maxCount || (lruSet.size() > minCount && sizeBytes.get() > maxBytes)) {
                    removeFirst();
                }
            }
        }

        public void removeFirst() {
            synchronized (lruSet) {
                final HgtFileLoadFuture future = lruSet.pollFirst();

                if (future != null) {
                    final long sizeBytes = future.getSizeBytes();
                    this.sizeBytes.addAndGet(-sizeBytes);
                }
            }
        }

        public void ensureEnoughSpace(long bytes) {
            synchronized (lruSet) {
                while (false == lruSet.isEmpty() && bytes + sizeBytes.get() > maxBytes) {
                    removeFirst();
                }
            }
        }
    }
}
