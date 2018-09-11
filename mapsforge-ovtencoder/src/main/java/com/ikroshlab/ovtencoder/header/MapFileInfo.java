/*
 * Copyright 2010, 2011, 2012 mapsforge.org
 * Copyright 2016 devemux86
 *
 * This file is part of the OpenScienceMap project (http://www.opensciencemap.org).
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
package com.ikroshlab.ovtencoder.header;

import org.oscim.core.Tag;

/**
 * Contains the immutable metadata of a map file.
 */
public class MapFileInfo extends com.ikroshlab.ovtencoder.MapInfo {

    /**
     * True if the map file includes debug information, false otherwise.
     */
    public final boolean debugFile;

    /**
     * The number of sub-files in the map file.
     */
    public final byte numberOfSubFiles;

    /**
     * The POI tags.
     */
    public final Tag[] poiTags;

    /**
     * The way tags.
     */
    public final Tag[] wayTags;

    /**
     * The size of the tiles in pixels.
     */
    public final int tilePixelSize;

    MapFileInfo(MapFileInfoBuilder mapFileInfoBuilder) {
        super(mapFileInfoBuilder.boundingBox,
                mapFileInfoBuilder.optionalFields.startZoomLevel,
                mapFileInfoBuilder.optionalFields.startPosition,
                mapFileInfoBuilder.projectionName,
                mapFileInfoBuilder.mapDate,
                mapFileInfoBuilder.fileSize,
                mapFileInfoBuilder.fileVersion,
                mapFileInfoBuilder.optionalFields.languagesPreference,
                mapFileInfoBuilder.optionalFields.comment,
                mapFileInfoBuilder.optionalFields.createdBy,
                mapFileInfoBuilder.zoomLevel);

        debugFile = mapFileInfoBuilder.optionalFields.isDebugFile;

        numberOfSubFiles = mapFileInfoBuilder.numberOfSubFiles;
        poiTags = mapFileInfoBuilder.poiTags;

        tilePixelSize = mapFileInfoBuilder.tilePixelSize;
        wayTags = mapFileInfoBuilder.wayTags;
    }
}
