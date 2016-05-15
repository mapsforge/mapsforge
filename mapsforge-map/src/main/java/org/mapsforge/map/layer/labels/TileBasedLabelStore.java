/*
 * Copyright 2014 Ludwig M Brinckmann
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
import org.mapsforge.core.model.Tile;
import org.mapsforge.core.util.WorkingSetCache;
import org.mapsforge.map.util.LayerUtil;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A LabelStore where the data is stored per tile.
 */

public class TileBasedLabelStore extends WorkingSetCache<Tile, List<MapElementContainer>> implements LabelStore {
    private static final long serialVersionUID = 1L;

    private Set<Tile> lastVisibleTileSet;
    private int version;

    public TileBasedLabelStore(int capacity) {
        super(capacity);
        lastVisibleTileSet = new HashSet<Tile>();
    }

    public void destroy() {
        this.clear();
    }

    /**
     * Stores a list of MapElements against a tile.
     *
     * @param tile     tile on which the mapItems reside.
     * @param mapItems the map elements.
     */
    public synchronized void storeMapItems(Tile tile, List<MapElementContainer> mapItems) {
        this.put(tile, LayerUtil.collisionFreeOrdered(mapItems));
        this.version += 1;
    }

    @Override
    public int getVersion() {
        return this.version;
    }

    @Override
    public synchronized List<MapElementContainer> getVisibleItems(Tile upperLeft, Tile lowerRight) {
        return getVisibleItems(LayerUtil.getTiles(upperLeft, lowerRight));
    }

    private synchronized List<MapElementContainer> getVisibleItems(Set<Tile> tiles) {

        lastVisibleTileSet = tiles;

        List<MapElementContainer> visibleItems = new ArrayList<MapElementContainer>();
        for (Tile tile : lastVisibleTileSet) {
            if (containsKey(tile)) {
                visibleItems.addAll(get(tile));
            }
        }
        return visibleItems;
    }

    /**
     * Returns if a tile is in the current tile set and no data is stored for this tile.
     *
     * @param tile the tile
     * @return true if the tile is in the current tile set, but no data is stored for it.
     */
    public synchronized boolean requiresTile(Tile tile) {
        return this.lastVisibleTileSet.contains(tile) && !this.containsKey(tile);
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<Tile, List<MapElementContainer>> eldest) {
        if (size() > this.capacity) {
            return true;
        }
        return false;
    }

}
