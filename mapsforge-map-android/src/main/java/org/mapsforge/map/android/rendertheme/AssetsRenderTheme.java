/*
 * Copyright 2010, 2011, 2012 mapsforge.org
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
package org.mapsforge.map.android.rendertheme;

import android.content.res.AssetManager;
import android.text.TextUtils;
import org.mapsforge.core.util.Utils;
import org.mapsforge.map.rendertheme.XmlRenderTheme;
import org.mapsforge.map.rendertheme.XmlRenderThemeMenuCallback;
import org.mapsforge.map.rendertheme.XmlThemeResourceProvider;

import java.io.IOException;
import java.io.InputStream;

/**
 * An AssetsRenderTheme allows for customizing the rendering style of the map
 * via an XML from the Android assets folder.
 */
public class AssetsRenderTheme implements XmlRenderTheme {

    private final AssetManager assetManager;
    private final String fileName;
    private XmlRenderThemeMenuCallback menuCallback;
    private final String relativePathPrefix;
    private XmlThemeResourceProvider resourceProvider;

    /**
     * @param assetManager       the Android asset manager.
     * @param relativePathPrefix the prefix for all relative resource paths.
     * @param fileName           the path to the XML render theme file.
     */
    public AssetsRenderTheme(AssetManager assetManager, String relativePathPrefix, String fileName) {
        this(assetManager, relativePathPrefix, fileName, null);
    }

    /**
     * @param assetManager       the Android asset manager.
     * @param relativePathPrefix the prefix for all relative resource paths.
     * @param fileName           the path to the XML render theme file.
     * @param menuCallback       the interface callback to create a settings menu on the fly.
     */
    public AssetsRenderTheme(AssetManager assetManager, String relativePathPrefix, String fileName, XmlRenderThemeMenuCallback menuCallback) {
        this.assetManager = assetManager;
        this.relativePathPrefix = relativePathPrefix;
        this.fileName = fileName;
        this.menuCallback = menuCallback;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (!(obj instanceof AssetsRenderTheme)) {
            return false;
        }
        AssetsRenderTheme other = (AssetsRenderTheme) obj;
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
        return this.assetManager.open((TextUtils.isEmpty(this.relativePathPrefix) ? "" : this.relativePathPrefix) + this.fileName);
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
