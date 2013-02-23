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
package org.mapsforge.map.controller;

import org.junit.Assert;
import org.junit.Test;
import org.mapsforge.core.model.Dimension;
import org.mapsforge.core.model.GeoPoint;
import org.mapsforge.core.model.MapPosition;
import org.mapsforge.map.model.Model;

public class FrameBufferControllerTest {
	@Test
	public void frameBufferControllerTest() {
		DummyFrameBufferInterface dummyFrameBufferInterface = new DummyFrameBufferInterface();
		Model model = new Model();
		new FrameBufferController(dummyFrameBufferInterface, model);

		model.frameBufferModel.setMapPosition(model.mapViewPosition.getMapPosition());
		model.mapViewModel.setDimension(new Dimension(100, 200));
		Assert.assertEquals(new Dimension(100, 200), dummyFrameBufferInterface.lastMapViewDimension);
		Assert.assertEquals(new Dimension(150, 300), dummyFrameBufferInterface.dimension);

		model.mapViewPosition.setMapPosition(new MapPosition(new GeoPoint(0, 180), (byte) 1));
		Assert.assertEquals(-256, dummyFrameBufferInterface.lastDiffX, 0);
		Assert.assertEquals(0, dummyFrameBufferInterface.lastDiffY, 0);
		Assert.assertEquals(2, dummyFrameBufferInterface.lastScaleFactor, 0);
	}
}
