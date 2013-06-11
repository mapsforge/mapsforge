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
package org.mapsforge.core.util;

import java.io.Closeable;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A utility class with IO-specific helper methods.
 */
public final class IOUtils {
	private static final Logger LOGGER = Logger.getLogger(IOUtils.class.getName());

	/**
	 * Invokes the {@link Closeable#close()} method on the given object. If an {@link IOException} occurs during the
	 * method call, it will be caught and logged on level {@link Level#WARNING}.
	 * 
	 * @param closeable
	 *            the data source which should be closed (may be null).
	 */
	public static void closeQuietly(Closeable closeable) {
		try {
			if (closeable != null) {
				closeable.close();
			}
		} catch (IOException e) {
			LOGGER.log(Level.FINE, e.getMessage(), e);
		}
	}

	private IOUtils() {
		throw new IllegalStateException();
	}
}
