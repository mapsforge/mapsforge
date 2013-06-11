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

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;
import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.map.awt.AwtGraphicFactory;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

public class SymbolBuilderTest {
	private static final GraphicFactory GRAPHIC_FACTORY = AwtGraphicFactory.INSTANCE;

	@Test
	public void buildTest() throws IOException, SAXException {
		AttributesImpl attributesImpl = new AttributesImpl();
		attributesImpl.addAttribute(null, null, SymbolBuilder.SRC, null, "jar:symbols/atm.png");

		SymbolBuilder symbolBuilder = new SymbolBuilder(GRAPHIC_FACTORY, "symbol", attributesImpl, "/osmarender/");

		Assert.assertNotNull(symbolBuilder.build());
	}
}
