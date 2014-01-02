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
package org.mapsforge.map.rendertheme.renderinstruction;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;
import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.map.awt.AwtGraphicFactory;
import org.mapsforge.map.model.DisplayModel;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

public class AreaBuilderTest {
	private static final GraphicFactory GRAPHIC_FACTORY = AwtGraphicFactory.INSTANCE;
	private static final int LEVEL = 2;
	private static final float STROKE_WIDTH = 3.3f;

	@Test
	public void buildTest() throws IOException, SAXException {
		AttributesImpl attributesImpl = new AttributesImpl();
		attributesImpl.addAttribute(null, null, AreaBuilder.STROKE_WIDTH, null, String.valueOf(STROKE_WIDTH));

		AreaBuilder areaBuilder = new AreaBuilder(GRAPHIC_FACTORY, new DisplayModel(), "area", attributesImpl, LEVEL, null);

		Assert.assertEquals(LEVEL, areaBuilder.level);
		Assert.assertEquals(STROKE_WIDTH, areaBuilder.strokeWidth, 0);
		Assert.assertNotNull(areaBuilder.build());
	}
}
