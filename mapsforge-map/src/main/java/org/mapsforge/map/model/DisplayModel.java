/*
 * Copyright © 2014 Ludwig M Brinckmann
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

package org.mapsforge.map.model;

import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.map.model.common.Observable;

/**
 * Encapsulates the display characteristics for a MapView, such as tile size and background color.
 *
 * The size of map tiles is used to adapt to devices with differing pixel densities and
 * users with different preferences: The larger the tile, the larger everything is rendered, the
 * effect is one of effectively stretching everything.
 *
 * The default device dependent scale factor is determined at the GraphicFactory level, while the
 * DisplayModel allows further adaptation to cater for user needs or application development (maybe
 * a small map and large map, or to prevent upscaling for downloaded tiles that do not scale well).
 */

public class DisplayModel extends Observable {

	private static final int DEFAULT_BACKGROUND_COLOR = 0xffeeeeee; // format AARRGGBB
	private static final int DEFAULT_TILE_SIZE = 256;

	private static float defaultUserScaleFactor = 1f;
	private static float deviceScaleFactor = 1f;

	private int tileSize = DEFAULT_TILE_SIZE;
	private float userScaleFactor = defaultUserScaleFactor;
	private int backgroundColor = DEFAULT_BACKGROUND_COLOR;

	public DisplayModel() {
		super();
		this.setTileSize();
	}

	/**
	 * Set the default scale factor for all newly created DisplayModels, so can
	 * be used to apply user settings from a device.
	 *
	 * @param scaleFactor the default scale factor to be applied to all new DisplayModels.
	 */
	public static synchronized void setDefaultUserScaleFactor(float scaleFactor) {
		defaultUserScaleFactor = scaleFactor;
	}

	/**
	 * Get the default scale factor for all newly created DisplayModels.
	 *
	 * @return the default scale factor to be applied to all new DisplayModels.
	 */
	public static synchronized float getDefaultUserScaleFactor() {
		return defaultUserScaleFactor;
	}

	/**
	 * Returns the device scale factor.
	 *
	 * @return the device scale factor.
	 */
	public static synchronized float getDeviceScaleFactor() {
		return deviceScaleFactor;
	}

	/**
	 * Returns the background color.
	 *
	 * @return the background color.
	 */
	public synchronized int getBackgroundColor() {
		return backgroundColor;
	}

	/**
	 * Returns the overall scale factor.
	 *
	 * @return the combined device/user scale factor.
	 */
	public synchronized float getScaleFactor() {
		return deviceScaleFactor * this.userScaleFactor;
	}

	/**
	 * Returns the user scale factor.
	 *
	 * @return the user scale factor.
	 */
	public synchronized float getUserScaleFactor() {
		return this.userScaleFactor;
	}

	/**
	 * Width and height of a map tile in pixel after system and user scaling is applied.
	 */
	public synchronized int getTileSize() {
		return tileSize;
	}

	/**
	 * Set the background color.
	 *
	 * @param color the color to use.
	 */
	public synchronized void setBackgroundColor(int color) {
		this.backgroundColor = color;
	}

	/**
	 * Set the device scale factor.
	 *
	 * @param scaleFactor the device scale factor.
	 */
	public static synchronized void setDeviceScaleFactor(float scaleFactor) {
		deviceScaleFactor = scaleFactor;
	}

	/**
	 * Set the user scale factor.
	 *
	 * @param scaleFactor the user scale factor to use.
	 */
	public synchronized void setUserScaleFactor(float scaleFactor) {
		userScaleFactor = scaleFactor;
		setTileSize();
	}

	private void setTileSize() {
		this.tileSize = (int) (DEFAULT_TILE_SIZE * deviceScaleFactor * userScaleFactor);
	}

}
