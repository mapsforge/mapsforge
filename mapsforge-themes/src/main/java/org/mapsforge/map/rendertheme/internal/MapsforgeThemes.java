/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright 2016-2021 devemux86
 * Copyright 2021 eddiemuc
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
package org.mapsforge.map.rendertheme.internal;

import org.mapsforge.map.rendertheme.XmlRenderTheme;
import org.mapsforge.map.rendertheme.XmlRenderThemeMenuCallback;
import org.mapsforge.map.rendertheme.XmlThemeResourceProvider;

import java.io.InputStream;

/**
 * Enumeration of all internal rendering themes.
 */
public enum MapsforgeThemes implements XmlRenderTheme {

    DEFAULT("/assets/mapsforge/default.xml"),
    MOTORIDER("/assets/mapsforge/motorider.xml"),
    MOTORIDER_DARK("/assets/mapsforge/motorider-dark.xml"),
    OSMARENDER("/assets/mapsforge/osmarender.xml");

    private XmlRenderThemeMenuCallback menuCallback;
    private final String path;

    MapsforgeThemes(String path) {
        this.path = path;
    }

    @Override
    public XmlRenderThemeMenuCallback getMenuCallback() {
        return menuCallback;
    }

    /**
     * @return the prefix for all relative resource paths.
     */
    @Override
    public String getRelativePathPrefix() {
        return "/assets/";
    }

    @Override
    public InputStream getRenderThemeAsStream() {
        return getClass().getResourceAsStream(this.path);
    }

    @Override
    public XmlThemeResourceProvider getResourceProvider() {
        return null;
    }

    @Override
    public void setMenuCallback(XmlRenderThemeMenuCallback menuCallback) {
        this.menuCallback = menuCallback;
    }

    @Override
    public void setResourceProvider(XmlThemeResourceProvider resourceProvider) {
    }
}
