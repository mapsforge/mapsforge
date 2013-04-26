package org.mapsforge.applications.android.samples.dummy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.mapsforge.core.model.LatLong;

/**
 * Helper class for providing sample content for user interfaces created by Android template wizards.
 * <p>
 * TODO: Replace all uses of this class before publishing your app.
 */
public class DummyContent {

	/**
	 * An array of sample (dummy) items.
	 */
	public static List<DummyItem> ITEMS = new ArrayList<DummyItem>();

	/**
	 * A map of sample (dummy) items, by ID.
	 */
	public static Map<String, DummyItem> ITEM_MAP = new HashMap<String, DummyItem>();

	static {
		addItem(new DummyItem("1", "Brandenburger Tor", new LatLong(52.516, 13.378),
				"This is the famous Brandenburger Tor"));
		addItem(new DummyItem("2", "Checkpoint Charlie", new LatLong(52.507, 13.390),
				"This used to be the famous Checkpoint Charlie"));
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

	/**
	 * A dummy item representing a piece of content.
	 */
	public static class DummyItem {
		public String id;
		public String content;
		public LatLong location;
		public String text;

		public DummyItem(String id, String content, LatLong location, String text) {
			this.id = id;
			this.content = content;
			this.location = location;
			this.text = text;
		}

		@Override
		public String toString() {
			return content;
		}
	}
}
