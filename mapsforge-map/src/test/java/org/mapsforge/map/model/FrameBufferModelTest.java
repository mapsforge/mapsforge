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
package org.mapsforge.map.model;

import org.junit.Assert;
import org.junit.Test;
import org.mapsforge.core.model.Dimension;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.MapPosition;
import org.mapsforge.map.model.common.DummyObserver;

public class FrameBufferModelTest {
	private static void verifyInvalidOverdrawFactor(int overdrawFactor) {
		FrameBufferModel frameBufferModel = new FrameBufferModel();
		try {
			frameBufferModel.setOverdrawFactor(overdrawFactor);
			Assert.fail("overdrawFactor: " + overdrawFactor);
		} catch (IllegalArgumentException e) {
			Assert.assertTrue(true);
		}
	}

	@Test
	public void dimensionTest() {
		FrameBufferModel frameBufferModel = new FrameBufferModel();
		Assert.assertNull(frameBufferModel.getDimension());

		frameBufferModel.setDimension(new Dimension(0, 0));
		Assert.assertEquals(new Dimension(0, 0), frameBufferModel.getDimension());
	}

	@Test
	public void mapPositionTest() {
		FrameBufferModel frameBufferModel = new FrameBufferModel();
		Assert.assertNull(frameBufferModel.getMapPosition());

		frameBufferModel.setMapPosition(new MapPosition(new LatLong(0, 0), (byte) 0));
		Assert.assertEquals(new MapPosition(new LatLong(0, 0), (byte) 0), frameBufferModel.getMapPosition());
	}

	@Test
	public void observerTest() {
		DummyObserver dummyObserver = new DummyObserver();
		FrameBufferModel frameBufferModel = new FrameBufferModel();
		frameBufferModel.addObserver(dummyObserver);
		Assert.assertEquals(0, dummyObserver.getCallbacks());

		frameBufferModel.setDimension(new Dimension(0, 0));
		Assert.assertEquals(1, dummyObserver.getCallbacks());

		frameBufferModel.setMapPosition(new MapPosition(new LatLong(0, 0), (byte) 0));
		Assert.assertEquals(2, dummyObserver.getCallbacks());

		frameBufferModel.setOverdrawFactor(1);
		Assert.assertEquals(3, dummyObserver.getCallbacks());
	}

	@Test
	public void overdrawFactorTest() {
		FrameBufferModel frameBufferModel = new FrameBufferModel();
		Assert.assertEquals(1.2, frameBufferModel.getOverdrawFactor(), 0);

		frameBufferModel.setOverdrawFactor(2);
		Assert.assertEquals(2, frameBufferModel.getOverdrawFactor(), 0);

		verifyInvalidOverdrawFactor(0);
		verifyInvalidOverdrawFactor(-1);
	}
}
