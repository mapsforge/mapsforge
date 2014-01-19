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

public class CircleBuilderTest {
	private static final GraphicFactory GRAPHIC_FACTORY = AwtGraphicFactory.INSTANCE;
	private static final int LEVEL = 2;
	private static final float RADIUS = 3.3f;
	private static final Boolean SCALE_RADIUS = Boolean.TRUE;
	private static final float STROKE_WIDTH = 4.4f;

	@Test
	public void buildTest() throws SAXException {
		AttributesImpl attributesImpl = new AttributesImpl();
		attributesImpl.addAttribute(null, null, CircleBuilder.RADIUS, null, String.valueOf(RADIUS));
		attributesImpl.addAttribute(null, null, CircleBuilder.SCALE_RADIUS, null, SCALE_RADIUS.toString());
		attributesImpl.addAttribute(null, null, CircleBuilder.STROKE_WIDTH, null, String.valueOf(STROKE_WIDTH));

		CircleBuilder circleBuilder = new CircleBuilder(GRAPHIC_FACTORY, new DisplayModel(), "circle", attributesImpl, LEVEL);

		Assert.assertEquals(LEVEL, circleBuilder.level);
		Assert.assertEquals(RADIUS, circleBuilder.radius.floatValue(), 0);
		Assert.assertEquals(SCALE_RADIUS, Boolean.valueOf(circleBuilder.scaleRadius));
		Assert.assertEquals(STROKE_WIDTH, circleBuilder.strokeWidth, 0);
		Assert.assertNotNull(circleBuilder.build());
	}
}
