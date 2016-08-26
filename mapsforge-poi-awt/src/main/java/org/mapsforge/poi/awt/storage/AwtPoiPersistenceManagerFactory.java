/*
 * Copyright 2015-2016 devemux86
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
package org.mapsforge.poi.awt.storage;

import org.mapsforge.poi.storage.PoiPersistenceManager;

/**
 * Factory providing methods for instantiating {@link PoiPersistenceManager} implementations.
 * This class is needed to differ between Android and AWT.
 */
public class AwtPoiPersistenceManagerFactory {
    /**
     * @param poiFilePath Path to a .poi file.
     * @return {@link PoiPersistenceManager} using an underlying SQLite database.
     */
    public static PoiPersistenceManager getPoiPersistenceManager(String poiFilePath) {
        return getPoiPersistenceManager(poiFilePath, true);
    }

    /**
     * @param poiFilePath Path to a .poi file.
     * @param readOnly    If the file does not exist it can be created and filled.
     * @return {@link PoiPersistenceManager} using an underlying SQLite database.
     */
    public static PoiPersistenceManager getPoiPersistenceManager(String poiFilePath,
                                                                 boolean readOnly) {
        return new AwtPoiPersistenceManager(poiFilePath, readOnly);
    }
}
