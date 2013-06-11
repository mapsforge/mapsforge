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

/**
 * Represents the configuration of zoom intervals. A zoom interval is defined by a base zoom level, a minimum zoom level
 * and a maximum zoom level.
 */
public final class ZoomIntervalConfiguration {
	/**
	 * Create a new ZoomIntervalConfiguration from the given string representation. Checks for validity.
	 * 
	 * @param confString
	 *            the string representation of a zoom interval configuration
	 * @return a new zoom interval configuration
	 */
	public static ZoomIntervalConfiguration fromString(String confString) {
		String[] splitted = confString.split(",");
		if (splitted.length % 3 != 0) {
			throw new IllegalArgumentException(
					"invalid zoom interval configuration, amount of comma-separated values must be a multiple of 3");
		}
		byte[][] intervals = new byte[splitted.length / 3][3];
		for (int i = 0; i < intervals.length; i++) {
			intervals[i][0] = Byte.parseByte(splitted[i * 3]);
			intervals[i][1] = Byte.parseByte(splitted[i * 3 + 1]);
			intervals[i][2] = Byte.parseByte(splitted[i * 3 + 2]);
		}

		return ZoomIntervalConfiguration.newInstance(intervals);
	}

	/**
	 * @return the standard configuration
	 */
	public static ZoomIntervalConfiguration getStandardConfiguration() {
		return new ZoomIntervalConfiguration(new byte[][] { new byte[] { 5, 0, 7 }, new byte[] { 10, 8, 11 },
				new byte[] { 14, 12, 21 } });
	}

	/**
	 * Create a new ZoomIntervalConfiguration from the given byte array. Checks for validity.
	 * 
	 * @param intervals
	 *            the intervals
	 * @return a zoom interval configuration that represents the given intervals
	 */
	public static ZoomIntervalConfiguration newInstance(byte[]... intervals) {
		return new ZoomIntervalConfiguration(intervals);
	}

	private final byte[] baseZoom;
	private final byte maxMaxZoom;

	private final byte[] maxZoom;

	private final byte minMinZoom;

	private final byte[] minZoom;

	private ZoomIntervalConfiguration(byte[][] intervals) {
		this.baseZoom = new byte[intervals.length];
		this.minZoom = new byte[intervals.length];
		this.maxZoom = new byte[intervals.length];

		int i = 0;
		for (byte[] interval : intervals) {
			i++;
			if (interval.length != 3) {
				throw new IllegalArgumentException("invalid interval configuration, found only " + interval.length
						+ "parameters for interval " + i);
			}
			if (interval[0] < interval[1] || interval[0] > interval[2]) {
				throw new IllegalArgumentException("invalid configuration for interval " + i
						+ ", make sure that minZoom < baseZoom < maxZoom");
			}
			if (i > 1) {
				if (interval[0] < this.baseZoom[i - 2]) {
					throw new IllegalArgumentException("interval configurations must follow an increasing order");
				}
				if (interval[1] != ((this.maxZoom[i - 2]) + 1)) {
					throw new IllegalArgumentException("minZoom of interval " + i
							+ " not adjacent to maxZoom of interval " + (i - 1));
				}
			}
			this.baseZoom[i - 1] = interval[0];
			this.minZoom[i - 1] = interval[1];
			this.maxZoom[i - 1] = interval[2];
		}
		this.minMinZoom = this.minZoom[0];
		this.maxMaxZoom = this.maxZoom[this.maxZoom.length - 1];
	}

	/**
	 * @param index
	 *            the zoom interval index
	 * @return the corresponding base zoom level
	 */
	public byte getBaseZoom(int index) {
		checkValidIndex(index);
		return this.baseZoom[index];
	}

	/**
	 * @return the maxMaxZoom
	 */
	public byte getMaxMaxZoom() {
		return this.maxMaxZoom;
	}

	/**
	 * @param index
	 *            the index
	 * @return the corresponding max zoom level
	 */
	public byte getMaxZoom(int index) {
		checkValidIndex(index);
		return this.maxZoom[index];
	}

	/**
	 * @return the minMinZoom
	 */
	public byte getMinMinZoom() {
		return this.minMinZoom;
	}

	/**
	 * @param index
	 *            the zoom interval index
	 * @return the minimum zoom level for this index
	 */
	public byte getMinZoom(int index) {
		checkValidIndex(index);
		return this.minZoom[index];
	}

	/**
	 * @return the number of zoom intervals
	 */
	public int getNumberOfZoomIntervals() {
		if (this.baseZoom == null) {
			return 0;
		}
		return this.baseZoom.length;
	}

	private void checkValidIndex(int index) {
		if (index < 0 || index >= this.baseZoom.length) {
			throw new IllegalArgumentException("illegal zoom interval index: " + index);
		}
	}
}
