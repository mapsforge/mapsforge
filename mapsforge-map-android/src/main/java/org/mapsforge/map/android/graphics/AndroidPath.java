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

import org.mapsforge.core.graphics.FillRule;
import org.mapsforge.core.graphics.Path;

import android.graphics.Path.FillType;

class AndroidPath implements Path {
	private static FillType getWindingRule(FillRule fillRule) {
		switch (fillRule) {
			case EVEN_ODD:
				return FillType.EVEN_ODD;
			case NON_ZERO:
				return FillType.WINDING;
		}

		throw new IllegalArgumentException("unknown fill rule:" + fillRule);
	}

	final android.graphics.Path path = new android.graphics.Path();

	@Override
	public void clear() {
		this.path.rewind();
	}

	@Override
	public void lineTo(float x, float y) {
		this.path.lineTo(x, y);
	}

	@Override
	public void moveTo(float x, float y) {
		this.path.moveTo(x, y);
	}

	@Override
	public void setFillRule(FillRule fillRule) {
		this.path.setFillType(getWindingRule(fillRule));
	}
}
