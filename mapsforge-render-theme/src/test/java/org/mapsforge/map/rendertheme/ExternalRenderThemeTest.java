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
package org.mapsforge.map.rendertheme;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import junit.framework.Assert;

import org.junit.Test;
import org.mapsforge.map.rendertheme.renderinstruction.DummyGraphicAdapter;
import org.mapsforge.map.rendertheme.rule.RenderThemeHandler;
import org.xml.sax.SAXException;

public class ExternalRenderThemeTest {
	private static final String EMPTY_RENDER_THEME_PATH = "src/test/resources/empty-render-theme.xml";
	private static final String RENDER_THEME_PATH = "src/test/resources/test-render-theme.xml";

	private static void equalsTest(Object object1, Object object2) {
		Assert.assertEquals(object1.hashCode(), object2.hashCode());
		Assert.assertEquals(object1, object2);
		Assert.assertEquals(object2, object1);
	}

	private static ExternalRenderTheme invokeConstructor(File file) throws FileNotFoundException {
		return new ExternalRenderTheme(file);
	}

	private static void verifyInvalid(String filePath) {
		try {
			invokeConstructor(new File(filePath));
			Assert.fail();
		} catch (FileNotFoundException e) {
			Assert.assertTrue(true);
		}
	}

	@Test
	public void equalsTest() throws FileNotFoundException {
		ExternalRenderTheme externalRenderTheme1 = new ExternalRenderTheme(new File(EMPTY_RENDER_THEME_PATH));
		ExternalRenderTheme externalRenderTheme2 = new ExternalRenderTheme(new File(EMPTY_RENDER_THEME_PATH));
		ExternalRenderTheme externalRenderTheme3 = new ExternalRenderTheme(new File(RENDER_THEME_PATH));

		equalsTest(externalRenderTheme1, externalRenderTheme2);

		Assert.assertFalse(externalRenderTheme1.equals(externalRenderTheme3));
		Assert.assertFalse(externalRenderTheme3.equals(externalRenderTheme1));
		Assert.assertFalse(externalRenderTheme1.equals(new Object()));
	}

	@Test
	public void invalidRenderThemeFileTest() {
		verifyInvalid("foo");
		verifyInvalid("src");
	}

	@Test
	public void validRenderThemeFileTest() throws SAXException, ParserConfigurationException, IOException {
		GraphicAdapter graphicAdapter = new DummyGraphicAdapter();
		XmlRenderTheme xmlRenderTheme = new ExternalRenderTheme(new File(EMPTY_RENDER_THEME_PATH));
		Assert.assertNotNull(RenderThemeHandler.getRenderTheme(graphicAdapter, xmlRenderTheme));
	}
}
