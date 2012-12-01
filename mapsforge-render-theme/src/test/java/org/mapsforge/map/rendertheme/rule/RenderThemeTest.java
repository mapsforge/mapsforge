/*
 * Copyright 2010, 2011, 2012 mapsforge.org
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

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.junit.Assert;
import org.junit.Test;
import org.mapsforge.core.model.Tag;
import org.mapsforge.map.rendertheme.ExternalRenderTheme;
import org.mapsforge.map.rendertheme.RenderCallback;
import org.mapsforge.map.rendertheme.XmlRenderTheme;
import org.mapsforge.map.rendertheme.renderinstruction.DummyGraphicAdapter;
import org.xml.sax.SAXException;

public class RenderThemeTest {
	private static final String INVALID_RENDER_THEME1 = "src/test/resources/invalid-render-theme1.xml";
	private static final String INVALID_RENDER_THEME2 = "src/test/resources/invalid-render-theme2.xml";
	private static final String INVALID_RENDER_THEME3 = "src/test/resources/invalid-render-theme3.xml";
	private static final String RENDER_THEME_PATH = "src/test/resources/test-render-theme.xml";

	private static void verifyInvalid(String pathname) throws ParserConfigurationException, IOException {
		XmlRenderTheme xmlRenderTheme = new ExternalRenderTheme(new File(pathname));

		try {
			RenderThemeHandler.getRenderTheme(new DummyGraphicAdapter(), xmlRenderTheme);
			Assert.fail();
		} catch (SAXException e) {
			Assert.assertTrue(true);
		}
	}

	@Test
	public void invalidRenderThemeTest() throws ParserConfigurationException, IOException {
		verifyInvalid(INVALID_RENDER_THEME1);
		verifyInvalid(INVALID_RENDER_THEME2);
		verifyInvalid(INVALID_RENDER_THEME3);
	}

	@Test
	public void validRenderThemeTest() throws SAXException, ParserConfigurationException, IOException {
		XmlRenderTheme xmlRenderTheme = new ExternalRenderTheme(new File(RENDER_THEME_PATH));
		RenderTheme renderTheme = RenderThemeHandler.getRenderTheme(new DummyGraphicAdapter(), xmlRenderTheme);

		Assert.assertEquals(3, renderTheme.getLevels());

		renderTheme.scaleStrokeWidth(12.34f);
		renderTheme.scaleTextSize(56.78f);

		RenderCallback renderCallback = new DummyRenderCallback();

		List<Tag> closedWayTags = Arrays.asList(new Tag("amenity", "parking"));
		List<Tag> linearWayTags = Arrays.asList(new Tag("highway", "primary"), new Tag("oneway", "yes"));
		List<Tag> nodeTags = Arrays.asList(new Tag("place", "city"), new Tag("highway", "turning_circle"));

		for (byte zoomLevel = 0; zoomLevel < 25; ++zoomLevel) {
			renderTheme.matchClosedWay(renderCallback, closedWayTags, zoomLevel);
			renderTheme.matchLinearWay(renderCallback, linearWayTags, zoomLevel);
			renderTheme.matchNode(renderCallback, nodeTags, zoomLevel);
		}

		renderTheme.destroy();
	}
}
