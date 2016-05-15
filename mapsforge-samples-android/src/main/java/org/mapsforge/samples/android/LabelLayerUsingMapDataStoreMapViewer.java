/*
 * Copyright 2015-2016 Ludwig M Brinckmann
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

import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.android.util.AndroidUtil;
import org.mapsforge.map.layer.labels.LabelLayer;
import org.mapsforge.map.layer.labels.LabelStore;
import org.mapsforge.map.layer.labels.MapDataStoreLabelStore;
import org.mapsforge.map.layer.renderer.TileRendererLayer;

/**
 * A map viewer that draws the labels onto a single separate layer. The LabelLayer used in this example
 * retrieves the data from the MapDataStore for the visible tile area, no caching involved.
 */
public class LabelLayerUsingMapDataStoreMapViewer extends RenderTheme4 {
    @Override
    protected void createLayers() {
        TileRendererLayer tileRendererLayer = AndroidUtil.createTileRendererLayer(this.tileCaches.get(0),
                this.mapView.getModel().mapViewPosition, getMapFile(), getRenderTheme(), false, false, false);
        mapView.getLayerManager().getLayers().add(tileRendererLayer);
        MapDataStoreLabelStore labelStore = new MapDataStoreLabelStore(getMapFile(), tileRendererLayer.getRenderThemeFuture(),
                tileRendererLayer.getTextScale(), tileRendererLayer.getDisplayModel(), AndroidGraphicFactory.INSTANCE);
        LabelLayer labelLayer = createLabelLayer(labelStore);
        mapView.getLayerManager().getLayers().add(labelLayer);
    }

    protected LabelLayer createLabelLayer(LabelStore labelStore) {
        return new LabelLayer(AndroidGraphicFactory.INSTANCE, labelStore);
    }
}
