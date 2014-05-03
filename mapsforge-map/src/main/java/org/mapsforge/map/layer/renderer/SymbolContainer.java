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
package org.mapsforge.map.layer.renderer;

import org.mapsforge.core.graphics.Bitmap;
import org.mapsforge.core.model.Point;

class SymbolContainer {
	final boolean alignCenter;
	final Point point;
	final Bitmap symbol;
	final float theta;

	SymbolContainer(Bitmap symbol, Point point) {
		this(symbol, point, false, 0);
	}

	SymbolContainer(Bitmap symbol, Point point, boolean alignCenter, float theta) {
		this.symbol = symbol;
		this.point = point;
		this.alignCenter = alignCenter;
		this.theta = theta;
	}
}
