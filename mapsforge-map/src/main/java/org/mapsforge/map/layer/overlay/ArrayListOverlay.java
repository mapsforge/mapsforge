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
package org.mapsforge.map.layer.overlay;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.MapPosition;
import org.mapsforge.core.model.Point;
import org.mapsforge.map.layer.Layer;
import org.mapsforge.map.layer.LayerUtil;

import android.graphics.Canvas;

/**
 * A thread-safe {@link Layer} implementation to display a list of {@link OverlayItem OverlayItems}.
 */
public class ArrayListOverlay extends Layer {
	private final List<OverlayItem> overlayItems = new CopyOnWriteArrayList<OverlayItem>();

	@Override
	public void draw(BoundingBox boundingBox, MapPosition mapPosition, Canvas canvas) {
		Point canvasPosition = LayerUtil.getCanvasPosition(mapPosition, canvas.getWidth(), canvas.getHeight());

		for (OverlayItem overlayItem : this.overlayItems) {
			overlayItem.draw(boundingBox, mapPosition.zoomLevel, canvas, canvasPosition);
		}
	}

	/**
	 * @return a thread-safe list of all {@link OverlayItem OverlayItems} on this {@code ArrayListOverlay}.
	 */
	public List<OverlayItem> getOverlayItems() {
		return this.overlayItems;
	}
}
