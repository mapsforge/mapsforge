/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright 2014-2015 Ludwig M Brinckmann
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
package org.mapsforge.map.reader;

import org.mapsforge.core.model.Tile;
import org.mapsforge.map.reader.header.SubFileParameter;

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


    public void calculateBaseTiles(Tile tile, SubFileParameter subFileParameter) {
        if (tile.zoomLevel < subFileParameter.baseZoomLevel) {
            // calculate the XY numbers of the upper left and lower right sub-tiles
            int zoomLevelDifference = subFileParameter.baseZoomLevel - tile.zoomLevel;
            this.fromBaseTileX = tile.tileX << zoomLevelDifference;
            this.fromBaseTileY = tile.tileY << zoomLevelDifference;
            this.toBaseTileX = this.fromBaseTileX + (1 << zoomLevelDifference) - 1;
            this.toBaseTileY = this.fromBaseTileY + (1 << zoomLevelDifference) - 1;
            this.useTileBitmask = false;
        } else if (tile.zoomLevel > subFileParameter.baseZoomLevel) {
            // calculate the XY numbers of the parent base tile
            int zoomLevelDifference = tile.zoomLevel - subFileParameter.baseZoomLevel;
            this.fromBaseTileX = tile.tileX >>> zoomLevelDifference;
            this.fromBaseTileY = tile.tileY >>> zoomLevelDifference;
            this.toBaseTileX = this.fromBaseTileX;
            this.toBaseTileY = this.fromBaseTileY;
            this.useTileBitmask = true;
            this.queryTileBitmask = QueryCalculations.calculateTileBitmask(tile, zoomLevelDifference);
        } else {
            // use the tile XY numbers of the requested tile
            this.fromBaseTileX = tile.tileX;
            this.fromBaseTileY = tile.tileY;
            this.toBaseTileX = this.fromBaseTileX;
            this.toBaseTileY = this.fromBaseTileY;
            this.useTileBitmask = false;
        }
    }

    public void calculateBaseTiles(Tile upperLeft, Tile lowerRight, SubFileParameter subFileParameter) {
        if (upperLeft.zoomLevel < subFileParameter.baseZoomLevel) {
            // here we need to combine multiple base tiles
            int zoomLevelDifference = subFileParameter.baseZoomLevel - upperLeft.zoomLevel;
            this.fromBaseTileX = upperLeft.tileX << zoomLevelDifference;
            this.fromBaseTileY = upperLeft.tileY << zoomLevelDifference;
            this.toBaseTileX = (lowerRight.tileX << zoomLevelDifference) + (1 << zoomLevelDifference) - 1;
            this.toBaseTileY = (lowerRight.tileY << zoomLevelDifference) + (1 << zoomLevelDifference) - 1;
            this.useTileBitmask = false;
        } else if (upperLeft.zoomLevel > subFileParameter.baseZoomLevel) {
            // we might have more than just one base tile as we might span boundaries
            int zoomLevelDifference = upperLeft.zoomLevel - subFileParameter.baseZoomLevel;
            this.fromBaseTileX = upperLeft.tileX >>> zoomLevelDifference;
            this.fromBaseTileY = upperLeft.tileY >>> zoomLevelDifference;
            this.toBaseTileX = lowerRight.tileX >>> zoomLevelDifference;
            this.toBaseTileY = lowerRight.tileY >>> zoomLevelDifference;
            // TODO understand what is going on here. The tileBitmask is used to extract just
            // the data from the base tiles that is relevant for the area, but how can this work
            // for a set of tiles, so not using tileBitmask for the moment.
            this.useTileBitmask = true;
            this.queryTileBitmask = QueryCalculations.calculateTileBitmask(upperLeft, lowerRight, zoomLevelDifference);
        } else {
            // we are on the base zoom level, so we just need all tiles in range
            this.fromBaseTileX = upperLeft.tileX;
            this.fromBaseTileY = upperLeft.tileY;
            this.toBaseTileX = lowerRight.tileX;
            this.toBaseTileY = lowerRight.tileY;
            this.useTileBitmask = false;
        }
    }


    public void calculateBlocks(SubFileParameter subFileParameter) {
        // calculate the blocks in the file which need to be read
        this.fromBlockX = Math.max(this.fromBaseTileX - subFileParameter.boundaryTileLeft, 0);
        this.fromBlockY = Math.max(this.fromBaseTileY - subFileParameter.boundaryTileTop, 0);
        this.toBlockX = Math.min(this.toBaseTileX - subFileParameter.boundaryTileLeft,
                subFileParameter.blocksWidth - 1);
        this.toBlockY = Math.min(this.toBaseTileY - subFileParameter.boundaryTileTop,
                subFileParameter.blocksHeight - 1);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (!(obj instanceof QueryParameters)) {
            return false;
        }
        QueryParameters other = (QueryParameters) obj;
        return fromBaseTileX == other.fromBaseTileX &&
                fromBlockX == other.fromBlockX &&
                fromBaseTileY == other.fromBaseTileY &&
                fromBlockY == other.fromBlockY &&
                queryTileBitmask == other.queryTileBitmask &&
                queryZoomLevel == other.queryZoomLevel &&
                toBaseTileX == other.toBaseTileX &&
                toBaseTileY == other.toBaseTileY &&
                toBlockX == other.toBlockX &&
                toBlockY == other.toBlockY &&
                useTileBitmask == other.useTileBitmask;

    }

    @Override
    public int hashCode() {
        int result = 7;
        result = 31 * result + (int) (this.fromBaseTileX ^ (this.fromBaseTileX >>> 16));
        result = 31 * result + (int) (this.fromBaseTileY ^ (this.fromBaseTileY >>> 16));
        result = 31 * result + (int) (this.toBaseTileX ^ (this.toBaseTileX >>> 16));
        result = 31 * result + (int) (this.toBaseTileY ^ (this.toBaseTileY >>> 16));
        result = 31 * result + (int) (this.fromBlockX ^ (this.fromBlockX >>> 16));
        result = 31 * result + (int) (this.fromBlockY ^ (this.fromBlockY >>> 16));
        result = 31 * result + (int) (this.toBlockX ^ (this.toBlockX >>> 16));
        result = 31 * result + (int) (this.toBlockY ^ (this.toBlockY >>> 16));
        result = 31 * result + this.queryZoomLevel;
        result = 31 * result + this.queryTileBitmask;
        return result;
    }

}
