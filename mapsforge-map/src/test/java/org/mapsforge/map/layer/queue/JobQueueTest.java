/*
 * Copyright 2010, 2011, 2012 mapsforge.org
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
package org.mapsforge.map.layer.queue;

import org.junit.Assert;
import org.junit.Test;
import org.mapsforge.core.model.Tile;
import org.mapsforge.map.model.MapViewPosition;

public class JobQueueTest {
	@Test
	public void jobQueueTest() throws InterruptedException {
		MapViewPosition mapViewPosition = new MapViewPosition();
		JobQueue<Job> jobQueue = new JobQueue<Job>(mapViewPosition);
		Assert.assertEquals(0, jobQueue.size());

		Tile tile1 = new Tile(0, 0, (byte) 1);
		Tile tile2 = new Tile(0, 0, (byte) 0);
		Tile tile3 = new Tile(0, 0, (byte) 2);

		Job job1 = new Job(tile1);
		Job job2 = new Job(tile2);
		Job job3 = new Job(tile3);
		jobQueue.add(job1);
		jobQueue.add(job2);
		jobQueue.add(job3);
		Assert.assertEquals(3, jobQueue.size());

		jobQueue.add(job1);
		Assert.assertEquals(3, jobQueue.size());

		jobQueue.notifyWorkers();

		Assert.assertEquals(job2, jobQueue.remove());
		Assert.assertEquals(job1, jobQueue.remove());
		Assert.assertEquals(job3, jobQueue.remove());

		Assert.assertEquals(0, jobQueue.size());
	}
}
