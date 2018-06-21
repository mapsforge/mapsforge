/*
 * Copyright 2012 Hannes Janetzek
 * Copyright 2016 Andrey Novikov
 * Copyright 2017-2018 Gustl22
 * Copyright 2018 devemux86
 *
 * This file is part of the OpenScienceMap project (http://www.opensciencemap.org).
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
package com.ikroshlab.ovtencoder;

import org.oscim.core.PointF;
import org.oscim.core.Tag;
import org.oscim.core.TagSet;

/**
 * The MapElement class is a reusable container for a geometry with tags.
 * MapElement is created by TileDataSource(s) and passed to MapTileLoader via ITileDataSink.process().
 * This is just a buffer that belongs to TileDataSource, so don't keep a reference to it when passed as parameter.
 */
public class MapElement extends com.ikroshlab.ovtencoder.GeometryBuffer {

    public       PointF   labelPosition;
    public       int      layer;                              // layer of the element (0-10) overrides the theme drawing order
    public final TagSet   tags           = new TagSet();



    public MapElement() {
        super(4, 4);
    }

    public MapElement(int numPoints, int numIndices) {
        super(numPoints, numIndices);
    }

    public MapElement(float[] points, int[] index) {
        super(points, index);
    }

    /**
     * @param element the map element to copy
     */
    public MapElement(MapElement element) {
        super(element);
        this.tags.set(element.tags.asArray());
        this.labelPosition = element.labelPosition;
        this.setLayer(element.layer);
    }

    /**
     * @return height in meters, if present
     */
    public Float getHeight() {
        String v = tags.getValue(Tag.KEY_HEIGHT);
        if (v == null)  v = tags.getValue("render_height"); // OpenMapTiles
        if (v != null)  return Float.parseFloat(v);
        return null;
    }

    /**
     * @return minimum height in meters, if present
     */
    public Float getMinHeight() {
        String v = tags.getValue(Tag.KEY_MIN_HEIGHT);
        if (v == null) v = tags.getValue("render_min_height"); // OpenMapTiles
        if (v != null) return Float.parseFloat(v);
        return null;
    }

    public boolean isBuilding() {
        return tags.containsKey(Tag.KEY_BUILDING)
                || "building".equals(tags.getValue("kind")) // Mapzen
                || "building".equals(tags.getValue("layer")); // OpenMapTiles
    }

    public boolean isBuildingPart() {
        return tags.containsKey(Tag.KEY_BUILDING_PART)
                || "building_part".equals(tags.getValue("kind")) // Mapzen
                || "building:part".equals(tags.getValue("layer")); // OpenMapTiles
    }

    public void setLabelPosition(float x, float y) {
        labelPosition = new PointF(x, y);
    }

    public void setLayer(int layer) {
        this.layer = layer;
    }

    @Override
    public MapElement clear() {
        layer = 5;
        super.clear();
        return this;
    }

    @Override
    public String toString() {
        return tags.toString() + '\n' + super.toString() + '\n';
    }
}
