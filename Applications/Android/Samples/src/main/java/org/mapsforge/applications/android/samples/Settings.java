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

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

/**
 * Activity to edit the application preferences.
 */
public class Settings extends PreferenceActivity implements OnSharedPreferenceChangeListener {
	SharedPreferences prefs;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.prefs = PreferenceManager.getDefaultSharedPreferences(this);
		Utils.enableHome(this);
		addPreferencesFromResource(R.xml.preferences);
	}

	@Override
	protected void onResume() {
		super.onResume();
		this.prefs.registerOnSharedPreferenceChangeListener(this);
	}

	@Override
	protected void onPause() {
		super.onPause();
		this.prefs.unregisterOnSharedPreferenceChangeListener(this);
	}

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
	public void onSharedPreferenceChanged(SharedPreferences preferences, String key) {
		if (SamplesApplication.SETTING_SCALE.equals(key)) {
			float userScaleFactor = DisplayModel.getDefaultUserScaleFactor();
			float fs = Float.valueOf(preferences.getString(SamplesApplication.SETTING_SCALE, Float.toString(userScaleFactor)));
			Log.e(SamplesApplication.TAG, "User ScaleFactor " + Float.toString(fs));
			if (fs != userScaleFactor) {
				DisplayModel.setDefaultUserScaleFactor(fs);
			}
		}
	}
}
