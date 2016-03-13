/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright 2015-2016 devemux86
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
package org.mapsforge.map.view;

import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.Dimension;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.map.layer.Layer;
import org.mapsforge.map.layer.LayerManager;
import org.mapsforge.map.model.Model;
import org.mapsforge.map.scalebar.MapScaleBar;
import org.mapsforge.map.util.MapViewProjection;

public interface MapView {

    void addLayer(Layer layer);

    /**
     * Clear map view.
     */
    void destroy();

    /**
     * Clear all map view elements.<br/>
     * i.e. layers, tile cache, label store, map view, resources, etc.
     */
    void destroyAll();

    BoundingBox getBoundingBox();

    Dimension getDimension();

    FpsCounter getFpsCounter();

    /**
     * @return the FrameBuffer used in this MapView.
     */
    FrameBuffer getFrameBuffer();

    int getHeight();

    LayerManager getLayerManager();

    MapScaleBar getMapScaleBar();

    MapViewProjection getMapViewProjection();

    Model getModel();

    int getWidth();

    /**
     * Requests a redrawing as soon as possible.
     */
    void repaint();

    void setCenter(LatLong center);

    void setMapScaleBar(MapScaleBar mapScaleBar);

    void setZoomLevel(byte zoomLevel);

    void setZoomLevelMax(byte zoomLevelMax);

    void setZoomLevelMin(byte zoomLevelMin);
}
