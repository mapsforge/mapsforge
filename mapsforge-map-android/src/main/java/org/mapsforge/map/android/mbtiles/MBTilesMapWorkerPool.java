/*
 * Copyright 2025 cpesch
 * Copyright 2025 moving-bits
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
package org.mapsforge.map.android.mbtiles;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.mapsforge.core.graphics.TileBitmap;
import org.mapsforge.core.util.Parameters;
import org.mapsforge.map.layer.Layer;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.layer.queue.JobQueue;

class MBTilesMapWorkerPool implements Runnable {
    private static final Logger LOGGER = Logger.getLogger(MBTilesMapWorkerPool.class.getName());

    private final MBTilesRenderer renderer;
    private boolean inShutdown, isRunning;
    private final JobQueue<MBTilesRendererJob> jobQueue;
    private final Layer layer;
    private ExecutorService self, workers;
    private final TileCache tileCache;

    MBTilesMapWorkerPool(final TileCache tileCache, final JobQueue<MBTilesRendererJob> jobQueue, final MBTilesRenderer renderer, final TileMBTilesLayer layer) {
        super();
        this.tileCache = tileCache;
        this.jobQueue = jobQueue;
        this.renderer = renderer;
        this.layer = layer;
        this.inShutdown = false;
        this.isRunning = false;
    }

    public void run() {
        try {
            while (!inShutdown) {
                final MBTilesRendererJob rendererJob = this.jobQueue.get(Parameters.NUMBER_OF_THREADS);
                if (rendererJob == null) {
                    continue;
                }
                if (!this.tileCache.containsKey(rendererJob)) {
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
        this.workers = Executors.newFixedThreadPool(Parameters.NUMBER_OF_THREADS);
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
                    LOGGER.fine("Shutdown self executor failed");
                }
            }
        } catch (InterruptedException e) {
            LOGGER.log(Level.SEVERE, "Shutdown self executor interrupted", e);
        }

        try {
            if (!this.workers.awaitTermination(100, TimeUnit.MILLISECONDS)) {
                this.workers.shutdownNow();
                if (!this.workers.awaitTermination(100, TimeUnit.MILLISECONDS)) {
                    LOGGER.fine("Shutdown workers executor failed");
                }
            }
        } catch (InterruptedException e) {
            LOGGER.log(Level.SEVERE, "Shutdown workers executor interrupted", e);
        }

        this.isRunning = false;
    }

    class MapWorker implements Runnable {
        private final MBTilesRendererJob rendererJob;

        MapWorker(final MBTilesRendererJob rendererJob) {
            this.rendererJob = rendererJob;
        }

        public void run() {
            TileBitmap bitmap = null;
            try {
                if (inShutdown) {
                    return;
                }
                bitmap = renderer.executeJob(rendererJob);
                if (inShutdown) {
                    return;
                }

                if (bitmap != null) {
                    tileCache.put(rendererJob, bitmap);
                }
                layer.requestRedraw();
            } finally {
                jobQueue.remove(rendererJob);
                if (bitmap != null) {
                    bitmap.decrementRefCount();
                }
            }
        }
    }
}
