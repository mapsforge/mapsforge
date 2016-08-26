/*
 * Copyright 2013-2014 Ludwig M Brinckmann
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
package org.mapsforge.samples.android;

/**
 * A map view that shows only the overlays, not base  map.
 */
public class OverlayWithoutBaseMapViewer extends OverlayMapViewer {
    @Override
    protected void createLayers() {
        // not calling super here, so that base layers are not created
        // just add few overlays to an empty map
        addOverlayLayers(mapView.getLayerManager().getLayers());
    }

    @Override
    protected void createTileCaches() {
        // we do not have tiles, so need no tile cache
    }
}
