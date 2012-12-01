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
package org.mapsforge.android.maps.inputhandling;

import org.mapsforge.android.maps.MapView;
import org.mapsforge.android.maps.PausableThread;

import android.os.SystemClock;
import android.view.KeyEvent;
import android.view.MotionEvent;

/**
 * A {@code MapMover} moves the map horizontally and vertically at a configurable speed. It runs in a separate thread to
 * avoid blocking the UI thread.
 */
public class MapMover extends PausableThread implements KeyEvent.Callback {
	private static final int DEFAULT_MOVE_SPEED_FACTOR = 8;
	private static final int FRAME_LENGTH_IN_MS = 15;
	private static final float MOVE_SPEED = 0.2f;
	private static final String THREAD_NAME = "MapMover";
	private static final float TRACKBALL_MOVE_SPEED_FACTOR = 40;

	private final MapView mapView;
	private float moveSpeedFactor;
	private float moveX;
	private float moveY;
	private long timePrevious;

	/**
	 * @param mapView
	 *            the MapView which should be moved by this MapMover.
	 */
	public MapMover(MapView mapView) {
		super();
		this.mapView = mapView;
		this.moveSpeedFactor = DEFAULT_MOVE_SPEED_FACTOR;
	}

	/**
	 * @return the move speed factor, used for trackball and keyboard events.
	 */
	public float getMoveSpeedFactor() {
		return this.moveSpeedFactor;
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent keyEvent) {
		if (!this.mapView.isClickable()) {
			return false;
		}

		if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
			moveLeft();
			return true;
		} else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
			moveRight();
			return true;
		} else if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
			moveUp();
			return true;
		} else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
			moveDown();
			return true;
		}
		return false;
	}

	@Override
	public boolean onKeyLongPress(int keyCode, KeyEvent keyEvent) {
		return false;
	}

	@Override
	public boolean onKeyMultiple(int keyCode, int count, KeyEvent keyEvent) {
		return false;
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent keyEvent) {
		if (!this.mapView.isClickable()) {
			return false;
		}

		if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
			this.moveX = 0;
			return true;
		} else if (keyCode == KeyEvent.KEYCODE_DPAD_UP || keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
			this.moveY = 0;
			return true;
		}
		return false;
	}

	/**
	 * @param motionEvent
	 *            the trackball event which should be handled.
	 * @return true if the event was handled, false otherwise.
	 */
	public boolean onTrackballEvent(MotionEvent motionEvent) {
		if (!this.mapView.isClickable()) {
			return false;
		}

		if (motionEvent.getAction() == MotionEvent.ACTION_MOVE) {
			float mapMoveX = motionEvent.getX() * TRACKBALL_MOVE_SPEED_FACTOR * getMoveSpeedFactor();
			float mapMoveY = motionEvent.getY() * TRACKBALL_MOVE_SPEED_FACTOR * getMoveSpeedFactor();
			this.mapView.getMapViewPosition().moveCenter(mapMoveX, mapMoveY);
			return true;
		}
		return false;
	}

	/**
	 * Sets the move speed factor of the map, used for trackball and keyboard events.
	 * 
	 * @param moveSpeedFactor
	 *            the factor by which the move speed of the map will be multiplied.
	 * @throws IllegalArgumentException
	 *             if the new move speed factor is negative.
	 */
	public void setMoveSpeedFactor(float moveSpeedFactor) {
		if (moveSpeedFactor < 0) {
			throw new IllegalArgumentException();
		}
		this.moveSpeedFactor = moveSpeedFactor;
	}

	/**
	 * Stops moving the map completely.
	 */
	public void stopMove() {
		this.moveX = 0;
		this.moveY = 0;
	}

	private void moveDown() {
		if (this.moveY > 0) {
			// stop moving the map vertically
			this.moveY = 0;
		} else if (this.moveY == 0) {
			// start moving the map
			this.moveY = -MOVE_SPEED * this.moveSpeedFactor;
			this.timePrevious = SystemClock.uptimeMillis();
			synchronized (this) {
				notify();
			}
		}
	}

	private void moveLeft() {
		if (this.moveX < 0) {
			// stop moving the map horizontally
			this.moveX = 0;
		} else if (this.moveX == 0) {
			// start moving the map
			this.moveX = MOVE_SPEED * this.moveSpeedFactor;
			this.timePrevious = SystemClock.uptimeMillis();
			synchronized (this) {
				notify();
			}
		}
	}

	private void moveRight() {
		if (this.moveX > 0) {
			// stop moving the map horizontally
			this.moveX = 0;
		} else if (this.moveX == 0) {
			// start moving the map
			this.moveX = -MOVE_SPEED * this.moveSpeedFactor;
			this.timePrevious = SystemClock.uptimeMillis();
			synchronized (this) {
				notify();
			}
		}
	}

	private void moveUp() {
		if (this.moveY < 0) {
			// stop moving the map vertically
			this.moveY = 0;
		} else if (this.moveY == 0) {
			// start moving the map
			this.moveY = MOVE_SPEED * this.moveSpeedFactor;
			this.timePrevious = SystemClock.uptimeMillis();
			synchronized (this) {
				notify();
			}
		}
	}

	@Override
	protected void afterPause() {
		this.timePrevious = SystemClock.uptimeMillis();
	}

	@Override
	protected void doWork() throws InterruptedException {
		long timeCurrent = SystemClock.uptimeMillis();
		long timeElapsed = timeCurrent - this.timePrevious;
		this.timePrevious = timeCurrent;

		this.mapView.getMapViewPosition().moveCenter(timeElapsed * this.moveX, timeElapsed * this.moveY);
		sleep(FRAME_LENGTH_IN_MS);
	}

	@Override
	protected String getThreadName() {
		return THREAD_NAME;
	}

	@Override
	protected ThreadPriority getThreadPriority() {
		return ThreadPriority.NORMAL;
	}

	@Override
	protected boolean hasWork() {
		return this.moveX != 0 || this.moveY != 0;
	}
}
