/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright 2015 devemux86
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
package org.mapsforge.map.android.input;

import org.mapsforge.map.android.util.AndroidUtil;
import org.mapsforge.map.android.view.MapView;
import org.mapsforge.map.model.common.Observer;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.AlphaAnimation;
import android.widget.LinearLayout;
import android.widget.ZoomButton;
import android.widget.ZoomControls;

/**
 * A MapZoomControls instance displays buttons for zooming in and out in a map.
 */
public class MapZoomControls extends LinearLayout implements Observer {

	public enum Orientation {

		/** Vertical arrangement, Zoom in on top of zoom out. */
		VERTICAL_IN_OUT(LinearLayout.VERTICAL, true),

		/** Vertical arrangement, Zoom in below zoom out. */
		VERTICAL_OUT_IN(LinearLayout.VERTICAL, false),

		/** Horizontal arrangement, Zoom in to the left of zoom out. */
		HORIZONTAL_IN_OUT(LinearLayout.HORIZONTAL, true),

		/** Horizontal arrangement, Zoom in to the right of zoom out. */
		HORIZONTAL_OUT_IN(LinearLayout.HORIZONTAL, false);

		public final int layoutOrientation;
		public final boolean zoomInFirst;

		private Orientation(int layoutOrientation, boolean zoomInFirst) {
			this.layoutOrientation = layoutOrientation;
			this.zoomInFirst = zoomInFirst;
		}
	};

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
	 * Auto-repeat delay of the zoom buttons in ms.
	 */
	private static final int DEFAULT_ZOOM_SPEED = 500;

	/**
	 * Message code for the handler to hide the zoom controls.
	 */
	private static final int MSG_ZOOM_CONTROLS_HIDE = 0;

	/**
	 * Horizontal margin for the zoom controls.
	 */
	private static final int DEFAULT_HORIZONTAL_MARGIN = 5;

	/**
	 * Vertical margin for the zoom controls.
	 */
	private static final int DEFAULT_VERTICAL_MARGIN = 0;

	/**
	 * Delay in milliseconds after which the zoom controls disappear.
	 */
	private static final long ZOOM_CONTROLS_TIMEOUT = ViewConfiguration.getZoomControlsTimeout();

	private boolean autoHide;
	private final MapView mapView;
	private boolean showMapZoomControls;
	private int zoomControlsGravity;
	private final Handler zoomControlsHideHandler;
	private byte zoomLevelMax;
	private byte zoomLevelMin;
	private ZoomButton buttonZoomIn, buttonZoomOut;

