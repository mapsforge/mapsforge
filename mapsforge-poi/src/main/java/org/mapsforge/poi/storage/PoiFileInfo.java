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
package org.mapsforge.poi.storage;

import org.mapsforge.core.model.BoundingBox;

/**
 * Contains the immutable metadata of a POI file.
 */
public class PoiFileInfo {
	/**
	 * The bounding box of the POI file (may be null).
	 */
	public final BoundingBox bounds;

	/**
	 * The comment field of the POI file (may be null).
	 */
	public final String comment;

	/**
	 * The date of the POI data in milliseconds since January 1, 1970.
	 */
	public final long date;

	/**
	 * The preferred language for names as defined in ISO 639-1 or ISO 639-2 (may be null).
	 */
	public final String language;

	/**
	 * The file version number of the POI file.
	 */
	public final int version;

	/**
	 * The writer field of the POI file.
	 */
	public final String writer;

	PoiFileInfo(PoiFileInfoBuilder poiFileInfoBuilder) {
		this.bounds = poiFileInfoBuilder.bounds;
		this.comment = poiFileInfoBuilder.comment;
		this.date = poiFileInfoBuilder.date;
		this.language = poiFileInfoBuilder.language;
		this.version = poiFileInfoBuilder.version;
		this.writer = poiFileInfoBuilder.writer;
	}
}
