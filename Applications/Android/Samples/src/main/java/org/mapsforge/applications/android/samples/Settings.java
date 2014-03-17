/*
 * Copyright 2011 Applantation.com
 * Copyright 2013-2014 Ludwig M Brinckmann
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.mapsforge.applications.android.samples;

import org.mapsforge.map.model.DisplayModel;
import org.mapsforge.map.rendertheme.XmlRenderThemeStyleLayer;
import org.mapsforge.map.rendertheme.XmlRenderThemeStyleMenu;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Activity to edit the application preferences.
 */
public class Settings extends PreferenceActivity implements
		OnSharedPreferenceChangeListener {

	public static final String RENDERTHEME_MENU = "renderthememenu";

	ListPreference baseLayerPreference;
	SharedPreferences prefs;
	XmlRenderThemeStyleMenu renderthemeOptions;
	PreferenceCategory renderthemeMenu;

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Intent intent;
		switch (item.getItemId()) {
			case android.R.id.home:
				intent = new Intent(this, Samples.class);
				startActivity(intent);
				return true;
		}
		return false;
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences preferences,
	                                      String key) {
		if (SamplesApplication.SETTING_SCALE.equals(key)) {
			float userScaleFactor = DisplayModel.getDefaultUserScaleFactor();
			float fs = Float.valueOf(preferences.getString(
					SamplesApplication.SETTING_SCALE,
					Float.toString(userScaleFactor)));
			Log.e(SamplesApplication.TAG,
					"User ScaleFactor " + Float.toString(fs));
			if (fs != userScaleFactor) {
				DisplayModel.setDefaultUserScaleFactor(fs);
			}
		} else if (this.renderthemeOptions != null && this.renderthemeOptions.getId().equals(key)) {
			createRenderthemeMenu();
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.prefs = PreferenceManager.getDefaultSharedPreferences(this);
		Utils.enableHome(this);
		addPreferencesFromResource(R.xml.preferences);

		// if the render theme has a style menu, its data is delivered via the intent
		renderthemeOptions = (XmlRenderThemeStyleMenu) getIntent().getSerializableExtra(RENDERTHEME_MENU);
		if (renderthemeOptions != null) {

			// the preference category is hard-wired into the Samples app and serves as
			// the hook to add a list preference to allow users to select a style
			this.renderthemeMenu = (PreferenceCategory) findPreference(RENDERTHEME_MENU);

			createRenderthemeMenu();
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		this.prefs.unregisterOnSharedPreferenceChangeListener(this);
	}

	@Override
	protected void onResume() {
		super.onResume();
		this.prefs.registerOnSharedPreferenceChangeListener(this);
	}


	private void createRenderthemeMenu() {
		this.renderthemeMenu.removeAll();

		this.baseLayerPreference = new ListPreference(this);

		// the id of the setting is the id of the stylemenu, that allows this
		// app to store different settings for different render themes.
		baseLayerPreference.setKey(this.renderthemeOptions.getId());

		baseLayerPreference.setTitle("Map style");

		// this is the user language for the app, in 'en', 'de' etc format
		// no dialects are supported at the moment
		String language = Locale.getDefault().getLanguage();

		// build data structure for the ListPreference
		Map<String, XmlRenderThemeStyleLayer> baseLayers = renderthemeOptions.getLayers();

		int visibleStyles = 0;
		for (XmlRenderThemeStyleLayer baseLayer : baseLayers.values()) {
			if (baseLayer.isVisible()) {
				++visibleStyles;
			}
		}

		CharSequence[] entries = new CharSequence[visibleStyles];
		CharSequence[] values = new CharSequence[visibleStyles];
		int i = 0;
		for (XmlRenderThemeStyleLayer baseLayer : baseLayers.values()) {
			if (baseLayer.isVisible()) {
				// build up the entries in the list
				entries[i] = baseLayer.getTitle(language);
				values[i] = baseLayer.getId();
				++i;
			}
		}

		baseLayerPreference.setEntries(entries);
		baseLayerPreference.setEntryValues(values);
		baseLayerPreference.setEnabled(true);
		baseLayerPreference.setPersistent(true);
		baseLayerPreference.setDefaultValue(renderthemeOptions.getDefaultValue());

		renderthemeMenu.addPreference(baseLayerPreference);

		String selection = baseLayerPreference.getValue();
		if (selection == null) {
			selection = renderthemeOptions.getLayer(renderthemeOptions.getDefaultValue()).getId();
		}
		// the new Android style is to display information here, not instruction
		baseLayerPreference.setSummary(renderthemeOptions.getLayer(selection).getTitle(language));

		for (XmlRenderThemeStyleLayer overlay : this.renderthemeOptions.getLayer(selection).getOverlays()) {
			CheckBoxPreference checkbox = new CheckBoxPreference(this);
			checkbox.setKey(overlay.getId());
			checkbox.setPersistent(true);
			checkbox.setTitle(overlay.getTitle(language));
			if (findPreference(overlay.getId()) == null)  {
				// value has never been set, so set from default
				checkbox.setChecked(overlay.isEnabled());
			}
			this.renderthemeMenu.addPreference(checkbox);
		}
	}
}
