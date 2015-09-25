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
 */
public class MultiLingualMapViewer extends RenderTheme4 {

	@Override
	protected MapDataStore getMapFile() {
		String language = sharedPreferences.getString(SamplesApplication.SETTING_PREFERRED_LANGUAGE, null);
		if (language.isEmpty()) {
			language = null;
		}
		Log.i(SamplesApplication.TAG, "Preferred language " + language);
		return new MapFile(new File(getMapFileDirectory(), this.getMapFileName()), language);
	}
}
