/*
 * Copyright 2014 Ludwig M Brinckmann
 * Copyright 2014 devemux86
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

public enum Position {
    AUTO, CENTER, BELOW, BELOW_LEFT, BELOW_RIGHT, ABOVE, ABOVE_LEFT, ABOVE_RIGHT, LEFT, RIGHT;

    public static Position fromString(String value) {
        if ("auto".equals(value)) {
            // deliberately returning BELOW for auto, by default
            // we are implementing auto positioning as below
            return BELOW;
        } else if (("center").equals(value)) {
            return CENTER;
        } else if (("below").equals(value)) {
            return BELOW;
        } else if (("below_left").equals(value)) {
            return BELOW_LEFT;
        } else if (("below_right").equals(value)) {
            return BELOW_RIGHT;
        } else if ("above".equals(value)) {
            return ABOVE;
        } else if ("above_left".equals(value)) {
            return ABOVE_LEFT;
        } else if ("above_right".equals(value)) {
            return ABOVE_RIGHT;
        } else if ("left".equals(value)) {
            return LEFT;
        } else if ("right".equals(value)) {
            return RIGHT;
        }
        throw new IllegalArgumentException("Invalid value for Position: " + value);
    }

}
