/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright 2014 Ludwig M Brinckmann
 * Copyright 2015-2016 devemux86
 * Copyright 2016 ksaihtam
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
package org.mapsforge.map.layer.renderer;

import org.mapsforge.core.graphics.TileBitmap;
import org.mapsforge.map.layer.Layer;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.layer.queue.JobQueue;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MapWorkerPool implements Runnable {
    private static final Logger LOGGER = Logger.getLogger(MapWorkerPool.class.getName());

    // The default number of threads is one greater than the number of processors as one thread is
    // likely to be blocked on I/O reading map data. Technically this value can change, so a better
    // implementation, maybe one that also takes the available memory into account, would be good.
    // For stability reasons (see #591), we set default number of threads to 1.
    public static final int DEFAULT_NUMBER_OF_THREADS = 1;//Runtime.getRuntime().availableProcessors() + 1;
    public static int NUMBER_OF_THREADS = DEFAULT_NUMBER_OF_THREADS;

    public static boolean DEBUG_TIMING = false;

    private final AtomicInteger concurrentJobs = new AtomicInteger();
    private final AtomicLong totalExecutions = new AtomicLong();
    private final AtomicLong totalTime = new AtomicLong();

    private final DatabaseRenderer databaseRenderer;
    private boolean inShutdown, isRunning;
    private final JobQueue<RendererJob> jobQueue;
    private final Layer layer;
    private ExecutorService self, workers;
    private final TileCache tileCache;

    public MapWorkerPool(TileCache tileCache, JobQueue<RendererJob> jobQueue, DatabaseRenderer databaseRenderer, Layer layer) {
        super();
        this.tileCache = tileCache;
        this.jobQueue = jobQueue;
        this.databaseRenderer = databaseRenderer;
        this.layer = layer;
        this.inShutdown = false;
        this.isRunning = false;
    }

    @Override
    public void run() {
        try {
            while (!inShutdown) {
                RendererJob rendererJob = this.jobQueue.get(NUMBER_OF_THREADS);
                if (rendererJob == null) {
                    continue;
                }
                if (!this.tileCache.containsKey(rendererJob) || rendererJob.labelsOnly) {
                    workers.execute(new MapWorker(rendererJob));
                } else {
                    jobQueue.remove(rendererJob);
                }
            }
        } catch (InterruptedException e) {
            LOGGER.log(Level.SEVERE, "MapWorkerPool interrupted", e);
        } catch (RejectedExecutionException e) {
            LOGGER.log(Level.SEVERE, "MapWorkerPool rejected", e);
        }
    }

    public synchronized void start() {
        if (this.isRunning) {
            return;
        }
        this.inShutdown = false;
        this.self = Executors.newSingleThreadExecutor();
        this.workers = Executors.newFixedThreadPool(NUMBER_OF_THREADS);
        this.self.execute(this);
        this.isRunning = true;
    }

    public synchronized void stop() {
        if (!this.isRunning) {
            return;
        }
        this.inShutdown = true;
        this.jobQueue.interrupt();

        // Shutdown executors
        this.self.shutdown();
        this.workers.shutdown();

        try {
            if (!this.self.awaitTermination(100, TimeUnit.MILLISECONDS)) {
                this.self.shutdownNow();
                if (!this.self.awaitTermination(100, TimeUnit.MILLISECONDS)) {
                    LOGGER.warning("Shutdown self executor failed");
                }
            }
        } catch (InterruptedException e) {
            LOGGER.log(Level.SEVERE, "Shutdown self executor interrupted", e);
        }

        try {
            if (!this.workers.awaitTermination(100, TimeUnit.MILLISECONDS)) {
                this.workers.shutdownNow();
                if (!this.workers.awaitTermination(100, TimeUnit.MILLISECONDS)) {
                    LOGGER.warning("Shutdown workers executor failed");
                }
            }
        } catch (InterruptedException e) {
            LOGGER.log(Level.SEVERE, "Shutdown workers executor interrupted", e);
        }

        this.isRunning = false;
    }

    class MapWorker implements Runnable {
        private final RendererJob rendererJob;

        MapWorker(RendererJob rendererJob) {
            this.rendererJob = rendererJob;
            this.rendererJob.renderThemeFuture.incrementRefCount();
        }

        @Override
        public void run() {
            TileBitmap bitmap = null;
            try {
                long start = 0;
                if (inShutdown) {
                    return;
                }
                if (DEBUG_TIMING) {
                    start = System.currentTimeMillis();
                    LOGGER.info("ConcurrentJobs " + concurrentJobs.incrementAndGet());
                }
                bitmap = MapWorkerPool.this.databaseRenderer.executeJob(rendererJob);
                if (inShutdown) {
                    return;
                }

                if (!rendererJob.labelsOnly && bitmap != null) {
                    MapWorkerPool.this.tileCache.put(rendererJob, bitmap);
                    MapWorkerPool.this.databaseRenderer.removeTileInProgress(rendererJob.tile);
                }
                MapWorkerPool.this.layer.requestRedraw();

                if (DEBUG_TIMING) {
                    long end = System.currentTimeMillis();
                    long te = totalExecutions.incrementAndGet();
                    long tt = totalTime.addAndGet(end - start);
                    if (te % 10 == 0) {
                        LOGGER.info("TIMING " + Long.toString(te) + " " + Double.toString(tt / te));
                    }
                    concurrentJobs.decrementAndGet();
                }
            } finally {
                this.rendererJob.renderThemeFuture.decrementRefCount();
                jobQueue.remove(rendererJob);
                if (bitmap != null) {
                    bitmap.decrementRefCount();
                }
            }
        }
    }
}
