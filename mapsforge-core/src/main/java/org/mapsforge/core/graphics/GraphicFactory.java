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
package org.mapsforge.core.graphics;

import java.io.IOException;
import java.io.InputStream;

public abstract class GraphicFactory {

	/**
	 * Default width and height of a map tile in pixel when no device or user scaling is applied.
	 */
	private static int defaultTileSize = 256;
	private static int tileSize = defaultTileSize;
	private static float deviceScaleFactor = 1f;
	private static float userScaleFactor = 1f;

	private static int defaultBackgroundColour = 0xffeeeeee; // format AARRGGBB
	private static int backgroundColor = defaultBackgroundColour;

	public abstract Bitmap createBitmap(int width, int height);

	public abstract Canvas createCanvas();

	public abstract int createColor(Color color);

	public abstract int createColor(int alpha, int red, int green, int blue);

	public abstract Matrix createMatrix();

	public abstract Paint createPaint();

	public abstract Path createPath();

	public abstract ResourceBitmap createResourceBitmap(InputStream inputStream, int hash) throws IOException;

	public abstract TileBitmap createTileBitmap();

	public abstract TileBitmap createTileBitmap(InputStream inputStream) throws IOException;

	public synchronized int getBackgroundColor() {
		return this.backgroundColor;
	}

	public synchronized float getScaleFactor() {
		return this.deviceScaleFactor * this.userScaleFactor;
	}

	public synchronized float getUserScaleFactor() {
		return this.userScaleFactor;
	}

	/**
	 * Width and height of a map tile in pixel after system and user scaling is applied.
	 */
	public static synchronized int getTileSize() {
		return tileSize;
	}

	public abstract InputStream platformSpecificSources(String relativePathPrefix, String src) throws IOException;

	public abstract ResourceBitmap renderSvg(InputStream inputStream, int hash);

	public synchronized void setBackgroundColor(int color) {
		this.backgroundColor = color;
	}

	public synchronized void setDeviceScaleFactor(float scaleFactor) {
		deviceScaleFactor = scaleFactor;
		setTileSize();
	}

	public synchronized void setUserScaleFactor(float scaleFactor) {
		userScaleFactor = scaleFactor;
		setTileSize();
	}

	private void setTileSize() {
		tileSize = (int) (defaultTileSize * deviceScaleFactor * userScaleFactor);
	}

}
