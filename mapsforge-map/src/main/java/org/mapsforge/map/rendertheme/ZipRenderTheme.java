/*
 * Copyright 2021 devemux86
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
package org.mapsforge.map.rendertheme;

import org.mapsforge.core.util.Utils;

import java.io.IOException;
import java.io.InputStream;

/**
 * A ZipRenderTheme allows for customizing the rendering style of the map
 * via an XML from an archive.
 */
public class ZipRenderTheme implements XmlRenderTheme {

    private XmlRenderThemeMenuCallback menuCallback;
    private final String relativePathPrefix;
    private XmlThemeResourceProvider resourceProvider;
    protected final String xmlTheme;

    /**
     * @param xmlTheme         the XML theme path in the archive.
     * @param resourceProvider the custom provider to retrieve resources internally referenced by "src" attribute (e.g. images, icons).
     */
    public ZipRenderTheme(String xmlTheme, XmlThemeResourceProvider resourceProvider) {
        this(xmlTheme, resourceProvider, null);
    }

    /**
     * @param xmlTheme         the XML theme path in the archive.
     * @param resourceProvider the custom provider to retrieve resources internally referenced by "src" attribute (e.g. images, icons).
     * @param menuCallback     the interface callback to create a settings menu on the fly.
     */
    public ZipRenderTheme(String xmlTheme, XmlThemeResourceProvider resourceProvider, XmlRenderThemeMenuCallback menuCallback) {
        this.xmlTheme = xmlTheme;
        this.resourceProvider = resourceProvider;
        this.menuCallback = menuCallback;

        this.relativePathPrefix = xmlTheme.substring(0, xmlTheme.lastIndexOf("/") + 1);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (!(obj instanceof ZipRenderTheme)) {
            return false;
        }
        ZipRenderTheme other = (ZipRenderTheme) obj;
        try {
            if (getRenderThemeAsStream() != other.getRenderThemeAsStream()) {
                return false;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        if (!Utils.equals(this.relativePathPrefix, other.relativePathPrefix)) {
            return false;
        }
        return true;
    }

    @Override
    public XmlRenderThemeMenuCallback getMenuCallback() {
        return this.menuCallback;
    }

    @Override
    public String getRelativePathPrefix() {
        return this.relativePathPrefix;
    }

    @Override
    public InputStream getRenderThemeAsStream() throws IOException {
        return this.resourceProvider.createInputStream(this.relativePathPrefix, this.xmlTheme.substring(this.xmlTheme.lastIndexOf("/") + 1));
    }

    @Override
    public XmlThemeResourceProvider getResourceProvider() {
        return this.resourceProvider;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        InputStream inputStream = null;
        try {
            inputStream = getRenderThemeAsStream();
        } catch (IOException e) {
            e.printStackTrace();
        }
        result = prime * result + ((inputStream == null) ? 0 : inputStream.hashCode());
        result = prime * result + ((this.relativePathPrefix == null) ? 0 : this.relativePathPrefix.hashCode());
        return result;
    }

    @Override
    public void setMenuCallback(XmlRenderThemeMenuCallback menuCallback) {
        this.menuCallback = menuCallback;
    }

    @Override
    public void setResourceProvider(XmlThemeResourceProvider resourceProvider) {
        this.resourceProvider = resourceProvider;
    }
}
