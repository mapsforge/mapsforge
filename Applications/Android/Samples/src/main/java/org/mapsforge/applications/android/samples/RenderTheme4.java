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

	protected final String THEMESTYLE = "r4theme";
	protected final String OUTDOOR = "outdoor";
	protected final String CITY = "city";
	protected final String DRIVING = "driving";
	protected final String EVERYTHING = "everything";

	@Override
	protected void createMapViews() {
		super.createMapViews();
		this.mapViews.get(0).getModel().displayModel.setTileSizeMultiple(64);
	}

	@Override
	protected List<String> getCategories() {
		// categories to render
		String themeStyle = this.sharedPreferences.getString(THEMESTYLE, "outdoor");

		if (EVERYTHING.equals(themeStyle)) {
			return null;
		}

		List<String> categories = new ArrayList<String>();
		if (OUTDOOR.equals(themeStyle)) {
			categories.add("outdoor");
			categories.add("natural");
			categories.add("natural-labels");
			categories.add("sport");
		}
		if (CITY.equals(themeStyle)) {
			categories.add("buildings");
			categories.add("shopping");
			categories.add("tourism");
		}
		if (DRIVING.equals(themeStyle)) {
			categories.add("transport");
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

		if (THEMESTYLE.equals(key)) {
			// just start this new
			finish();
			startActivity(this.getIntent());
		}
	}

}
