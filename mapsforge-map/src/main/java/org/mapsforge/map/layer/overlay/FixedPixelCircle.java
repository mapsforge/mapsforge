package org.mapsforge.map.layer.overlay;

import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Point;

/**
 * A Circle class that is always drawn with the same size in pixels.
 */
public class FixedPixelCircle extends Circle {
	/**
	 * @param latLong
	 *            the initial center point of this circle (may be null).
	 * @param radius
	 *            the initial non-negative radius of this circle in pixels.
	 * @param paintFill
	 *            the initial {@code Paint} used to fill this circle (may be null).
	 * @param paintStroke
	 *            the initial {@code Paint} used to stroke this circle (may be null).
	 * @throws IllegalArgumentException
	 *             if the given {@code radius} is negative or {@link Float#NaN}.
	 */
	public FixedPixelCircle(LatLong latLong, float radius, Paint paintFill, Paint paintStroke) {
		super(latLong, radius, paintFill, paintStroke);
	}

	protected int getRadiusInPixels(double latitude, byte zoomLevel) {
		return (int)this.getRadius();
	}

	public boolean contains(Point center, Point point) {
		return (center.distance(point) < this.getRadius());
	}

}
