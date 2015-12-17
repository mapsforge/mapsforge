/*
 * Copyright 2015 devemux86
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
package org.mapsforge.poi.writer.util;

/**
 * Defines some constants.
 */
public final class Constants {
	/**
	 * The name of the POI writer.
	 */
	public static final String CREATOR_NAME = "mapsforge-poi-writer";
	/**
	 * Default name for out file.
	 */
	public static final String DEFAULT_PARAM_OUTFILE = "mapsforge.poi";
	/**
	 * The name of the property that refers to the version of the POI writer.
	 */
	public static final String PROPERTY_NAME_WRITER_VERSION = "poi.writer.version";

	private Constants() {
		throw new IllegalStateException();
	}
}
