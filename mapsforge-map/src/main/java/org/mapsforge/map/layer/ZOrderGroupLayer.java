/*
 * Copyright 2024 eddiemuc
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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.mapsforge.core.graphics.Canvas;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Point;
import org.mapsforge.core.model.Rotation;
import org.mapsforge.map.model.DisplayModel;

/**
 * A <href a="https://en.wikipedia.org/wiki/Z-order">Z-order-aware</href> {@link Layer} which is a group of other layers.
 * <br>
 * Each contained layer is assigned a Z-Order which determines the order in which the layers are drawn on top
 * of each other. Higher Z-Order values are drawn on top of lower Z-Order values. The order of drawing
 * layers with same Z-Order value is undefined.
 * <br>
 * Each layer can only be added once to this group layer. If a layer is re-added with a different Z-order value
 * then the Z-Order of this layer is changed.
 * <br>
 * Adding null layers is silently ignored.
 * <br>
 * This class is thread-safe.
 */
public class ZOrderGroupLayer extends Layer {

    //SORTED map of Z-Orders -> used to draw objects in right z-order
    private final TreeMap<Integer, Set<Layer>> layerMap = new TreeMap<>();
    private final Map<Layer, Integer> zOrderMap = new HashMap<>();

    /**
     * add a layer to this group with a given zOrder.
     * <br>
     * If the layer is already part of this group then its zOrder is changed.
     **/
    public synchronized void put(final Layer layer, final int zOrder, final boolean redraw) {
        if (layer == null) {
            return;
        }
        //see if layer is currently part of this group layer
        final Integer currentZOrder = this.zOrderMap.get(layer);
        if (currentZOrder != null) {
            if (zOrder == currentZOrder) {
                return;
            }
            remove(layer, false);
        }
        //assign to new zOrderSet
        Set<Layer> zLevelSet = this.layerMap.get(zOrder);
        if (zLevelSet == null) {
            zLevelSet = new HashSet<>();
            this.layerMap.put(zOrder, zLevelSet);
        }
        zLevelSet.add(layer);
        this.zOrderMap.put(layer, zOrder);
        if (redraw) {
            requestRedraw();
        }
    }

    /** removes a layer from this group. If it is not part of the group, nothing happens */
    public synchronized void remove(final Layer layer, final boolean redraw) {
        if (layer == null) {
            return;
        }
        final Integer currentZOrder = this.zOrderMap.remove(layer);
        if (currentZOrder == null) {
            return;
        }
        final Set<Layer> zOrderSet = this.layerMap.get(currentZOrder);
        zOrderSet.remove(layer);
        if (zOrderSet.isEmpty()) {
            this.layerMap.remove(currentZOrder);
        }
        if (redraw) {
            requestRedraw();
        }
    }

    @Override
    public synchronized void draw(final BoundingBox boundingBox, final byte zoomLevel, final Canvas canvas, final Point topLeftPoint, final Rotation rotation) {
        for (Set<Layer> zOrderSet : this.layerMap.values()) {
            for (Layer layer : zOrderSet) {
                layer.draw(boundingBox, zoomLevel, canvas, topLeftPoint, rotation);
            }
        }
    }

    @Override
    public synchronized void onDestroy() {
        for (Layer layer : this.zOrderMap.keySet()) {
            layer.onDestroy();
        }
    }

    @Override
    public synchronized boolean onLongPress(LatLong tapLatLong, Point layerXY, Point tapXY) {
        for (Set<Layer> zOrderSet : this.layerMap.descendingMap().values()) {
            for (Layer layer : zOrderSet) {
                if (layer.onLongPress(tapLatLong, layerXY, tapXY)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public synchronized boolean onTap(LatLong tapLatLong, Point layerXY, Point tapXY) {
        for (Set<Layer> zOrderSet : this.layerMap.descendingMap().values()) {
            for (Layer layer : zOrderSet) {
                if (layer.onTap(tapLatLong, layerXY, tapXY)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public synchronized void setDisplayModel(DisplayModel displayModel) {
        super.setDisplayModel(displayModel);
        for (Layer layer : this.zOrderMap.keySet()) {
            layer.setDisplayModel(displayModel);
        }
    }
}
