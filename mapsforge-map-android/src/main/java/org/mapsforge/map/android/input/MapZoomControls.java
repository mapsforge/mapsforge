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
package org.mapsforge.map.android.input;

import org.mapsforge.map.android.util.AndroidUtil;
import org.mapsforge.map.android.view.MapView;
import org.mapsforge.map.model.MapViewPosition;
import org.mapsforge.map.model.common.Observer;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ZoomControls;

/**
 * A MapZoomControls instance displays buttons for zooming in and out in a map.
 */
public class MapZoomControls implements Observer {
	private static class ZoomControlsHideHandler extends Handler {
		private final ZoomControls zoomControls;

		ZoomControlsHideHandler(ZoomControls zoomControls) {
			super();
			this.zoomControls = zoomControls;
		}

		@Override
		public void handleMessage(Message message) {
			this.zoomControls.hide();
		}
	}

	private static class ZoomInClickListener implements View.OnClickListener {
		private final MapViewPosition mapViewPosition;

		ZoomInClickListener(MapViewPosition mapViewPosition) {
			this.mapViewPosition = mapViewPosition;
		}

		@Override
		public void onClick(View view) {
			this.mapViewPosition.zoomIn();
		}
	}

	private static class ZoomOutClickListener implements View.OnClickListener {
		private final MapViewPosition mapViewPosition;

		ZoomOutClickListener(MapViewPosition mapViewPosition) {
			this.mapViewPosition = mapViewPosition;
		}

		@Override
		public void onClick(View view) {
			this.mapViewPosition.zoomOut();
		}
	}

	/**
	 * Default {@link Gravity} of the zoom controls.
	 */
	private static final int DEFAULT_ZOOM_CONTROLS_GRAVITY = Gravity.BOTTOM | Gravity.RIGHT;

	/**
	 * Default maximum zoom level.
	 */
	private static final byte DEFAULT_ZOOM_LEVEL_MAX = 22;

	/**
	 * Default minimum zoom level.
	 */
	private static final byte DEFAULT_ZOOM_LEVEL_MIN = 0;

	/**
	 * Message code for the handler to hide the zoom controls.
	 */
	private static final int MSG_ZOOM_CONTROLS_HIDE = 0;

	/**
	 * Horizontal margin for the zoom controls.
	 */
	private static final int DEFAULT_HORIZONTAL_MARGIN = 12;

	/**
	 * Vertical margin for the zoom controls.
	 */
	private static final int DEFAULT_VERTICAL_MARGIN = 0;

	/**
	 * Delay in milliseconds after which the zoom controls disappear.
	 */
	private static final long ZOOM_CONTROLS_TIMEOUT = ViewConfiguration.getZoomControlsTimeout();

	private boolean autoHide;
	private boolean layoutChanged;
	private final MapView mapView;
	private int marginHorizontal;
	private int marginVertical;
	private boolean showMapZoomControls;
	private final ZoomControls zoomControls;
	private int zoomControlsGravity;
	private final Handler zoomControlsHideHandler;
	private byte zoomLevelMax;
	private byte zoomLevelMin;

	public MapZoomControls(Context context, final MapView mapView) {
		this.mapView = mapView;
		this.zoomControls = new ZoomControls(context);
		this.autoHide = true;
		this.marginHorizontal = DEFAULT_HORIZONTAL_MARGIN;
		this.marginVertical = DEFAULT_VERTICAL_MARGIN;
		this.showMapZoomControls = true;
		this.zoomLevelMax = DEFAULT_ZOOM_LEVEL_MAX;
		this.zoomLevelMin = DEFAULT_ZOOM_LEVEL_MIN;
		this.zoomControls.setVisibility(View.GONE);
		this.zoomControlsGravity = DEFAULT_ZOOM_CONTROLS_GRAVITY;

		this.zoomControls.setOnZoomInClickListener(new ZoomInClickListener(mapView.getModel().mapViewPosition));
		this.zoomControls.setOnZoomOutClickListener(new ZoomOutClickListener(mapView.getModel().mapViewPosition));
		this.zoomControlsHideHandler = new ZoomControlsHideHandler(this.zoomControls);
		this.mapView.getModel().mapViewPosition.addObserver(this);
		int wrapContent = android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
		LayoutParams layoutParams = new LayoutParams(wrapContent, wrapContent);
		this.mapView.addView(this.zoomControls, layoutParams);
	}

