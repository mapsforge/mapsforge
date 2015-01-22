/*
 * Copyright 2014 Ludwig M Brinckmann
 * Copyright 2015 devemux86
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

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.mapsforge.map.TestUtils;

public class DisplayModelTest {

	@Test
	public void equalsTest() {
		DisplayModel displayModel1 = new DisplayModel();
		DisplayModel displayModel2 = new DisplayModel();

		DisplayModel displayModel3 = new DisplayModel();
		displayModel3.setBackgroundColor(0xffff0000);

		DisplayModel displayModel4 = new DisplayModel();
		displayModel4.setFixedTileSize(512);

		DisplayModel displayModel5 = new DisplayModel();
		displayModel5.setMaxTextWidthFactor(0.5f);

		DisplayModel displayModel6 = new DisplayModel();
		displayModel6.setTileSizeMultiple(200);

		DisplayModel displayModel7 = new DisplayModel();
		displayModel7.setUserScaleFactor(0.3f);

		TestUtils.equalsTest(displayModel1, displayModel2);

		TestUtils.notEqualsTest(displayModel1, displayModel3);
		TestUtils.notEqualsTest(displayModel1, displayModel4);
		TestUtils.notEqualsTest(displayModel1, displayModel5);
		TestUtils.notEqualsTest(displayModel1, displayModel6);
		TestUtils.notEqualsTest(displayModel1, displayModel7);
		TestUtils.notEqualsTest(displayModel1, new Object());
		TestUtils.notEqualsTest(displayModel1, null);
	}

	@Test
	public void tileSizeTest() {
		DisplayModel displayModel = new DisplayModel();

		Assert.assertEquals(256, displayModel.getTileSize());
		displayModel.setUserScaleFactor(2);
		Assert.assertEquals(512, displayModel.getTileSize());
		displayModel.setUserScaleFactor(1.5f);
		Assert.assertEquals(384, displayModel.getTileSize());
		displayModel.setTileSizeMultiple(100);
		Assert.assertEquals(400, displayModel.getTileSize());
		displayModel.setUserScaleFactor(2);
		Assert.assertEquals(500, displayModel.getTileSize());
		displayModel.setTileSizeMultiple(64);
		Assert.assertEquals(512, displayModel.getTileSize());

		DisplayModel.setDeviceScaleFactor(2);
		displayModel.setUserScaleFactor(1);
		displayModel.setTileSizeMultiple(1);

		Assert.assertEquals(512, displayModel.getTileSize());
		displayModel.setUserScaleFactor(2);
		Assert.assertEquals(1024, displayModel.getTileSize());
		displayModel.setUserScaleFactor(1.5f);
		Assert.assertEquals(768, displayModel.getTileSize());
		displayModel.setTileSizeMultiple(100);
		Assert.assertEquals(800, displayModel.getTileSize());
		displayModel.setUserScaleFactor(2);
		Assert.assertEquals(1000, displayModel.getTileSize());
		displayModel.setTileSizeMultiple(64);
		Assert.assertEquals(1024, displayModel.getTileSize());

		DisplayModel.setDeviceScaleFactor(1);
		displayModel.setUserScaleFactor(1);
		displayModel.setTileSizeMultiple(1);

		Assert.assertEquals(256, displayModel.getTileSize());
		displayModel.setUserScaleFactor(2);
		Assert.assertEquals(512, displayModel.getTileSize());
		displayModel.setUserScaleFactor(1.5f);
		Assert.assertEquals(384, displayModel.getTileSize());
		displayModel.setTileSizeMultiple(100);
		Assert.assertEquals(400, displayModel.getTileSize());
		displayModel.setUserScaleFactor(2);
		Assert.assertEquals(500, displayModel.getTileSize());
		displayModel.setTileSizeMultiple(64);
		Assert.assertEquals(512, displayModel.getTileSize());

		DisplayModel.setDeviceScaleFactor(1.2f);
		displayModel.setUserScaleFactor(1);
		displayModel.setTileSizeMultiple(1);

		Assert.assertEquals(307, displayModel.getTileSize());
		displayModel.setUserScaleFactor(2);
		Assert.assertEquals(614, displayModel.getTileSize());
		displayModel.setUserScaleFactor(1.5f);
		Assert.assertEquals(461, displayModel.getTileSize());
		displayModel.setTileSizeMultiple(100);
		Assert.assertEquals(500, displayModel.getTileSize());
		displayModel.setUserScaleFactor(2);
		Assert.assertEquals(600, displayModel.getTileSize());
		displayModel.setTileSizeMultiple(64);
		Assert.assertEquals(640, displayModel.getTileSize());
	}

	@After
	public void tearDown() {
		// reset to 1 for all following tests
		DisplayModel.setDeviceScaleFactor(1);
	}

}
