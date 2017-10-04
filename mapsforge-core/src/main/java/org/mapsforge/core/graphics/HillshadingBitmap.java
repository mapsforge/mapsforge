/*
 * Copyright 2017 usrusr
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
package org.mapsforge.core.graphics;

import org.mapsforge.core.model.BoundingBox;

public interface HillshadingBitmap extends Bitmap {
    enum Border {
        WEST(true), NORTH(false), EAST(true), SOUTH(false);

        public final boolean vertical;

        Border(boolean vertical) {
            this.vertical = vertical;
        }
    }

    /**
     * Return geo bounds of the area within the padding.
     */
    BoundingBox getAreaRect();

    /**
     * Optional padding (lies outside of areaRect).
     */
    int getPadding();
}
