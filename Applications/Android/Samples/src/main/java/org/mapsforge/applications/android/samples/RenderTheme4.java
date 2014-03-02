/*
 * Copyright 2013-2014 Ludwig M Brinckmann
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
package org.mapsforge.applications.android.samples;

import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Example of the capabilities of RenderTheme version 4
 */
public class RenderTheme4 extends AssetsRenderThemeMapViewer {

	protected final String KEY_PREFIX = "r4_";

	@Override
	protected void createMapViews() {
		super.createMapViews();
		this.mapViews.get(0).getModel().displayModel.setTileSizeMultiple(64);
	}

	@Override
	protected List<String> getCategories() {

		List<String> categories = new ArrayList<String>();
		Map<String, ?> preferences = this.sharedPreferences.getAll();
		for (String key : preferences.keySet()) {
			if (key.startsWith(KEY_PREFIX)) {
				if (this.sharedPreferences.getBoolean(key, true)) {
					categories.add(key.substring(KEY_PREFIX.length()));
				}
			}
		}

		return categories;
	}

	@Override
	protected String getRenderThemeFile() {
		return "renderthemes/rendertheme-v4.xml";
	}


	@Override
	public void onSharedPreferenceChanged(SharedPreferences preferences, String key) {
		super.onSharedPreferenceChanged(preferences, key);

		if (key.startsWith(KEY_PREFIX)) {
			destroyLayers();
			destroyTileCaches();
			createTileCaches();
			createLayers();
		}
	}

}
