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
import android.os.Bundle;
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

	final class BitmapChanger implements Runnable {
		@Override
		public void run() {
			if (current != null) {
				// since we want to keep the green bitmap around, we have to increment
				// its ref count, otherwise it gets recycled automatically when it is
				// replaced with the other colour.
				current.incrementRefCount();
			}
			if (current == bitmapGreen) {
				marker.setBitmap(bitmapRed);
				current = bitmapRed;
			} else {
				marker.setBitmap(bitmapGreen);
				current = bitmapGreen;
			}
			layerManagers.get(0).redrawLayers();
			handler.postDelayed(this, 2000);
		}
	}

	final Handler handler = new Handler();
	final LatLong latLong = new LatLong(52.5, 13.4);
	Bitmap bitmapGreen;
	Bitmap bitmapRed;
	Bitmap current;
	Marker marker;
	BitmapChanger bitmapChanger;

	@Override
	public void onCreate(Bundle sis) {
		super.onCreate(sis);
		Drawable drawableGreen = getResources().getDrawable(R.drawable.marker_green);
		Drawable drawableRed = getResources().getDrawable(R.drawable.marker_red);
		bitmapRed = AndroidGraphicFactory.convertToBitmap(drawableRed);
		bitmapGreen = AndroidGraphicFactory.convertToBitmap(drawableGreen);
		marker = new Marker(latLong, bitmapGreen, 0, -bitmapGreen.getHeight() / 2);
	}

	@Override
	public void createLayers() {
		super.createLayers();
		layerManagers.get(0).getLayers().add(marker);
		bitmapChanger = new BitmapChanger();
		handler.post(bitmapChanger);
	}

	@Override
	public void destroyLayers() {
		handler.removeCallbacks(bitmapChanger);
		super.destroyLayers();
		// we need to decrement the ref count for the bitmap that is being kept
		// stored and not in use, the other is automatically destroyed via the marker
		if (current == bitmapGreen) {
			bitmapRed.decrementRefCount();
		} else {
			bitmapGreen.decrementRefCount();
		}
	}

}