	public int getMarginHorizontal() {
		return marginHorizontal;
	}

	public int getMarginVertical() {
		return marginVertical;
	}

	public int getMeasuredHeight() {
		return this.zoomControls.getMeasuredHeight();
	}

	public int getMeasuredWidth() {
		return this.zoomControls.getMeasuredWidth();
	}

	/**
	 * @return the current gravity for the placing of the zoom controls.
	 * @see Gravity
	 */
	public int getZoomControlsGravity() {
		return this.zoomControlsGravity;
	}

	/**
	 * @return the maximum zoom level of the map.
	 */
	public byte getZoomLevelMax() {
		return this.zoomLevelMax;
	}

	/**
	 * @return the minimum zoom level of the map.
	 */
	public byte getZoomLevelMin() {
		return this.zoomLevelMin;
	}

	/**
	 * @return true if the zoom controls hide automatically, false otherwise.
	 */
	public boolean isAutoHide() {
		return this.autoHide;
	}

	/**
	 * @return true if the zoom controls are visible, false otherwise.
	 */
	public boolean isShowMapZoomControls() {
		return this.showMapZoomControls;
	}

	public void layout(boolean changed, int left, int top, int right, int bottom) {
		if (!changed && !this.layoutChanged) {
			return;
		}

		int zoomControlsWidth = this.zoomControls.getMeasuredWidth();
		int zoomControlsHeight = this.zoomControls.getMeasuredHeight();

		int positionLeft = calculatePositionLeft(left, right, zoomControlsWidth);
		int positionTop = calculatePositionTop(top, bottom, zoomControlsHeight);
		int positionRight = positionLeft + zoomControlsWidth;
		int positionBottom = positionTop + zoomControlsHeight;

		this.zoomControls.layout(positionLeft, positionTop, positionRight, positionBottom);
		this.layoutChanged = false;
	}

	public void measure(int widthMeasureSpec, int heightMeasureSpec) {
		this.zoomControls.measure(widthMeasureSpec, heightMeasureSpec);
	}

	@Override
	public void onChange() {
		this.onZoomLevelChange(this.mapView.getModel().mapViewPosition.getZoomLevel());
	}

