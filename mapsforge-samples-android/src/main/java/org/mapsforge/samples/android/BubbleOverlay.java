/*
 * Copyright 2013-2014 Ludwig M Brinckmann
 * Copyright 2015-2018 devemux86
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
import android.graphics.Color;
import android.os.Build;
import android.view.Gravity;
import android.widget.TextView;

import org.mapsforge.core.graphics.Bitmap;
import org.mapsforge.map.layer.overlay.Marker;
import org.mapsforge.samples.android.dummy.DummyContent;

/**
 * Basic map viewer that shows bubbles with content at a few locations.
 */
public class BubbleOverlay extends DownloadLayerViewer {

    private Bitmap bubble;

    @SuppressWarnings("deprecation")
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void createLayers() {
        super.createLayers();

        // Bubble overlays
        for (DummyContent.DummyItem item : DummyContent.ITEMS) {
            TextView bubbleView = new TextView(this);
            Utils.setBackground(bubbleView, Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ? getDrawable(R.drawable.balloon_overlay_unfocused) : getResources().getDrawable(R.drawable.balloon_overlay_unfocused));
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
    protected void onDestroy() {
        bubble.decrementRefCount();
        super.onDestroy();
    }
}
