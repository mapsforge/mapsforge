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
package org.mapsforge.map.rendertheme.renderinstruction;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;
import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.map.rendertheme.DummyGraphicFactory;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

public class LineBuilderTest {
	private static final int LEVEL = 2;
	private static final float STROKE_WIDTH = 3.3f;

	@Test
	public void buildTest() throws IOException, SAXException {
		GraphicFactory graphicFactory = DummyGraphicFactory.INSTANCE;
		AttributesImpl attributesImpl = new AttributesImpl();
		attributesImpl.addAttribute(null, null, LineBuilder.STROKE_WIDTH, null, String.valueOf(STROKE_WIDTH));

		LineBuilder lineBuilder = new LineBuilder(graphicFactory, "line", attributesImpl, LEVEL, null);

		Assert.assertEquals(LEVEL, lineBuilder.level);
		Assert.assertEquals(STROKE_WIDTH, lineBuilder.strokeWidth, 0);
		Assert.assertNotNull(lineBuilder.build());
	}
}