	public MapZoomControls(Context context, final MapView mapView) {
		super(context);
		this.mapView = mapView;
		this.autoHide = true;
		setMarginHorizontal(DEFAULT_HORIZONTAL_MARGIN);
		setMarginVertical(DEFAULT_VERTICAL_MARGIN);
		this.showMapZoomControls = true;
		this.zoomLevelMax = DEFAULT_ZOOM_LEVEL_MAX;
		this.zoomLevelMin = DEFAULT_ZOOM_LEVEL_MIN;
		setVisibility(View.GONE);
		this.zoomControlsGravity = DEFAULT_ZOOM_CONTROLS_GRAVITY;

		this.zoomControlsHideHandler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				MapZoomControls.this.hide();
			}
		};

		// hack to get default zoom buttons
		final ZoomControls tempControls = new ZoomControls(context);
		buttonZoomIn = (ZoomButton) tempControls.getChildAt(1);
		buttonZoomOut = (ZoomButton) tempControls.getChildAt(0);
		tempControls.removeAllViews();
		setOrientation(tempControls.getOrientation());
		setZoomInFirst(false);

		setZoomSpeed(DEFAULT_ZOOM_SPEED);
		buttonZoomIn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				mapView.getModel().mapViewPosition.zoomIn();
			}
		});
		buttonZoomOut.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				mapView.getModel().mapViewPosition.zoomOut();
			}
		});
		this.mapView.getModel().mapViewPosition.addObserver(this);
	}

	/**
	 * Set background drawable of the zoom in button.
	 * 
	 * @param resId
	 *            resource id of drawable.
	 */
	public void setZoomInResource(int resId) {
		buttonZoomIn.setBackgroundResource(resId);
	}

	/**
	 * Set background drawable of the zoom out button.
	 * 
	 * @param resId
	 *            resource id of drawable.
	 */
	public void setZoomOutResource(int resId) {
		buttonZoomOut.setBackgroundResource(resId);
	}

	/**
	 * Set orientation of controls.
	 * 
	 * @param orientation
	 *            one of the four orientations.
	 */
	public void setControlOrientation(Orientation orientation) {
		setOrientation(orientation.layoutOrientation);
		setZoomInFirst(orientation.zoomInFirst);
	}

	/**
	 * For horizontal orientation, "zoom in first" means the zoom in button will appear on top of the zoom out button.
	 * For vertical orientation, "zoom in first" means the zoom in button will appear to the left of the zoom out
	 * button.
	 * 
	 * @param zoomInFirst
	 *            zoom in button will be first in layout.
	 */
	private void setZoomInFirst(boolean zoomInFirst) {
		this.removeAllViews();
		final LayoutParams layoutParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		if (zoomInFirst) {
			this.addView(buttonZoomIn, layoutParams);
			this.addView(buttonZoomOut, layoutParams);
		}
		else {
			this.addView(buttonZoomOut, layoutParams);
			this.addView(buttonZoomIn, layoutParams);
		}
	}

	/**
	 * Set auto-repeat delay of the zoom buttons.
	 * 
	 * @param ms
	 *            delay in ms.
	 */
	public void setZoomSpeed(int ms) {
		buttonZoomIn.setZoomSpeed(ms);
		buttonZoomOut.setZoomSpeed(ms);
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
		setPadding(marginHorizontal, getPaddingTop(), marginHorizontal, getPaddingBottom());
	}

	public void setMarginVertical(int marginVertical) {
		setPadding(getPaddingLeft(), marginVertical, getPaddingRight(), marginVertical);
	}

	/**
	 * Sets the gravity for the placing of the zoom controls.
	 * 
	 * @param zoomControlsGravity
	 *            a combination of {@link Gravity} constants describing the desired placement.
	 */
	public void setZoomControlsGravity(int zoomControlsGravity) {
		this.zoomControlsGravity = zoomControlsGravity;
		mapView.requestLayout();
	}

	/**
	 * @return the current gravity for the placing of the zoom controls.
	 * @see Gravity
	 */
	public int getZoomControlsGravity() {
		return this.zoomControlsGravity;
	}

	/**
	 * @param showMapZoomControls
	 *            true if the zoom controls should be visible, false otherwise.
	 */
	public void setShowMapZoomControls(boolean showMapZoomControls) {
		this.showMapZoomControls = showMapZoomControls;
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

	private void changeZoomControls(int newZoomLevel) {
		buttonZoomIn.setEnabled(newZoomLevel < this.zoomLevelMax);
		buttonZoomOut.setEnabled(newZoomLevel > this.zoomLevelMin);
	}

	private void showZoomControls() {
		this.zoomControlsHideHandler.removeMessages(MSG_ZOOM_CONTROLS_HIDE);
		if (getVisibility() != View.VISIBLE) {
			this.show();
		}
	}

	private void showZoomControlsWithTimeout() {
		showZoomControls();
		this.zoomControlsHideHandler.sendEmptyMessageDelayed(MSG_ZOOM_CONTROLS_HIDE, ZOOM_CONTROLS_TIMEOUT);
	}

	public void show() {
		fade(View.VISIBLE, 0.0f, 1.0f);
	}

	public void hide() {
		fade(View.GONE, 1.0f, 0.0f);
	}

	private void fade(final int visibility, final float startAlpha, final float endAlpha) {
		final AlphaAnimation anim = new AlphaAnimation(startAlpha, endAlpha);
		anim.setDuration(500);
		startAnimation(anim);
		setVisibility(visibility);
	}
}
