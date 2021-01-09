/*
 * Copyright 2020-2021 devemux86
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
package org.mapsforge.map.android.rendertheme;

import android.content.ContentResolver;
import android.net.Uri;
import org.mapsforge.map.rendertheme.XmlRenderTheme;
import org.mapsforge.map.rendertheme.XmlRenderThemeMenuCallback;
import org.mapsforge.map.rendertheme.XmlThemeResourceProvider;

import java.io.IOException;
import java.io.InputStream;

/**
 * An ContentRenderTheme allows for customizing the rendering style of the map
 * via an XML from the Android content providers.
 */
public class ContentRenderTheme implements XmlRenderTheme {

    private final ContentResolver contentResolver;
    private XmlRenderThemeMenuCallback menuCallback;
    private XmlThemeResourceProvider resourceProvider;
    private final Uri uri;

    /**
     * @param contentResolver the Android content resolver.
     * @param uri             the XML render theme URI.
     */
    public ContentRenderTheme(ContentResolver contentResolver, Uri uri) {
        this(contentResolver, uri, null);
    }

    /**
     * @param contentResolver the Android content resolver.
     * @param uri             the XML render theme URI.
     * @param menuCallback    the interface callback to create a settings menu on the fly.
     */
    public ContentRenderTheme(ContentResolver contentResolver, Uri uri, XmlRenderThemeMenuCallback menuCallback) {
        this.contentResolver = contentResolver;
        this.uri = uri;
        this.menuCallback = menuCallback;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (!(obj instanceof ContentRenderTheme)) {
            return false;
        }
        ContentRenderTheme other = (ContentRenderTheme) obj;
        try {
            if (getRenderThemeAsStream() != other.getRenderThemeAsStream()) {
                return false;
            }
        } catch (IOException e) {
            e.printStackTrace();
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
        return "";
    }

    @Override
    public InputStream getRenderThemeAsStream() throws IOException {
        return this.contentResolver.openInputStream(uri);
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
