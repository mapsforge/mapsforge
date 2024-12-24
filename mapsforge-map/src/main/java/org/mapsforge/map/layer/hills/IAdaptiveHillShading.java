/*
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
package org.mapsforge.map.layer.hills;

/**
 * Interface that specifies adaptive hill shading algorithms.
 */
public interface IAdaptiveHillShading {
    /**
     * @return Whether the high quality (bicubic) algorithm is enabled or not.
     */
    boolean isHqEnabled();

    /**
     * @return {@code true} if the algorithm decides which zoom levels are supported (default), {@code false} to obey values as set in the render theme.
     */
    boolean isAdaptiveZoomEnabled();

    /**
     * @param isEnabled {@code true} to let the algorithm decide which zoom levels are supported (default); {@code false} to obey values as set in the render theme.
     */
    IAdaptiveHillShading setAdaptiveZoomEnabled(boolean isEnabled);
}
