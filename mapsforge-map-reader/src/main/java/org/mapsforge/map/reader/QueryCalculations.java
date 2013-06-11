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
package org.mapsforge.map.reader;

import org.mapsforge.core.model.Tile;
import org.mapsforge.map.reader.header.SubFileParameter;

final class QueryCalculations {
	static void calculateBaseTiles(QueryParameters queryParameters, Tile tile, SubFileParameter subFileParameter) {
		if (tile.zoomLevel < subFileParameter.baseZoomLevel) {
			// calculate the XY numbers of the upper left and lower right sub-tiles
			int zoomLevelDifference = subFileParameter.baseZoomLevel - tile.zoomLevel;
			queryParameters.fromBaseTileX = tile.tileX << zoomLevelDifference;
			queryParameters.fromBaseTileY = tile.tileY << zoomLevelDifference;
			queryParameters.toBaseTileX = queryParameters.fromBaseTileX + (1 << zoomLevelDifference) - 1;
			queryParameters.toBaseTileY = queryParameters.fromBaseTileY + (1 << zoomLevelDifference) - 1;
			queryParameters.useTileBitmask = false;
		} else if (tile.zoomLevel > subFileParameter.baseZoomLevel) {
			// calculate the XY numbers of the parent base tile
			int zoomLevelDifference = tile.zoomLevel - subFileParameter.baseZoomLevel;
			queryParameters.fromBaseTileX = tile.tileX >>> zoomLevelDifference;
			queryParameters.fromBaseTileY = tile.tileY >>> zoomLevelDifference;
			queryParameters.toBaseTileX = queryParameters.fromBaseTileX;
			queryParameters.toBaseTileY = queryParameters.fromBaseTileY;
			queryParameters.useTileBitmask = true;
			queryParameters.queryTileBitmask = calculateTileBitmask(tile, zoomLevelDifference);
		} else {
			// use the tile XY numbers of the requested tile
			queryParameters.fromBaseTileX = tile.tileX;
			queryParameters.fromBaseTileY = tile.tileY;
			queryParameters.toBaseTileX = queryParameters.fromBaseTileX;
			queryParameters.toBaseTileY = queryParameters.fromBaseTileY;
			queryParameters.useTileBitmask = false;
		}
	}

	static void calculateBlocks(QueryParameters queryParameters, SubFileParameter subFileParameter) {
		// calculate the blocks in the file which need to be read
		queryParameters.fromBlockX = Math.max(queryParameters.fromBaseTileX - subFileParameter.boundaryTileLeft, 0);
		queryParameters.fromBlockY = Math.max(queryParameters.fromBaseTileY - subFileParameter.boundaryTileTop, 0);
		queryParameters.toBlockX = Math.min(queryParameters.toBaseTileX - subFileParameter.boundaryTileLeft,
				subFileParameter.blocksWidth - 1);
		queryParameters.toBlockY = Math.min(queryParameters.toBaseTileY - subFileParameter.boundaryTileTop,
				subFileParameter.blocksHeight - 1);
	}

	static int calculateTileBitmask(Tile tile, int zoomLevelDifference) {
		if (zoomLevelDifference == 1) {
			return getFirstLevelTileBitmask(tile);
		}

		// calculate the XY numbers of the second level sub-tile
		long subtileX = tile.tileX >>> (zoomLevelDifference - 2);
		long subtileY = tile.tileY >>> (zoomLevelDifference - 2);

		// calculate the XY numbers of the parent tile
		long parentTileX = subtileX >>> 1;
		long parentTileY = subtileY >>> 1;

		// determine the correct bitmask for all 16 sub-tiles
		if (parentTileX % 2 == 0 && parentTileY % 2 == 0) {
			return getSecondLevelTileBitmaskUpperLeft(subtileX, subtileY);
		} else if (parentTileX % 2 == 1 && parentTileY % 2 == 0) {
			return getSecondLevelTileBitmaskUpperRight(subtileX, subtileY);
		} else if (parentTileX % 2 == 0 && parentTileY % 2 == 1) {
			return getSecondLevelTileBitmaskLowerLeft(subtileX, subtileY);
		} else {
			return getSecondLevelTileBitmaskLowerRight(subtileX, subtileY);
		}
	}

	private static int getFirstLevelTileBitmask(Tile tile) {
		if (tile.tileX % 2 == 0 && tile.tileY % 2 == 0) {
			// upper left quadrant
			return 0xcc00;
		} else if (tile.tileX % 2 == 1 && tile.tileY % 2 == 0) {
			// upper right quadrant
			return 0x3300;
		} else if (tile.tileX % 2 == 0 && tile.tileY % 2 == 1) {
			// lower left quadrant
			return 0xcc;
		} else {
			// lower right quadrant
			return 0x33;
		}
	}

	private static int getSecondLevelTileBitmaskLowerLeft(long subtileX, long subtileY) {
		if (subtileX % 2 == 0 && subtileY % 2 == 0) {
			// upper left sub-tile
			return 0x80;
		} else if (subtileX % 2 == 1 && subtileY % 2 == 0) {
			// upper right sub-tile
			return 0x40;
		} else if (subtileX % 2 == 0 && subtileY % 2 == 1) {
			// lower left sub-tile
			return 0x8;
		} else {
			// lower right sub-tile
			return 0x4;
		}
	}

	private static int getSecondLevelTileBitmaskLowerRight(long subtileX, long subtileY) {
		if (subtileX % 2 == 0 && subtileY % 2 == 0) {
			// upper left sub-tile
			return 0x20;
		} else if (subtileX % 2 == 1 && subtileY % 2 == 0) {
			// upper right sub-tile
			return 0x10;
		} else if (subtileX % 2 == 0 && subtileY % 2 == 1) {
			// lower left sub-tile
			return 0x2;
		} else {
			// lower right sub-tile
			return 0x1;
		}
	}

	private static int getSecondLevelTileBitmaskUpperLeft(long subtileX, long subtileY) {
		if (subtileX % 2 == 0 && subtileY % 2 == 0) {
			// upper left sub-tile
			return 0x8000;
		} else if (subtileX % 2 == 1 && subtileY % 2 == 0) {
			// upper right sub-tile
			return 0x4000;
		} else if (subtileX % 2 == 0 && subtileY % 2 == 1) {
			// lower left sub-tile
			return 0x800;
		} else {
			// lower right sub-tile
			return 0x400;
		}
	}

	private static int getSecondLevelTileBitmaskUpperRight(long subtileX, long subtileY) {
		if (subtileX % 2 == 0 && subtileY % 2 == 0) {
			// upper left sub-tile
			return 0x2000;
		} else if (subtileX % 2 == 1 && subtileY % 2 == 0) {
			// upper right sub-tile
			return 0x1000;
		} else if (subtileX % 2 == 0 && subtileY % 2 == 1) {
			// lower left sub-tile
			return 0x200;
		} else {
			// lower right sub-tile
			return 0x100;
		}
	}

	private QueryCalculations() {
		throw new IllegalStateException();
	}
}
