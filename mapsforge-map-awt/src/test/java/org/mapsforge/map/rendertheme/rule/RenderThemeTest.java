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
package org.mapsforge.map.rendertheme.rule;

import org.junit.Assert;
import org.junit.Test;
import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.map.awt.graphics.AwtGraphicFactory;
import org.mapsforge.map.model.DisplayModel;
import org.mapsforge.map.rendertheme.ExternalRenderTheme;
import org.mapsforge.map.rendertheme.XmlRenderTheme;
import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.IOException;

public class RenderThemeTest {
    private static final GraphicFactory GRAPHIC_FACTORY = AwtGraphicFactory.INSTANCE;
    private static final String RESOURCE_FOLDER = "src/test/resources/rendertheme/";

    private static void verifyInvalid(String pathname) throws XmlPullParserException, IOException {
        XmlRenderTheme xmlRenderTheme = new ExternalRenderTheme(new File(pathname));

        try {
            RenderThemeHandler.getRenderTheme(GRAPHIC_FACTORY, new DisplayModel(), xmlRenderTheme);
            Assert.fail();
        } catch (XmlPullParserException e) {
            Assert.assertTrue(true);
        }
    }

    @Test
    public void invalidRenderThemeTest() throws XmlPullParserException, IOException {
        verifyInvalid(RESOURCE_FOLDER + "invalid-render-theme1.xml");
        verifyInvalid(RESOURCE_FOLDER + "invalid-render-theme2.xml");
        verifyInvalid(RESOURCE_FOLDER + "invalid-render-theme3.xml");
    }

    @Test
    public void validRenderThemeTest() throws XmlPullParserException, IOException {
        XmlRenderTheme xmlRenderTheme = new ExternalRenderTheme(new File(RESOURCE_FOLDER, "test-render-theme.xml"));
        RenderTheme renderTheme = RenderThemeHandler
                .getRenderTheme(GRAPHIC_FACTORY, new DisplayModel(), xmlRenderTheme);

        Assert.assertEquals(3, renderTheme.getLevels());

        renderTheme.scaleStrokeWidth(12.34f, (byte) 12);
        renderTheme.scaleTextSize(56.78f, (byte) 12);

        // RenderCallback renderCallback = new DummyRenderCallback();

        // List<Tag> closedWayTags = Arrays.asList(new Tag("amenity", "parking"));
        // List<Tag> linearWayTags = Arrays.asList(new Tag("highway", "primary"), new Tag("oneway", "yes"));
        // List<Tag> nodeTags = Arrays.asList(new Tag("place", "city"), new Tag("highway", "turning_circle"));

        for (byte zoomLevel = 0; zoomLevel < 25; ++zoomLevel) {
            // renderTheme.matchClosedWay(renderCallback, closedWayTags, zoomLevel, 256);
            // renderTheme.matchLinearWay(renderCallback, linearWayTags, zoomLevel);
            // renderTheme.matchNode(renderCallback, nodeTags, zoomLevel);
        }

        renderTheme.destroy();
    }
}
