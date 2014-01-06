/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.mapsforge.core.graphics.ResourceBitmap;
import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.map.model.DisplayModel;
import org.xml.sax.SAXException;

public final class XmlUtils {
	private static final String PREFIX_FILE = "file:";
	private static final String PREFIX_JAR = "jar:";
	private static final String PREFIX_JAR_V1 = "jar:/org/mapsforge/android/maps/rendertheme";

	private static final String UNSUPPORTED_COLOR_FORMAT = "unsupported color format: ";

	public static boolean supportOlderRenderThemes = true;

	public static void checkMandatoryAttribute(String elementName, String attributeName, Object attributeValue)
			throws SAXException {
		if (attributeValue == null) {
			throw new SAXException("missing attribute '" + attributeName + "' for element: " + elementName);
		}
	}

	public static ResourceBitmap createBitmap(GraphicFactory graphicFactory, DisplayModel displayModel, String relativePathPrefix, String src)
			throws IOException {
		if (src == null || src.length() == 0) {
			// no image source defined
			return null;
		}

		InputStream inputStream = graphicFactory.platformSpecificSources(relativePathPrefix, src);
		if (inputStream == null) {
			inputStream = createInputStream(relativePathPrefix, src);
		}
		try {
			String absoluteName = getAbsoluteName(relativePathPrefix, src);
			if (src.endsWith(".svg")) {
				return graphicFactory.renderSvg(inputStream, displayModel.getScaleFactor(), absoluteName.hashCode());
			}
			return graphicFactory.createResourceBitmap(inputStream, absoluteName.hashCode());
		} finally {
			inputStream.close();
		}
	}

	public static SAXException createSAXException(String element, String name, String value, int attributeIndex) {
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("unknown attribute (");
		stringBuilder.append(attributeIndex);
		stringBuilder.append(") in element '");
		stringBuilder.append(element);
		stringBuilder.append("': ");
		stringBuilder.append(name);
		stringBuilder.append('=');
		stringBuilder.append(value);

		return new SAXException(stringBuilder.toString());
	}

	/**
	 * Supported formats are {@code #RRGGBB} and {@code #AARRGGBB}.
	 */
	public static int getColor(GraphicFactory graphicFactory, String colorString) {
		if (colorString.isEmpty() || colorString.charAt(0) != '#') {
			throw new IllegalArgumentException(UNSUPPORTED_COLOR_FORMAT + colorString);
		} else if (colorString.length() == 7) {
			return getColor(graphicFactory, colorString, 255, 1);
		} else if (colorString.length() == 9) {
			return getColor(graphicFactory, colorString, Integer.parseInt(colorString.substring(1, 3), 16), 3);
		} else {
			throw new IllegalArgumentException(UNSUPPORTED_COLOR_FORMAT + colorString);
		}
	}

	public static byte parseNonNegativeByte(String name, String value) throws SAXException {
		byte parsedByte = Byte.parseByte(value);
		checkForNegativeValue(name, parsedByte);
		return parsedByte;
	}

	public static float parseNonNegativeFloat(String name, String value) throws SAXException {
		float parsedFloat = Float.parseFloat(value);
		checkForNegativeValue(name, parsedFloat);
		return parsedFloat;
	}

	public static int parseNonNegativeInteger(String name, String value) throws SAXException {
		int parsedInt = Integer.parseInt(value);
		checkForNegativeValue(name, parsedInt);
		return parsedInt;
	}

	private static void checkForNegativeValue(String name, float value) throws SAXException {
		if (value < 0) {
			throw new SAXException("Attribute '" + name + "' must not be negative: " + value);
		}
	}

	private static InputStream createInputStream(String relativePathPrefix, String src) throws FileNotFoundException {

		if (src.startsWith(PREFIX_JAR)) {
			final String prefixJar;
			if (!supportOlderRenderThemes) {
				prefixJar = PREFIX_JAR;
			} else {
				prefixJar = src.startsWith(PREFIX_JAR_V1) ? PREFIX_JAR_V1 : PREFIX_JAR;
			}
			String absoluteName = getAbsoluteName(relativePathPrefix, src.substring(prefixJar.length()));
			InputStream inputStream = XmlUtils.class.getResourceAsStream(absoluteName);
			if (inputStream == null) {
				throw new FileNotFoundException("resource not found: " + absoluteName);
			}
			return inputStream;
		} else if (src.startsWith(PREFIX_FILE)) {
			File file = getFile(relativePathPrefix, src.substring(PREFIX_FILE.length()));
			if (!file.exists()) {
				final String pathName = src.substring(PREFIX_FILE.length());
				if (pathName.length() > 0 && pathName.charAt(0) == File.separatorChar) {
					file = getFile(relativePathPrefix, pathName.substring(1));
				}
				if (!file.exists()) {
					throw new FileNotFoundException("file does not exist: " + file.getAbsolutePath());
				}
			} else if (!file.isFile()) {
				throw new FileNotFoundException("not a file: " + file.getAbsolutePath());
			} else if (!file.canRead()) {
				throw new FileNotFoundException("cannot read file: " + file.getAbsolutePath());
			}
			return new FileInputStream(file);
		}

		throw new FileNotFoundException("invalid bitmap source: " + src);
	}

	private static String getAbsoluteName(String relativePathPrefix, String name) {
		if (name.charAt(0) == '/') {
			return name;
		}
		return relativePathPrefix + name;
	}

	private static int getColor(GraphicFactory graphicFactory, String colorString, int alpha, int rgbStartIndex) {
		int red = Integer.parseInt(colorString.substring(rgbStartIndex, rgbStartIndex + 2), 16);
		int green = Integer.parseInt(colorString.substring(rgbStartIndex + 2, rgbStartIndex + 4), 16);
		int blue = Integer.parseInt(colorString.substring(rgbStartIndex + 4, rgbStartIndex + 6), 16);

		return graphicFactory.createColor(alpha, red, green, blue);
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
