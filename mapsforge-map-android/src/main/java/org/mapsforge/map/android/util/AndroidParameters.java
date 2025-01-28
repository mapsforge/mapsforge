/*
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
package org.mapsforge.map.android.util;

import android.os.Build;

public final class AndroidParameters {

    /**
     * To prevent {@code libhwui.so} "null pointer dereference" SIGSEGV crashes on some Androids.
     * The only drawback seems to be that line joins of a few paths may not be rounded.
     * Defaults to {@code true} for Android API levels 32 and below.
     */
    public static boolean ANDROID_LIBHWUI_FIX = Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2;
}
