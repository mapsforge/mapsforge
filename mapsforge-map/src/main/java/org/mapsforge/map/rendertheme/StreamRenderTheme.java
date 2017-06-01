/*
 * Copyright 2016-2017 devemux86
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

import java.io.InputStream;

/**
 * A StreamRenderTheme allows for customizing the rendering style of the map
 * via an XML input stream.
 */
public class StreamRenderTheme implements XmlRenderTheme {

    private final InputStream inputStream;
    private XmlRenderThemeMenuCallback menuCallback;
    private final String relativePathPrefix;

    /**
     * @param relativePathPrefix the prefix for all relative resource paths.
     * @param inputStream        an input stream containing valid render theme XML data.
     */
    public StreamRenderTheme(String relativePathPrefix, InputStream inputStream) {
        this(relativePathPrefix, inputStream, null);
    }

    /**
     * @param relativePathPrefix the prefix for all relative resource paths.
     * @param inputStream        an input stream containing valid render theme XML data.
     * @param menuCallback       the interface callback to create a settings menu on the fly.
     */
    public StreamRenderTheme(String relativePathPrefix, InputStream inputStream, XmlRenderThemeMenuCallback menuCallback) {
        this.relativePathPrefix = relativePathPrefix;
        this.inputStream = inputStream;
        this.menuCallback = menuCallback;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (!(obj instanceof StreamRenderTheme)) {
            return false;
        }
        StreamRenderTheme other = (StreamRenderTheme) obj;
        if (this.inputStream != other.inputStream) {
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
    public InputStream getRenderThemeAsStream() {
        return this.inputStream;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.inputStream == null) ? 0 : this.inputStream.hashCode());
        result = prime * result + ((this.relativePathPrefix == null) ? 0 : this.relativePathPrefix.hashCode());
        return result;
    }

    @Override
    public void setMenuCallback(XmlRenderThemeMenuCallback menuCallback) {
        this.menuCallback = menuCallback;
    }
}
