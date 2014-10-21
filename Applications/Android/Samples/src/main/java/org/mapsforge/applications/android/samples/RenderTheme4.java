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
import android.util.Log;

import org.mapsforge.map.android.rendertheme.AssetsRenderTheme;
import org.mapsforge.map.android.util.AndroidUtil;
import org.mapsforge.map.rendertheme.XmlRenderTheme;
import org.mapsforge.map.rendertheme.XmlRenderThemeMenuCallback;
import org.mapsforge.map.rendertheme.XmlRenderThemeStyleLayer;
import org.mapsforge.map.rendertheme.XmlRenderThemeStyleMenu;

import java.io.IOException;
import java.util.Set;

/**
 * Standard map view with use of Rendertheme V4, loading the render theme from the Android
 * assets folder and showing a configuration menu based on the stylemenu.
 */
public class RenderTheme4 extends SamplesBaseActivity implements XmlRenderThemeMenuCallback {


	@Override
	protected XmlRenderTheme getRenderTheme() {
		try {
			return new AssetsRenderTheme(this, getRenderThemePrefix(), getRenderThemeFile(), this);
		} catch (IOException e) {
			Log.e(SamplesApplication.TAG, "Render theme failure " + e.toString());
		}
		return null;
	}

	protected String getRenderThemePrefix() {
		return "";
	}

	@Override
	public Set<String> getCategories(XmlRenderThemeStyleMenu menuStyle) {
		this.renderThemeStyleMenu = menuStyle;
		String id = this.sharedPreferences.getString(this.renderThemeStyleMenu.getId(),
				this.renderThemeStyleMenu.getDefaultValue());

		XmlRenderThemeStyleLayer baseLayer = this.renderThemeStyleMenu.getLayer(id);
		if (baseLayer == null) {
			Log.w(SamplesApplication.TAG, "Invalid style " + id);
			return null;
		}
		Set<String> result = baseLayer.getCategories();

		// add the categories from overlays that are enabled
		for (XmlRenderThemeStyleLayer overlay : baseLayer.getOverlays()) {
			if (this.sharedPreferences.getBoolean(overlay.getId(), overlay.isEnabled())) {
				result.addAll(overlay.getCategories());
			}
		}

		return result;
	}

	protected String getRenderThemeFile() {
		return "renderthemes/rendertheme-v4.xml";
	}


	@Override
	public void onSharedPreferenceChanged(SharedPreferences preferences, String key) {
		super.onSharedPreferenceChanged(preferences, key);
		// difficult to know which render theme options have changed since we
		// do not know all the keys, so we just redraw the map whenever there
		// is a change in the settings.
		AndroidUtil.restartActivity(this);
	}

}
