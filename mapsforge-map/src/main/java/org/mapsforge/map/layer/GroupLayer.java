/*
 * Copyright 2016 devemux86
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
package org.mapsforge.map.layer;

import org.mapsforge.core.graphics.Canvas;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Point;
import org.mapsforge.map.model.DisplayModel;

import java.util.ArrayList;
import java.util.List;

/**
 * A layer which is a group of other layers.
 */
public class GroupLayer extends Layer {

    /**
     * The group of other layers.
     */
    public final List<Layer> layers = new ArrayList<>();

    public GroupLayer() {
        super();
    }

    @Override
    public void draw(BoundingBox boundingBox, byte zoomLevel, Canvas canvas, Point topLeftPoint) {
        for (Layer layer : layers) {
            layer.draw(boundingBox, zoomLevel, canvas, topLeftPoint);
        }
    }

    @Override
    public void onDestroy() {
        for (Layer layer : layers) {
            layer.onDestroy();
        }
    }

    /**
     * GroupLayer does not have a position, layerXY is null.
     */
    @Override
    public boolean onLongPress(LatLong tapLatLong, Point layerXY, Point tapXY) {
        for (int i = layers.size() - 1; i >= 0; i--) {
            Layer layer = layers.get(i);
            if (layer.onLongPress(tapLatLong, layerXY, tapXY)) {
                return true;
            }
        }
        return false;
    }

    /**
     * GroupLayer does not have a position, layerXY is null.
     */
    @Override
    public boolean onTap(LatLong tapLatLong, Point layerXY, Point tapXY) {
        for (int i = layers.size() - 1; i >= 0; i--) {
            Layer layer = layers.get(i);
            if (layer.onTap(tapLatLong, layerXY, tapXY)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public synchronized void setDisplayModel(DisplayModel displayModel) {
        super.setDisplayModel(displayModel);
        for (Layer layer : layers) {
            layer.setDisplayModel(displayModel);
        }
    }
}
