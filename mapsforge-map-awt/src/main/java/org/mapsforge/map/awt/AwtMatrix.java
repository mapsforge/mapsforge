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
package org.mapsforge.map.awt;

import java.awt.geom.AffineTransform;

import org.mapsforge.core.graphics.Matrix;

class AwtMatrix implements Matrix {
	final AffineTransform affineTransform = new AffineTransform();

	@Override
	public void reset() {
		this.affineTransform.setToIdentity();
	}

	@Override
	public void rotate(float theta) {
		this.affineTransform.rotate(theta);
	}

	@Override
	public void rotate(float theta, float pivotX, float pivotY) {
		this.affineTransform.rotate(theta, pivotX, pivotY);
	}

    @Override
    public void scale(float scaleX, float scaleY) {
        this.affineTransform.scale(scaleX, scaleY);
    }

    @Override
    public void scale(float scaleX, float scaleY, float pivotX, float pivotY) {
	    this.affineTransform.translate(pivotX, pivotY);
	    this.affineTransform.scale(scaleX, scaleY);
	    this.affineTransform.translate(-pivotX, -pivotY);
    }

	@Override
	public void translate(float translateX, float translateY) {
		this.affineTransform.translate(translateX, translateY);
	}


}
