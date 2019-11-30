/*
 * Copyright 2016 mapicke
 * Copyright 2016-2019 devemux86
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

import android.graphics.BitmapFactory;
import org.mapsforge.core.graphics.Bitmap;
import org.mapsforge.core.graphics.Color;
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.graphics.Style;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.map.android.graphics.AndroidBitmap;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.samples.android.group.ChildMarker;
import org.mapsforge.samples.android.group.GroupMarker;

/**
 * Group marker example.
 */
public class GroupMarkerExample extends DefaultTheme {

    private static final Paint BLACK = Utils.createPaint(AndroidGraphicFactory.INSTANCE.createColor(Color.BLACK), 0, Style.FILL);

    @Override
    protected void createLayers() {
        super.createLayers();

        BLACK.setTextSize(12 * this.mapView.getModel().displayModel.getScaleFactor());
        addGroupMarker();
    }

    private void addGroupMarker() {
        LatLong latLong = new LatLong(52.525582, 13.370061);
        Bitmap bitmap = new AndroidBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.marker_green));
        GroupMarker groupMarker = new GroupMarker(latLong, bitmap, 0, -bitmap.getHeight() / 2, mapView.getLayerManager().getLayers(), BLACK);
        for (int i = 0; i < 10.; i++) {
            groupMarker.getChildren().add(new ChildMarker(latLong, bitmap, 0, 0, BLACK));
        }
        mapView.addLayer(groupMarker);
    }
}
