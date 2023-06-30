/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright 2014 Ludwig M Brinckmann
 * Copyright 2016-2019 devemux86
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
package org.mapsforge.map.rendertheme.rule;

import org.mapsforge.core.graphics.Color;
import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.map.model.DisplayModel;
import org.mapsforge.map.rendertheme.XmlUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

/**
 * A builder for {@link RenderTheme} instances.
 */
public class RenderThemeBuilder {

    private static final String BASE_STROKE_WIDTH = "base-stroke-width";
    private static final String BASE_TEXT_SIZE = "base-text-size";
    private static final String MAP_BACKGROUND = "map-background";
    private static final String MAP_BACKGROUND_OUTSIDE = "map-background-outside";
    private static final int RENDER_THEME_VERSION = 6;
    private static final String VERSION = "version";
    private static final String XMLNS = "xmlns";
    private static final String XMLNS_XSI = "xmlns:xsi";
    private static final String XSI_SCHEMALOCATION = "xsi:schemaLocation";

    float baseStrokeWidth;
    float baseTextSize;
    private final DisplayModel displayModel;
    boolean hasBackgroundOutside;
    int mapBackground;
    int mapBackgroundOutside;
    private Integer version;

    public RenderThemeBuilder(GraphicFactory graphicFactory, DisplayModel displayModel, String elementName, XmlPullParser pullParser)
            throws XmlPullParserException {
        this.displayModel = displayModel;
        this.baseStrokeWidth = 1f;
        this.baseTextSize = 1f;
        this.mapBackground = graphicFactory.createColor(Color.WHITE);

        extractValues(graphicFactory, elementName, pullParser);
    }

    /**
     * @return a new {@code RenderTheme} instance.
     */
    public RenderTheme build() {
        return new RenderTheme(this);
    }

    private void extractValues(GraphicFactory graphicFactory, String elementName, XmlPullParser pullParser)
            throws XmlPullParserException {
        for (int i = 0; i < pullParser.getAttributeCount(); ++i) {
            String name = pullParser.getAttributeName(i);
            String value = pullParser.getAttributeValue(i);

            if (XMLNS.equals(name)) {
                continue;
            } else if (XMLNS_XSI.equals(name)) {
                continue;
            } else if (XSI_SCHEMALOCATION.equals(name)) {
                continue;
            } else if (VERSION.equals(name)) {
                this.version = Integer.valueOf(XmlUtils.parseNonNegativeInteger(name, value));
            } else if (MAP_BACKGROUND.equals(name)) {
                this.mapBackground = XmlUtils.getColor(graphicFactory, value, displayModel.getThemeCallback(), null);
            } else if (MAP_BACKGROUND_OUTSIDE.equals(name)) {
                this.mapBackgroundOutside = XmlUtils.getColor(graphicFactory, value, displayModel.getThemeCallback(), null);
                this.hasBackgroundOutside = true;
            } else if (BASE_STROKE_WIDTH.equals(name)) {
                this.baseStrokeWidth = XmlUtils.parseNonNegativeFloat(name, value);
            } else if (BASE_TEXT_SIZE.equals(name)) {
                this.baseTextSize = XmlUtils.parseNonNegativeFloat(name, value);
            } else {
                XmlUtils.logUnknownAttribute(elementName, name, value, i);
            }
        }

        validate(elementName);
    }

    private void validate(String elementName) throws XmlPullParserException {
        XmlUtils.checkMandatoryAttribute(elementName, VERSION, this.version);

        if (this.version > RENDER_THEME_VERSION) {
            throw new XmlPullParserException("unsupported render theme version: " + this.version);
        }
    }
}
