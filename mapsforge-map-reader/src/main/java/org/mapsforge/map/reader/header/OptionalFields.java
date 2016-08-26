/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright 2014 Ludwig M Brinckmann
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
     * Bitmask for the language(s) preference field in the file header.
     */
    private static final int HEADER_BITMASK_LANGUAGES_PREFERENCE = 0x10;

    /**
     * Bitmask for the start position field in the file header.
     */
    private static final int HEADER_BITMASK_START_POSITION = 0x40;

    /**
     * Bitmask for the start zoom level field in the file header.
     */
    private static final int HEADER_BITMASK_START_ZOOM_LEVEL = 0x20;

    /**
     * Maximum valid start zoom level.
     */
    private static final int START_ZOOM_LEVEL_MAX = 22;

    static void readOptionalFields(ReadBuffer readBuffer, MapFileInfoBuilder mapFileInfoBuilder) {
        OptionalFields optionalFields = new OptionalFields(readBuffer.readByte());
        mapFileInfoBuilder.optionalFields = optionalFields;

        optionalFields.readOptionalFields(readBuffer);
    }

    String comment;
    String createdBy;
    final boolean hasComment;
    final boolean hasCreatedBy;
    final boolean hasLanguagesPreference;
    final boolean hasStartPosition;
    final boolean hasStartZoomLevel;
    final boolean isDebugFile;
    String languagesPreference;
    LatLong startPosition;
    Byte startZoomLevel;

    private OptionalFields(byte flags) {
        this.isDebugFile = (flags & HEADER_BITMASK_DEBUG) != 0;
        this.hasStartPosition = (flags & HEADER_BITMASK_START_POSITION) != 0;
        this.hasStartZoomLevel = (flags & HEADER_BITMASK_START_ZOOM_LEVEL) != 0;
        this.hasLanguagesPreference = (flags & HEADER_BITMASK_LANGUAGES_PREFERENCE) != 0;
        this.hasComment = (flags & HEADER_BITMASK_COMMENT) != 0;
        this.hasCreatedBy = (flags & HEADER_BITMASK_CREATED_BY) != 0;
    }

    private void readLanguagesPreference(ReadBuffer readBuffer) {
        if (this.hasLanguagesPreference) {
            this.languagesPreference = readBuffer.readUTF8EncodedString();
        }
    }

    private void readMapStartPosition(ReadBuffer readBuffer) {
        if (this.hasStartPosition) {
            double mapStartLatitude = LatLongUtils.microdegreesToDegrees(readBuffer.readInt());
            double mapStartLongitude = LatLongUtils.microdegreesToDegrees(readBuffer.readInt());
            try {
                this.startPosition = new LatLong(mapStartLatitude, mapStartLongitude);
            } catch (IllegalArgumentException e) {
                throw new MapFileException(e.getMessage());
            }
        }
    }

    private void readMapStartZoomLevel(ReadBuffer readBuffer) {
        if (this.hasStartZoomLevel) {
            // get and check the start zoom level (1 byte)
            byte mapStartZoomLevel = readBuffer.readByte();
            if (mapStartZoomLevel < 0 || mapStartZoomLevel > START_ZOOM_LEVEL_MAX) {
                throw new MapFileException("invalid map start zoom level: " + mapStartZoomLevel);
            }

            this.startZoomLevel = Byte.valueOf(mapStartZoomLevel);
        }
    }

    private void readOptionalFields(ReadBuffer readBuffer) {
        readMapStartPosition(readBuffer);

        readMapStartZoomLevel(readBuffer);

        readLanguagesPreference(readBuffer);

        if (this.hasComment) {
            this.comment = readBuffer.readUTF8EncodedString();
        }

        if (this.hasCreatedBy) {
            this.createdBy = readBuffer.readUTF8EncodedString();
        }
    }
}
