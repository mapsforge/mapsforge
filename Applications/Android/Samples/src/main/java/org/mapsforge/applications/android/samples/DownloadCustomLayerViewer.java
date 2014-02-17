/*
 * Copyright © 2014 devemux86
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
import org.mapsforge.map.layer.download.TileDownloadLayer;
import org.mapsforge.map.layer.download.tilesource.OnlineTileSource;

/**
 * Shows how to use a custom tile download layer.
 */
public class DownloadCustomLayerViewer extends BasicMapViewerXml {
	private TileDownloadLayer downloadLayer;

	@Override
	public void onPause() {
		super.onPause();
		this.downloadLayer.onPause();
	}

	@Override
	public void onResume() {
		super.onResume();
		this.downloadLayer.onResume();
	}

	@Override
	protected void createLayers() {
		OnlineTileSource onlineTileSource = new OnlineTileSource(new String[]{
				"otile1.mqcdn.com", "otile2.mqcdn.com", "otile3.mqcdn.com",
				"otile4.mqcdn.com"}, 80);
		onlineTileSource.setName("MapQuest").setAlpha(false)
				.setBaseUrl("/tiles/1.0.0/map/").setExtension("png")
				.setParallelRequestsLimit(8).setProtocol("http")
				.setTileSize(256).setZoomLevelMax((byte) 18)
				.setZoomLevelMin((byte) 0);
		this.downloadLayer = new TileDownloadLayer(this.tileCache,
				this.mapViewPositions.get(0), onlineTileSource,
				AndroidGraphicFactory.INSTANCE);
		this.layerManagers.get(0).getLayers().add(this.downloadLayer);
	}

}
