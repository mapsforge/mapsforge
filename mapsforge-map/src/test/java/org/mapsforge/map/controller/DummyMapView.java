/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright 2015 devemux86
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

import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.Dimension;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.map.layer.Layer;
import org.mapsforge.map.layer.LayerManager;
import org.mapsforge.map.model.Model;
import org.mapsforge.map.scalebar.MapScaleBar;
import org.mapsforge.map.view.FpsCounter;
import org.mapsforge.map.view.FrameBuffer;
import org.mapsforge.map.view.MapView;

public class DummyMapView implements MapView {
	public int repaintCounter;

	@Override
	public void addLayer(Layer layer) {
		// no-op
	}

	@Override
	public void destroy() {
		// do nothing
	}

	@Override
	public void destroyAll() {
		// do nothing
	}

	@Override
	public BoundingBox getBoundingBox() {
		return null;
	}

	@Override
	public Dimension getDimension() {
		return null;
	}

	@Override
	public FpsCounter getFpsCounter() {
		return null;
	}

	@Override
	public FrameBuffer getFrameBuffer() {
		return null;
	}

	@Override
	public int getHeight() {
		return 0;
	}

	@Override
	public LayerManager getLayerManager() {
		return null;
	}

	@Override
	public MapScaleBar getMapScaleBar() {
		return null;
	}

	@Override
	public Model getModel() {
		return null;
	}

	@Override
	public int getWidth() {
		return 0;
	}

	@Override
	public void repaint() {
		++this.repaintCounter;
	}

	@Override
	public void setCenter(LatLong center) {
		// no-op
	}

	@Override
	public void setMapScaleBar(MapScaleBar mapScaleBar) {
		// no-op
	}

	@Override
	public void setZoomLevel(byte zoomLevel) {
		// no-op
	}
}
