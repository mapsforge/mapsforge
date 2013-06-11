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
package org.mapsforge.core.model;

import java.io.Serializable;

/**
 * A Point represents an immutable pair of double coordinates.
 */
public class Point implements Comparable<Point>, Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * The x coordinate of this point.
	 */
	public final double x;

	/**
	 * The y coordinate of this point.
	 */
	public final double y;

	/**
	 * @param x
	 *            the x coordinate of this point.
	 * @param y
	 *            the y coordinate of this point.
	 */
	public Point(double x, double y) {
		this.x = x;
		this.y = y;
	}

	@Override
	public int compareTo(Point point) {
		if (this.x > point.x) {
			return 1;
		} else if (this.x < point.x) {
			return -1;
		} else if (this.y > point.y) {
			return 1;
		} else if (this.y < point.y) {
			return -1;
		}
		return 0;
	}

	/**
	 * @return the euclidian distance from this point to the given point.
	 */
	public double distance(Point point) {
		return Math.hypot(this.x - point.x, this.y - point.y);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		} else if (!(obj instanceof Point)) {
			return false;
		}
		Point other = (Point) obj;
		if (Double.doubleToLongBits(this.x) != Double.doubleToLongBits(other.x)) {
			return false;
		} else if (Double.doubleToLongBits(this.y) != Double.doubleToLongBits(other.y)) {
			return false;
		}
		return true;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		long temp;
		temp = Double.doubleToLongBits(this.x);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(this.y);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		return result;
	}

	@Override
	public String toString() {
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("x=");
		stringBuilder.append(this.x);
		stringBuilder.append(", y=");
		stringBuilder.append(this.y);
		return stringBuilder.toString();
	}
}
