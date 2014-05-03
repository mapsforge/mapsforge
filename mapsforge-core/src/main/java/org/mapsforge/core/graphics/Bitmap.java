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
import java.io.OutputStream;

public interface Bitmap {
	void compress(OutputStream outputStream) throws IOException;

	void decrementRefCount();

	/**
	 * @return the height of this bitmap in pixels.
	 */
	int getHeight();

	/**
	 * @return the width of this bitmap in pixels.
	 */
	int getWidth();

	void incrementRefCount();

	void scaleTo(int width, int height);

	void setBackgroundColor(int color);
}
