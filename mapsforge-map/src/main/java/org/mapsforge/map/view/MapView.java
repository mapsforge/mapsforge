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
package org.mapsforge.map.view;

import org.mapsforge.core.model.Dimension;
import org.mapsforge.map.controller.FrameBufferController;
import org.mapsforge.map.controller.LayerManagerController;
import org.mapsforge.map.controller.MapViewController;
import org.mapsforge.map.controller.input.TouchEventHandler;
import org.mapsforge.map.controller.input.TouchGestureDetector;
import org.mapsforge.map.model.Model;
import org.mapsforge.map.scalebar.MapScaleBar;
import org.mapsforge.map.viewinterfaces.MapViewInterface;

import android.content.Context;
import android.graphics.Canvas;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

public class MapView extends View implements MapViewInterface {
	private final FpsCounter fpsCounter = new FpsCounter();
	private final FrameBuffer frameBuffer;
	private final LayerManager layerManager;
	private final MapScaleBar mapScaleBar;
	private final Model model;
	private final TouchEventHandler touchEventHandler;

	public MapView(Context context) {
		super(context);

		this.model = new Model();

		this.frameBuffer = new FrameBuffer(this.model.frameBufferModel);
		new FrameBufferController(this.frameBuffer, this.model);

		this.layerManager = new LayerManager(this, this.model.mapViewPosition);
		this.layerManager.start();
		new LayerManagerController(this.layerManager, this.model);

		new MapViewController(this, this.model);

		ViewConfiguration viewConfiguration = ViewConfiguration.get(context);
		TouchGestureDetector touchGestureDetector = new TouchGestureDetector(this.model, viewConfiguration);
		this.touchEventHandler = new TouchEventHandler(this.model.mapViewPosition, viewConfiguration);
		this.touchEventHandler.addListener(touchGestureDetector);

		this.mapScaleBar = new MapScaleBar(this.model.mapViewPosition);
	}

	public void destroy() {
		this.layerManager.interrupt();
	}

	public FpsCounter getFpsCounter() {
		return this.fpsCounter;
	}

	@Override
	public FrameBuffer getFrameBuffer() {
		return this.frameBuffer;
	}

	public LayerManager getLayerManager() {
		return this.layerManager;
	}

	public MapScaleBar getMapScaleBar() {
		return this.mapScaleBar;
	}

	public Model getModel() {
		return this.model;
	}

	@Override
	public boolean onTouchEvent(MotionEvent motionEvent) {
		if (!isClickable()) {
			return false;
		}

		return this.touchEventHandler.onTouchEvent(motionEvent);
	}

	@Override
	public void repaint() {
		if (Thread.currentThread() == Looper.getMainLooper().getThread()) {
			invalidate();
		} else {
			postInvalidate();
		}
	}

	@Override
	protected void onDraw(Canvas canvas) {
		this.frameBuffer.draw(canvas);
		this.mapScaleBar.draw(canvas);
		this.fpsCounter.draw(canvas);
	}

	@Override
	protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
		this.model.mapViewModel.setDimension(new Dimension(width, height));
	}
}
