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
package org.mapsforge.map.writer.model;

/**
 * Represents preferred encoding.
 */
public enum EncodingChoice {
	/**
	 * AUTO.
	 */
	AUTO,
	/**
	 * DOUBLE.
	 */
	DOUBLE,
	/**
	 * SINGLE.
	 */
	SINGLE;

	/**
	 * Reads preferred encoding from a String.
	 * 
	 * @param encoding
	 *            the encoding
	 * @return the preferred encoding, AUTO if preferred encoding unknown
	 */
	public static EncodingChoice fromString(String encoding) {
		if ("auto".equalsIgnoreCase(encoding)) {
			return AUTO;
		}

		if ("single".equalsIgnoreCase(encoding)) {
			return SINGLE;
		}

		if ("double".equalsIgnoreCase(encoding)) {
			return DOUBLE;
		}

		return AUTO;
	}
}
