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
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.graphics.Position;
import org.mapsforge.core.model.Rectangle;
import org.mapsforge.map.datastore.PointOfInterest;
import org.mapsforge.map.layer.renderer.PolylineContainer;
import org.mapsforge.map.rendertheme.RenderCallback;
import org.mapsforge.map.rendertheme.RenderContext;

class DummyRenderCallback implements RenderCallback {
    @Override
    public void renderArea(final RenderContext renderContext, Paint fill, Paint stroke, int level, PolylineContainer way) {
        // do nothing
    }

    @Override
    public void renderAreaCaption(final RenderContext renderContext, Display display, int priority, String caption, float horizontalOffset, float verticalOffset, Paint fill, Paint stroke, Position position, int maxTextWidth, PolylineContainer way) {
        // do nothing
    }

    @Override
    public void renderAreaSymbol(final RenderContext renderContext, Display display, int priority, Bitmap symbol, PolylineContainer way) {
        // do nothing
    }

    @Override
    public void renderPointOfInterestCaption(final RenderContext renderContext, Display display, int priority, String caption, float horizontalOffset, float verticalOffset, Paint fill, Paint stroke, Position position, int maxTextWidth, PointOfInterest poi) {
        // do nothing
    }

    @Override
    public void renderPointOfInterestCircle(final RenderContext renderContext, float radius, Paint fill, Paint stroke, int level, PointOfInterest poi) {
        // do nothing
    }

    @Override
    public void renderPointOfInterestSymbol(final RenderContext renderContext, Display display, int priority, Rectangle boundary, Bitmap symbol, PointOfInterest poi) {
        // do nothing
    }

    @Override
    public void renderWay(final RenderContext renderContext, Paint stroke, float dy, int level, PolylineContainer way) {
        // do nothing
    }

    @Override
    public void renderWaySymbol(final RenderContext renderContext, Display display, int priority, Bitmap symbol, float dy, Rectangle boundary, boolean repeat, float repeatGap, float repeatStart, boolean rotate, PolylineContainer way) {
        // do nothing
    }

    @Override
    public void renderWayText(final RenderContext renderContext, Display display, int priority, String text, float dy, Paint fill, Paint stroke,
                              boolean repeat, float repeatGap, float repeatStart, boolean rotate, PolylineContainer way) {
        // do nothing
    }
}
