/*
 * Copyright 2014-2015 Ludwig M Brinckmann
 * Copyright 2015 devemux86
 * Copyright 2015 lincomatic
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
package org.mapsforge.map.datastore;

import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Tile;

import java.util.Locale;

/**
 * Base class for map data retrieval.
 */
public abstract class MapDataStore {

	/**
	 * Extracts substring of preferred language from multilingual string.<br/>
	 * Example multilingual string: "Base\ren\bEnglish\rjp\bJapan\rzh_py\bPin-yin".
	 * <p>
	 * Use '\r' delimiter among names and '\b' delimiter between each language and name.
	 */
	public static String extract(String s, String language) {
		if (s == null || s.trim().isEmpty()) {
			return null;
		}

		String[] langNames = s.split("\r");
		if (language == null || language.trim().isEmpty()) {
			return langNames[0];
		}

		String fallback = null;
		for (int i = 1; i < langNames.length; i++) {
			String[] langName = langNames[i].split("\b");
			if (langName.length != 2) {
				continue;
			}

			// Perfect match
			if (langName[0].equalsIgnoreCase(language)) {
				return langName[1];
			}

			// Fall back to base, e.g. zh-min-lan -> zh
			if (fallback == null && !langName[0].contains("-") && (language.contains("-") || language.contains("_"))
					&& language.toLowerCase(Locale.ENGLISH).startsWith(langName[0].toLowerCase(Locale.ENGLISH))) {
				fallback = langName[1];
			}
		}
		return (fallback != null) ? fallback : langNames[0];
	}

	/**
	 * the preferred language when extracting labels from this data store. The actual
	 * implementation is up to the concrete implementation, which can also simply ignore
	 * this setting.
	 */
	protected String preferredLanguage;

	/**
	 * Ctor for MapDataStore that will use default language.
	 */
	public MapDataStore() {
		this(null);
	}

	/**
	 * Ctor for MapDataStore setting preferred language.
	 * @param language the preferred language or null if default language is used.
	 */
	public MapDataStore(String language) {
		this.preferredLanguage = language;
	}

	/**
	 * Returns the area for which data is supplied.
	 * @return bounding box of area.
	 */
	public abstract BoundingBox boundingBox();

	/*
	 * Closes the map database.
	 */
	public abstract void close();

	/**
	 * Extracts substring of preferred language from multilingual string using
	 * the preferredLanguage setting.
	 */
	protected String extractLocalized(String s) {
		return MapDataStore.extract(s, preferredLanguage);
	}

	/**
	 * Returns the timestamp of the data used to render a specific tile.
	 *
	 * @param tile
	 *            A tile.
	 * @return the timestamp of the data used to render the tile
	 */
	public abstract long getDataTimestamp(Tile tile);

	/**
	 * Reads data for tile.
	 * @param tile tile for which data is requested.
	 * @return map data for the tile.
	 */
	public abstract MapReadResult readMapData(Tile tile);

	/**
	 * Gets the initial map position.
	 * @return the start position, if available.
	 */
	public abstract LatLong startPosition();

	/**
	 * Gets the initial zoom level.
	 * @return the start zoom level.
	 */
	public abstract Byte startZoomLevel();

	/**
	 * Returns true if MapDatabase contains tile.
	 * @param tile tile to be rendered.
	 * @return true if tile is part of database.
	 */
	public abstract boolean supportsTile(Tile tile);

}