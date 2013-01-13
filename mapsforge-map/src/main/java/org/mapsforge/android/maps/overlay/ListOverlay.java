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
package org.mapsforge.android.maps.overlay;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.Point;
import org.mapsforge.core.util.MercatorProjection;

import android.graphics.Canvas;

/**
 * A thread-safe {@link Overlay} implementation to display a list of {@link OverlayItem OverlayItems}.
 */
public class ListOverlay implements Overlay {
	private final List<OverlayItem> overlayItems = Collections.synchronizedList(new ArrayList<OverlayItem>());

	@Override
	public synchronized void draw(BoundingBox boundingBox, byte zoomLevel, Canvas canvas) {
		double canvasPixelLeft = MercatorProjection.longitudeToPixelX(boundingBox.minLongitude, zoomLevel);
		double canvasPixelTop = MercatorProjection.latitudeToPixelY(boundingBox.maxLatitude, zoomLevel);
		Point canvasPosition = new Point(canvasPixelLeft, canvasPixelTop);

		synchronized (this.overlayItems) {
			int numberOfOverlayItems = this.overlayItems.size();
			for (int i = 0; i < numberOfOverlayItems; ++i) {
				OverlayItem overlayItem = this.overlayItems.get(i);
				overlayItem.draw(boundingBox, zoomLevel, canvas, canvasPosition);
			}
		}
	}

	/**
	 * @return a synchronized (thread-safe) list of all {@link OverlayItem OverlayItems} on this
	 *         {@code ArrayListOverlay}. Manual synchronization on this list is necessary when iterating over it.
	 */
	public List<OverlayItem> getOverlayItems() {
		synchronized (this.overlayItems) {
			return this.overlayItems;
		}
	}
}
