/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright 2014 Ludwig M Brinckmann
 * Copyright 2014, 2015 devemux86
 * Copyright 2015 Andreas Schildbach
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
package org.mapsforge.map.android.view;

import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.Dimension;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Point;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.android.input.MapZoomControls;
import org.mapsforge.map.android.input.ScaleListener;
import org.mapsforge.map.android.input.TouchEventHandler;
import org.mapsforge.map.android.input.TouchGestureDetector;
import org.mapsforge.map.controller.FrameBufferController;
import org.mapsforge.map.controller.LayerManagerController;
import org.mapsforge.map.controller.MapViewController;
import org.mapsforge.map.layer.Layer;
import org.mapsforge.map.layer.LayerManager;
import org.mapsforge.map.layer.TileLayer;
import org.mapsforge.map.layer.labels.LabelStore;
import org.mapsforge.map.layer.renderer.TileRendererLayer;
import org.mapsforge.map.model.Model;
import org.mapsforge.map.model.common.Observer;
import org.mapsforge.map.scalebar.DefaultMapScaleBar;
import org.mapsforge.map.scalebar.MapScaleBar;
import org.mapsforge.map.util.MapPositionUtil;
import org.mapsforge.map.util.MapViewProjection;
import org.mapsforge.map.view.FpsCounter;
import org.mapsforge.map.view.FrameBuffer;

import android.content.Context;
import android.graphics.Canvas;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;

public class MapView extends ViewGroup implements org.mapsforge.map.view.MapView, Observer {

	private static final GraphicFactory GRAPHIC_FACTORY = AndroidGraphicFactory.INSTANCE;

	private final FpsCounter fpsCounter;
	private final FrameBuffer frameBuffer;
	private final FrameBufferController frameBufferController;
	private GestureDetector gestureDetector;
	private final LayerManager layerManager;
	private MapScaleBar mapScaleBar;
	private final MapZoomControls mapZoomControls;
	private final Model model;
	private final TouchEventHandler touchEventHandler;
	private final MapViewProjection projection;
	private final Handler handler = new Handler();

	public MapView(Context context) {
		this(context, null);
	}

