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

import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.model.DisplayModel;

import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * @author ludwig
 */
public class SamplesApplication extends Application {

	public static final String SETTING_SCALE = "scale";
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
	}
}
