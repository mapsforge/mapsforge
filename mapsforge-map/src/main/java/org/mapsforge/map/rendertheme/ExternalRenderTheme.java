/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright 2016-2020 devemux86
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

import java.io.*;

/**
 * An ExternalRenderTheme allows for customizing the rendering style of the map
 * via an XML file.
 */
public class ExternalRenderTheme implements XmlRenderTheme {
    private final long lastModifiedTime;
    private XmlRenderThemeMenuCallback menuCallback;
    private final File renderThemeFile;

    /**
     * @param renderThemeFile the XML render theme file.
     * @throws FileNotFoundException if the file does not exist or cannot be read.
     */
    public ExternalRenderTheme(File renderThemeFile) throws FileNotFoundException {
        this(renderThemeFile, null);
    }

    /**
     * @param renderThemeFile the XML render theme file.
     * @throws FileNotFoundException if the file does not exist or cannot be read.
     */
    public ExternalRenderTheme(File renderThemeFile, XmlRenderThemeMenuCallback menuCallback) throws FileNotFoundException {
        if (!renderThemeFile.exists()) {
            throw new FileNotFoundException("file does not exist: " + renderThemeFile.getAbsolutePath());
        } else if (!renderThemeFile.isFile()) {
            throw new FileNotFoundException("not a file: " + renderThemeFile.getAbsolutePath());
        } else if (!renderThemeFile.canRead()) {
            throw new FileNotFoundException("cannot read file: " + renderThemeFile.getAbsolutePath());
        }


        this.lastModifiedTime = renderThemeFile.lastModified();
        if (this.lastModifiedTime == 0L) {
            throw new FileNotFoundException("cannot read last modified time: " + renderThemeFile.getAbsolutePath());
        }
        this.renderThemeFile = renderThemeFile;
        this.menuCallback = menuCallback;
    }

    /**
     * @param renderThemePath the path of the XML render theme file.
     * @throws FileNotFoundException if the file does not exist or cannot be read.
     */
    public ExternalRenderTheme(String renderThemePath) throws FileNotFoundException {
        this(renderThemePath, null);
    }

    /**
     * @param renderThemePath the path of the XML render theme file.
     * @throws FileNotFoundException if the file does not exist or cannot be read.
     */
    public ExternalRenderTheme(String renderThemePath, XmlRenderThemeMenuCallback menuCallback) throws FileNotFoundException {
        this(new File(renderThemePath), menuCallback);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (!(obj instanceof ExternalRenderTheme)) {
            return false;
        }
        ExternalRenderTheme other = (ExternalRenderTheme) obj;
        if (this.lastModifiedTime != other.lastModifiedTime) {
            return false;
        }
        if (this.renderThemeFile == null) {
            if (other.renderThemeFile != null) {
                return false;
            }
        } else if (!this.renderThemeFile.equals(other.renderThemeFile)) {
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
        return this.renderThemeFile.getParent();
    }

    @Override
    public InputStream getRenderThemeAsStream() throws IOException {
        return new FileInputStream(this.renderThemeFile);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (this.lastModifiedTime ^ (this.lastModifiedTime >>> 32));
        result = prime * result + ((this.renderThemeFile == null) ? 0 : this.renderThemeFile.hashCode());
        return result;
    }

    @Override
    public void setMenuCallback(XmlRenderThemeMenuCallback menuCallback) {
        this.menuCallback = menuCallback;
    }
}
