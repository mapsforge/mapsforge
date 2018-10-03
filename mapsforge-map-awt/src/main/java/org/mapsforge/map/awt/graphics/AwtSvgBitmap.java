/*
 * Copyright 2015-2018 devemux86
 * Copyright 2018 Adrian Batzill
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
package org.mapsforge.map.awt.graphics;

import com.kitfox.svg.SVGCache;
import com.kitfox.svg.SVGDiagram;
import com.kitfox.svg.app.beans.SVGIcon;

import org.mapsforge.core.graphics.GraphicUtils;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

public class AwtSvgBitmap extends AwtResourceBitmap {
    /**
     * Default size is 20x20px (400px).
     */
    public static float DEFAULT_SIZE = 400f;

    public static BufferedImage getResourceBitmap(InputStream inputStream, String name, float scaleFactor, float defaultSize, int width, int height, int percent) throws IOException {
        try {
            URI uri = SVGCache.getSVGUniverse().loadSVG(inputStream, name);
            SVGDiagram diagram = SVGCache.getSVGUniverse().getDiagram(uri);

            double scale = scaleFactor / Math.sqrt((diagram.getHeight() * diagram.getWidth()) / defaultSize);

            float[] bmpSize = GraphicUtils.imageSize(diagram.getWidth(), diagram.getHeight(), (float) scale, width, height, percent);

            SVGIcon icon = new SVGIcon();
            icon.setAntiAlias(true);
            icon.setAutosize(SVGIcon.AUTOSIZE_STRETCH);
            icon.setPreferredSize(new Dimension((int) bmpSize[0], (int) bmpSize[1]));
            icon.setSvgURI(uri);
            BufferedImage bufferedImage = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
            icon.paintIcon(null, bufferedImage.createGraphics(), 0, 0);

            return bufferedImage;
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    private static BufferedImage getResourceBitmapImpl(InputStream inputStream, int hash, float scaleFactor, int width, int height, int percent) throws IOException {
        synchronized (SVGCache.getSVGUniverse()) {
            return getResourceBitmap(inputStream, Integer.toString(hash), scaleFactor, DEFAULT_SIZE, width, height, percent);
        }
    }

    public AwtSvgBitmap(InputStream inputStream, int hash, float scaleFactor, int width, int height, int percent) throws IOException {
        super(getResourceBitmapImpl(inputStream, hash, scaleFactor, width, height, percent));
    }
}
