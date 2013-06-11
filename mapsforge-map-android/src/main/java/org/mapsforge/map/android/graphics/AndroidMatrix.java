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
package org.mapsforge.map.android.graphics;

import org.mapsforge.core.graphics.Matrix;

class AndroidMatrix implements Matrix {
	final android.graphics.Matrix matrix = new android.graphics.Matrix();

	@Override
	public void reset() {
		this.matrix.reset();
	}

	@Override
	public void rotate(float theta) {
		this.matrix.preRotate((float) Math.toDegrees(theta));
	}

	@Override
	public void rotate(float theta, float pivotX, float pivotY) {
		this.matrix.preRotate((float) Math.toDegrees(theta), pivotX, pivotY);
	}

	@Override
	public void scale(float scaleX, float scaleY) {
		this.matrix.preScale(scaleX, scaleY);
	}

	@Override
	public void translate(float translateX, float translateY) {
		this.matrix.preTranslate(translateX, translateY);
	}
}
