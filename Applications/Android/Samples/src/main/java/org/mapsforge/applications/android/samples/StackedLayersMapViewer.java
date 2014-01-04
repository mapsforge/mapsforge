/*
 * Copyright 2013-2014 Ludwig M Brinckmann
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

import java.io.IOException;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.rendertheme.XmlRenderTheme;
import org.mapsforge.map.android.util.AndroidUtil;
import org.mapsforge.map.android.rendertheme.AssetsRenderTheme;
import android.util.Log;

/**
 * An activity with two tile renderer layers stacked on top of each other using a partially transparent render theme to
 * show the lower layer. This will show as buildings on top of labels and other stuff, so the display is wrong, but
 * that is intentional.
 */

public class StackedLayersMapViewer extends RenderThemeMapViewer {

	protected TileCache tileCache2;

	@Override
	protected void createLayers() {
		super.createLayers();
		try {

			XmlRenderTheme secondRenderTheme = new AssetsRenderTheme(this, "", "renderthemes/onlybuildings.xml");
			this.layerManagers
					.get(0)
					.getLayers()
					.add(Utils.createTileRendererLayer(this.tileCache2, this.mapViewPositions.get(0),
							getMapFile(), secondRenderTheme, true));

		} catch (IOException e) {
			Log.e("ERROR", "Rendertheme not found");
		}

	}

	@Override
	protected void createTileCaches() {
		super.createTileCaches();
		this.tileCache2 = AndroidUtil.createTileCache(this, getPersistableId2(),
				this.mapViews.get(0).getModel().displayModel.getTileSize(),
				this.getScreenRatio(),
				this.mapViews.get(0).getModel().frameBufferModel.getOverdrawFactor());
	}

	protected String getPersistableId2() {
		return this.getPersistableId() + "-2";
	}

	@Override
	protected void destroyTileCaches() {
		super.destroyTileCaches();
		this.tileCache2.destroy();
	}

}
