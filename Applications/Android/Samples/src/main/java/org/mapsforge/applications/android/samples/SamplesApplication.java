/*
 * Copyright 2013-2014 Ludwig M Brinckmann
 * Copyright © 2014 devemux86
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

import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.layer.renderer.MapWorker;
import org.mapsforge.map.model.DisplayModel;
import org.mapsforge.map.reader.MapDatabase;

import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * @author ludwig
 */
public class SamplesApplication extends Application {

	public static final String SETTING_DEBUG_TIMING = "debug_timing";
	public static final String SETTING_SCALE = "scale";
	public static final String SETTING_TEXTWIDTH = "textwidth";
	public static final String SETTING_WAYFILTERING = "wayfiltering";
	public static final String SETTING_WAYFILTERING_DISTANCE = "wayfiltering_distance";
	public static final String SETTING_TILECACHE_THREADING = "tilecache_threading";
	public static final String SETTING_TILECACHE_QUEUESIZE = "tilecache_queuesize";
	public static final String TAG = "SAMPLES APP";

	@Override
	public void onCreate() {
		super.onCreate();
		AndroidGraphicFactory.createInstance(this);
		Log.e(TAG,
				"Device scale factor "
						+ Float.toString(DisplayModel.getDeviceScaleFactor()));
		SharedPreferences preferences = PreferenceManager
				.getDefaultSharedPreferences(this);
		float fs = Float.valueOf(preferences.getString(SETTING_SCALE,
				Float.toString(DisplayModel.getDefaultUserScaleFactor())));
		Log.e(TAG, "User ScaleFactor " + Float.toString(fs));
		if (fs != DisplayModel.getDefaultUserScaleFactor()) {
			DisplayModel.setDefaultUserScaleFactor(fs);
		}

		MapDatabase.wayFilterEnabled = preferences.getBoolean(SETTING_WAYFILTERING, true);
		if (MapDatabase.wayFilterEnabled) {
			MapDatabase.wayFilterDistance = Integer.parseInt(preferences.getString(SETTING_WAYFILTERING_DISTANCE, "20"));
		}
		MapWorker.DEBUG_TIMING = preferences.getBoolean(SETTING_DEBUG_TIMING, false);
	}
}
