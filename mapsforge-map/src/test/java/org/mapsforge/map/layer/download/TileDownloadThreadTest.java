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
package org.mapsforge.map.layer.download;

import java.io.File;
import java.io.IOException;
import java.lang.Thread.State;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;
import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.model.Tile;
import org.mapsforge.map.HttpServerTest;
import org.mapsforge.map.awt.AwtGraphicFactory;
import org.mapsforge.map.layer.DummyLayer;
import org.mapsforge.map.layer.Layer;
import org.mapsforge.map.layer.cache.InMemoryTileCache;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.layer.download.tilesource.OpenStreetMapMapnik;
import org.mapsforge.map.layer.download.tilesource.TileSource;
import org.mapsforge.map.layer.queue.JobQueue;
import org.mapsforge.map.model.MapViewPosition;

public class TileDownloadThreadTest extends HttpServerTest {
	private static final GraphicFactory GRAPHIC_FACTORY = AwtGraphicFactory.INSTANCE;
	private static final int WAIT_TIME_MILLIS = 3000;

	private static void awaitWaitingState(Thread thread) throws InterruptedException {
		long startTime = System.nanoTime();

		while (!Thread.currentThread().isInterrupted()) {
			State state = thread.getState();

			switch (state) {
				case NEW:
				case RUNNABLE:
				case BLOCKED:
					if (System.nanoTime() - startTime > TimeUnit.MILLISECONDS.toNanos(WAIT_TIME_MILLIS)) {
						Assert.fail("state: " + state);
					}
					Thread.sleep(50);
					break;

				case TERMINATED:
				case TIMED_WAITING:
					Assert.fail("state: " + state);
					break;

				case WAITING:
					return;
			}
		}
	}

	@Test
	public void cachedTileDownloadTest() throws InterruptedException {
		TileCache tileCache = new InMemoryTileCache(1);
		JobQueue<DownloadJob> jobQueue = new JobQueue<DownloadJob>(new MapViewPosition());
		Layer layer = new DummyLayer();

		TileDownloadThread tileDownloadThread = new TileDownloadThread(tileCache, jobQueue, layer, GRAPHIC_FACTORY);
		try {
			tileDownloadThread.start();
			awaitWaitingState(tileDownloadThread);

			Tile tile = new Tile(0, 0, (byte) 0);
			TileSource tileSource = new InvalidTileSource();
			DownloadJob downloadJob = new DownloadJob(tile, tileSource);
			Assert.assertFalse(tileCache.containsKey(downloadJob));

			tileCache.put(downloadJob, GRAPHIC_FACTORY.createBitmap(1, 1));
			Assert.assertTrue(tileCache.containsKey(downloadJob));

			jobQueue.add(downloadJob);
			jobQueue.notifyWorkers();

			awaitWaitingState(tileDownloadThread);
		} finally {
			tileDownloadThread.interrupt();
			tileDownloadThread.join(WAIT_TIME_MILLIS);
		}
	}

	@Test
	public void newTileDownloadTest() throws InterruptedException, IOException {
		addFile("/0/0/0.png", new File("src/test/resources/0_0_0.png"));

		TileCache tileCache = new InMemoryTileCache(1);
		JobQueue<DownloadJob> jobQueue = new JobQueue<DownloadJob>(new MapViewPosition());
		Layer layer = new DummyLayer();

		TileDownloadThread tileDownloadThread = new TileDownloadThread(tileCache, jobQueue, layer, GRAPHIC_FACTORY);
		try {
			tileDownloadThread.start();
			awaitWaitingState(tileDownloadThread);

			Tile tile = new Tile(0, 0, (byte) 0);
			TileSource tileSource = new OpenStreetMapMapnik("localhost", getPort());
			DownloadJob downloadJob = new DownloadJob(tile, tileSource);
			Assert.assertFalse(tileCache.containsKey(downloadJob));

			jobQueue.add(downloadJob);
			jobQueue.notifyWorkers();

			awaitWaitingState(tileDownloadThread);
			Assert.assertTrue(tileCache.containsKey(downloadJob));
			Assert.assertEquals(0, jobQueue.size());
		} finally {
			tileDownloadThread.interrupt();
			tileDownloadThread.join(WAIT_TIME_MILLIS);
		}
	}
}
