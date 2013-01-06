/*
 * Copyright 2010, 2011, 2012 mapsforge.org
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
package org.mapsforge.map;

import org.mapsforge.map.input.TouchEventHandler;
import org.mapsforge.map.input.TouchGestureDetector;
import org.mapsforge.map.model.MapViewPosition;
import org.mapsforge.map.scalebar.MapScaleBar;

import android.content.Context;
import android.graphics.Canvas;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

public class MapView extends View {
	private final FpsCounter fpsCounter = new FpsCounter();
	private final FrameBuffer frameBuffer = new FrameBuffer();
	private final LayerManager layerManager = new LayerManager(this);
	private final MapScaleBar mapScaleBar;
	private final MapViewPosition mapViewPosition = new MapViewPosition();
	private final TouchEventHandler touchEventHandler;

	public MapView(Context context) {
		super(context);

		this.layerManager.start();

		ViewConfiguration viewConfiguration = ViewConfiguration.get(context);
		TouchGestureDetector touchGestureDetector = new TouchGestureDetector(this.mapViewPosition, viewConfiguration);
		this.touchEventHandler = new TouchEventHandler(this.mapViewPosition, viewConfiguration);
		this.touchEventHandler.addListener(touchGestureDetector);

		this.mapViewPosition.addObserver(new MapViewPositionObserver(this));

		this.mapScaleBar = new MapScaleBar(this.mapViewPosition);
	}

	public void destroy() {
		this.layerManager.interrupt();
	}

	public FpsCounter getFpsCounter() {
		return this.fpsCounter;
	}

	/**
	 * @return the FrameBuffer used in this MapView.
	 */
	public FrameBuffer getFrameBuffer() {
		return this.frameBuffer;
	}

	public LayerManager getLayerManager() {
		return this.layerManager;
	}

	public MapScaleBar getMapScaleBar() {
		return this.mapScaleBar;
	}

	public MapViewPosition getMapViewPosition() {
		return this.mapViewPosition;
	}

	/**
	 * Calls either {@link #invalidate()} or {@link #postInvalidate()}, depending on the current thread.
	 */
	public void invalidateOnUiThread() {
		if (Thread.currentThread() == Looper.getMainLooper().getThread()) {
			invalidate();
		} else {
			postInvalidate();
		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent motionEvent) {
		if (!isClickable()) {
			return false;
		}

		return this.touchEventHandler.onTouchEvent(motionEvent);
	}

	@Override
	protected void onDraw(Canvas canvas) {
		this.frameBuffer.draw(canvas);
		this.mapScaleBar.draw(canvas);
		this.fpsCounter.draw(canvas);
	}

	@Override
	protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
		this.frameBuffer.changeSize(width, height);
		if (width > 0 && height > 0) {
			this.layerManager.redrawLayers();
		}
	}
}
