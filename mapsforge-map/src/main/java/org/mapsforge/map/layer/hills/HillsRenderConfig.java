package org.mapsforge.map.layer.hills;

import org.mapsforge.core.graphics.Bitmap;
import org.mapsforge.core.model.Dimension;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by ulf on 17.01.2017.
 */
public class HillsRenderConfig {
    private File demFolder;
    private int demCacheSize = 4;

    private ShadingAlgorithm algorithm;

    private FutureTask<HgtCache> hgtCache;

    public HillsRenderConfig(File demFolder) {
        this.demFolder = demFolder;


    }

//    public Future<Bitmap> getShadingTileFuture(float east, float north) {
//        final int eastInt = (int) Math.floor(east);
//        final int northInt = (int) Math.floor(north);
//
//        if (hgtCache == null) return new FailedFuture<>(new RuntimeException("DEM folder not set"));
//        if (hgtCache.isDone()) {
//            try {
//                HgtCache hgtCache = this.hgtCache.get();
//                HgtCache.HgtFileInfo hgtFileInfo = hgtCache.hgtFiles.get(new Dimension(eastInt, northInt));
//                return hgtFileInfo.getParsed();
//            } catch (InterruptedException | ExecutionException e) {
//                return new FailedFuture(e);
//            }
//        } else {
//            return new FlatMappedFuture<HgtCache, T>(hgtCache) {
//                @Override
//                protected Future<Bitmap> mapToFuture(HgtCache hgtCache) throws ExecutionException, InterruptedException {
//                    HgtCache.HgtFileInfo hgtFileInfo = hgtCache.hgtFiles.get(new Dimension(eastInt, northInt));
//                    return hgtFileInfo.getParsed();
//                }
//            };
//        }
//    }

    public Bitmap getShadingTile(float east, float north) throws ExecutionException, InterruptedException {
        final int eastInt = (int) Math.floor(east);
        final int northInt = (int) Math.floor(north);

        if (hgtCache == null) return null;

        HgtCache hgtCache = this.hgtCache.get();
        HgtCache.HgtFileInfo hgtFileInfo = hgtCache.hgtFiles.get(new Dimension(eastInt, northInt));
        Future<Bitmap> future = hgtFileInfo.getParsed();
        return future.get();
    }


    public File getDemFolder() {
        return demFolder;
    }

    public void setDemFolder(final File demFolder) {
        if (demFolder == null) {
            hgtCache = null;
        } else if (demFolder.equals(this.demFolder)) {
            return;
        }
        this.demFolder = demFolder;
        hgtCache = new FutureTask<HgtCache>(new Callable<HgtCache>() {
            @Override
            public HgtCache call() throws Exception {
                return new HgtCache(demFolder);
            }
        });
        new Thread(hgtCache, "DEM HGT index").start();
    }

    class HgtCache{
        LinkedHashSet<Future<Bitmap>> lru = new LinkedHashSet<>();

        List<String> problems = new ArrayList<>();

        public HgtCache(File demFolder) {

            crawl(demFolder, Pattern.compile("n(\\d{1,2})e(\\d{1,3})\\.\\hgt", Pattern.CASE_INSENSITIVE).matcher(""), problems);

        }

