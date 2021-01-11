/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright 2014 Ludwig M Brinckmann
 * Copyright 2014-2021 devemux86
 * Copyright 2018 Adrian Batzill
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

import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.graphics.ResourceBitmap;
import org.mapsforge.map.model.DisplayModel;
import org.mapsforge.map.rendertheme.renderinstruction.RenderInstruction;
import org.xmlpull.v1.XmlPullParserException;

import java.io.*;
import java.util.Locale;
import java.util.logging.Logger;

public final class XmlUtils {
    private static final Logger LOGGER = Logger.getLogger(XmlUtils.class.getName());

    private static final String PREFIX_ASSETS = "assets:";
    public static final String PREFIX_FILE = "file:";
    private static final String PREFIX_JAR = "jar:";

    private static final String PREFIX_JAR_V1 = "jar:/org/mapsforge/android/maps/rendertheme";

    private static final String UNSUPPORTED_COLOR_FORMAT = "unsupported color format: ";

    public static void checkMandatoryAttribute(String elementName, String attributeName, Object attributeValue)
            throws XmlPullParserException {
        if (attributeValue == null) {
            throw new XmlPullParserException("missing attribute '" + attributeName + "' for element: " + elementName);
        }
    }

    public static ResourceBitmap createBitmap(GraphicFactory graphicFactory, DisplayModel displayModel,
                                              String relativePathPrefix, String src, XmlThemeResourceProvider resourceProvider,
                                              int width, int height, int percent) throws IOException {
        if (src == null || src.length() == 0) {
            // no image source defined
            return null;
        }

        InputStream inputStream = createInputStream(graphicFactory, relativePathPrefix, src, resourceProvider);
        try {
            String absoluteName = getAbsoluteName(relativePathPrefix, src);
            // we need to hash with the width/height included as the same symbol could be required
            // in a different size and must be cached with a size-specific hash
            // we also need to include the resourceProvider as different providers may give different input streams for same source
            StringBuilder sb = new StringBuilder().append(absoluteName).append(width).append(height).append(percent);
            if (resourceProvider != null)
                sb.append(resourceProvider.hashCode());
            int hash = sb.toString().hashCode();
            if (src.toLowerCase(Locale.ENGLISH).endsWith(".svg")) {
                try {
                    return graphicFactory.renderSvg(inputStream, displayModel.getScaleFactor(), width, height, percent, hash);
                } catch (IOException e) {
                    throw new IOException("SVG render failed " + src, e);
                }
            }
            try {
                return graphicFactory.createResourceBitmap(inputStream, displayModel.getScaleFactor(), width, height, percent, hash);
            } catch (IOException e) {
                throw new IOException("Reading bitmap file failed " + src, e);
            }
        } finally {
            inputStream.close();
        }
    }

    public static XmlPullParserException createXmlPullParserException(String element, String name, String value, int attributeIndex) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("unknown attribute (");
        stringBuilder.append(attributeIndex);
        stringBuilder.append(") in element '");
        stringBuilder.append(element);
        stringBuilder.append("': ");
        stringBuilder.append(name);
        stringBuilder.append('=');
        stringBuilder.append(value);

