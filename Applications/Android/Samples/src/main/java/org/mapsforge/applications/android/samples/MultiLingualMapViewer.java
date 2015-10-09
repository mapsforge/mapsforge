/*
 * Copyright 2015 Ludwig M Brinckmann
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

import android.util.Log;

import org.mapsforge.map.datastore.MapDataStore;
import org.mapsforge.map.reader.MapFile;

import java.io.File;

/**
 * Demonstration of multilingual maps. Multilingual map must be loaded, the language options do
 * not come from the map file but from the settings menu.
 *
 * In the settings it is possible to specify to just return one language or a combination of the
 * default and the user selected language. This is also an example how names can be styled prior to
 * rendering.
 */
public class MultiLingualMapViewer extends RenderTheme4 {

	@Override
	protected MapDataStore getMapFile() {
		String language = sharedPreferences.getString(SamplesApplication.SETTING_PREFERRED_LANGUAGE, null);
		if (language.isEmpty()) {
			language = null;
		}
		final String userLanguage = language;
		if (userLanguage == null || !sharedPreferences.getBoolean(SamplesApplication.SETTING_LANGUAGE_SHOWLOCAL, false)) {
			Log.i(SamplesApplication.TAG, "Preferred language " + userLanguage);
			return new MapFile(new File(getMapFileDirectory(), this.getMapFileName()), language);
		} else {
			Log.i(SamplesApplication.TAG, "Default + preferred language " + userLanguage );
			return new MapFile(new File(getMapFileDirectory(), this.getMapFileName()), userLanguage) {
				@Override
				protected String extractLocalized(String s) {
					String local = MapDataStore.extract(s, null);
					String user = MapDataStore.extract(s, userLanguage);
					if (local.equals(user)) {
						return local;
					}
					return new StringBuilder(local).append( " (").append(user).append(")").toString();
				}
			};
		}
	}
}
