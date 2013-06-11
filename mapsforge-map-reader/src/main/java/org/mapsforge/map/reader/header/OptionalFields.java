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

import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.util.LatLongUtils;
import org.mapsforge.map.reader.ReadBuffer;

final class OptionalFields {
	/**
	 * Bitmask for the comment field in the file header.
	 */
	private static final int HEADER_BITMASK_COMMENT = 0x08;

	/**
	 * Bitmask for the created by field in the file header.
	 */
	private static final int HEADER_BITMASK_CREATED_BY = 0x04;

	/**
	 * Bitmask for the debug flag in the file header.
	 */
	private static final int HEADER_BITMASK_DEBUG = 0x80;

	/**
	 * Bitmask for the language preference field in the file header.
	 */
	private static final int HEADER_BITMASK_LANGUAGE_PREFERENCE = 0x10;

	/**
	 * Bitmask for the start position field in the file header.
	 */
	private static final int HEADER_BITMASK_START_POSITION = 0x40;

	/**
	 * Bitmask for the start zoom level field in the file header.
	 */
	private static final int HEADER_BITMASK_START_ZOOM_LEVEL = 0x20;

	/**
	 * The length of the language preference string.
	 */
	private static final int LANGUAGE_PREFERENCE_LENGTH = 2;

	/**
	 * Maximum valid start zoom level.
	 */
	private static final int START_ZOOM_LEVEL_MAX = 22;

	static FileOpenResult readOptionalFields(ReadBuffer readBuffer, MapFileInfoBuilder mapFileInfoBuilder) {
		OptionalFields optionalFields = new OptionalFields(readBuffer.readByte());
		mapFileInfoBuilder.optionalFields = optionalFields;

		FileOpenResult fileOpenResult = optionalFields.readOptionalFields(readBuffer);
		if (!fileOpenResult.isSuccess()) {
			return fileOpenResult;
		}
		return FileOpenResult.SUCCESS;
	}

	String comment;
	String createdBy;
	final boolean hasComment;
	final boolean hasCreatedBy;
	final boolean hasLanguagePreference;
	final boolean hasStartPosition;
	final boolean hasStartZoomLevel;
	final boolean isDebugFile;
	String languagePreference;
	LatLong startPosition;
	Byte startZoomLevel;

	private OptionalFields(byte flags) {
		this.isDebugFile = (flags & HEADER_BITMASK_DEBUG) != 0;
		this.hasStartPosition = (flags & HEADER_BITMASK_START_POSITION) != 0;
		this.hasStartZoomLevel = (flags & HEADER_BITMASK_START_ZOOM_LEVEL) != 0;
		this.hasLanguagePreference = (flags & HEADER_BITMASK_LANGUAGE_PREFERENCE) != 0;
		this.hasComment = (flags & HEADER_BITMASK_COMMENT) != 0;
		this.hasCreatedBy = (flags & HEADER_BITMASK_CREATED_BY) != 0;
	}

	private FileOpenResult readLanguagePreference(ReadBuffer readBuffer) {
		if (this.hasLanguagePreference) {
			String countryCode = readBuffer.readUTF8EncodedString();
			if (countryCode.length() != LANGUAGE_PREFERENCE_LENGTH) {
				return new FileOpenResult("invalid language preference: " + countryCode);
			}
			this.languagePreference = countryCode;
		}
		return FileOpenResult.SUCCESS;
	}

	private FileOpenResult readMapStartPosition(ReadBuffer readBuffer) {
		if (this.hasStartPosition) {
			double mapStartLatitude = LatLongUtils.microdegreesToDegrees(readBuffer.readInt());
			double mapStartLongitude = LatLongUtils.microdegreesToDegrees(readBuffer.readInt());
			try {
				this.startPosition = new LatLong(mapStartLatitude, mapStartLongitude);
			} catch (IllegalArgumentException e) {
				return new FileOpenResult(e.getMessage());
			}
		}
		return FileOpenResult.SUCCESS;
	}

	private FileOpenResult readMapStartZoomLevel(ReadBuffer readBuffer) {
		if (this.hasStartZoomLevel) {
			// get and check the start zoom level (1 byte)
			byte mapStartZoomLevel = readBuffer.readByte();
			if (mapStartZoomLevel < 0 || mapStartZoomLevel > START_ZOOM_LEVEL_MAX) {
				return new FileOpenResult("invalid map start zoom level: " + mapStartZoomLevel);
			}

			this.startZoomLevel = Byte.valueOf(mapStartZoomLevel);
		}
		return FileOpenResult.SUCCESS;
	}

	private FileOpenResult readOptionalFields(ReadBuffer readBuffer) {
		FileOpenResult fileOpenResult = readMapStartPosition(readBuffer);
		if (!fileOpenResult.isSuccess()) {
			return fileOpenResult;
		}

		fileOpenResult = readMapStartZoomLevel(readBuffer);
		if (!fileOpenResult.isSuccess()) {
			return fileOpenResult;
		}

		fileOpenResult = readLanguagePreference(readBuffer);
		if (!fileOpenResult.isSuccess()) {
			return fileOpenResult;
		}

		if (this.hasComment) {
			this.comment = readBuffer.readUTF8EncodedString();
		}

		if (this.hasCreatedBy) {
			this.createdBy = readBuffer.readUTF8EncodedString();
		}

		return FileOpenResult.SUCCESS;
	}
}
