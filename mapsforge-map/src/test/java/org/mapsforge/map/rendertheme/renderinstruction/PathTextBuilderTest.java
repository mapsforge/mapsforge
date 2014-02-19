/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
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
package org.mapsforge.map.rendertheme.renderinstruction;

import org.junit.Assert;
import org.junit.Test;
import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.map.awt.AwtGraphicFactory;
import org.mapsforge.map.model.DisplayModel;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

public class PathTextBuilderTest {
	private static final float FONT_SIZE = 3.3f;
	private static final GraphicFactory GRAPHIC_FACTORY = AwtGraphicFactory.INSTANCE;
	private static final String K = "ele";

	@Test
	public void buildTest() throws SAXException {
		AttributesImpl attributesImpl = new AttributesImpl();
		attributesImpl.addAttribute(null, null, PathTextBuilder.FONT_SIZE, null, String.valueOf(FONT_SIZE));
		attributesImpl.addAttribute(null, null, PathTextBuilder.K, null, K);

		PathTextBuilder pathTextBuilder = new PathTextBuilder(GRAPHIC_FACTORY, new DisplayModel(), "pathText",
				attributesImpl);

		Assert.assertEquals(FONT_SIZE, pathTextBuilder.fontSize, 0);
		Assert.assertEquals(TextKey.getInstance(K), pathTextBuilder.textKey);
		Assert.assertNotNull(pathTextBuilder.build());
	}
}
