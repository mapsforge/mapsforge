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
package org.mapsforge.map.layer;

import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.MapPosition;

import android.graphics.Canvas;

public abstract class Layer {
	private boolean visible;

	public void destroy() {
		// do nothing
	}

	protected final void redraw() {

	}

	public abstract void draw(BoundingBox boundingBox, MapPosition mapPosition, Canvas canvas);

	public final boolean isVisible() {
		return this.visible;
	}

	public final void setVisible(boolean visible) {
		this.visible = visible;
	}
}
