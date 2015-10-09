/*
 * Copyright 2015 Andreas Schildbach
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

import org.mapsforge.applications.android.samples.dummy.DummyContent;
import org.mapsforge.map.android.view.MapView;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

/**
 * Basic map viewer that shows <code>View</code>s with content at a few locations.
 */
public class ViewOverlayViewer extends RenderTheme4 {

	@SuppressLint("InflateParams")
	@Override
	protected void createLayers() {
		super.createLayers();

		// View overlays
		final LayoutInflater inflater = LayoutInflater.from(this);
		for (final DummyContent.DummyItem item : DummyContent.ITEMS) {
			final Button button = (Button) inflater.inflate(R.layout.pointer_bubble, null);
			button.setText(item.text);
			button.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					Toast.makeText(ViewOverlayViewer.this, item.text, Toast.LENGTH_SHORT).show();
				}
			});
			this.mapView.addView(button, new MapView.LayoutParams(MapView.LayoutParams.WRAP_CONTENT, MapView.LayoutParams.WRAP_CONTENT,
					item.location, MapView.LayoutParams.Alignment.BOTTOM_CENTER));
		}
	}

	@Override
	protected void createMapViews() {
		super.createMapViews();
		this.mapView.getModel().mapViewPosition.setCenter(DummyContent.ITEMS.get(1).location);
	}
}
