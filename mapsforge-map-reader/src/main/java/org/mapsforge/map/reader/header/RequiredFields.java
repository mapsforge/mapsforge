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
	 * Version of the map file format which is supported by this implementation.
	 */
	private static final int SUPPORTED_FILE_VERSION = 3;

	static FileOpenResult readBoundingBox(ReadBuffer readBuffer, MapFileInfoBuilder mapFileInfoBuilder) {
		double minLatitude = LatLongUtils.microdegreesToDegrees(readBuffer.readInt());
		double minLongitude = LatLongUtils.microdegreesToDegrees(readBuffer.readInt());
		double maxLatitude = LatLongUtils.microdegreesToDegrees(readBuffer.readInt());
		double maxLongitude = LatLongUtils.microdegreesToDegrees(readBuffer.readInt());

		try {
			mapFileInfoBuilder.boundingBox = new BoundingBox(minLatitude, minLongitude, maxLatitude, maxLongitude);
		} catch (IllegalArgumentException e) {
			return new FileOpenResult(e.getMessage());
		}
		return FileOpenResult.SUCCESS;
	}

	static FileOpenResult readFileSize(ReadBuffer readBuffer, long fileSize, MapFileInfoBuilder mapFileInfoBuilder) {
		// get and check the file size (8 bytes)
		long headerFileSize = readBuffer.readLong();
		if (headerFileSize != fileSize) {
			return new FileOpenResult("invalid file size: " + headerFileSize);
		}
		mapFileInfoBuilder.fileSize = fileSize;
		return FileOpenResult.SUCCESS;
	}

	static FileOpenResult readFileVersion(ReadBuffer readBuffer, MapFileInfoBuilder mapFileInfoBuilder) {
		// get and check the file version (4 bytes)
		int fileVersion = readBuffer.readInt();
		if (fileVersion != SUPPORTED_FILE_VERSION) {
			return new FileOpenResult("unsupported file version: " + fileVersion);
		}
		mapFileInfoBuilder.fileVersion = fileVersion;
		return FileOpenResult.SUCCESS;
	}

	static FileOpenResult readMagicByte(ReadBuffer readBuffer) throws IOException {
		// read the the magic byte and the file header size into the buffer
		int magicByteLength = BINARY_OSM_MAGIC_BYTE.length();
		if (!readBuffer.readFromFile(magicByteLength + 4)) {
			return new FileOpenResult("reading magic byte has failed");
		}

		// get and check the magic byte
		String magicByte = readBuffer.readUTF8EncodedString(magicByteLength);
		if (!BINARY_OSM_MAGIC_BYTE.equals(magicByte)) {
			return new FileOpenResult("invalid magic byte: " + magicByte);
		}
		return FileOpenResult.SUCCESS;
	}

	static FileOpenResult readMapDate(ReadBuffer readBuffer, MapFileInfoBuilder mapFileInfoBuilder) {
		// get and check the the map date (8 bytes)
		long mapDate = readBuffer.readLong();
		// is the map date before 2010-01-10 ?
		if (mapDate < 1200000000000L) {
			return new FileOpenResult("invalid map date: " + mapDate);
		}
		mapFileInfoBuilder.mapDate = mapDate;
		return FileOpenResult.SUCCESS;
	}

	static FileOpenResult readPoiTags(ReadBuffer readBuffer, MapFileInfoBuilder mapFileInfoBuilder) {
		// get and check the number of POI tags (2 bytes)
		int numberOfPoiTags = readBuffer.readShort();
		if (numberOfPoiTags < 0) {
			return new FileOpenResult("invalid number of POI tags: " + numberOfPoiTags);
		}

		Tag[] poiTags = new Tag[numberOfPoiTags];
		for (int currentTagId = 0; currentTagId < numberOfPoiTags; ++currentTagId) {
			// get and check the POI tag
			String tag = readBuffer.readUTF8EncodedString();
			if (tag == null) {
				return new FileOpenResult("POI tag must not be null: " + currentTagId);
			}
			poiTags[currentTagId] = new Tag(tag);
		}
		mapFileInfoBuilder.poiTags = poiTags;
		return FileOpenResult.SUCCESS;
	}

	static FileOpenResult readProjectionName(ReadBuffer readBuffer, MapFileInfoBuilder mapFileInfoBuilder) {
		// get and check the projection name
		String projectionName = readBuffer.readUTF8EncodedString();
		if (!MERCATOR.equals(projectionName)) {
			return new FileOpenResult("unsupported projection: " + projectionName);
		}
		mapFileInfoBuilder.projectionName = projectionName;
		return FileOpenResult.SUCCESS;
	}

	static FileOpenResult readRemainingHeader(ReadBuffer readBuffer) throws IOException {
		// get and check the size of the remaining file header (4 bytes)
		int remainingHeaderSize = readBuffer.readInt();
		if (remainingHeaderSize < HEADER_SIZE_MIN || remainingHeaderSize > HEADER_SIZE_MAX) {
			return new FileOpenResult("invalid remaining header size: " + remainingHeaderSize);
		}

		// read the header data into the buffer
		if (!readBuffer.readFromFile(remainingHeaderSize)) {
			return new FileOpenResult("reading header data has failed: " + remainingHeaderSize);
		}
		return FileOpenResult.SUCCESS;
	}

	static FileOpenResult readTilePixelSize(ReadBuffer readBuffer, MapFileInfoBuilder mapFileInfoBuilder) {
		// get and check the tile pixel size (2 bytes)
		int tilePixelSize = readBuffer.readShort();
		// if (tilePixelSize != Tile.TILE_SIZE) {
		// return new FileOpenResult("unsupported tile pixel size: " + tilePixelSize);
		// }
		mapFileInfoBuilder.tilePixelSize = tilePixelSize;
		return FileOpenResult.SUCCESS;
	}

	static FileOpenResult readWayTags(ReadBuffer readBuffer, MapFileInfoBuilder mapFileInfoBuilder) {
		// get and check the number of way tags (2 bytes)
		int numberOfWayTags = readBuffer.readShort();
		if (numberOfWayTags < 0) {
			return new FileOpenResult("invalid number of way tags: " + numberOfWayTags);
		}

		Tag[] wayTags = new Tag[numberOfWayTags];

		for (int currentTagId = 0; currentTagId < numberOfWayTags; ++currentTagId) {
			// get and check the way tag
			String tag = readBuffer.readUTF8EncodedString();
			if (tag == null) {
				return new FileOpenResult("way tag must not be null: " + currentTagId);
			}
			wayTags[currentTagId] = new Tag(tag);
		}
		mapFileInfoBuilder.wayTags = wayTags;
		return FileOpenResult.SUCCESS;
	}

	private RequiredFields() {
		throw new IllegalStateException();
	}
}
