/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
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

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.Serializable;

/**
 * Interface for a render theme which is defined in XML.
 */
public interface XmlRenderTheme extends Serializable {
	/**
	 * @return the prefix for all relative resource paths.
	 */
	String getRelativePathPrefix();

	/**
	 * @return an InputStream to read the render theme data from.
	 * @throws FileNotFoundException
	 *             if the render theme file cannot be found.
	 */
	InputStream getRenderThemeAsStream() throws FileNotFoundException;
}
