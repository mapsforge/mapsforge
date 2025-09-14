/*
 * Copyright 2014 Ludwig M Brinckmann
 * Copyright 2025 Sublimis
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
 * The enum Display governs whether map elements should be displayed and how.
 * The main choice is between {@link #IFSPACE} and {@link #ORDER}.
 * <p/>
 * <ul>
 * <li>{@link #IFSPACE} means an element is displayed if there is space for it (also depends on priority). Default.</li>
 * <li>{@link #ORDER} means an element will always be displayed at the expense of {@link #IFSPACE} elements.
 * If there is another {@link #ORDER} element that collides, a choice will be made (based on priority etc.)
 * as to which single element will be displayed to prevent overlap.</li>
 * <li>{@link #ALWAYS} is a convenience fallback, which means that an element will always be displayed
 * regardless of whether there is an overlap or not (so others can easily cover it).</li>
 * <li>{@link #NEVER} is also a convenience fallback, which means that an element will never be displayed.</li>
 * </ul>
 */

public enum Display {
    IFSPACE, ORDER, ALWAYS, NEVER;

    public static Display fromString(String value) {
        if ("ifspace".equals(value)) {
            return IFSPACE;
        } else if ("order".equals(value)) {
            return ORDER;
        } else if ("always".equals(value)) {
            return ALWAYS;
        } else if ("never".equals(value)) {
            return NEVER;
        }
        throw new IllegalArgumentException("Invalid value for Display: " + value);
    }
}
