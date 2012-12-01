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
package org.mapsforge.applications.android.advancedmapviewer;

import java.text.DecimalFormat;

import android.content.res.Resources;

final class FileUtils {
	private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#.00 ");
	private static final double ONE_GIGABYTE = 1000000000;
	private static final double ONE_KILOBYTE = 1000;
	private static final double ONE_MEGABYTE = 1000000;

	/**
	 * Formats the given file size as a human readable string, using SI prefixes.
	 * 
	 * @param fileSize
	 *            the file size to be formatted.
	 * @param resources
	 *            a reference to the application resources.
	 * @return a human readable file size.
	 * @throws IllegalArgumentException
	 *             if the given file size is negative.
	 */
	static String formatFileSize(long fileSize, Resources resources) {
		if (fileSize < 0) {
			throw new IllegalArgumentException("invalid file size: " + fileSize);
		} else if (fileSize < 1000) {
			if (fileSize == 1) {
				// singular
				return "1 " + resources.getString(R.string.file_size_byte);
			}

			// plural, including zero
			return fileSize + " " + resources.getString(R.string.file_size_bytes);
		} else {
			if (fileSize < ONE_MEGABYTE) {
				return DECIMAL_FORMAT.format(fileSize / ONE_KILOBYTE) + resources.getString(R.string.file_size_kb);
			} else if (fileSize < ONE_GIGABYTE) {
				return DECIMAL_FORMAT.format(fileSize / ONE_MEGABYTE) + resources.getString(R.string.file_size_mb);
			}
			return DECIMAL_FORMAT.format(fileSize / ONE_GIGABYTE) + resources.getString(R.string.file_size_gb);
		}
	}

	private FileUtils() {
		throw new IllegalStateException();
	}
}
