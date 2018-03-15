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
package org.mapsforge.map.layer.tilestore;

import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.graphics.TileBitmap;
import org.mapsforge.core.model.Tile;
import org.mapsforge.map.layer.TileLayer;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.layer.queue.Job;
import org.mapsforge.map.model.IMapViewPosition;


public class TileStoreLayer extends TileLayer<Job> {

    public TileStoreLayer(TileCache tileCache, IMapViewPosition mapViewPosition, GraphicFactory graphicFactory, boolean isTransparent) {
        super(tileCache, mapViewPosition, graphicFactory.createMatrix(), isTransparent, false);
    }

    @Override
    protected Job createJob(Tile tile) {
        return new Job(tile, isTransparent);
    }

    /**
     * Whether the tile is stale and should be refreshed.
     * <p/>
     * This method is not needed for a TileStoreLayer and will always return {@code false}. Both arguments can be null.
     *
     * @param tile   A tile.
     * @param bitmap The bitmap for {@code tile} currently held in the layer's cache.
     */
    @Override
    protected boolean isTileStale(Tile tile, TileBitmap bitmap) {
        return false;
    }
}
