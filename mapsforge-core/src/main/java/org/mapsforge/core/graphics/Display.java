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
 * The enum Display governs whether map elements should be displayed.
 * <p/>
 * The main choice is
 * between IFSPACE which means an element is displayed if there is space for it (also depends on
 * priority), while ALWAYS means that an element will always be displayed (so it will be overlapped by
 * others and will not be part of the element placing algorithm). NEVER is a convenience fallback, which
 * means that an element will never be displayed.
 */

public enum Display {
    NEVER, ALWAYS, IFSPACE;

    public static Display fromString(String value) {
        if ("never".equals(value)) {
            return NEVER;
        } else if (("always").equals(value)) {
            return ALWAYS;
        } else if (("ifspace").equals(value)) {
            return IFSPACE;
        }
        throw new IllegalArgumentException("Invalid value for Display: " + value);
    }

}
