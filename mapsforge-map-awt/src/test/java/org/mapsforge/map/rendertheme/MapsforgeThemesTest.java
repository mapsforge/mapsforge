/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright 2014 Ludwig M Brinckmann
 * Copyright 2016 devemux86
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

import org.junit.Assert;
import org.junit.Test;
import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.map.awt.graphics.AwtGraphicFactory;
import org.mapsforge.map.model.DisplayModel;
import org.mapsforge.map.rendertheme.internal.MapsforgeThemes;
import org.mapsforge.map.rendertheme.rule.RenderThemeHandler;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

public class MapsforgeThemesTest {
    private static final GraphicFactory GRAPHIC_FACTORY = AwtGraphicFactory.INSTANCE;

    @Test
    public void defaultTest() throws XmlPullParserException, IOException {
        XmlRenderTheme xmlRenderTheme = MapsforgeThemes.DEFAULT;
        Assert.assertNotNull(RenderThemeHandler.getRenderTheme(GRAPHIC_FACTORY, new DisplayModel(), xmlRenderTheme));
    }

    @Test
    public void osmarenderTest() throws XmlPullParserException, IOException {
        XmlRenderTheme xmlRenderTheme = MapsforgeThemes.OSMARENDER;
        Assert.assertNotNull(RenderThemeHandler.getRenderTheme(GRAPHIC_FACTORY, new DisplayModel(), xmlRenderTheme));
    }

    @Test
    public void motoriderTest() throws XmlPullParserException, IOException {
        XmlRenderTheme xmlRenderTheme = MapsforgeThemes.MOTORIDER;
        Assert.assertNotNull(RenderThemeHandler.getRenderTheme(GRAPHIC_FACTORY, new DisplayModel(), xmlRenderTheme));
    }

    @Test
    public void motoriderDarkTest() throws XmlPullParserException, IOException {
        XmlRenderTheme xmlRenderTheme = MapsforgeThemes.MOTORIDER_DARK;
        Assert.assertNotNull(RenderThemeHandler.getRenderTheme(GRAPHIC_FACTORY, new DisplayModel(), xmlRenderTheme));
    }
}
