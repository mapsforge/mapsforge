/*
 * Copyright 2017 usrusr
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

import org.mapsforge.core.graphics.Bitmap;
import org.mapsforge.core.graphics.GraphicFactory;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HillsRenderConfig {
    private File demFolder;
    private int demCacheSize = 4;

    private ShadingAlgorithm algorithm;

    private FutureTask<HgtCache> hgtCacheFuture;
    private final GraphicFactory graphicsFactory;

    public HillsRenderConfig(File demFolder, GraphicFactory graphicsFactory, ShadingAlgorithm algorithm) {
        this.graphicsFactory = graphicsFactory;
        this.algorithm = algorithm;
        setDemFolder(demFolder);
    }

    public Bitmap getShadingTile(int latitudeOfSouthWestCorner, int longituedOfSouthWestCorner) throws ExecutionException, InterruptedException {
        if (hgtCacheFuture == null)
            return null;

        Bitmap ret = getShadingTileInternal(latitudeOfSouthWestCorner, longituedOfSouthWestCorner);
        if (ret == null && Math.abs(longituedOfSouthWestCorner) > 178) { // don't think too hard about where exactly the border is (not much height data there anyway)
            ret = getShadingTileInternal(latitudeOfSouthWestCorner, longituedOfSouthWestCorner > 0 ? longituedOfSouthWestCorner - 180 : longituedOfSouthWestCorner + 180);
        }

        return ret;
    }

    public Bitmap getShadingTileInternal(int northInt, int eastInt) throws ExecutionException, InterruptedException {
        HgtCache hgtCache = this.hgtCacheFuture.get();
        HgtCache.HgtFileInfo hgtFileInfo = hgtCache.hgtFiles.get(new TileKey(northInt, eastInt));
        if (hgtFileInfo == null)
            return null;

        Future<Bitmap> future = hgtFileInfo.getParsed();
        return future.get();
    }

    public File getDemFolder() {
        return demFolder;
    }

    public void setDemFolder(final File demFolder) {
        if (demFolder == null) {
            hgtCacheFuture = null;
        } else if (demFolder.equals(this.demFolder)) {
            return;
        }
        this.demFolder = demFolder;
        hgtCacheFuture = new FutureTask<>(new Callable<HgtCache>() {
            @Override
            public HgtCache call() throws Exception {
                return new HgtCache(demFolder);
            }
        });
        new Thread(hgtCacheFuture, "DEM HGT index").start();
    }

    class HgtCache {
        LinkedHashSet<Future<Bitmap>> lru = new LinkedHashSet<>();

        List<String> problems = new ArrayList<>();

        HgtCache(File demFolder) {
            crawl(demFolder, Pattern.compile("(n|s)(\\d{1,2})(e|w)(\\d{1,3})\\.hgt", Pattern.CASE_INSENSITIVE).matcher(""), problems);
        }

        private void crawl(File file, Matcher matcher, List<String> problems) {
            if (file.exists()) {
                if (file.isFile()) {
                    String name = file.getName();
                    if (matcher.reset(name).matches()) {
                        int northsouth = Integer.parseInt(matcher.group(2));
                        int eastwest = Integer.parseInt(matcher.group(4));

                        int north = "n".equals(matcher.group(1).toLowerCase()) ? northsouth : -northsouth;
                        int east = "e".equals(matcher.group(3).toLowerCase()) ? eastwest : -eastwest;

                        long length = file.length();
                        long heights = length / 2;
                        long sqrt = (long) Math.sqrt(heights);
                        if (sqrt * sqrt != heights) {
                            if (problems != null)
                                problems.add(file + " length in shorts (" + heights + ") is not a square number");
                        } else {
                            TileKey tileKey = new TileKey(north, east);
                            HgtFileInfo existing = hgtFiles.get(tileKey);
                            if (existing == null || existing.size < length) {
                                hgtFiles.put(tileKey, new HgtFileInfo(file, length, east, north));
                            }
                        }
                    }
                } else if (file.isDirectory()) {
                    for (File sub : file.listFiles()) {
                        crawl(sub, matcher, problems);
                    }
                }
            }
        }

        class HgtFileInfo implements Callable<Bitmap>, ShadingAlgorithm.RawHillTileSource {
            final File file;
            WeakReference<Future<Bitmap>> weakRef = null;

            final long size;
            private final int east;
            private final int north;

            HgtFileInfo(File file, int east, int north) {
                this(file, file.length(), east, north);
            }

            HgtFileInfo(File file, long length, int east, int north) {
                this.file = file;
                size = file.length();
                this.east = east;
                this.north = north;
            }

            Future<Bitmap> getParsed() {
                Future<Bitmap> future = getBeforeLru();
                if (demCacheSize > 0) {
                    synchronized (lru) {
                        if (!lru.remove(future) && lru.size() + 1 >= demCacheSize) {
                            Future<Bitmap> oldest = lru.iterator().next();
                            lru.remove(oldest);
                        }
                        lru.add(future);
                    }
                }
                return future;
            }

            private Future<Bitmap> getBeforeLru() {
                Future<Bitmap> existing = weakRef == null ? null : weakRef.get();
                if (existing != null)
                    return existing;

                FutureTask<Bitmap> created = new FutureTask<>(this);

                weakRef = new WeakReference<Future<Bitmap>>(created);

                created.run();

                return created;
            }

            @Override
            public Bitmap call() throws Exception {
                return algorithm.convertTile(this, graphicsFactory);
            }

            @Override
            public long getSize() {
                return size;
            }

            @Override
            public BufferedInputStream openInputStream() throws IOException {
                return new BufferedInputStream(new FileInputStream(file));
            }

            @Override
            public ShadingAlgorithm.RawHillTileSource getNeighborNorth() {
                return hgtFiles.get(new TileKey(north + 1, east));
            }

            @Override
            public ShadingAlgorithm.RawHillTileSource getNeighborSouth() {
                return hgtFiles.get(new TileKey(north + 1, east));
            }

            @Override
            public ShadingAlgorithm.RawHillTileSource getNeighborEast() {
                return hgtFiles.get(new TileKey(north, east + 1));
            }

            @Override
            public ShadingAlgorithm.RawHillTileSource getNeighborWest() {
                return hgtFiles.get(new TileKey(north, east - 1));
            }
        }

        Map<TileKey, HgtFileInfo> hgtFiles = new HashMap<>();
    }

    private final static class TileKey {
        final int north;
        final int east;

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;

            TileKey tileKey = (TileKey) o;

            if (north != tileKey.north)
                return false;
            return east == tileKey.east;
        }

        @Override
        public int hashCode() {
            int result = north;
            result = 31 * result + east;
            return result;
        }

        private TileKey(int north, int east) {
            this.east = east;
            this.north = north;
        }
    }
}
