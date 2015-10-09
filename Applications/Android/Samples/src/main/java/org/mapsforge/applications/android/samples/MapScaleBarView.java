/*
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

import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.model.common.Observer;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;

public class MapScaleBarView extends View implements Observer {

	private MapScaleBarImpl mapScaleBar;

	public MapScaleBarView(Context context) {
		this(context, null);
	}

	public MapScaleBarView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	@Override
	public void onChange() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			if (isHardwareAccelerated()) {
				postInvalidate();
			}
		}
	}

	@Override
	protected void onDraw(Canvas canvas) {
		org.mapsforge.core.graphics.Canvas graphicContext = AndroidGraphicFactory.createGraphicContext(canvas);
		mapScaleBar.draw(graphicContext);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		setMeasuredDimension(mapScaleBar.getMapScaleBitmap().getWidth(), mapScaleBar.getMapScaleBitmap().getHeight());
	}

	public void setMapScaleBar(MapScaleBarImpl mapScaleBar) {
		this.mapScaleBar = mapScaleBar;
	}
}
