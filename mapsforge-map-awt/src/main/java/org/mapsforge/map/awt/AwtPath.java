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

import java.awt.geom.Path2D;

import org.mapsforge.core.graphics.FillRule;
import org.mapsforge.core.graphics.Path;

class AwtPath implements Path {
	private static int getWindingRule(FillRule fillRule) {
		switch (fillRule) {
			case EVEN_ODD:
				return Path2D.WIND_EVEN_ODD;
			case NON_ZERO:
				return Path2D.WIND_NON_ZERO;
		}

		throw new IllegalArgumentException("unknown fill rule:" + fillRule);
	}

	final Path2D path2D = new Path2D.Float();

	@Override
	public void clear() {
		this.path2D.reset();
	}

	@Override
	public void lineTo(int x, int y) {
		this.path2D.lineTo(x, y);
	}

	@Override
	public void moveTo(int x, int y) {
		this.path2D.moveTo(x, y);
	}

	@Override
	public void setFillRule(FillRule fillRule) {
		this.path2D.setWindingRule(getWindingRule(fillRule));
	}
}
