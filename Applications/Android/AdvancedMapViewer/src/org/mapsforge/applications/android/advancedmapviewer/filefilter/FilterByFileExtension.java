/*
 * Copyright 2010, 2011, 2012 mapsforge.org
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
package org.mapsforge.applications.android.advancedmapviewer.filefilter;

import java.io.File;
import java.io.FileFilter;

/**
 * Accepts all readable directories and all readable files with a given extension.
 */
public class FilterByFileExtension implements FileFilter {
	private final String extension;

	/**
	 * @param extension
	 *            the allowed file name extension.
	 */
	public FilterByFileExtension(String extension) {
		this.extension = extension;
	}

	@Override
	public boolean accept(File file) {
		// accept only readable files
		if (file.canRead()) {
			if (file.isDirectory()) {
				// accept all directories
				return true;
			} else if (file.isFile() && file.getName().endsWith(this.extension)) {
				return true;
			}
		}
		return false;
	}
}
