/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright © 2014 Ludwig M Brinckmann
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

import org.junit.Assert;
import org.junit.Test;
import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.map.awt.AwtGraphicFactory;
import org.mapsforge.map.model.DisplayModel;
import org.mapsforge.map.rendertheme.rule.RenderThemeHandler;
import org.xml.sax.SAXException;

public class ExternalRenderThemeTest {
	private static final GraphicFactory GRAPHIC_FACTORY = AwtGraphicFactory.INSTANCE;
	private static final String RESOURCE_FOLDER = "src/test/resources/rendertheme/";

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
		File renderThemeFile = new File(RESOURCE_FOLDER, "empty-render-theme.xml");
		ExternalRenderTheme externalRenderTheme1 = new ExternalRenderTheme(renderThemeFile);
		ExternalRenderTheme externalRenderTheme2 = new ExternalRenderTheme(renderThemeFile);
		ExternalRenderTheme externalRenderTheme3 = new ExternalRenderTheme(new File(RESOURCE_FOLDER,
				"test-render-theme.xml"));

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
		File renderThemeFile = new File(RESOURCE_FOLDER, "empty-render-theme.xml");
		XmlRenderTheme xmlRenderTheme = new ExternalRenderTheme(renderThemeFile);
		Assert.assertNotNull(RenderThemeHandler.getRenderTheme(GRAPHIC_FACTORY, new DisplayModel(), xmlRenderTheme));
	}
}
