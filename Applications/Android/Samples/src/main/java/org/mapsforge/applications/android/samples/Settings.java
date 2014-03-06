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
import org.mapsforge.map.rendertheme.XmlRenderThemeStyleMenu;
import org.mapsforge.map.rendertheme.XmlRenderThemeStyleMenuEntry;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import java.util.Locale;
import java.util.Map;

/**
 * Activity to edit the application preferences.
 */
public class Settings extends PreferenceActivity implements
		OnSharedPreferenceChangeListener {

	public static final String RENDERTHEME_MENU = "renderthememenu";

	SharedPreferences prefs;

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
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.prefs = PreferenceManager.getDefaultSharedPreferences(this);
		Utils.enableHome(this);
		addPreferencesFromResource(R.xml.preferences);

		// if the render theme has a style menu, its data is delivered via the intent
		XmlRenderThemeStyleMenu xmlStyleMenu = (XmlRenderThemeStyleMenu) getIntent().getSerializableExtra(RENDERTHEME_MENU);
		if (xmlStyleMenu != null) {

			// the preference category is hard-wired into the Samples app and serves as
			// the hook to add a list preference to allow users to select a style
			PreferenceCategory renderthemeMenu = (PreferenceCategory) findPreference(RENDERTHEME_MENU);
			ListPreference listPreference = new ListPreference(this);

			// the id of the setting is the id of the stylemenu, that allows this
			// app to store different settings for different render themes.
			listPreference.setKey(xmlStyleMenu.getId());

			listPreference.setTitle("Map style");
			listPreference.setSummary("Customize the appearance of the map");

			// this is the user language for the app, in 'en', 'de' etc format
			// no dialects are supported at the moment
			String language = Locale.getDefault().getLanguage();

			// build data structure for the ListPreference
			Map<String, XmlRenderThemeStyleMenuEntry> styles = xmlStyleMenu.getStyles();

			int visibleStyles = 0;
			for (XmlRenderThemeStyleMenuEntry style : styles.values()) {
				if (style.isVisible()) {
					++visibleStyles;
				}
			}

					CharSequence[] entries = new CharSequence[visibleStyles];
			CharSequence[] values = new CharSequence[visibleStyles];
			int i = 0;
			for (XmlRenderThemeStyleMenuEntry style : styles.values()) {
				if (style.isVisible()) {
					// build up the entries in the list
					entries[i] = style.getTitles().get(language);
					if (entries[i] == null) {
						// if we cannot find the value for the user language, use default language
						entries[i] = style.getTitles().get(xmlStyleMenu.getDefaultLanguage());
					}
					values[i] = style.getId();
					++i;
				}
			}

			listPreference.setEntries(entries);
			listPreference.setEntryValues(values);
			listPreference.setEnabled(true);
			listPreference.setPersistent(true);
			listPreference.setDefaultValue(xmlStyleMenu.getDefaultValue());
			renderthemeMenu.addPreference(listPreference);
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
}
