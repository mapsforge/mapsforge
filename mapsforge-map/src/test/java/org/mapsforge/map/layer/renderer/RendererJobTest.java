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
package org.mapsforge.map.layer.renderer;

import java.io.File;

import org.junit.Assert;
import org.junit.Test;
import org.mapsforge.core.model.Tile;
import org.mapsforge.map.TestUtils;
import org.mapsforge.map.rendertheme.InternalRenderTheme;
import org.mapsforge.map.rendertheme.XmlRenderTheme;

public class RendererJobTest {
	@Test
	public void equalsTest() {
		File mapFile = new File("foo");
		XmlRenderTheme xmlRenderTheme = InternalRenderTheme.OSMARENDER;

		RendererJob rendererJob1 = new RendererJob(new Tile(0, 1, (byte) 2), mapFile, xmlRenderTheme, 1);
		RendererJob rendererJob2 = new RendererJob(new Tile(0, 1, (byte) 2), mapFile, xmlRenderTheme, 1);
		RendererJob rendererJob3 = new RendererJob(new Tile(0, 1, (byte) 2), mapFile, xmlRenderTheme, 2);

		TestUtils.equalsTest(rendererJob1, rendererJob2);

		Assert.assertNotEquals(rendererJob1, rendererJob3);
		Assert.assertNotEquals(rendererJob3, rendererJob1);
		Assert.assertNotEquals(rendererJob1, new Object());
	}
}
