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
import org.mapsforge.core.graphics.Display;
import org.mapsforge.core.graphics.Position;
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.model.Tile;
import org.mapsforge.map.layer.renderer.PolylineContainer;
import org.mapsforge.map.reader.PointOfInterest;
import org.mapsforge.map.rendertheme.RenderCallback;

class DummyRenderCallback implements RenderCallback {
	@Override
	public void renderArea(PolylineContainer way, Paint fill, Paint stroke, int level) {
		// do nothing
	}

	@Override
	public void renderAreaCaption(PolylineContainer way, Display display, int priority, String caption, float horizontalOffset, float verticalOffset,
	                              Paint fill, Paint stroke, Position position, int maxTextWidth) {
		// do nothing
	}

	@Override
	public void renderAreaSymbol(PolylineContainer way, Display display, int priority, Bitmap symbol) {
		// do nothing
	}

	@Override
	public void renderPointOfInterestCaption(PointOfInterest poi, Display display, int priority, String caption, float horizontalOffset, float verticalOffset,
	                                         Paint fill, Paint stroke, Position position, int maxTextWidth, Tile tile) {
		// do nothing
	}

	@Override
	public void renderPointOfInterestCircle(PointOfInterest poi, float radius, Paint fill, Paint stroke, int level, Tile tile) {
		// do nothing
	}

	@Override
	public void renderPointOfInterestSymbol(PointOfInterest poi, Display display, int priority, Bitmap symbol, Tile tile) {
		// do nothing
	}

	@Override
	public void renderWay(PolylineContainer way, Paint stroke, float dy, int level) {
		// do nothing
	}

	@Override
	public void renderWaySymbol(PolylineContainer way, Display display, int priority, Bitmap symbol, float dy, boolean alignCenter,
	                            boolean repeat, float repeatGap, float repeatStart, boolean rotate) {
		// do nothing
	}

	@Override
	public void renderWayText(PolylineContainer way, Display display, int priority, String text, float dy, Paint fill, Paint stroke) {
		// do nothing
	}
}
