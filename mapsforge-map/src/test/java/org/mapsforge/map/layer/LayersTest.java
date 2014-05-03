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
package org.mapsforge.map.layer;

import org.junit.Assert;
import org.junit.Test;

public class LayersTest {
	static class DummyRedrawer implements Redrawer {
		@Override
		public void redrawLayers() {
			// do nothing
		}
	}

	private static void checkCallbacks(DummyLayer dummyLayer, int expectedAddCalls, int expectedRemoveCalls) {
		Assert.assertEquals(expectedAddCalls, dummyLayer.onAddCalls);
		Assert.assertEquals(expectedRemoveCalls, dummyLayer.onRemoveCalls);
	}

	@Test
	public void callbackTest() {
		Layers layers = new Layers(new DummyRedrawer());

		DummyLayer dummyLayer = new DummyLayer();
		checkCallbacks(dummyLayer, 0, 0);

		layers.add(dummyLayer);
		checkCallbacks(dummyLayer, 1, 0);

		layers.remove(dummyLayer);
		checkCallbacks(dummyLayer, 1, 1);

		layers.add(0, dummyLayer);
		checkCallbacks(dummyLayer, 2, 1);

		layers.clear();
		checkCallbacks(dummyLayer, 2, 2);
	}

	@Test
	public void isEmptyTest() {
		Layers layers = new Layers(new DummyRedrawer());
		Assert.assertTrue(layers.isEmpty());

		layers.add(new DummyLayer());
		Assert.assertFalse(layers.isEmpty());

		layers.clear();
		Assert.assertTrue(layers.isEmpty());
	}

	@Test
	public void sizeTest() {
		Layers layers = new Layers(new DummyRedrawer());
		Assert.assertEquals(0, layers.size());

		layers.add(new DummyLayer());
		Assert.assertEquals(1, layers.size());

		layers.clear();
		Assert.assertEquals(0, layers.size());
	}
}
