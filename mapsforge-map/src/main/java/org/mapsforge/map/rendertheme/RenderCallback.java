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
package org.mapsforge.map.rendertheme;

import org.mapsforge.core.graphics.Bitmap;
import org.mapsforge.core.graphics.Display;
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.graphics.Position;
import org.mapsforge.map.datastore.PointOfInterest;
import org.mapsforge.map.layer.renderer.PolylineContainer;

/**
 * Callback methods for rendering areas, ways and points of interest (POIs).
 */
public interface RenderCallback {
    /**
     * Renders an area with the given parameters.
     *
     * @param renderContext
     * @param fill          the paint to be used for rendering the area.
     * @param stroke        an optional paint for the area casing (may be null).
     * @param level
     */
    void renderArea(final RenderContext renderContext, Paint fill, Paint stroke, int level, PolylineContainer way);

    /**
     * Renders an area caption with the given text.
     *
     * @param renderContext
     * @param display          display mode
     * @param priority         priority level
     * @param caption          the text.
     * @param horizontalOffset the horizontal offset of the text.
     * @param verticalOffset   the vertical offset of the text.
     * @param fill             the paint for the text.
     * @param stroke           the casing of the text (may be null).
     * @param position         optional position (may be null)
     * @param maxTextWidth     maximum text width .
     * @param way              the way for the caption.
     */
    void renderAreaCaption(final RenderContext renderContext, Display display, int priority, String caption, float horizontalOffset, float verticalOffset, Paint fill, Paint stroke, Position position, int maxTextWidth, PolylineContainer way);

    /**
     * Renders an area symbol with the given bitmap.
     *
     * @param renderContext
     * @param symbol
     */
    void renderAreaSymbol(final RenderContext renderContext, Display display, int priority, Bitmap symbol, PolylineContainer way);

    /**
     * Renders a point of interest caption with the given text.
     *
     * @param renderContext
     * @param caption          the text to be rendered.
     * @param horizontalOffset the horizontal offset of the caption.
     * @param verticalOffset   the vertical offset of the caption.
     * @param fill             the paint to be used for rendering the text.
     * @param stroke           an optional paint for the text casing (may be null).
     * @param position
     */
    void renderPointOfInterestCaption(final RenderContext renderContext, Display display, int priority, String caption, float horizontalOffset, float verticalOffset, Paint fill, Paint stroke, Position position, int maxTextWidth, PointOfInterest poi);

    /**
     * Renders a point of interest circle with the given parameters.
     *
     * @param renderContext
     * @param radius        the radius of the circle.
     * @param fill          the paint to be used for rendering the circle.
     * @param stroke        an optional paint for the circle casing (may be null).
     * @param level
     */
    void renderPointOfInterestCircle(final RenderContext renderContext, float radius, Paint fill, Paint stroke, int level, PointOfInterest poi);

    /**
     * Renders a point of interest symbol with the given bitmap.
     *
     * @param renderContext
     * @param symbol
     */
    void renderPointOfInterestSymbol(final RenderContext renderContext, Display display, int priority, Bitmap symbol, PointOfInterest poi);

    /**
     * Renders a way with the given parameters.
     *
     * @param renderContext
     * @param stroke        the paint to be used for rendering the way.
     * @param dy            the offset of the way.
     * @param level
     */
    void renderWay(final RenderContext renderContext, Paint stroke, float dy, int level, PolylineContainer way);

    /**
     * Renders a way with the given symbol along the way path.
     *
     * @param renderContext
     * @param symbol        the symbol to be rendered.
     * @param dy            the offset of the way.
     * @param alignCenter   true if the symbol should be centered, false otherwise.
     * @param repeat        true if the symbol should be repeated, false otherwise.
     * @param repeatGap     distance between repetitions.
     * @param repeatStart
     */
    void renderWaySymbol(final RenderContext renderContext, Display display, int priority, Bitmap symbol, float dy, boolean alignCenter, boolean repeat, float repeatGap, float repeatStart, boolean rotate, PolylineContainer way);

    /**
     * Renders a way with the given text along the way path.
     *
     * @param renderContext
     * @param text          the text to be rendered.
     * @param dy            the offset of the way text.
     * @param fill          the paint to be used for rendering the text.
     * @param stroke
     */
    void renderWayText(final RenderContext renderContext, Display display, int priority, String text,
                       float dy, Paint fill, Paint stroke,
                       boolean repeat, float repeatGap, float repeatStart, boolean rotate, PolylineContainer way);
}
