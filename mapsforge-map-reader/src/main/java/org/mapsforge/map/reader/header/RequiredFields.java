/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright 2014 Ludwig M Brinckmann
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

import java.io.IOException;

import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.Tag;
import org.mapsforge.core.util.LatLongUtils;
import org.mapsforge.map.reader.ReadBuffer;

final class RequiredFields {
	/**
	 * Magic byte at the beginning of a valid binary map file.
	 */
	private static final String BINARY_OSM_MAGIC_BYTE = "mapsforge binary OSM";

	/**
	 * Maximum size of the file header in bytes.
	 */
	private static final int HEADER_SIZE_MAX = 1000000;

	/**
	 * Minimum size of the file header in bytes.
	 */
	private static final int HEADER_SIZE_MIN = 70;

	/**
	 * The name of the Mercator projection as stored in the file header.
	 */
	private static final String MERCATOR = "Mercator";

	/**
	 * Lowest version of the map file format supported by this implementation.
	 */
	private static final int SUPPORTED_FILE_VERSION_MIN = 3;

	/**
	 * Highest version of the map file format supported by this implementation.
	 */
	private static final int SUPPORTED_FILE_VERSION_MAX = 4;

	static void readBoundingBox(ReadBuffer readBuffer, MapFileInfoBuilder mapFileInfoBuilder) {
		double minLatitude = LatLongUtils.microdegreesToDegrees(readBuffer.readInt());
		double minLongitude = LatLongUtils.microdegreesToDegrees(readBuffer.readInt());
		double maxLatitude = LatLongUtils.microdegreesToDegrees(readBuffer.readInt());
		double maxLongitude = LatLongUtils.microdegreesToDegrees(readBuffer.readInt());

		try {
			mapFileInfoBuilder.boundingBox = new BoundingBox(minLatitude, minLongitude, maxLatitude, maxLongitude);
		} catch (IllegalArgumentException e) {
			throw new MapFileException(e.getMessage());
		}
	}

	static void readFileSize(ReadBuffer readBuffer, long fileSize, MapFileInfoBuilder mapFileInfoBuilder) {
		// get and check the file size (8 bytes)
		long headerFileSize = readBuffer.readLong();
		if (headerFileSize != fileSize) {
			throw new MapFileException("invalid file size: " + headerFileSize);
		}
		mapFileInfoBuilder.fileSize = fileSize;
	}

	static void readFileVersion(ReadBuffer readBuffer, MapFileInfoBuilder mapFileInfoBuilder) {
		// get and check the file version (4 bytes)
		int fileVersion = readBuffer.readInt();
		if (fileVersion < SUPPORTED_FILE_VERSION_MIN || fileVersion > SUPPORTED_FILE_VERSION_MAX) {
			throw new MapFileException("unsupported file version: " + fileVersion);
		}
		mapFileInfoBuilder.fileVersion = fileVersion;
	}

	static void readMagicByte(ReadBuffer readBuffer) throws IOException {
		// read the the magic byte and the file header size into the buffer
		int magicByteLength = BINARY_OSM_MAGIC_BYTE.length();
		if (!readBuffer.readFromFile(magicByteLength + 4)) {
			throw new MapFileException("reading magic byte has failed");
		}

		// get and check the magic byte
		String magicByte = readBuffer.readUTF8EncodedString(magicByteLength);
		if (!BINARY_OSM_MAGIC_BYTE.equals(magicByte)) {
			throw new MapFileException("invalid magic byte: " + magicByte);
		}
	}

	static void readMapDate(ReadBuffer readBuffer, MapFileInfoBuilder mapFileInfoBuilder) {
		// get and check the the map date (8 bytes)
		long mapDate = readBuffer.readLong();
		// is the map date before 2010-01-10 ?
		if (mapDate < 1200000000000L) {
			throw new MapFileException("invalid map date: " + mapDate);
		}
		mapFileInfoBuilder.mapDate = mapDate;
	}

	static void readPoiTags(ReadBuffer readBuffer, MapFileInfoBuilder mapFileInfoBuilder) {
		// get and check the number of POI tags (2 bytes)
		int numberOfPoiTags = readBuffer.readShort();
		if (numberOfPoiTags < 0) {
			throw new MapFileException("invalid number of POI tags: " + numberOfPoiTags);
		}

		Tag[] poiTags = new Tag[numberOfPoiTags];
		for (int currentTagId = 0; currentTagId < numberOfPoiTags; ++currentTagId) {
			// get and check the POI tag
			String tag = readBuffer.readUTF8EncodedString();
			if (tag == null) {
				throw new MapFileException("POI tag must not be null: " + currentTagId);
			}
			poiTags[currentTagId] = new Tag(tag);
		}
		mapFileInfoBuilder.poiTags = poiTags;
	}

	static void readProjectionName(ReadBuffer readBuffer, MapFileInfoBuilder mapFileInfoBuilder) {
		// get and check the projection name
		String projectionName = readBuffer.readUTF8EncodedString();
		if (!MERCATOR.equals(projectionName)) {
			throw new MapFileException("unsupported projection: " + projectionName);
		}
		mapFileInfoBuilder.projectionName = projectionName;
	}

	static void readRemainingHeader(ReadBuffer readBuffer) throws IOException {
		// get and check the size of the remaining file header (4 bytes)
		int remainingHeaderSize = readBuffer.readInt();
		if (remainingHeaderSize < HEADER_SIZE_MIN || remainingHeaderSize > HEADER_SIZE_MAX) {
			throw new MapFileException("invalid remaining header size: " + remainingHeaderSize);
		}

		// read the header data into the buffer
		if (!readBuffer.readFromFile(remainingHeaderSize)) {
			throw new MapFileException("reading header data has failed: " + remainingHeaderSize);
		}
	}

	static void readTilePixelSize(ReadBuffer readBuffer, MapFileInfoBuilder mapFileInfoBuilder) {
		// get and check the tile pixel size (2 bytes)
		int tilePixelSize = readBuffer.readShort();
		// if (tilePixelSize != Tile.TILE_SIZE) {
		// return new FileOpenResult("unsupported tile pixel size: " + tilePixelSize);
		// }
		mapFileInfoBuilder.tilePixelSize = tilePixelSize;
	}

	static void readWayTags(ReadBuffer readBuffer, MapFileInfoBuilder mapFileInfoBuilder) {
		// get and check the number of way tags (2 bytes)
		int numberOfWayTags = readBuffer.readShort();
		if (numberOfWayTags < 0) {
			throw new MapFileException("invalid number of way tags: " + numberOfWayTags);
		}

		Tag[] wayTags = new Tag[numberOfWayTags];

		for (int currentTagId = 0; currentTagId < numberOfWayTags; ++currentTagId) {
			// get and check the way tag
			String tag = readBuffer.readUTF8EncodedString();
			if (tag == null) {
				throw new MapFileException("way tag must not be null: " + currentTagId);
			}
			wayTags[currentTagId] = new Tag(tag);
		}
		mapFileInfoBuilder.wayTags = wayTags;
	}

	private RequiredFields() {
		throw new IllegalStateException();
	}
}
