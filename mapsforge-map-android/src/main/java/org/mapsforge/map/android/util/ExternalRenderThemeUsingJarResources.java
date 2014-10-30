/*
 * Copyright 2013-2014 Ludwig M Brinckmann
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

import java.io.File;
import java.io.FileNotFoundException;

import org.mapsforge.map.rendertheme.ExternalRenderTheme;

/**
 * Combining an external render theme file with the jar-embedded resources of
 * the internal osmarender style.
 */

public class ExternalRenderThemeUsingJarResources extends ExternalRenderTheme {

	public ExternalRenderThemeUsingJarResources(File renderThemeFile)
			throws FileNotFoundException {
		super(renderThemeFile);
	}

	@Override
	public String getRelativePathPrefix() {
		return "/osmarender/";
	}

}
