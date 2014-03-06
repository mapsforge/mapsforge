/*
 * Copyright 2014 Ludwig M Brinckmann
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
package org.mapsforge.map.rendertheme;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class XmlRenderThemeStyleMenuEntry implements Serializable {
	private final Set<String> categories;
	private final String defaultLanguage;
	private final Map<String, String> descriptions;
	private final String id;
	private final Map<String, String> titles;
	private final boolean visible;

	XmlRenderThemeStyleMenuEntry(String id, String defaultLanguage, boolean visible) {
		this.defaultLanguage = defaultLanguage;
		this.id = id;
		this.titles = new HashMap<String, String>();
		this.descriptions = new HashMap<String, String>();
		this.categories = new HashSet<String>();
		this.visible = visible;
	}

	public void addCategory(String category) {
		this.categories.add(category);
	}

	public void addTitle(String language, String name) {
		titles.put(language, name);
	}

	public Set<String> getCategories() {
		return this.categories;
	}

	public String getId() {
		return this.id;
	}

	public Map<String, String> getTitles() {
		return this.titles;
	}

	public boolean isVisible() {
		return this.visible;
	}
}
