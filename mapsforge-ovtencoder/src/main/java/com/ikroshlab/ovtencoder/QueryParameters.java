/*
 * Copyright 2010, 2011, 2012 mapsforge.org
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
package com.ikroshlab.ovtencoder;

class QueryParameters {
    long fromBaseTileX;
    long fromBaseTileY;
    long fromBlockX;
    long fromBlockY;
    int queryTileBitmask;
    int queryZoomLevel;
    long toBaseTileX;
    long toBaseTileY;
    long toBlockX;
    long toBlockY;
    boolean useTileBitmask;

    @Override
    public String toString() {
        return new StringBuilder()
                .append("QueryParameters [fromBaseTileX=")
                .append(this.fromBaseTileX)
                .append(", fromBaseTileY=")
                .append(this.fromBaseTileY)
                .append(", fromBlockX=")
                .append(this.fromBlockX)
                .append(", fromBlockY=")
                .append(this.fromBlockY)
                .append(", queryTileBitmask=")
                .append(this.queryTileBitmask)
                .append(", queryZoomLevel=")
                .append(this.queryZoomLevel)
                .append(", toBaseTileX=")
                .append(this.toBaseTileX)
                .append(", toBaseTileY=")
                .append(this.toBaseTileY)
                .append(", toBlockX=")
                .append(this.toBlockX)
                .append(", toBlockY=")
                .append(this.toBlockY)
                .append(", useTileBitmask=")
                .append(this.useTileBitmask)
                .append("]")
                .toString();
    }
}
