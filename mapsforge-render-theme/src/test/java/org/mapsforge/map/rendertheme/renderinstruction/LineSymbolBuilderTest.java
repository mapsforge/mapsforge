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

import junit.framework.Assert;

import org.junit.Test;
import org.mapsforge.map.rendertheme.GraphicAdapter;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

public class LineSymbolBuilderTest {
	private static final boolean ALIGN_CENTER = true;
	private static final boolean REPEAT = true;

	@Test
	public void buildTest() throws IOException, SAXException {
		GraphicAdapter graphicAdapter = new DummyGraphicAdapter();
		AttributesImpl attributesImpl = new AttributesImpl();
		attributesImpl.addAttribute(null, null, LineSymbolBuilder.ALIGN_CENTER, null, String.valueOf(ALIGN_CENTER));
		attributesImpl.addAttribute(null, null, LineSymbolBuilder.REPEAT, null, String.valueOf(REPEAT));
		attributesImpl.addAttribute(null, null, LineSymbolBuilder.SRC, null, "jar:symbols/atm.png");

		LineSymbolBuilder lineSymbolBuilder = new LineSymbolBuilder(graphicAdapter, "lineSymbol", attributesImpl,
				"/osmarender/");

		Assert.assertEquals(ALIGN_CENTER, lineSymbolBuilder.alignCenter);
		Assert.assertEquals(REPEAT, lineSymbolBuilder.repeat);
		Assert.assertNotNull(lineSymbolBuilder.build());
	}
}
