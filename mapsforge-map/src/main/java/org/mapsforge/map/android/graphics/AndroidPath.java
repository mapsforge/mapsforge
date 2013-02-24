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
package org.mapsforge.map.android.graphics;

import org.mapsforge.core.graphics.Path;

class AndroidPath implements Path {
	final android.graphics.Path path;

	AndroidPath() {
		this.path = new android.graphics.Path();
		this.path.setFillType(android.graphics.Path.FillType.EVEN_ODD);
	}

	@Override
	public void addPoint(int x, int y) {
		if (isEmpty()) {
			this.path.moveTo(x, y);
		} else {
			this.path.lineTo(x, y);
		}
	}

	@Override
	public void clear() {
		this.path.rewind();
	}

	@Override
	public boolean isEmpty() {
		return this.path.isEmpty();
	}
}
