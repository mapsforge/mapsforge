/*
 * Copyright 2022 usrusr
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

import java.util.logging.Logger;

/**
 * should implement equals
 */
public interface DemFolder {
    Logger LOGGER = Logger.getLogger(DemFolder.class.getName());

    Iterable<DemFolder> subs();

    Iterable<DemFile> files();
}
