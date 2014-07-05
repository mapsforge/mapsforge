/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
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
package org.mapsforge.map.awt;

import java.io.IOException;
import java.io.InputStream;

import org.mapsforge.core.graphics.TileBitmap;

public class AwtTileBitmap extends AwtBitmap implements TileBitmap {

	private long expiration = 0;
	private long timestamp = System.currentTimeMillis();

	AwtTileBitmap(InputStream inputStream) throws IOException {
		super(inputStream);
	}

	AwtTileBitmap(int tileSize) {
		super(tileSize, tileSize);
	}

	@Override
	public long getTimestamp() {
		return timestamp;
	}

	@Override
	public boolean isExpired() {
		if (expiration == 0)
			return false;
		return (expiration >= System.currentTimeMillis());
	}

	@Override
	public void setExpiration(long expiration) {
		this.expiration = expiration;
	}

	@Override
	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

}
