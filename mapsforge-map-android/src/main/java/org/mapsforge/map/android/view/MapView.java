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
package org.mapsforge.map.android.view;

import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.model.Dimension;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Point;
import org.mapsforge.core.util.MercatorProjection;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.android.input.TouchEventHandler;
import org.mapsforge.map.android.input.TouchGestureDetector;
import org.mapsforge.map.android.input.MapZoomControls;

import org.mapsforge.map.controller.FrameBufferController;
import org.mapsforge.map.controller.LayerManagerController;
import org.mapsforge.map.controller.MapViewController;
import org.mapsforge.map.layer.LayerManager;
import org.mapsforge.map.model.MapViewPosition;
import org.mapsforge.map.model.Model;
import org.mapsforge.map.android.input.ScaleListener;
import org.mapsforge.map.scalebar.MapScaleBar;
import org.mapsforge.map.view.FpsCounter;
import org.mapsforge.map.view.FrameBuffer;

import android.content.Context;
import android.graphics.Canvas;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ViewGroup;
import android.view.ViewConfiguration;

public class MapView extends ViewGroup implements org.mapsforge.map.view.MapView {
    private static final GraphicFactory GRAPHIC_FACTORY = AndroidGraphicFactory.INSTANCE;

    private final FpsCounter fpsCounter;
	private final FrameBuffer frameBuffer;
	private final FrameBufferController frameBufferController;
	private final LayerManager layerManager;
    private final MapScaleBar mapScaleBar;
    private final Model model;
    private final TouchEventHandler touchEventHandler;
    private GestureDetector gestureDetector;
    private final MapZoomControls mapZoomControls;

    public MapView(Context context) {
        this(context, null);
    }

    public MapView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);

        setDescendantFocusability(FOCUS_BLOCK_DESCENDANTS);
        setWillNotDraw(false);

        this.model = new Model();

        this.fpsCounter = new FpsCounter(GRAPHIC_FACTORY);
        this.frameBuffer = new FrameBuffer(this.model.frameBufferModel, GRAPHIC_FACTORY);
        this.frameBufferController = FrameBufferController.create(this.frameBuffer, this.model);

        this.layerManager = new LayerManager(this, this.model.mapViewPosition, GRAPHIC_FACTORY);
        this.layerManager.start();
        LayerManagerController.create(this.layerManager, this.model);

        MapViewController.create(this, this.model);

        ViewConfiguration viewConfiguration = ViewConfiguration.get(context);
	    ScaleGestureDetector sgd = new ScaleGestureDetector(context, new ScaleListener(this.getModel().mapViewPosition));
	    TouchGestureDetector touchGestureDetector = new TouchGestureDetector(this, viewConfiguration);
	    this.touchEventHandler = new TouchEventHandler(this, viewConfiguration, sgd);
		this.touchEventHandler.addListener(touchGestureDetector);
        this.mapZoomControls = new MapZoomControls(context, this);
        this.mapScaleBar = new MapScaleBar(this.model.mapViewPosition, this.model.mapViewDimension, GRAPHIC_FACTORY);
    }

    @Override
    public void destroy() {
        this.layerManager.interrupt();
	    this.frameBufferController.destroy();
        this.frameBuffer.destroy();
        this.mapScaleBar.destroy();
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

    public MapScaleBar getMapScaleBar() {
        return this.mapScaleBar;
    }

    @Override
    public Model getModel() {
        return this.model;
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        if (!isClickable()) {
            return false;
        }
        this.mapZoomControls.onMapViewTouchEvent(motionEvent);
        if (this.gestureDetector != null) {
            if (this.gestureDetector.onTouchEvent(motionEvent)) {
                return true;
            }
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
    protected void onDraw(Canvas androidCanvas) {
	    org.mapsforge.core.graphics.Canvas graphicContext = AndroidGraphicFactory.createGraphicContext(androidCanvas);
        this.frameBuffer.draw(graphicContext);
        this.mapScaleBar.draw(graphicContext);
        this.fpsCounter.draw(graphicContext);
	    graphicContext.destroy();
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        this.model.mapViewDimension.setDimension(new Dimension(width, height));
    }

    public void setGestureDetector(GestureDetector gestureDetector) {
        this.gestureDetector = gestureDetector;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        this.mapZoomControls.onLayout(changed, left, top, right, bottom);
    }

	public LatLong fromPixels(Point p) {
		MapViewPosition mapPosition = this.getModel().mapViewPosition;
		LatLong geoPoint = mapPosition.getMapPosition().latLong;

		double pixelX = MercatorProjection.longitudeToPixelX(geoPoint.longitude, mapPosition.getZoomLevel());
		double pixelY = MercatorProjection.latitudeToPixelY(geoPoint.latitude, mapPosition.getZoomLevel());

		pixelX -= this.getWidth() >> 1;
		pixelY -= this.getHeight() >> 1;

		LatLong l = new LatLong(MercatorProjection.pixelYToLatitude(pixelY + p.y, mapPosition.getZoomLevel()),
				MercatorProjection.pixelXToLongitude(pixelX + p.x, mapPosition.getZoomLevel()));

		return l;
	}

	/**
     * @return the zoom controls instance which is used in this MapView.
     */
    public MapZoomControls getMapZoomControls() {
        return this.mapZoomControls;
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
    protected final void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // find out how big the zoom controls should be
        this.mapZoomControls.measure(
                MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.AT_MOST),
                MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(heightMeasureSpec), MeasureSpec.AT_MOST));

        // make sure that MapView is big enough to display the zoom controls
        setMeasuredDimension(Math.max(MeasureSpec.getSize(widthMeasureSpec), this.mapZoomControls.getMeasuredWidth()),
                Math.max(MeasureSpec.getSize(heightMeasureSpec), this.mapZoomControls.getMeasuredHeight()));
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

	public Point toPixels(LatLong in) {
		if (in == null || this.getWidth() <= 0 || this.getHeight() <= 0) {
			return null;
		}

		MapViewPosition mapPosition = this.getModel().mapViewPosition;

		// calculate the pixel coordinates of the top left corner
		LatLong geoPoint = mapPosition.getMapPosition().latLong;
		double pixelX = MercatorProjection.longitudeToPixelX(geoPoint.longitude, mapPosition.getZoomLevel());
		double pixelY = MercatorProjection.latitudeToPixelY(geoPoint.latitude, mapPosition.getZoomLevel());
		pixelX -= this.getWidth() >> 1;
		pixelY -= this.getHeight() >> 1;
		return new Point((int) (MercatorProjection.longitudeToPixelX(in.longitude, mapPosition.getZoomLevel()) - pixelX),
				(int) (MercatorProjection.latitudeToPixelY(in.latitude, mapPosition.getZoomLevel()) - pixelY));
	}

}
