/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright © 2013-2014 Ludwig M Brinckmann
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
import org.mapsforge.map.android.util.AndroidUtil;
import org.mapsforge.map.android.view.MapView;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.layer.download.TileDownloadLayer;
import org.mapsforge.map.layer.download.tilesource.OpenStreetMapMapnik;

/**
 * An activity with two {@link MapView MapViews} tied to each other.
 */
public class DualMapnikMapViewer extends DualMapViewer {
	private TileDownloadLayer downloadLayer;
	private MapViewPositionObserver observer1;
	private MapViewPositionObserver observer2;

	@Override
	protected void createLayers2() {
		this.downloadLayer = new TileDownloadLayer(this.tileCache2,
				this.mapViewPositions.get(1), OpenStreetMapMapnik.INSTANCE,
				AndroidGraphicFactory.INSTANCE);
		this.layerManagers.get(1).getLayers().add(this.downloadLayer);
	}

	@Override
	protected void createMapViewPositions() {
		super.createMapViewPositions();
		// any position change in one view will be reflected in the other
		this.observer1 = new MapViewPositionObserver(
				this.mapViewPositions.get(0), this.mapViewPositions.get(1));
		this.observer2 = new MapViewPositionObserver(
				this.mapViewPositions.get(1), this.mapViewPositions.get(0));
	}

	@Override
	protected TileCache createTileCache2() {
		int tileSize = this.mapViews.get(1).getModel().displayModel
				.getTileSize();
		return AndroidUtil.createTileCache(this, getPersistableId2(), tileSize,
				getScreenRatio2(),
				this.mapViews.get(1).getModel().frameBufferModel
						.getOverdrawFactor());
	}

	@Override
	protected void destroyMapViewPositions() {
		super.destroyMapViewPositions();
		this.observer1.removeObserver();
		this.observer2.removeObserver();
	}

	@Override
	protected void destroyTileCaches() {
		super.destroyTileCaches();
		this.tileCache2.destroy();
	}

	@Override
	protected void onPause() {
		super.onPause();
		this.downloadLayer.onPause();
		this.mapViews.get(1).getModel().save(this.preferencesFacade);
		this.preferencesFacade.save();
	}

	@Override
	protected void onResume() {
		super.onResume();
		this.downloadLayer.onResume();
	}
}
