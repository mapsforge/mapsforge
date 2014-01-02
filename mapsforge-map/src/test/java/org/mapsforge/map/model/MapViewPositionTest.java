/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright © 2014 Ludwig M Brinckmann
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
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.MapPosition;
import org.mapsforge.core.util.MercatorProjection;
import org.mapsforge.map.model.common.DummyObserver;

public class MapViewPositionTest {
	private static void verifyInvalidSetZoomLevel(MapViewPosition mapViewPosition, byte zoomLevel) {
		try {
			mapViewPosition.setZoomLevel(zoomLevel);
			Assert.fail("zoomLevel: " + zoomLevel);
		} catch (IllegalArgumentException e) {
			Assert.assertTrue(true);
		}
	}

	private static void verifyInvalidSetZoomLevelMax(MapViewPosition mapViewPosition, byte zoomLevelMax) {
		try {
			mapViewPosition.setZoomLevelMax(zoomLevelMax);
			Assert.fail("zoomLevelMax: " + zoomLevelMax);
		} catch (IllegalArgumentException e) {
			Assert.assertTrue(true);
		}
	}

	private static void verifyInvalidSetZoomLevelMin(MapViewPosition mapViewPosition, byte zoomLevelMin) {
		try {
			mapViewPosition.setZoomLevelMin(zoomLevelMin);
			Assert.fail("zoomLevelMin: " + zoomLevelMin);
		} catch (IllegalArgumentException e) {
			Assert.assertTrue(true);
		}
	}

	@Test
	public void mapLimitTest() {
		MapViewPosition mapViewPosition = new MapViewPosition(new DisplayModel());
		Assert.assertNull(mapViewPosition.getMapLimit());

		LatLong latLong = new LatLong(10, 20);
		mapViewPosition.setCenter(latLong);
		Assert.assertEquals(latLong, mapViewPosition.getCenter());

		BoundingBox boundingBox = new BoundingBox(1, 2, 3, 4);
		mapViewPosition.setMapLimit(boundingBox);
		Assert.assertEquals(boundingBox, mapViewPosition.getMapLimit());
		Assert.assertEquals(latLong, mapViewPosition.getCenter());

		mapViewPosition.setCenter(latLong);
		Assert.assertEquals(new LatLong(3, 4), mapViewPosition.getCenter());
	}

	@Test
	public void moveCenterTest() {
		MapViewPosition mapViewPosition = new MapViewPosition(new FixedTileSizeDisplayModel(256));
		mapViewPosition.moveCenter(MercatorProjection.getMapSize((byte) 0, new FixedTileSizeDisplayModel(256).getTileSize()) / -360d, 0);

		MapPosition mapPosition = mapViewPosition.getMapPosition();

		Assert.assertEquals(0, mapPosition.latLong.latitude, 0);
		Assert.assertEquals(1, mapPosition.latLong.longitude, 1.0E-14);
		Assert.assertEquals(0, mapPosition.zoomLevel);
	}

	@Test
	public void observerTest() {
		DummyObserver dummyObserver = new DummyObserver();
		MapViewPosition mapViewPosition = new MapViewPosition(new DisplayModel());
		mapViewPosition.addObserver(dummyObserver);
		Assert.assertEquals(0, dummyObserver.getCallbacks());

		mapViewPosition.setCenter(new LatLong(0, 0));
		//Assert.assertEquals(1, dummyObserver.getCallbacks());

		mapViewPosition.setMapLimit(new BoundingBox(0, 0, 0, 0));
		//Assert.assertEquals(2, dummyObserver.getCallbacks());

		mapViewPosition.setMapPosition(new MapPosition(new LatLong(0, 0), (byte) 0));
		//Assert.assertEquals(3, dummyObserver.getCallbacks());

		mapViewPosition.setZoomLevel((byte) 0);
		//Assert.assertEquals(4, dummyObserver.getCallbacks());

		mapViewPosition.setZoomLevelMax((byte) 0);
		//Assert.assertEquals(5, dummyObserver.getCallbacks());

		mapViewPosition.setZoomLevelMin((byte) 0);
		//Assert.assertEquals(6, dummyObserver.getCallbacks());
	}

