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
package org.mapsforge.map.controller;

import org.mapsforge.core.model.Dimension;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.MapPosition;
import org.mapsforge.core.model.Point;
import org.mapsforge.core.util.MercatorProjection;
import org.mapsforge.map.model.Model;
import org.mapsforge.map.model.common.Observer;
import org.mapsforge.map.view.FrameBuffer;

public final class FrameBufferController implements Observer {
	public static FrameBufferController create(FrameBuffer frameBuffer, Model model) {
		FrameBufferController frameBufferController = new FrameBufferController(frameBuffer, model);

		model.frameBufferModel.addObserver(frameBufferController);
		model.mapViewDimension.addObserver(frameBufferController);
		model.mapViewPosition.addObserver(frameBufferController);

		return frameBufferController;
	}

	private static Dimension calculateFrameBufferDimension(Dimension mapViewDimension, double overdrawFactor) {
		int width = (int) (mapViewDimension.width * overdrawFactor);
		int height = (int) (mapViewDimension.height * overdrawFactor);
		return new Dimension(width, height);
	}

	private static Point getPixel(LatLong latLong, byte zoomLevel) {
		double pixelX = MercatorProjection.longitudeToPixelX(latLong.longitude, zoomLevel);
		double pixelY = MercatorProjection.latitudeToPixelY(latLong.latitude, zoomLevel);
		return new Point(pixelX, pixelY);
	}

	private final FrameBuffer frameBuffer;
	private Dimension lastMapViewDimension;
	private double lastOverdrawFactor;
	private final Model model;

	private FrameBufferController(FrameBuffer frameBuffer, Model model) {
		this.frameBuffer = frameBuffer;
		this.model = model;
	}

	@Override
	public void onChange() {
		Dimension mapViewDimension = this.model.mapViewDimension.getDimension();
		if (mapViewDimension != null) {
			double overdrawFactor = this.model.frameBufferModel.getOverdrawFactor();
			if (dimensionChangeNeeded(mapViewDimension, overdrawFactor)) {
				this.frameBuffer.setDimension(calculateFrameBufferDimension(mapViewDimension, overdrawFactor));
				this.lastMapViewDimension = mapViewDimension;
				this.lastOverdrawFactor = overdrawFactor;
			}

			synchronized (this.frameBuffer) {
				MapPosition mapPositionFrameBuffer = this.model.frameBufferModel.getMapPosition();
				if (mapPositionFrameBuffer != null) {
					adjustFrameBufferMatrix(mapPositionFrameBuffer, mapViewDimension);
				}
			}
		}
	}

	private void adjustFrameBufferMatrix(MapPosition mapPositionFrameBuffer, Dimension mapViewDimension) {
		MapPosition mapPosition = this.model.mapViewPosition.getMapPosition();

		Point pointFrameBuffer = getPixel(mapPositionFrameBuffer.latLong, mapPosition.zoomLevel);
		Point pointMapPosition = getPixel(mapPosition.latLong, mapPosition.zoomLevel);
		float diffX = (float) (pointFrameBuffer.x - pointMapPosition.x);
		float diffY = (float) (pointFrameBuffer.y - pointMapPosition.y);

		int zoomLevelDiff = mapPosition.zoomLevel - mapPositionFrameBuffer.zoomLevel;
		float scaleFactor = (float) Math.pow(2, zoomLevelDiff);

		this.frameBuffer.adjustMatrix(diffX, diffY, scaleFactor, mapViewDimension);
	}

	private boolean dimensionChangeNeeded(Dimension mapViewDimension, double overdrawFactor) {
		if (Double.compare(overdrawFactor, this.lastOverdrawFactor) != 0) {
			return true;
		} else if (!mapViewDimension.equals(this.lastMapViewDimension)) {
			return true;
		}
		return false;
	}
}
