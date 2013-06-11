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
package org.mapsforge.map.rendertheme;

import org.junit.Assert;
import org.junit.Test;
import org.mapsforge.core.graphics.Color;
import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.map.awt.AwtGraphicFactory;

public class XmlUtilsTest {
	private static final GraphicFactory GRAPHIC_FACTORY = AwtGraphicFactory.INSTANCE;

	private static void verifyEqualColor(Color color, String colorString) {
		Assert.assertEquals(GRAPHIC_FACTORY.createColor(color), XmlUtils.getColor(GRAPHIC_FACTORY, colorString));
	}

	private static void verifyInvalidgetColor(String colorString) {
		try {
			XmlUtils.getColor(GRAPHIC_FACTORY, colorString);
			Assert.fail("colorString: " + colorString);
		} catch (IllegalArgumentException e) {
			Assert.assertTrue(true);
		}
	}

	@Test
	public void getColorTest() {
		verifyEqualColor(Color.RED, "#FF0000");
		verifyEqualColor(Color.RED, "#FFFF0000");
		verifyEqualColor(Color.GREEN, "#00ff00");
		verifyEqualColor(Color.BLUE, "#Ff0000Ff");

		verifyEqualColor(Color.BLACK, "#ff000000");
		verifyEqualColor(Color.WHITE, "#ffFFff");
		verifyEqualColor(Color.TRANSPARENT, "#00000000");

		verifyInvalidgetColor("#FF000");
		verifyInvalidgetColor("#00FF000");

		verifyInvalidgetColor("FF0000");
		verifyInvalidgetColor("00FF0000");

		verifyInvalidgetColor("#FF00000");
		verifyInvalidgetColor("#00FF00000");

		verifyInvalidgetColor(" #FF0000");
		verifyInvalidgetColor("# FF0000");
		verifyInvalidgetColor("#FF0000 ");

		verifyInvalidgetColor("#FFGGFF");
		verifyInvalidgetColor("#FFggFF");

		verifyInvalidgetColor("");
	}
}
