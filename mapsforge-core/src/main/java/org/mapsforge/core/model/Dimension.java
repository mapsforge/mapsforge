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

public class Dimension implements Serializable {
	private static final long serialVersionUID = 1L;

	public final int height;
	public final int width;

	public Dimension(int width, int height) {
		if (width < 0) {
			throw new IllegalArgumentException("width must not be negative: " + width);
		} else if (height < 0) {
			throw new IllegalArgumentException("height must not be negative: " + height);
		}

		this.width = width;
		this.height = height;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		} else if (!(obj instanceof Dimension)) {
			return false;
		}
		Dimension other = (Dimension) obj;
		if (this.width != other.width) {
			return false;
		} else if (this.height != other.height) {
			return false;
		}
		return true;
	}

	/**
	 * Gets the center point of the dimension.
	 * 
	 * @return the center point
	 */
	public Point getCenter() {
		return new Point((float) this.width / 2, (float) this.height / 2);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + this.width;
		result = prime * result + this.height;
		return result;
	}

	@Override
	public String toString() {
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("width=");
		stringBuilder.append(this.width);
		stringBuilder.append(", height=");
		stringBuilder.append(this.height);
		return stringBuilder.toString();
	}
}