	public void onMapViewTouchEvent(MotionEvent event) {
		if (event.getPointerCount() > 1) {
			// no multitouch
			return;
		}
		if (this.showMapZoomControls && this.autoHide) {
			switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN:
					showZoomControls();
					break;
				case MotionEvent.ACTION_CANCEL:
					showZoomControlsWithTimeout();
					break;
				case MotionEvent.ACTION_UP:
					showZoomControlsWithTimeout();
					break;
			}
		}
	}

	public void onZoomLevelChange(final int newZoomLevel) {
		// to allow changing zoom level programmatically, i.e. not just
		// by user interaction
		if (AndroidUtil.currentThreadIsUiThread()) {
			changeZoomControls(newZoomLevel);
		} else {
			this.mapView.post(new Runnable() {
				@Override
				public void run() {
					changeZoomControls(newZoomLevel);
				}
			});
		}
	}

	/**
	 * @param autoHide
	 *            true if the zoom controls hide automatically, false otherwise.
	 */
	public void setAutoHide(boolean autoHide) {
		this.autoHide = autoHide;
		if (!this.autoHide) {
			showZoomControls();
		}
	}

	public void setMarginHorizontal(int marginHorizontal) {
		if (this.marginHorizontal != marginHorizontal) {
			this.marginHorizontal = marginHorizontal;
			this.layoutChanged = true;
		}
	}

	public void setMarginVertical(int marginVertical) {
		if (this.marginVertical != marginVertical) {
			this.marginVertical = marginVertical;
			this.layoutChanged = true;
		}
	}

	/**
	 * @param showMapZoomControls
	 *            true if the zoom controls should be visible, false otherwise.
	 */
	public void setShowMapZoomControls(boolean showMapZoomControls) {
		this.showMapZoomControls = showMapZoomControls;
	}

	/**
	 * Sets the gravity for the placing of the zoom controls. Supported values are {@link Gravity#TOP},
	 * {@link Gravity#CENTER_VERTICAL}, {@link Gravity#BOTTOM}, {@link Gravity#LEFT}, {@link Gravity#CENTER_HORIZONTAL}
	 * and {@link Gravity#RIGHT}.
	 * 
	 * @param zoomControlsGravity
	 *            a combination of {@link Gravity} constants describing the desired placement.
	 */
	public void setZoomControlsGravity(int zoomControlsGravity) {
		if (this.zoomControlsGravity != zoomControlsGravity) {
			this.zoomControlsGravity = zoomControlsGravity;
			this.layoutChanged = true;
		}
	}

	/**
	 * Sets the maximum zoom level of the map.
	 * <p>
	 * The maximum possible zoom level of the MapView depends also on other elements. For example, downloading map tiles
	 * may only be possible up to a certain zoom level. Setting a higher maximum zoom level has no effect in this case.
	 * 
	 * @param zoomLevelMax
	 *            the maximum zoom level.
	 * @throws IllegalArgumentException
	 *             if the maximum zoom level is smaller than the current minimum zoom level.
	 */
	public void setZoomLevelMax(byte zoomLevelMax) {
		if (zoomLevelMax < this.zoomLevelMin) {
			throw new IllegalArgumentException();
		}
		this.zoomLevelMax = zoomLevelMax;
	}

	/**
	 * Sets the minimum zoom level of the map.
	 * 
	 * @param zoomLevelMin
	 *            the minimum zoom level.
	 * @throws IllegalArgumentException
	 *             if the minimum zoom level is larger than the current maximum zoom level.
	 */
	public void setZoomLevelMin(byte zoomLevelMin) {
		if (zoomLevelMin > this.zoomLevelMax) {
			throw new IllegalArgumentException();
		}
		this.zoomLevelMin = zoomLevelMin;
	}

	private int calculatePositionLeft(int left, int right, int zoomControlsWidth) {
		int gravity = this.zoomControlsGravity & Gravity.HORIZONTAL_GRAVITY_MASK;
		switch (gravity) {
			case Gravity.LEFT:
				return marginHorizontal;

			case Gravity.CENTER_HORIZONTAL:
				return (right - left - zoomControlsWidth) / 2;

			case Gravity.RIGHT:
				return right - left - zoomControlsWidth - marginHorizontal;
		}

		throw new IllegalArgumentException("unknown horizontal gravity: " + gravity);
	}

	private int calculatePositionTop(int top, int bottom, int zoomControlsHeight) {
		int gravity = this.zoomControlsGravity & Gravity.VERTICAL_GRAVITY_MASK;
		switch (gravity) {
			case Gravity.TOP:
				return marginVertical;

			case Gravity.CENTER_VERTICAL:
				return (bottom - top - zoomControlsHeight) / 2;

			case Gravity.BOTTOM:
				return bottom - top - zoomControlsHeight - marginVertical;
		}

		throw new IllegalArgumentException("unknown vertical gravity: " + gravity);
	}

	private void changeZoomControls(int newZoomLevel) {
		boolean zoomInEnabled = newZoomLevel < this.zoomLevelMax;
		boolean zoomOutEnabled = newZoomLevel > this.zoomLevelMin;
		this.zoomControls.setIsZoomInEnabled(zoomInEnabled);
		this.zoomControls.setIsZoomOutEnabled(zoomOutEnabled);
	}

	private void showZoomControls() {
		this.zoomControlsHideHandler.removeMessages(MSG_ZOOM_CONTROLS_HIDE);
		if (this.zoomControls.getVisibility() != View.VISIBLE) {
			this.zoomControls.show();
		}
	}

	private void showZoomControlsWithTimeout() {
		showZoomControls();
		this.zoomControlsHideHandler.sendEmptyMessageDelayed(MSG_ZOOM_CONTROLS_HIDE, ZOOM_CONTROLS_TIMEOUT);
	}
}
