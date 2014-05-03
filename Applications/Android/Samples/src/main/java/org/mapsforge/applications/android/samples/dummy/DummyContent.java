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
package org.mapsforge.applications.android.samples.dummy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.mapsforge.core.model.LatLong;

/**
 * Helper class for providing sample content for user interfaces created by
 * Android template wizards.
 * <p>
 * TODO: Replace all uses of this class before publishing your app.
 */
public class DummyContent {
	/**
	 * A dummy item representing a piece of content.
	 */
	public static class DummyItem {
		public final String content;
		public final String id;
		public final LatLong location;
		public final String text;

		public DummyItem(String id, String content, LatLong location,
				String text) {
			this.id = id;
			this.content = content;
			this.location = location;
			this.text = text;
		}

		@Override
		public String toString() {
			return this.content;
		}
	}

	/**
	 * A map of sample (dummy) items, by ID.
	 */
	public static final Map<String, DummyItem> ITEM_MAP = new HashMap<String, DummyItem>();

	/**
	 * An array of sample (dummy) items.
	 */
	public static final List<DummyItem> ITEMS = new ArrayList<DummyItem>();

	static {
		addItem(new DummyItem("1", "Brandenburger Tor", new LatLong(52.516,
				13.378), "This is the famous Brandenburger Tor"));
		addItem(new DummyItem("2", "Checkpoint Charlie", new LatLong(52.507,
				13.390), "This used to be the famous Checkpoint Charlie"));
		addItem(new DummyItem(
				"3",
				"Savigny Platz",
				new LatLong(52.505, 13.322),
				"This is a square in Berlin with a longer text that does not really say anything at all and you would see more of the map if this useless text was not here."));
	}

	private static void addItem(DummyItem item) {
		ITEMS.add(item);
		ITEM_MAP.put(item.id, item);
	}
}
