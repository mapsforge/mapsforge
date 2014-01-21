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

/**
 * Basic map viewer with a few overlays added.
 */
public class OverlayWithoutBaseMapViewer extends OverlayMapViewer {
	@Override
	protected void createLayers() {
		// not calling super here, so that base layers are not created
		// just add few overlays to an empty map
		addOverlayLayers(this.layerManagers.get(0).getLayers());
	}

	@Override
	protected void createTileCaches() {
		// do not need a tile cache here
		this.tileCache = null;
	}

	@Override
	protected void destroyTileCaches() {
		// do not have tile cache, do nothing
	}
}
