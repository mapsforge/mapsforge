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
package org.mapsforge.applications.android.samples;

import org.mapsforge.core.graphics.Color;
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.graphics.Style;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.layer.overlay.Polyline;
import org.mapsforge.map.util.MapPositionUtil;

/**
 * An activity with a smaller mapview giving the position of the larger map view
 */
public class DualOverviewMapViewer extends DualMapViewer {
	private MapViewPositionObserver observer;

	@Override
	protected void createMapViewPositions() {
		super.createMapViewPositions();

		this.mapView2.getModel().mapViewPosition.setZoomLevel((byte) 12);

		this.observer = new MapViewPositionObserver(this.mapView.getModel().mapViewPosition,
				this.mapView2.getModel().mapViewPosition) {
			Polyline lastLine;

			@Override
			protected void setCenter() {
				super.setCenter();
				BoundingBox bbox = MapPositionUtil.getBoundingBox(
						DualOverviewMapViewer.this.mapView.getModel().mapViewPosition.getMapPosition(),
						DualOverviewMapViewer.this.mapView.getDimension());
				Paint paintStroke = Utils.createPaint(AndroidGraphicFactory.INSTANCE.createColor(Color.RED), 2,
						Style.STROKE);
				Polyline polygon = new Polyline(paintStroke, AndroidGraphicFactory.INSTANCE);
				polygon.getLatLongs().add(new LatLong(bbox.minLatitude, bbox.minLongitude));
				polygon.getLatLongs().add(new LatLong(bbox.minLatitude, bbox.maxLongitude));
				polygon.getLatLongs().add(new LatLong(bbox.maxLatitude, bbox.maxLongitude));
				polygon.getLatLongs().add(new LatLong(bbox.maxLatitude, bbox.minLongitude));
				polygon.getLatLongs().add(new LatLong(bbox.minLatitude, bbox.minLongitude));
				if (this.lastLine != null) {
					DualOverviewMapViewer.this.mapView2.getLayerManager().getLayers().remove(this.lastLine);
				}
				DualOverviewMapViewer.this.mapView2.getLayerManager().getLayers().add(polygon);
				this.lastLine = polygon;
			}

			@Override
			protected void setZoom() {
				// do not change zoom, the overview stays zoomed out
			}
		};
	}

	@Override
	protected int getLayoutId() {
		// provides a layout with two mapViews
		return R.layout.dualoverviewmapviewer;
	}

	@Override
	protected void destroyMapViewPositions() {
		super.destroyMapViewPositions();
		this.observer.removeObserver();
	}
}
