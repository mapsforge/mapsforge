/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
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

import org.mapsforge.core.graphics.Bitmap;
import org.mapsforge.map.layer.Layer;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.layer.queue.JobQueue;
import org.mapsforge.map.util.PausableThread;

public class MapWorker extends PausableThread {
	private final DatabaseRenderer databaseRenderer;
	private final JobQueue<RendererJob> jobQueue;
	private final Layer layer;
	private final TileCache tileCache;

	public MapWorker(TileCache tileCache, JobQueue<RendererJob> jobQueue, DatabaseRenderer databaseRenderer, Layer layer) {
		super();

		this.tileCache = tileCache;
		this.jobQueue = jobQueue;
		this.databaseRenderer = databaseRenderer;
		this.layer = layer;
	}

	@Override
	protected void doWork() throws InterruptedException {
		RendererJob rendererJob = this.jobQueue.get();

		try {
			if (!this.tileCache.containsKey(rendererJob)) {
				renderTile(rendererJob);
			}
		} finally {
			this.jobQueue.remove(rendererJob);
		}
	}

	@Override
	protected ThreadPriority getThreadPriority() {
		return ThreadPriority.BELOW_NORMAL;
	}

	@Override
	protected boolean hasWork() {
		return true;
	}

	private void renderTile(RendererJob rendererJob) {
		Bitmap bitmap = this.databaseRenderer.executeJob(rendererJob);

		if (!isInterrupted() && bitmap != null) {
			this.tileCache.put(rendererJob, bitmap);
			this.layer.requestRedraw();
		}
	}
}
