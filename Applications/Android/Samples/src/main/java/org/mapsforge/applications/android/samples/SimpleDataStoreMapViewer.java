/*
 * Copyright 2014-2015 Ludwig M Brinckmann
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

import android.os.Bundle;

import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Tag;
import org.mapsforge.core.model.Tile;
import org.mapsforge.map.datastore.MapDataStore;
import org.mapsforge.map.datastore.MapReadResult;
import org.mapsforge.map.datastore.PointOfInterest;
import org.mapsforge.map.datastore.MultiMapDataStore;

import java.util.ArrayList;
import java.util.List;

/**
 * Illustrates the use of the MultiMapDataStore concept together with a non-mapfile data store.
 */
class PointDataStore extends MapDataStore {

	private final BoundingBox boundingBox = new BoundingBox(-90, -180, 90, 180);

	@Override
	public BoundingBox boundingBox() {
		return boundingBox;
	}

	@Override
	public void close() {
		// no-op
	}

	@Override
	public long getDataTimestamp(final Tile tile) {
		return 0;
	}

	@Override
	public MapReadResult readMapData(final Tile tile) {

		// a dummy operation that puts a fake turning_circle into the middle of each tile.
		// this is just to not having to change the rendertheme
		MapReadResult result = new MapReadResult();
		result.pointOfInterests = new ArrayList<>();
		result.ways = new ArrayList<>();
		Tag tag = new Tag("highway", "turning_circle");
		List<Tag> tags = new ArrayList<>();
		tags.add(tag);
		result.pointOfInterests.add(new PointOfInterest((byte) 5, tags, tile.getBoundingBox().getCenterPoint()));
		return result;
	}

	@Override
	public LatLong startPosition() {
		return null;
	}

	@Override
	public Byte startZoomLevel() {
		return null;
	}

	@Override
	public boolean supportsTile(final Tile tile) {
		return true;
	}
}

public class SimpleDataStoreMapViewer extends RenderTheme4 {

	private MultiMapDataStore multiMapDataStore;
	private PointDataStore pointDataStore;

	/**
	 * @return the base map file.
	 */
	protected MapDataStore getBaseMapFile() {
		return super.getMapFile();
	}

	@Override
	public MapDataStore getMapFile() {
		return this.multiMapDataStore;
	}

	/**
	 * @return the user data store.
	 */
	protected MapDataStore getUserDataStore() {
		return pointDataStore;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		pointDataStore = new PointDataStore();
		multiMapDataStore = new MultiMapDataStore(MultiMapDataStore.DataPolicy.RETURN_ALL);
		multiMapDataStore.addMapDataStore(getBaseMapFile(), true, true);
		multiMapDataStore.addMapDataStore(getUserDataStore(), false, false);

		super.onCreate(savedInstanceState);
	}
}
