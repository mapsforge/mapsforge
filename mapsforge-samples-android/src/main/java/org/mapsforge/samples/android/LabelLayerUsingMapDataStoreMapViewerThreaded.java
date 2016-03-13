/*
 * Copyright 2016 Ludwig M Brinckmann
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
import org.mapsforge.map.layer.labels.LabelLayer;
import org.mapsforge.map.layer.labels.LabelStore;
import org.mapsforge.map.layer.labels.ThreadedLabelLayer;

/**
 * A map viewer that draws the labels onto a single separate layer. The LabelLayer used in this example
 * uses a separate thread to retrieve the data from the MapDataStore to be more responsive.
 */
public class LabelLayerUsingMapDataStoreMapViewerThreaded extends LabelLayerUsingMapDataStoreMapViewer {
    @Override
    protected LabelLayer createLabelLayer(LabelStore labelStore) {
        return new ThreadedLabelLayer(AndroidGraphicFactory.INSTANCE, labelStore);
    }
}
