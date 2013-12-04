package org.mapsforge.applications.android.samples;

import org.mapsforge.map.android.graphics.AndroidGraphicFactory;

import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * @author ludwig
 */
public class SamplesApplication extends Application {

	@Override
	public void onCreate() {
		super.onCreate();
		AndroidGraphicFactory.createInstance(this);
		Log.e("SAMPLES", "ScaleFactor " + Float.toString(AndroidGraphicFactory.INSTANCE.getScaleFactor()));
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		float userScaleFactor = AndroidGraphicFactory.INSTANCE.getUserScaleFactor();
		float fs = Float.valueOf(preferences.getString("scale", Float.toString(userScaleFactor)));
		Log.e("SAMPLES", "User ScaleFactor " + Float.toString(fs));
		if (fs != userScaleFactor) {
			AndroidGraphicFactory.INSTANCE.setUserScaleFactor(fs);
		}
	}
}
