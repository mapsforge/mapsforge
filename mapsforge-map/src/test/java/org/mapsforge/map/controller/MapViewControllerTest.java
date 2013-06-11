/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
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
package org.mapsforge.map.controller;

import org.junit.Assert;
import org.junit.Test;
import org.mapsforge.map.model.Model;

public class MapViewControllerTest {
	@Test
	public void repaintTest() {
		DummyMapView dummyMapView = new DummyMapView();
		Model model = new Model();
		MapViewController.create(dummyMapView, model);
		Assert.assertEquals(0, dummyMapView.repaintCounter);

		model.mapViewPosition.setZoomLevel((byte) 1);
		Assert.assertEquals(1, dummyMapView.repaintCounter);
	}
}
