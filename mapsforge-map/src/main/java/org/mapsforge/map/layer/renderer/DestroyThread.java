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

import org.mapsforge.map.reader.MapDataStore;
import org.mapsforge.map.reader.MapFile;

class DestroyThread extends Thread {

	private final MapDataStore mapDataStore;
	private final DatabaseRenderer renderer;
	private final Thread thread;

	DestroyThread(Thread thread, MapDataStore dataStore, DatabaseRenderer renderer) {
		super();

		this.thread = thread;
		this.mapDataStore = dataStore;
		this.renderer = renderer;
	}

	@Override
	public void run() {
		try {
			this.thread.interrupt();
			this.thread.join();
		} catch (InterruptedException e) {
			// restore the interrupted status
			interrupt();
		} finally {
			this.renderer.destroy();
			this.mapDataStore.close();
		}
	}
}
