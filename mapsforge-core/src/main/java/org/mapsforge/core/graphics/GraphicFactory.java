/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright 2014 Ludwig M Brinckmann
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

	protected static float deviceScaleFactor;

	public static final int DEFAULT_BACKGROUND_COLOR = 0xffeeeeee; // format AARRGGBB
	public static final int DEFAULT_TILE_SIZE = 256;

	public abstract Bitmap createBitmap(int width, int height);

	public abstract Bitmap createBitmap(int width, int height, boolean isTransparent);

	public abstract Canvas createCanvas();

	public abstract int createColor(Color color);

	public abstract int createColor(int alpha, int red, int green, int blue);

	public abstract Matrix createMatrix();

	public abstract Paint createPaint();

	public abstract Path createPath();

	public abstract ResourceBitmap createResourceBitmap(InputStream inputStream, int hash) throws IOException;

	public abstract TileBitmap createTileBitmap(int tileSize, boolean isTransparent);

	public abstract TileBitmap createTileBitmap(InputStream inputStream, int tileSize, boolean isTransparent) throws IOException;

	public static int getAlpha(int color) {
		return (color >> 24) & 0xff;
	}

	public static float getDeviceScaleFactor() {
		return deviceScaleFactor;
	}

	public abstract InputStream platformSpecificSources(String relativePathPrefix, String src) throws IOException;

	public abstract ResourceBitmap renderSvg(InputStream inputStream, float scaleFactor, int hash) throws IOException;

}
