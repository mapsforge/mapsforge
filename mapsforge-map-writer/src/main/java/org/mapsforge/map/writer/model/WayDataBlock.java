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
package org.mapsforge.map.writer.model;

import java.util.List;

/**
 * Class to store a WayDataBlock. Each WayDataBlock can store one way and a list of corresponding inner ways. Simple
 * ways and simple polygons have zero inner ways while multi polygons have one or more inner ways.
 */
public class WayDataBlock {
	private final Encoding encoding;
	private final List<List<Integer>> innerWays;
	private final List<Integer> outerWay;

	/**
	 * Creates a WayDataBlock in which way coordinates are not encoded.
	 * 
	 * @param outerWay
	 *            the outer way of the way data block
	 * @param innerWays
	 *            the inner ways of the way data block, or null if not existent
	 */
	public WayDataBlock(List<Integer> outerWay, List<List<Integer>> innerWays) {
		this.outerWay = outerWay;
		this.innerWays = innerWays;
		this.encoding = Encoding.NONE;
	}

	/**
	 * @param outerWay
	 *            the outer way of the way data block
	 * @param innerWays
	 *            the inner ways of the way data block, or null if not existent
	 * @param encoding
	 *            the encoding used to represent the coordinates
	 */
	public WayDataBlock(List<Integer> outerWay, List<List<Integer>> innerWays, Encoding encoding) {
		super();
		this.outerWay = outerWay;
		this.innerWays = innerWays;
		this.encoding = encoding;
	}

	/**
	 * @return the encoding
	 */
	public Encoding getEncoding() {
		return this.encoding;
	}

	/**
	 * @return the innerWays
	 */
	public List<List<Integer>> getInnerWays() {
		return this.innerWays;
	}

	/**
	 * @return the outerWay
	 */
	public List<Integer> getOuterWay() {
		return this.outerWay;
	}
}