        private void crawl(File file, Matcher matcher, List<String> problems) {
            if (file.exists()) {
                if (file.isFile()) {
                    if (matcher.reset(file.getName()).matches()) {
                        int north = Integer.parseInt(matcher.group(1));
                        int east = Integer.parseInt(matcher.group(2));

                        long length = file.length();
                        long heights = length / 2;
                        long sqrt = (long) Math.sqrt(heights);
                        if (sqrt * sqrt != heights) {
                            if (problems != null)
                                problems.add(file + " length in shorts (" + heights + ") is not a square number");
                        } else {
                            Dimension dimension = new Dimension(east, north);
                            HgtFileInfo existing = hgtFiles.get(dimension);
                            if (existing == null || existing.size < length) {
                                hgtFiles.put(dimension, new HgtFileInfo(file, length, east, north));
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

            public Future<Bitmap> getParsed() {
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
                Future<Bitmap> fut = weakRef.get();
                if (fut != null) return fut;
                fut = new FutureTask<>(this);

                weakRef = new WeakReference<Future<Bitmap>>(fut);

                return fut;
            }

            @Override
            public Bitmap call() throws Exception {
                return null;
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
                return hgtFiles.get(new Dimension(east, north+1));
            }

            @Override
            public ShadingAlgorithm.RawHillTileSource getNeighborSouth() {
                return hgtFiles.get(new Dimension(east, north+1));
            }

            @Override
            public ShadingAlgorithm.RawHillTileSource getNeighborEast() {
                return hgtFiles.get(new Dimension(east+1, north));
            }

            @Override
            public ShadingAlgorithm.RawHillTileSource getNeighborWest() {
                return hgtFiles.get(new Dimension(east-1, north));
            }
        }

        Map<Dimension, HgtFileInfo> hgtFiles = new HashMap<>();

    }
//
//    private class ResultFuture<X> implements Future<X> {
//        private X result;
//
//        public ResultFuture(X result) {
//            this.result = result;
//        }
//
//        @Override
//        public boolean cancel(boolean mayInterruptIfRunning) {
//            return false;
//        }
//
//        @Override
//        public boolean isCancelled() {
//            return false;
//        }
//
//        @Override
//        public boolean isDone() {
//            return true;
//        }
//
//        @Override
//        public X get() throws InterruptedException, ExecutionException {
//            return result;
//        }
//
//        @Override
//        public X get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
//            return result;
//        }
//    }
//
//    private static class FailedFuture<X> implements Future<X> {
//        private ExecutionException result;
//
//        public FailedFuture(Throwable t) {
//            if (t instanceof ExecutionException) result = (ExecutionException) t;
//            this.result = new ExecutionException(t);
//        }
//
//        @Override
//        public boolean cancel(boolean mayInterruptIfRunning) {
//            return false;
//        }
//
//        @Override
//        public boolean isCancelled() {
//            return false;
//        }
//
//        @Override
//        public boolean isDone() {
//            return true;
//        }
//
//        @Override
//        public X get() throws InterruptedException, ExecutionException {
//            throw result;
//        }
//
//        @Override
//        public X get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
//            throw result;
//        }
//    }
//
//    /**
//     * @param <F>
//     * @param <T>
//     */
//    abstract static class MappedFuture<F, T> implements Future<Bitmap> {
//        private Future<F> wrapped;
//        private T result;
//        ExecutionException exception;
//        public boolean cancelled;
//        private boolean done;
//
//        MappedFuture(Future<F> wrapped) {
//            this.wrapped = wrapped;
//            if (wrapped == null) exception = new ExecutionException(new NullPointerException("missing future"));
//        }
//
//        @Override
//        public boolean cancel(boolean mayInterruptIfRunning) {
//            this.cancelled = true;
//            wrapped = null;
//            return true;
//        }
//
//        @Override
//        public boolean isCancelled() {
//            return cancelled;
//        }
//
//        @Override
//        public boolean isDone() {
//            return cancelled || done || exception != null;
//        }
//
//        @Override
//        public T get() throws InterruptedException, ExecutionException {
//            synchronized (this) {
//                if (exception != null) throw exception;
//                if (done) {
//                    return result;
//                }
//            }
//
//            F f = wrapped.get();
//            synchronized (this) {
//                if (exception != null) throw exception;
//                if (!done) try {
//                    result = map(f);
//                    done = true;
//                } catch (RuntimeException e) {
//                    exception = new ExecutionException(e);
//                    throw exception;
//                }
//                return result;
//            }
//        }
//
//        @Override
//        public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
//            synchronized (this) {
//                if (exception != null) throw exception;
//                if (done) {
//                    return result;
//                }
//            }
//
//            F f = wrapped.get(timeout, unit);
//            synchronized (this) {
//                if (exception != null) throw exception;
//                if (!done) try {
//                    result = map(f);
//                    done = true;
//                } catch (RuntimeException e) {
//                    exception = new ExecutionException(e);
//                    throw exception;
//                }
//                return result;
//            }
//        }
//
//        protected abstract T map(F f) throws ExecutionException, InterruptedException;
//    }
//
//    abstract static class FlatMappedFuture<F, T> extends MappedFuture<F, T> {
//        private Future<Bitmap> mappedFuture;
//
//        FlatMappedFuture(Future<F> wrapped) {
//            super(wrapped);
//        }
//
//        @Override
//        public boolean cancel(boolean mayInterruptIfRunning) {
//            if(mappedFuture!=null){
//                if(mappedFuture.cancel(mayInterruptIfRunning)){
//                    super.cancel(mayInterruptIfRunning);
//                    return true;
//                }
//            }
//            return super.cancel(mayInterruptIfRunning);
//        }
//
//
//        @Override
//        protected T map(F f) throws ExecutionException, InterruptedException {
//            mappedFuture = mapToFuture(f);
//            return mappedFuture.get();
//        }
//
//        protected abstract Future<Bitmap> mapToFuture(F f) throws ExecutionException, InterruptedException;
//    }

}