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

import org.mapsforge.core.graphics.Bitmap;

public class HillshadingContainer implements ShapeContainer {

    public final float magnitude;
    public final Bitmap bitmap;
    public final double topLeftX;
    public final double topLeftY;
    public final double botRightX;
    public final double botRightY;

    public HillshadingContainer(Bitmap bitmap, float magnitude, double topLeftX, double topLeftY, double botRightX, double botRightY) {
        this.magnitude = magnitude;
        this.bitmap = bitmap;
        this.topLeftX = topLeftX;
        this.topLeftY = topLeftY;
        this.botRightX = botRightX;
        this.botRightY = botRightY;
    }


    @Override
    public ShapeType getShapeType() {
        return ShapeType.HILLSHADING;
    }

    @Override
    public String toString() {
        return "HillshadingContainer{" +
                  "topLeftX=" + topLeftX +
                ", topLeftY=" + topLeftY +
                ", botRightX=" + botRightX +
                ", botRightY=" + botRightY +
                '}';
    }
}
