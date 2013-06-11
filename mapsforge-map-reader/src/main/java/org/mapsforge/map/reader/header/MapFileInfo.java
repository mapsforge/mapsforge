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
package org.mapsforge.map.reader.header;

import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Tag;
import org.mapsforge.map.reader.MapDatabase;

/**
 * Contains the immutable metadata of a map file.
 * 
 * @see MapDatabase#getMapFileInfo()
 */
public class MapFileInfo {
	/**
	 * The bounding box of the map file.
	 */
	public final BoundingBox boundingBox;

	/**
	 * The comment field of the map file (may be null).
	 */
	public final String comment;

	/**
	 * The created by field of the map file (may be null).
	 */
	public final String createdBy;

	/**
	 * True if the map file includes debug information, false otherwise.
	 */
	public final boolean debugFile;

	/**
	 * The size of the map file, measured in bytes.
	 */
	public final long fileSize;

	/**
	 * The file version number of the map file.
	 */
	public final int fileVersion;

	/**
	 * The preferred language for names as defined in ISO 3166-1 (may be null).
	 */
	public final String languagePreference;

	/**
	 * The date of the map data in milliseconds since January 1, 1970.
	 */
	public final long mapDate;

	/**
	 * The number of sub-files in the map file.
	 */
	public final byte numberOfSubFiles;

	/**
	 * The POI tags.
	 */
	public final Tag[] poiTags;

	/**
	 * The name of the projection used in the map file.
	 */
	public final String projectionName;

	/**
	 * The map start position from the file header (may be null).
	 */
	public final LatLong startPosition;

	/**
	 * The map start zoom level from the file header (may be null).
	 */
	public final Byte startZoomLevel;

	/**
	 * The size of the tiles in pixels.
	 */
	public final int tilePixelSize;

	/**
	 * The way tags.
	 */
	public final Tag[] wayTags;

	MapFileInfo(MapFileInfoBuilder mapFileInfoBuilder) {
		this.comment = mapFileInfoBuilder.optionalFields.comment;
		this.createdBy = mapFileInfoBuilder.optionalFields.createdBy;
		this.debugFile = mapFileInfoBuilder.optionalFields.isDebugFile;
		this.fileSize = mapFileInfoBuilder.fileSize;
		this.fileVersion = mapFileInfoBuilder.fileVersion;
		this.languagePreference = mapFileInfoBuilder.optionalFields.languagePreference;
		this.boundingBox = mapFileInfoBuilder.boundingBox;
		this.mapDate = mapFileInfoBuilder.mapDate;
		this.numberOfSubFiles = mapFileInfoBuilder.numberOfSubFiles;
		this.poiTags = mapFileInfoBuilder.poiTags;
		this.projectionName = mapFileInfoBuilder.projectionName;
		this.startPosition = mapFileInfoBuilder.optionalFields.startPosition;
		this.startZoomLevel = mapFileInfoBuilder.optionalFields.startZoomLevel;
		this.tilePixelSize = mapFileInfoBuilder.tilePixelSize;
		this.wayTags = mapFileInfoBuilder.wayTags;
	}
}
