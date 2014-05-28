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
import org.mapsforge.core.util.LRUCache;
import org.mapsforge.map.layer.Layer;
import org.mapsforge.map.util.LayerUtil;
import org.mapsforge.map.model.DisplayModel;
import org.mapsforge.map.util.PausableThread;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.logging.Logger;

/**
 * A LabelStore where the data is stored per tile.
 * This store is suitable when the label data is retrieved per tile and needs to be cached
 * for the LabelLayer.
 */
public class TileBasedLabelStore extends LRUCache<Tile, List<MapElementContainer>> implements LabelStore {

	private static final Logger LOGGER = Logger.getLogger(TileBasedLabelStore.class.getName());

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
			// sort items by priority (highest first)
			Collections.sort(itemsOnTiles, Collections.reverseOrder());

			// in order of priority, see if an item can be drawn, i.e. none of the items
			// in the currentItemsToDraw list overlaps with it.
			List<MapElementContainer> currentItemsToDraw = new LinkedList<MapElementContainer>();
			for (MapElementContainer item : itemsOnTiles) {
				boolean hasSpace = true;
				for (MapElementContainer itemToDraw : currentItemsToDraw) {
					if (itemToDraw.getBoundaryAbsolute().intersects(item.getBoundaryAbsolute())) {
						hasSpace = false;
						break;
					}
				}
				if (hasSpace) {
					item.incrementRefCount();
					currentItemsToDraw.add(item);
				}
			}

			synchronized (itemsToDraw) {
				Iterator<MapElementContainer> iter = itemsToDraw.iterator();
				while (iter.hasNext()) {
					iter.next().decrementRefCount();
					iter.remove();
				}
				itemsToDraw.addAll(currentItemsToDraw);
			}

			TileBasedLabelStore.this.layer.requestRedraw();
		}

		@Override
		protected void afterRun() {
			LOGGER.info("Calculator exiting");
		}

		protected ThreadPriority getThreadPriority() {
			return ThreadPriority.NORMAL;
		}

		synchronized protected boolean hasWork() {
			return next != null;
		}
	}

	private final LayoutCalculator calculator;
	private final List<MapElementContainer> itemsToDraw;
	private final Set<Tile> lastVisibleTileSet;
	private Layer layer;


	public TileBasedLabelStore(int capacity) {
		super(capacity);
		lastVisibleTileSet = new CopyOnWriteArraySet<Tile>();
		itemsToDraw = new ArrayList<MapElementContainer>();
		calculator = new LayoutCalculator();
		calculator.start();
	}

	void setLayer(Layer layer) {
		this.layer = layer;
	}

	void startLayoutEngine() {
		LOGGER.info("Calculator proceed");
		calculator.proceed();
	}

	void stopLayoutEngine() {
		LOGGER.info("Calculator pause");
		calculator.pause();
	}

	public void destroy() {
		LOGGER.info("Calculator interrupt");
		calculator.interrupt();
		synchronized (this) {
			// there is still a risk that one of the map workers just finishes
			// a job to store elements.
			for (List<MapElementContainer> tile : this.values()) {
				for (MapElementContainer element : tile) {
					element.decrementRefCount();
				}
			}
			this.clear();
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
		synchronized (this) {
			this.put(tile, mapItems);
		}

		if (lastVisibleTileSet.contains(tile)) {
			calculator.put(lastVisibleTileSet);
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

		if (!newVisibileTileSet.equals(lastVisibleTileSet)) {
			lastVisibleTileSet.clear();
			lastVisibleTileSet.addAll(newVisibileTileSet);
			calculator.put(lastVisibleTileSet);
		}

		List<MapElementContainer> currentItemsToDraw;
		synchronized (itemsToDraw) {
			currentItemsToDraw = new ArrayList<MapElementContainer>(itemsToDraw);
		}
		return currentItemsToDraw;
	}

	/**
	 * Returns if a tile is in the current tile set and no data is stored for this tile.
	 * @param tile the tile
	 * @return
	 */
	synchronized public boolean requiresTile(Tile tile) {
		return this.lastVisibleTileSet.contains(tile) && !this.containsKey(tile);
	}

	@Override
	protected boolean removeEldestEntry(Map.Entry<Tile, List<MapElementContainer>> eldest) {
		if (size() > this.capacity) {
			remove(eldest.getKey());
			List<MapElementContainer> list= eldest.getValue();
			for (MapElementContainer item : list) {
				item.decrementRefCount();
			}
			return true;
		}
		return false;
	}

}
