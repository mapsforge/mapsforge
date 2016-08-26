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
package org.mapsforge.core.graphics;

/**
 * Specifies the shape to be used for the endpoints of a line.
 */
public enum Join {
    BEVEL, MITER, ROUND;

    public static Join fromString(String value) {
        if ("bevel".equals(value)) {
            return BEVEL;
        } else if (("round").equals(value)) {
            return ROUND;
        } else if ("miter".equals(value)) {
            return MITER;
        }
        throw new IllegalArgumentException("Invalid value for Join: " + value);
    }

}
