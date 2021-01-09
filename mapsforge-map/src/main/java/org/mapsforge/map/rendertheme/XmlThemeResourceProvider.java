/*
 * Copyright 2021 eddiemuc
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
package org.mapsforge.map.rendertheme;

import java.io.IOException;
import java.io.InputStream;

/**
 * Interface for a provider of resources referenced inside XML themes.
 */
public interface XmlThemeResourceProvider {

    /**
     * @param relativePath a relative path to use as a base for search in the resource provuider
     * @param source       a source string parsed out of an XML render theme "src" attribute.
     * @return an InputStream to read the resource data from.
     * @throws IOException if the resource cannot be found or an access error occurred.
     */
    InputStream createInputStream(String relativePath, String source) throws IOException;
}
