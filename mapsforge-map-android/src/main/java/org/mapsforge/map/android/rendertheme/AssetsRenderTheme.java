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
package org.mapsforge.map.android.rendertheme;

import java.io.IOException;
import java.io.InputStream;

import org.mapsforge.map.rendertheme.XmlRenderTheme;

import android.content.Context;

/**
 * An AssetRenderTheme is an XmlRenderTheme that is picked up from the Android apk assets folder
 */
public class AssetsRenderTheme implements XmlRenderTheme {
	private static final long serialVersionUID = 1L;

	private final String assetName;
	private final InputStream inputStream;
	private final String relativePathPrefix;

	public AssetsRenderTheme(Context context, String relativePathPrefix, String fileName) throws IOException {
		this.assetName = fileName;
		this.relativePathPrefix = relativePathPrefix;
		this.inputStream = context.getAssets().open(this.assetName);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		} else if (!(obj instanceof AssetsRenderTheme)) {
			return false;
		}
		AssetsRenderTheme other = (AssetsRenderTheme) obj;
		if (this.assetName != other.assetName) {
			return false;
		}
		if (this.relativePathPrefix != other.relativePathPrefix) {
			return false;
		}
		return true;
	}

	@Override
	public InputStream getRenderThemeAsStream() {
		return this.inputStream;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((this.assetName == null) ? 0 : this.assetName.hashCode());
		result = prime * result + ((this.relativePathPrefix == null) ? 0 : this.relativePathPrefix.hashCode());
		return result;
	}

	@Override
	public String getRelativePathPrefix() {
		return this.relativePathPrefix;
	}
}
