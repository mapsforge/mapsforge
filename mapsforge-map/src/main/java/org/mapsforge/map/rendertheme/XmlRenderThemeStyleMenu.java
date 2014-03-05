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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 */
public class XmlRenderThemeStyleMenu implements Serializable {

	private final String defaultLanguage;
	private final String defaultValue;
	private final String id;
	private final Map<String, XmlRenderThemeStyleMenuEntry> styles;

	public XmlRenderThemeStyleMenu(String id, String defaultLanguage, String defaultValue) {
		this.defaultLanguage = defaultLanguage;
		this.defaultValue = defaultValue;
		this.id = id;
		this.styles = new HashMap<String, XmlRenderThemeStyleMenuEntry>();
	}

	public XmlRenderThemeStyleMenuEntry createStyle(String id) {
		XmlRenderThemeStyleMenuEntry style = new XmlRenderThemeStyleMenuEntry(id, this.defaultLanguage);
		this.styles.put(id, style);
		return style;
	}

	public String getDefaultLanguage() {
		return this.defaultLanguage;
	}

	public String getDefaultValue() {
		return this.defaultValue;
	}

	public String getId() {
		return this.id;
	}

	public XmlRenderThemeStyleMenuEntry getStyle(String id) {
		return this.styles.get(id);
	}

	public Map<String, XmlRenderThemeStyleMenuEntry> getStyles() {
		return this.styles;
	}
}
