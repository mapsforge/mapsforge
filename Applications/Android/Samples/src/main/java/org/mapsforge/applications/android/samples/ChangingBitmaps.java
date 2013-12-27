/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
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

import android.graphics.drawable.Drawable;
import android.os.Handler;

import org.mapsforge.core.graphics.Bitmap;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.layer.overlay.Marker;


/**
 * A map viewer demonstrating changing bitmap drawables: every two seconds the view changes
 * the bitmap.
 */
public class ChangingBitmaps extends BasicMapViewerXml {

	@Override
	public void onResume() {
		super.onResume();
		final LatLong latLong = new LatLong(52.5, 13.4);
		final Drawable drawableGreen = getResources().getDrawable(R.drawable.marker_green);
		final Drawable drawableRed = getResources().getDrawable(R.drawable.marker_red);

		final Bitmap bitmapGreen = AndroidGraphicFactory.convertToBitmap(drawableGreen);
		final Bitmap bitmapRed = AndroidGraphicFactory.convertToBitmap(drawableRed);
		final Marker marker = new Marker(latLong, bitmapGreen, 0, -bitmapGreen.getHeight() / 2);
		layerManagers.get(0).getLayers().add(marker);

		final Handler handler = new Handler();
		handler.postDelayed(new Runnable() {
			boolean markerIsGreen = true;
			@Override
			public void run() {
				if (markerIsGreen) {
					// since we want to keep the green bitmap around, we have to increment
					// its ref count, otherwise it gets recycled automatically when it is
					// replaced with the other colour.
					bitmapGreen.incrementRefCount();
					marker.setBitmap(bitmapRed);
					markerIsGreen = false;
				} else {
					bitmapRed.incrementRefCount();
					marker.setBitmap(bitmapGreen);
					markerIsGreen = true;
				}
				layerManagers.get(0).redrawLayers();
				handler.postDelayed(this, 2000);
			}
		}, 2000);

	}
}