	public MapView(Context context, AttributeSet attributeSet) {
		super(context, attributeSet);

		setDescendantFocusability(FOCUS_BLOCK_DESCENDANTS);
		setWillNotDraw(false);

		this.model = new Model();

		this.fpsCounter = new FpsCounter(GRAPHIC_FACTORY, this.model.displayModel);
		this.frameBuffer = new FrameBuffer(this.model.frameBufferModel, this.model.displayModel, GRAPHIC_FACTORY);
		this.frameBufferController = FrameBufferController.create(this.frameBuffer, this.model);

		this.layerManager = new LayerManager(this, this.model.mapViewPosition, GRAPHIC_FACTORY);
		this.layerManager.start();
		LayerManagerController.create(this.layerManager, this.model);

		MapViewController.create(this, this.model);

		ViewConfiguration viewConfiguration = ViewConfiguration.get(context);
		ScaleGestureDetector sgd = new ScaleGestureDetector(context, new ScaleListener(this));
		TouchGestureDetector touchGestureDetector = new TouchGestureDetector(this, viewConfiguration);
		this.touchEventHandler = new TouchEventHandler(this, viewConfiguration, sgd);
		this.touchEventHandler.addListener(touchGestureDetector);
		this.mapZoomControls = new MapZoomControls(context, this);
		this.addView(this.mapZoomControls, new ViewGroup.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
		this.mapScaleBar = new DefaultMapScaleBar(this.model.mapViewPosition, this.model.mapViewDimension,
				GRAPHIC_FACTORY, this.model.displayModel);
		this.projection = new MapViewProjection(this);

		model.mapViewPosition.addObserver(this);
	}

	@Override
	public void addLayer(Layer layer) {
		this.layerManager.getLayers().add(layer);
	}

	/**
	 * Clear map view.
	 */
	@Override
	public void destroy() {
		this.layerManager.interrupt();
		this.frameBufferController.destroy();
		this.frameBuffer.destroy();
		if (this.mapScaleBar != null) {
			this.mapScaleBar.destroy();
		}
		this.getModel().mapViewPosition.destroy();
	}

	/**
	 * Clear all map view elements.<br/>
	 * i.e. layers, tile cache, label store, map view, resources, etc.
	 */
	@Override
	public void destroyAll() {
		for (Layer layer : this.layerManager.getLayers()) {
			this.layerManager.getLayers().remove(layer);
			layer.onDestroy();
			if (layer instanceof TileLayer) {
				((TileLayer<?>) layer).getTileCache().destroy();
			}
			if (layer instanceof TileRendererLayer) {
				LabelStore labelStore = ((TileRendererLayer) layer).getLabelStore();
				if (labelStore != null) {
					labelStore.clear();
				}
			}
		}
		destroy();
		AndroidGraphicFactory.clearResourceMemoryCache();
	}

	@Override
	public BoundingBox getBoundingBox() {
		return MapPositionUtil.getBoundingBox(this.model.mapViewPosition.getMapPosition(),
				getDimension(), this.model.displayModel.getTileSize());
	}

	@Override
	public Dimension getDimension() {
		return new Dimension(getWidth(), getHeight());
	}

	@Override
	public FpsCounter getFpsCounter() {
		return this.fpsCounter;
	}

	@Override
	public FrameBuffer getFrameBuffer() {
		return this.frameBuffer;
	}

	@Override
	public LayerManager getLayerManager() {
		return this.layerManager;
	}

	@Override
	public MapScaleBar getMapScaleBar() {
		return this.mapScaleBar;
	}

	/**
	 * @return the zoom controls instance which is used in this MapView.
	 */
	public MapZoomControls getMapZoomControls() {
		return this.mapZoomControls;
	}

	@Override
	public Model getModel() {
		return this.model;
	}

	@Override
	protected void onDraw(Canvas androidCanvas) {
		org.mapsforge.core.graphics.Canvas graphicContext = AndroidGraphicFactory.createGraphicContext(androidCanvas);
		this.frameBuffer.draw(graphicContext);
		if (this.mapScaleBar != null) {
			this.mapScaleBar.draw(graphicContext);
		}
		this.fpsCounter.draw(graphicContext);
		graphicContext.destroy();
	}

	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		if (this.mapZoomControls.getVisibility() == View.VISIBLE) {
			final int childGravity = this.mapZoomControls.getZoomControlsGravity();
			final int childWidth = this.mapZoomControls.getMeasuredWidth();
			final int childHeight = this.mapZoomControls.getMeasuredHeight();

			final int childLeft;
			switch (childGravity & Gravity.HORIZONTAL_GRAVITY_MASK) {
				case Gravity.LEFT:
					childLeft = left;
					break;
				case Gravity.CENTER_HORIZONTAL:
					childLeft = left + (right - left - childWidth) / 2;
					break;
				case Gravity.RIGHT:
				default:
					childLeft = right - childWidth;
					break;
			}

			final int childTop;
			switch (childGravity & Gravity.VERTICAL_GRAVITY_MASK) {
				case Gravity.TOP:
					childTop = top;
					break;
				case Gravity.CENTER_VERTICAL:
					childTop = top + (bottom - top - childHeight) / 2;
					break;
				case Gravity.BOTTOM:
				default:
					childTop = bottom - childHeight;
					break;
			}

			this.mapZoomControls.layout(childLeft, childTop, childLeft + childWidth, childTop + childHeight);
		}

		final int count = getChildCount();
		for (int i = 0; i < count; i++) {
			final View child = getChildAt(i);
			if (child.getVisibility() != View.GONE && checkLayoutParams(child.getLayoutParams())) {
				final LayoutParams params = (LayoutParams) child.getLayoutParams();
				final int childHeight = child.getMeasuredHeight();
				final int childWidth = child.getMeasuredWidth();
				final Point p = projection.toPixels(params.latLong);
				if (p != null) {
					int childLeft = (int) Math.round(p.x) + getPaddingLeft();
					int childTop = (int) Math.round(p.y) + getPaddingTop();
					switch (params.align) {
						case TOP_LEFT:
							break;
						case TOP_CENTER:
							childLeft -= childWidth / 2;
							break;
						case TOP_RIGHT:
							childLeft -= childWidth;
							break;
						case CENTER_LEFT:
							childTop -= childHeight / 2;
							break;
						case CENTER:
							childLeft -= childWidth / 2;
							childTop -= childHeight / 2;
							break;
						case CENTER_RIGHT:
							childLeft -= childWidth;
							childTop -= childHeight / 2;
							break;
						case BOTTOM_LEFT:
							childTop -= childHeight;
							break;
						case BOTTOM_CENTER:
							childLeft -= childWidth / 2;
							childTop -= childHeight;
							break;
						case BOTTOM_RIGHT:
							childLeft -= childWidth;
							childTop -= childHeight;
							break;
					}
					child.layout(childLeft, childTop, childLeft + childWidth, childTop + childHeight);
				}
			}
		}
	}

