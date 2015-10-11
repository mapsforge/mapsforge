/*
 * Copyright 2013-2015 Ludwig M Brinckmann
 * Copyright 2014 devemux86
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
import org.mapsforge.map.layer.renderer.MapWorkerPool;
import org.mapsforge.map.model.DisplayModel;
import org.mapsforge.map.reader.MapFile;

import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

public class SamplesApplication extends Application {

	public static final String TAG = "Mapsforge Samples";

	/*
	 * type to use for maps to store in the external files directory
	 */
	public static final String MAPS = "maps";

	public static final String SETTING_DEBUG_TIMING = "debug_timing";
	public static final String SETTING_SCALE = "scale";
	public static final String SETTING_TEXTWIDTH = "textwidth";
	public static final String SETTING_WAYFILTERING = "wayfiltering";
	public static final String SETTING_WAYFILTERING_DISTANCE = "wayfiltering_distance";
	public static final String SETTING_TILECACHE_PERSISTENCE = "tilecache_persistence";
	public static final String SETTING_RENDERING_THREADS = "rendering_threads";
	public static final String SETTING_PREFERRED_LANGUAGE = "language_selection";
	public static final String SETTING_LANGUAGE_SHOWLOCAL = "language_showlocal";

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

		MapFile.wayFilterEnabled = preferences.getBoolean(SETTING_WAYFILTERING, true);
		if (MapFile.wayFilterEnabled) {
			MapFile.wayFilterDistance = Integer.parseInt(preferences.getString(SETTING_WAYFILTERING_DISTANCE, "20"));
		}
		MapWorkerPool.DEBUG_TIMING = preferences.getBoolean(SETTING_DEBUG_TIMING, false);
		MapWorkerPool.NUMBER_OF_THREADS = Integer.parseInt(preferences.getString(SamplesApplication.SETTING_RENDERING_THREADS, Integer.toString(MapWorkerPool.DEFAULT_NUMBER_OF_THREADS)));

	}
}
