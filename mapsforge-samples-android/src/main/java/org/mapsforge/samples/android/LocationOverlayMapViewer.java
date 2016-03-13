/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
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

import android.annotation.TargetApi;
import android.graphics.drawable.Drawable;
import android.os.Build;

import org.mapsforge.core.graphics.Bitmap;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.samples.android.location.MyLocationOverlay;

/**
 * MapViewer that shows current position. In the data directory of the Samples
 * project is the file berlin.gpx that can be loaded in the Android Monitor to
 * simulate location data in the center of Berlin.
 */
public class LocationOverlayMapViewer extends DownloadLayerViewer {
    private MyLocationOverlay myLocationOverlay;

    @SuppressWarnings("deprecation")
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void createLayers() {
        super.createLayers();

        // a marker to show at the position
        Drawable drawable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ? getDrawable(R.drawable.marker_red) : getResources().getDrawable(R.drawable.marker_red);
        Bitmap bitmap = AndroidGraphicFactory.convertToBitmap(drawable);

        // create the overlay and tell it to follow the location
        this.myLocationOverlay = new MyLocationOverlay(this,
                this.mapView.getModel().mapViewPosition, bitmap);
        this.myLocationOverlay.setSnapToLocationEnabled(true);
        mapView.getLayerManager().getLayers().add(this.myLocationOverlay);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        myLocationOverlay.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onStart() {
        super.onStart();
        this.myLocationOverlay.enableMyLocation(true);
    }

    @Override
    protected void onStop() {
        myLocationOverlay.disableMyLocation();
        super.onStop();
    }
}
