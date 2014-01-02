/*
 * Copyright © 2014 Ludwig M Brinckmann
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

import org.mapsforge.map.android.util.AndroidUtil;
import org.mapsforge.map.android.view.MapView;
import org.mapsforge.map.layer.cache.TileCache;

/**
 * Illustrates the ability to set different tile sizes and background colors to MapViews.
 */

public class DualMapViewerWithDifferentDisplayModels extends DualMapViewer {

	@Override
	protected void createMapViews() {
		super.createMapViews();
		mapViews.get(1).getModel().displayModel.setUserScaleFactor(0.3f);
		mapViews.get(1).getModel().displayModel.setBackgroundColor(0xffff0000); // red background

	}

	@Override
	protected TileCache createTileCache2() {
		int tileSize = this.mapViews.get(1).getModel().displayModel.getTileSize();
		return AndroidUtil.createTileCache(this, getPersistableId2(), tileSize, getScreenRatio2(), this.mapViews.get(1).getModel().frameBufferModel.getOverdrawFactor());
	}

	@Override
	protected void destroyTileCaches() {
		super.destroyTileCaches();
		this.tileCache2.destroy();
	}

}
