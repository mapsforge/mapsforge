/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
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
package org.mapsforge.applications.android.samples;

import org.mapsforge.core.graphics.Bitmap;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.layer.overlay.Marker;

import android.annotation.TargetApi;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;

/**
 * A map viewer demonstrating changing bitmap drawables: every two seconds the
 * view changes the bitmap.
 */
public class ChangingBitmaps extends RenderTheme4 {

	final class BitmapChanger implements Runnable {
		@Override
		public void run() {
			if (current != null) {
				// since we want to keep the green bitmap around, we have to increment
				// its ref count, otherwise it gets recycled automatically when it is
				// replaced with the other colour.
				current.incrementRefCount();
			}
			if (bitmapGreen.equals(current)) {
				marker.setBitmap(bitmapRed);
				current = bitmapRed;
			} else {
				marker.setBitmap(bitmapGreen);
				current = bitmapGreen;
			}
			redrawLayers();
			handler.postDelayed(this, 2000);
		}
	}

	BitmapChanger bitmapChanger;
	Bitmap bitmapGreen;
	Bitmap bitmapRed;
	Bitmap current;
	final Handler handler = new Handler();
	final LatLong latLong = new LatLong(52.5, 13.4);
	Marker marker;

	@Override
	public void createLayers() {
		super.createLayers();
		mapView.getLayerManager().getLayers().add(marker);
		bitmapChanger = new BitmapChanger();
		handler.post(bitmapChanger);
	}

	@Override
	public void destroyLayers() {
		handler.removeCallbacks(bitmapChanger);
		// we need to increment the ref count here as otherwise the bitmap gets
		// destroyed, but we might need to reuse it when this is only part of
		// a pause/resume cycle.
		current.incrementRefCount();
		super.destroyLayers();
	}

	@SuppressWarnings("deprecation")
	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	@Override
	public void onCreate(Bundle sis) {
		Drawable drawableWhite = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ? getDrawable(R.drawable.marker_white) : getResources().getDrawable(R.drawable.marker_white);
		Paint paint = new Paint();
		paint.setAntiAlias(true);
		paint.setColorFilter(new PorterDuffColorFilter(Color.GREEN, PorterDuff.Mode.MULTIPLY));
		bitmapGreen = AndroidGraphicFactory.convertToBitmap(drawableWhite, paint);
		paint.setColorFilter(new PorterDuffColorFilter(Color.RED, PorterDuff.Mode.MULTIPLY));
		bitmapRed = AndroidGraphicFactory.convertToBitmap(drawableWhite, paint);
		marker = new Marker(latLong, bitmapGreen, 0, -bitmapGreen.getHeight() / 2);
		super.onCreate(sis);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		bitmapRed.decrementRefCount();
		bitmapGreen.decrementRefCount();
	}
}