	@Override
	protected final void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		measureChildren(widthMeasureSpec, heightMeasureSpec);
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
	}

	@Override
	protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
		this.model.mapViewDimension.setDimension(new Dimension(width, height));
	}

	@Override
	public boolean onTouchEvent(MotionEvent motionEvent) {
		if (!isClickable()) {
			return false;
		}
		this.mapZoomControls.onMapViewTouchEvent(motionEvent);
		if (this.gestureDetector != null && this.gestureDetector.onTouchEvent(motionEvent)) {
			return true;
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

	/**
	 * Sets the visibility of the zoom controls.
	 * 
	 * @param showZoomControls
	 *            true if the zoom controls should be visible, false otherwise.
	 */
	public void setBuiltInZoomControls(boolean showZoomControls) {
		this.mapZoomControls.setShowMapZoomControls(showZoomControls);
	}

	@Override
	public void setCenter(LatLong center) {
		this.model.mapViewPosition.setCenter(center);
	}

	public void setGestureDetector(GestureDetector gestureDetector) {
		this.gestureDetector = gestureDetector;
	}

	@Override
	public void setMapScaleBar(MapScaleBar mapScaleBar) {
		if (this.mapScaleBar != null) {
			this.mapScaleBar.destroy();
		}
		this.mapScaleBar = mapScaleBar;
	}

	@Override
	public void setZoomLevel(byte zoomLevel) {
		this.model.mapViewPosition.setZoomLevel(zoomLevel);
	}

	@Override
	protected ViewGroup.LayoutParams generateDefaultLayoutParams() {
		return new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, null,
				LayoutParams.Align.BOTTOM_CENTER);
	}

	@Override
	public ViewGroup.LayoutParams generateLayoutParams(AttributeSet attrs) {
		return new LayoutParams(getContext(), attrs);
	}

	@Override
	protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
		return new LayoutParams(p);
	}

	@Override
	protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
		return p instanceof LayoutParams;
	}

	@Override
	public void onChange() {
		handler.post(new Runnable() {
			@Override
			public void run() {
				requestLayout();
			}
		});
	}

	/**
	 * Used by views to tell MapView how they want to be laid out.
	 */
	public static class LayoutParams extends ViewGroup.LayoutParams {

		/**
		 * Alignment requested by a view.
		 */
		public enum Align {
			/**
			 * Location is at the top left the view.
			 */
			TOP_LEFT,

			/**
			 * Location is centered at the top of the view.
			 */
			TOP_CENTER,

			/**
			 * Location is at the top right the view.
			 */
			TOP_RIGHT,

			/**
			 * Location is at the center left the view.
			 */
			CENTER_LEFT,

			/**
			 * Location is centered at the center of the view.
			 */
			CENTER,

			/**
			 * Location is at the center right the view.
			 */
			CENTER_RIGHT,

			/**
			 * Location is at the bottom left of the view.
			 */
			BOTTOM_LEFT,

			/**
			 * Location is centered at the bottom of the view.
			 */
			BOTTOM_CENTER,

			/**
			 * Location is at the bottom right of the view.
			 */
			BOTTOM_RIGHT
		}

		/**
		 * The location of the child within the map view.
		 */
		public LatLong latLong;

		/**
		 * The alignment of the view compared to the location.
		 */
		public Align align;

		/**
		 * Creates a new set of layout parameters for a child view of MapView.
		 *
		 * @param width
		 *            the width of the child, either {@link #WRAP_CONTENT}, {@link #MATCH_PARENT} or a fixed size in pixels.
		 * @param height
		 *            the height of the child, either {@link #WRAP_CONTENT}, {@link #MATCH_PARENT} or a fixed size in pixels.
		 * @param latLong
		 *            the location of the child within the map view.
		 * @param align
		 *            the alignment of the view compared to the location.
		 */
		public LayoutParams(int width, int height, LatLong latLong, Align align) {
			super(width, height);
			this.latLong = latLong;
			this.align = align;
		}

		public LayoutParams(Context c, AttributeSet attrs) {
			super(c, attrs);
			this.align = LayoutParams.Align.BOTTOM_CENTER;
		}

		public LayoutParams(ViewGroup.LayoutParams source) {
			super(source);
		}
	}
}
