/*
 * Copyright 2024 devemux86
 * Copyright 2024 Sublimis
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
package org.mapsforge.core.util;

public final class Constants {

    public static final int HILLSHADING_COLOR_DEFAULT = 0xff000000;
    public static final int HILLSHADING_MAGNITUDE_DEFAULT = 128;

    public static final long MAX_MEMORY_MB = Runtime.getRuntime().maxMemory() / 1000 / 1000;

    private Constants() {
        throw new IllegalStateException();
    }
}
