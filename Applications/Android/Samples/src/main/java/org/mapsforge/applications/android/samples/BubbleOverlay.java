/*
 * Copyright © 2013-2014 Ludwig M Brinckmann
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
import org.mapsforge.core.graphics.Bitmap;
import org.mapsforge.map.layer.overlay.Marker;

import android.graphics.Color;
import android.view.Gravity;
import android.widget.TextView;

/**
 * Basic map viewer that shows bubbles with content at a few locations.
 */
public class BubbleOverlay extends RenderTheme4 {

	private Bitmap bubble;

	@Override
	protected void createLayers() {
		super.createLayers();
		for (DummyContent.DummyItem item : DummyContent.ITEMS) {
			TextView bubbleView = new TextView(this);
			Utils.setBackground(bubbleView, getResources().getDrawable(R.drawable.balloon_overlay_unfocused));
			bubbleView.setGravity(Gravity.CENTER);
			bubbleView.setMaxEms(20);
			bubbleView.setTextSize(15);
			bubbleView.setTextColor(Color.BLACK);
			bubbleView.setText(item.text);
			bubble = Utils.viewToBitmap(this, bubbleView);
			bubble.incrementRefCount();
			this.mapView.getLayerManager().getLayers().add(new Marker(item.location, bubble, 0, -bubble.getHeight() / 2));
		}
	}

	@Override
	protected void createMapViews() {
		super.createMapViews();
		this.mapView.getModel().mapViewPosition.setCenter(DummyContent.ITEMS.get(1).location);
	}

	@Override
	protected void destroyLayers() {
		bubble.decrementRefCount();
	}

	@Override
	protected void onStart() {
		super.onStart();
		this.mapView.getModel().mapViewPosition.setCenter(DummyContent.ITEMS.get(1).location);
	}
}
