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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/*
 * An individual layer in the rendertheme V4+ menu system.
 * A layer can have translations, categories that will always be enabled
 * when the layer is selected as well as optional overlays.
 */
public class XmlRenderThemeStyleLayer implements Serializable {
    private static final long serialVersionUID = 1L;

    private final Set<String> categories;
    private final String defaultLanguage;
    private final String id;
    private final List<XmlRenderThemeStyleLayer> overlays;
    private final Map<String, String> titles;
    private final boolean visible;
    private final boolean enabled;

    XmlRenderThemeStyleLayer(String id, boolean visible, boolean enabled, String defaultLanguage) {
        this.id = id;
        this.titles = new HashMap<String, String>();
        this.categories = new LinkedHashSet<>();
        this.visible = visible;
        this.defaultLanguage = defaultLanguage;
        this.enabled = enabled;
        this.overlays = new ArrayList<XmlRenderThemeStyleLayer>();
    }

    public void addCategory(String category) {
        this.categories.add(category);
    }

    public void addOverlay(XmlRenderThemeStyleLayer overlay) {
        this.overlays.add(overlay);
    }

    public void addTranslation(String language, String name) {
        this.titles.put(language, name);
    }

    public Set<String> getCategories() {
        return this.categories;
    }

    public String getId() {
        return this.id;
    }

    public List<XmlRenderThemeStyleLayer> getOverlays() {
        return this.overlays;
    }

    public String getTitle(String language) {
        String result = this.titles.get(language);
        if (result == null) {
            return this.titles.get(this.defaultLanguage);
        }
        return result;
    }

    public Map<String, String> getTitles() {
        return this.titles;
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public boolean isVisible() {
        return this.visible;
    }
}
