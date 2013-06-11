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

import java.io.InputStream;

/**
 * Enumeration of all internal rendering themes.
 */
public enum InternalRenderTheme implements XmlRenderTheme {
	/**
	 * A render-theme similar to the OpenStreetMap Osmarender style.
	 * 
	 * @see <a href="http://wiki.openstreetmap.org/wiki/Osmarender">Osmarender</a>
	 */
	OSMARENDER("/osmarender/", "osmarender.xml");

	private final String absolutePath;
	private final String file;

	private InternalRenderTheme(String absolutePath, String file) {
		this.absolutePath = absolutePath;
		this.file = file;
	}

	@Override
	public String getRelativePathPrefix() {
		return this.absolutePath;
	}

	@Override
	public InputStream getRenderThemeAsStream() {
		return Thread.currentThread().getClass().getResourceAsStream(this.absolutePath + this.file);
	}
}
