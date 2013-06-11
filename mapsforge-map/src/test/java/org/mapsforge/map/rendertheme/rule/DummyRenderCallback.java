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
package org.mapsforge.map.rendertheme.rule;

import org.mapsforge.core.graphics.Bitmap;
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.map.rendertheme.RenderCallback;

class DummyRenderCallback implements RenderCallback {
	@Override
	public void renderArea(Paint fill, Paint stroke, int level) {
		// do nothing
	}

	@Override
	public void renderAreaCaption(String caption, float verticalOffset, Paint fill, Paint stroke) {
		// do nothing
	}

	@Override
	public void renderAreaSymbol(Bitmap symbol) {
		// do nothing
	}

	@Override
	public void renderPointOfInterestCaption(String caption, float verticalOffset, Paint fill, Paint stroke) {
		// do nothing
	}

	@Override
	public void renderPointOfInterestCircle(float radius, Paint fill, Paint stroke, int level) {
		// do nothing
	}

	@Override
	public void renderPointOfInterestSymbol(Bitmap symbol) {
		// do nothing
	}

	@Override
	public void renderWay(Paint stroke, int level) {
		// do nothing
	}

	@Override
	public void renderWaySymbol(Bitmap symbol, boolean alignCenter, boolean repeat) {
		// do nothing
	}

	@Override
	public void renderWayText(String text, Paint fill, Paint stroke) {
		// do nothing
	}
}
