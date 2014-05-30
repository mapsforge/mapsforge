/*
 * Copyright © 2014 Ludwig M Brinckmann
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

package org.mapsforge.map.layer.labels;

import org.mapsforge.core.mapelements.MapElementContainer;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.Point;
import org.mapsforge.core.model.Tile;
import org.mapsforge.core.util.WorkingSetCache;
import org.mapsforge.map.layer.Layer;
import org.mapsforge.map.util.LayerUtil;
import org.mapsforge.map.model.DisplayModel;
import org.mapsforge.map.util.PausableThread;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * A LabelStore where the data is stored per tile.
 * This store is suitable when the label data is retrieved per tile and needs to be cached
 * for the LabelLayer.
 */
public class TileBasedLabelStore extends WorkingSetCache<Tile, List<MapElementContainer>> implements LabelStore {

	/**
	 * The LayoutCalculator calculates a collision-free set of MapElements where
	 * higher priority elements are tested first. So, lower priority elements are only drawn
	 * if the space they require has not yet been taken up by a higher priority item.
	 *
	 * Essentially a job for the LayoutCalculator lies in a set of tiles that is visible. For this
	 * set the LayoutCalculator gets all the items on the tiles from the TileBaseLabelStore.
	 * This set is then sorted in order of priority and then each element is tested if it overlaps
	 * with an already drawn item. The list is then copied into the itemsToDraw list.
	 */
	private class LayoutCalculator extends PausableThread {
		Set<Tile> next;

		synchronized void put(Set<Tile> next) {
			this.next = next;
			notify();
		}

		protected void doWork() throws InterruptedException {

			Set<Tile> tiles;
			synchronized (this) {
				tiles = next;
				next = null;
			}

			// get the items on tiles through a copy operation
			List<MapElementContainer> itemsOnTiles = new LinkedList<MapElementContainer>();
			synchronized (TileBasedLabelStore.this) {
				// first get all the items on the visible tiles
				for (Tile tile : tiles) {
					List<MapElementContainer> items = TileBasedLabelStore.this.get(tile);
					if (items != null) {
						itemsOnTiles.addAll(items);
					}
				}
			}

			List<MapElementContainer> collisonFreeOrdered = LayerUtil.collisionFreeOrdered(itemsOnTiles);

			try {
				lock.writeLock().lock();
				Iterator<MapElementContainer> iter = itemsToDraw.iterator();
				while (iter.hasNext()) {
					iter.next().decrementRefCount();
					iter.remove();
				}
				itemsToDraw.addAll(collisonFreeOrdered);
			} finally {
				lock.writeLock().unlock();
			}

			TileBasedLabelStore.this.layer.requestRedraw();
		}

		protected ThreadPriority getThreadPriority() {
			return ThreadPriority.BELOW_NORMAL;
		}

		synchronized protected boolean hasWork() {
			return next != null;
		}
	}

	private final LayoutCalculator calculator;
	private final List<MapElementContainer> itemsToDraw;
	private final Set<Tile> lastVisibleTileSet;
	private Layer layer;
	private final ReentrantReadWriteLock lock;


	public TileBasedLabelStore(int capacity) {
		super(capacity);
		lastVisibleTileSet = new CopyOnWriteArraySet<Tile>();
		itemsToDraw = new ArrayList<MapElementContainer>();
		calculator = new LayoutCalculator();
		this.lock = new ReentrantReadWriteLock();
		calculator.start();
	}

	void setLayer(Layer layer) {
		this.layer = layer;
	}

	void startLayoutEngine() {
		calculator.proceed();
	}

	void stopLayoutEngine() {
		calculator.pause();
	}

	public void destroy() {
		calculator.interrupt();
		try {
			lock.writeLock().lock();
			// there is still a risk that one of the map workers just finishes
			// a job to store elements.
			for (List<MapElementContainer> tile : this.values()) {
				for (MapElementContainer element : tile) {
					element.decrementRefCount();
				}
			}
			this.clear();
		} finally {
			lock.writeLock().unlock();
		}
	}

	/**
	 * Stores a list of MapElements against a tile. If the lastVisibleTileSet contains
	 * this tile, we put the tileSet as a job to the LayoutCalculator as now there is
	 * new data for this currently visible tiles.
	 *
	 * @param tile tile on which the mapItems reside.
	 * @param mapItems the map elements.
	 */
	public void storeMapItems(Tile tile, List<MapElementContainer> mapItems) {
		try {
			lock.writeLock().lock();
			this.put(tile, mapItems);
		} finally {
			lock.writeLock().unlock();
		}

		try {
			lock.readLock().lock();
			if (lastVisibleTileSet.contains(tile)) {
				calculator.put(lastVisibleTileSet);
			}
		} finally {
			lock.readLock().unlock();
		}
	}

	/**
	 * Returns a list of the currently visible items in the given bounding box.
	 * @param boundingBox
	 * @param displayModel
	 * @param zoomLevel
	 * @param topLeftPoint
	 * @return
	 */
	@Override
	public List<MapElementContainer> getVisibleItems(BoundingBox boundingBox, DisplayModel displayModel, byte zoomLevel, Point topLeftPoint) {

		Set<Tile> newVisibileTileSet = LayerUtil.getTiles(boundingBox, zoomLevel, topLeftPoint, displayModel.getTileSize());

		try {
			lock.writeLock().lock();
			if (!newVisibileTileSet.equals(lastVisibleTileSet)) {
				lastVisibleTileSet.clear();
				lastVisibleTileSet.addAll(newVisibileTileSet);
				calculator.put(lastVisibleTileSet);
			}
		} finally {
			lock.writeLock().unlock();
		}

		List<MapElementContainer> currentItemsToDraw;
		try {
			lock.readLock().lock();
			currentItemsToDraw = new ArrayList<MapElementContainer>(itemsToDraw);
		} finally {
			lock.readLock().unlock();
		}
		return currentItemsToDraw;
	}

	/**
	 * Returns if a tile is in the current tile set and no data is stored for this tile.
	 * @param tile the tile
	 * @return
	 */
	public boolean requiresTile(Tile tile) {
		try {
			lock.readLock().lock();
			return this.lastVisibleTileSet.contains(tile) && !this.containsKey(tile);
		} finally {
			lock.readLock().unlock();
		}
	}

	@Override
	protected boolean removeEldestEntry(Map.Entry<Tile, List<MapElementContainer>> eldest) {
		if (size() > this.capacity) {
			List<MapElementContainer> list = eldest.getValue();
			for (MapElementContainer item : list) {
				item.decrementRefCount();
			}
			return true;
		}
		return false;
	}

}