        return new XmlPullParserException(stringBuilder.toString());
    }

    /**
     * Supported formats are {@code #RRGGBB} and {@code #AARRGGBB}.
     */
    public static int getColor(GraphicFactory graphicFactory, String colorString) {
        return getColor(graphicFactory, colorString, null, null);
    }

    /**
     * Supported formats are {@code #RRGGBB} and {@code #AARRGGBB}.
     */
    public static int getColor(GraphicFactory graphicFactory, String colorString, ThemeCallback themeCallback, RenderInstruction origin) {
        if (colorString.isEmpty() || colorString.charAt(0) != '#') {
            throw new IllegalArgumentException(UNSUPPORTED_COLOR_FORMAT + colorString);
        } else if (colorString.length() == 7) {
            return getColor(graphicFactory, colorString, 255, 1, themeCallback, origin);
        } else if (colorString.length() == 9) {
            return getColor(graphicFactory, colorString, Integer.parseInt(colorString.substring(1, 3), 16), 3, themeCallback, origin);
        } else {
            throw new IllegalArgumentException(UNSUPPORTED_COLOR_FORMAT + colorString);
        }
    }

    public static byte parseNonNegativeByte(String name, String value) throws XmlPullParserException {
        byte parsedByte = Byte.parseByte(value);
        checkForNegativeValue(name, parsedByte);
        return parsedByte;
    }

    public static float parseNonNegativeFloat(String name, String value) throws XmlPullParserException {
        float parsedFloat = Float.parseFloat(value);
        checkForNegativeValue(name, parsedFloat);
        return parsedFloat;
    }

    public static int parseNonNegativeInteger(String name, String value) throws XmlPullParserException {
        int parsedInt = Integer.parseInt(value);
        checkForNegativeValue(name, parsedInt);
        return parsedInt;
    }

    private static void checkForNegativeValue(String name, float value) throws XmlPullParserException {
        if (value < 0) {
            throw new XmlPullParserException("Attribute '" + name + "' must not be negative: " + value);
        }
    }

    /**
     * Create InputStream from assets, file or jar resource.
     * <p/>
     * If the resource has not a location prefix, then the search order is (file, assets, jar).
     */
    private static InputStream createInputStream(GraphicFactory graphicFactory, String relativePathPrefix, String src, XmlThemeResourceProvider resourceProvider) throws IOException {
        if (resourceProvider != null) {
            try {
                InputStream inputStream = resourceProvider.createInputStream(relativePathPrefix, src);
                if (inputStream != null) {
                    return inputStream;
                }
            } catch (IOException ioe) {
                LOGGER.fine("Exception trying to access resource: " + src + " using custom provider: " + ioe);
                // Ignore and try to resolve input stream using the standard process
            }
        }

        InputStream inputStream;
        if (src.startsWith(PREFIX_ASSETS)) {
            src = src.substring(PREFIX_ASSETS.length());
            inputStream = inputStreamFromAssets(graphicFactory, relativePathPrefix, src);
        } else if (src.startsWith(PREFIX_FILE)) {
            src = src.substring(PREFIX_FILE.length());
            inputStream = inputStreamFromFile(relativePathPrefix, src);
        } else if (src.startsWith(PREFIX_JAR) || src.startsWith(PREFIX_JAR_V1)) {
            if (src.startsWith(PREFIX_JAR)) {
                src = src.substring(PREFIX_JAR.length());
            } else if (src.startsWith(PREFIX_JAR_V1)) {
                src = src.substring(PREFIX_JAR_V1.length());
            }
            inputStream = inputStreamFromJar(relativePathPrefix, src);
        } else {
            inputStream = inputStreamFromFile(relativePathPrefix, src);

            if (inputStream == null) {
                inputStream = inputStreamFromAssets(graphicFactory, relativePathPrefix, src);
            }

            if (inputStream == null) {
                inputStream = inputStreamFromJar(relativePathPrefix, src);
            }
        }

        // Fallback to internal resources
        if (inputStream == null) {
            inputStream = inputStreamFromJar("/assets/", src);
            if (inputStream != null) {
                LOGGER.info("internal resource: " + src);
            }
        }

        if (inputStream != null) {
            return inputStream;
        }

        LOGGER.severe("invalid resource: " + src);
        throw new FileNotFoundException("invalid resource: " + src);
    }

    /**
     * Create InputStream from (platform specific) assets resource.
     */
    private static InputStream inputStreamFromAssets(GraphicFactory graphicFactory, String relativePathPrefix, String src) throws IOException {
        InputStream inputStream = null;
        try {
            inputStream = graphicFactory.platformSpecificSources(relativePathPrefix, src);
        } catch (IOException e) {
        }
        if (inputStream != null) {
            return inputStream;
        }
        return null;
    }

    /**
     * Create InputStream from file resource.
     */
    private static InputStream inputStreamFromFile(String relativePathPrefix, String src) throws IOException {
        File file = getFile(relativePathPrefix, src);
        if (!file.exists()) {
            if (src.length() > 0 && src.charAt(0) == File.separatorChar) {
                file = getFile(relativePathPrefix, src.substring(1));
            }
            if (!file.exists()) {
                file = null;
            }
        } else if (!file.isFile() || !file.canRead()) {
            file = null;
        }
        if (file != null) {
            return new FileInputStream(file);
        }
        return null;
    }

    /**
     * Create InputStream from jar resource.
     */
    private static InputStream inputStreamFromJar(String relativePathPrefix, String src) throws IOException {
        String absoluteName = getAbsoluteName(relativePathPrefix, src);
        return XmlUtils.class.getResourceAsStream(absoluteName);
    }

    private static String getAbsoluteName(String relativePathPrefix, String name) {
        if (name.charAt(0) == File.separatorChar) {
            return name;
        }
        return relativePathPrefix + name;
    }

    private static int getColor(GraphicFactory graphicFactory, String colorString, int alpha, int rgbStartIndex, ThemeCallback themeCallback, RenderInstruction origin) {
        int red = Integer.parseInt(colorString.substring(rgbStartIndex, rgbStartIndex + 2), 16);
        int green = Integer.parseInt(colorString.substring(rgbStartIndex + 2, rgbStartIndex + 4), 16);
        int blue = Integer.parseInt(colorString.substring(rgbStartIndex + 4, rgbStartIndex + 6), 16);

        int color = graphicFactory.createColor(alpha, red, green, blue);
        if (themeCallback != null) {
            color = themeCallback.getColor(origin, color);
        }
        return color;
    }

    private static File getFile(String parentPath, String pathName) {
        if (pathName.charAt(0) == File.separatorChar) {
            return new File(pathName);
        }
        return new File(parentPath, pathName);
    }

    private XmlUtils() {
        throw new IllegalStateException();
    }
}
