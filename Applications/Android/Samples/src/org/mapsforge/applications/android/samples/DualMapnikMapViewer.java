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

import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.android.view.MapView;
import org.mapsforge.map.layer.LayerManager;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.layer.download.TileDownloadLayer;
import org.mapsforge.map.layer.download.tilesource.OpenStreetMapMapnik;
import org.mapsforge.map.model.MapViewPosition;

/**
 * An activity with two {@link MapView MapViews} tied to each other.
 */
public class DualMapnikMapViewer extends DualMapViewer {
	private TileDownloadLayer downloadLayer;
	private MapViewPositionObserver observer1;
	private MapViewPositionObserver observer2;

	@Override
	protected void addSecondMapLayer(LayerManager layerManager, TileCache tileCache, MapViewPosition mapViewPosition) {
		this.downloadLayer = new TileDownloadLayer(tileCache, mapViewPosition, OpenStreetMapMapnik.INSTANCE,
				AndroidGraphicFactory.INSTANCE);
		layerManager.getLayers().add(this.downloadLayer);
	}

	@Override
	protected int getLayoutId() {
		// provides a layout with two mapViews
		return R.layout.dualmapviewer;
	}

	@Override
	protected void init() {
		super.init();

		// any position change in one view will be reflected in the other
		this.observer1 = new MapViewPositionObserver(this.mapView.getModel().mapViewPosition,
				this.mapView2.getModel().mapViewPosition);
		this.observer2 = new MapViewPositionObserver(this.mapView2.getModel().mapViewPosition,
				this.mapView.getModel().mapViewPosition);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		this.observer1.removeObserver();
		this.observer2.removeObserver();
	}

	@Override
	protected void onPause() {
		super.onPause();
		this.mapView2.getModel().save(this.preferencesFacade);
		this.preferencesFacade.save();
	}

	@Override
	protected void onResume() {
		super.onResume();
		this.downloadLayer.start();
	}
}
