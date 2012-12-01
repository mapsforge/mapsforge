/*
 * Copyright 2010, 2011, 2012 mapsforge.org
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
package org.mapsforge.map.writer.model;

/**
 * @author bross
 */
public class TileGridLayout {
	private final TileCoordinate upperLeft;
	private final int amountTilesHorizontal;
	private final int amountTilesVertical;

	/**
	 * Constructor.
	 * 
	 * @param upperLeft
	 *            the upper left tile coordinate
	 * @param amountTilesHorizontal
	 *            the amount of columns
	 * @param amountTilesVertical
	 *            the amount of rows
	 */
	public TileGridLayout(TileCoordinate upperLeft, int amountTilesHorizontal, int amountTilesVertical) {
		super();
		this.upperLeft = upperLeft;
		this.amountTilesHorizontal = amountTilesHorizontal;
		this.amountTilesVertical = amountTilesVertical;
	}

	/**
	 * @return the upperLeft
	 */
	public TileCoordinate getUpperLeft() {
		return this.upperLeft;
	}

	/**
	 * @return the amountTilesHorizontal
	 */
	public int getAmountTilesHorizontal() {
		return this.amountTilesHorizontal;
	}

	/**
	 * @return the amountTilesVertical
	 */
	public int getAmountTilesVertical() {
		return this.amountTilesVertical;
	}
}
