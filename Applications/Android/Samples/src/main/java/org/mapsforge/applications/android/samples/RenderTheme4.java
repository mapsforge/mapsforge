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

/**
 * Example of the capabilities of RenderTheme version 4
 */
public class RenderTheme4 extends AssetsRenderThemeMapViewer {

	protected final String BUILDINGS = "buildings";
	protected final String LANDUSE = "landuse";

	@Override
	protected List<String> getCategories() {
		// categories to render
		List<String> categories = new ArrayList<String>();
		if (this.sharedPreferences.getBoolean(BUILDINGS, false)) {
			categories.add(BUILDINGS);
		}
		if (this.sharedPreferences.getBoolean(LANDUSE, true)) {
			categories.add(LANDUSE);
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

		if (BUILDINGS.equals(key) || LANDUSE.equals(key)) {
			// just start this new
			startActivity(this.getIntent());
			finish();
		}
	}

}