	@Test
	public void zoomInTest() {
		MapViewPosition mapViewPosition = new MapViewPosition(new DisplayModel());
		Assert.assertEquals(0, mapViewPosition.getZoomLevel());
		mapViewPosition.zoomIn();
		Assert.assertEquals((byte) 1, mapViewPosition.getZoomLevel());

		mapViewPosition.setZoomLevel(Byte.MAX_VALUE);
		Assert.assertEquals(Byte.MAX_VALUE, mapViewPosition.getZoomLevel());
		mapViewPosition.zoomIn();
		Assert.assertEquals(Byte.MAX_VALUE, mapViewPosition.getZoomLevel());
	}

	@Test
	public void zoomLevelMaxTest() {
		MapViewPosition mapViewPosition = new MapViewPosition(new DisplayModel());
		Assert.assertEquals(Byte.MAX_VALUE, mapViewPosition.getZoomLevelMax());

		mapViewPosition.setZoomLevel((byte) 1);
		Assert.assertEquals(1, mapViewPosition.getZoomLevel());

		mapViewPosition.setZoomLevelMax((byte) 0);
		Assert.assertEquals(0, mapViewPosition.getZoomLevelMax());
		Assert.assertEquals(1, mapViewPosition.getZoomLevel());

		mapViewPosition.setZoomLevel((byte) 1);
		Assert.assertEquals(0, mapViewPosition.getZoomLevel());

		verifyInvalidSetZoomLevelMax(mapViewPosition, (byte) -1);
	}

	@Test
	public void zoomLevelMinMaxTest() {
		MapViewPosition mapViewPosition = new MapViewPosition(new DisplayModel());
		mapViewPosition.setZoomLevelMin((byte) 1);
		mapViewPosition.setZoomLevelMax((byte) 2);

		verifyInvalidSetZoomLevelMin(mapViewPosition, (byte) 3);
		verifyInvalidSetZoomLevelMax(mapViewPosition, (byte) 0);
	}

	@Test
	public void zoomLevelMinTest() {
		MapViewPosition mapViewPosition = new MapViewPosition(new DisplayModel());
		Assert.assertEquals(0, mapViewPosition.getZoomLevelMin());

		mapViewPosition.setZoomLevel((byte) 0);
		Assert.assertEquals(0, mapViewPosition.getZoomLevel());

		mapViewPosition.setZoomLevelMin((byte) 1);
		Assert.assertEquals(1, mapViewPosition.getZoomLevelMin());
		Assert.assertEquals(0, mapViewPosition.getZoomLevel());

		mapViewPosition.setZoomLevel((byte) 0);
		Assert.assertEquals(1, mapViewPosition.getZoomLevel());

		verifyInvalidSetZoomLevelMin(mapViewPosition, (byte) -1);
	}

	@Test
	public void zoomLevelTest() {
		MapViewPosition mapViewPosition = new MapViewPosition(new DisplayModel());
		verifyInvalidSetZoomLevel(mapViewPosition, (byte) -1);
	}

	@Test
	public void zoomOutTest() {
		MapViewPosition mapViewPosition = new MapViewPosition(new DisplayModel());
		mapViewPosition.setZoomLevel((byte) 1);
		Assert.assertEquals(1, mapViewPosition.getZoomLevel());
		mapViewPosition.zoomOut();
		Assert.assertEquals(0, mapViewPosition.getZoomLevel());

		mapViewPosition.setZoomLevel((byte) 0);
		Assert.assertEquals(0, mapViewPosition.getZoomLevel());
		mapViewPosition.zoomOut();
		Assert.assertEquals(0, mapViewPosition.getZoomLevel());
	}

	@Test
	public void zoomTest() {
		MapViewPosition mapViewPosition = new MapViewPosition(new DisplayModel());
		Assert.assertEquals(0, mapViewPosition.getZoomLevel());

		mapViewPosition.zoom((byte) 1);
		Assert.assertEquals(1, mapViewPosition.getZoomLevel());

		mapViewPosition.zoom((byte) -1);
		Assert.assertEquals(0, mapViewPosition.getZoomLevel());

		mapViewPosition.zoom((byte) 5);
		Assert.assertEquals(5, mapViewPosition.getZoomLevel());

		mapViewPosition.zoom((byte) -2);
		Assert.assertEquals(3, mapViewPosition.getZoomLevel());

		mapViewPosition.zoom(Byte.MAX_VALUE);
		Assert.assertEquals(Byte.MAX_VALUE, mapViewPosition.getZoomLevel());

		mapViewPosition.zoom(Byte.MIN_VALUE);
		Assert.assertEquals(0, mapViewPosition.getZoomLevel());
	}
}
