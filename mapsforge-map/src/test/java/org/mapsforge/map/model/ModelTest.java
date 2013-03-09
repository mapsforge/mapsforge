/*
 * Copyright 2010, 2011, 2012 mapsforge.org
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
package org.mapsforge.map.model;

import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.MapPosition;
import org.mapsforge.map.model.common.JavaUtilPreferences;
import org.mapsforge.map.model.common.PreferencesFacade;

public class ModelTest {
	private final java.util.prefs.Preferences preferences = Preferences.userNodeForPackage(ModelTest.class);

	@After
	public void afterTest() throws BackingStoreException {
		this.preferences.removeNode();
	}

	@Test
	public void constructorTest() {
		Model model = new Model();
		Assert.assertNotNull(model.frameBufferModel);
		Assert.assertNotNull(model.mapViewModel);
		Assert.assertNotNull(model.mapViewPosition);
	}

	@Test
	public void saveAndInitTest() {
		MapPosition mapPosition1 = new MapPosition(new LatLong(1, 1), (byte) 1);
		MapPosition mapPosition2 = new MapPosition(new LatLong(2, 2), (byte) 2);

		Model model = new Model();
		model.mapViewPosition.setMapPosition(mapPosition1);
		Assert.assertEquals(mapPosition1, model.mapViewPosition.getMapPosition());

		PreferencesFacade preferencesFacade = new JavaUtilPreferences(this.preferences);
		model.save(preferencesFacade);

		model.mapViewPosition.setMapPosition(mapPosition2);
		Assert.assertEquals(mapPosition2, model.mapViewPosition.getMapPosition());

		model.init(preferencesFacade);
		Assert.assertEquals(mapPosition1, model.mapViewPosition.getMapPosition());
	}
}
