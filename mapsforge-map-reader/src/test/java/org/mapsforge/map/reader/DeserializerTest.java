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
package org.mapsforge.map.reader;

import org.junit.Assert;
import org.junit.Test;

public class DeserializerTest {
	@Test
	public void getIntTest() {
		byte[] buffer = new byte[] { 0, 0, 0, 0 };
		Assert.assertEquals(0, Deserializer.getInt(buffer, 0));

		buffer = new byte[] { 0, 0, 0, 1 };
		Assert.assertEquals(1, Deserializer.getInt(buffer, 0));

		buffer = new byte[] { 0, 0, 0, 127 };
		Assert.assertEquals(127, Deserializer.getInt(buffer, 0));

		buffer = new byte[] { 0, 0, 0, -128 };
		Assert.assertEquals(128, Deserializer.getInt(buffer, 0));

		buffer = new byte[] { 0, 0, 0, -127 };
		Assert.assertEquals(129, Deserializer.getInt(buffer, 0));

		buffer = new byte[] { 0, 0, 0, -1 };
		Assert.assertEquals(255, Deserializer.getInt(buffer, 0));

		buffer = new byte[] { 0, 0, 1, 0 };
		Assert.assertEquals(256, Deserializer.getInt(buffer, 0));

		buffer = new byte[] { 0, 0, 1, 1 };
		Assert.assertEquals(257, Deserializer.getInt(buffer, 0));

		buffer = new byte[] { 0, 1, 0, 0 };
		Assert.assertEquals(65536, Deserializer.getInt(buffer, 0));

		buffer = new byte[] { 0, 1, 0, 1 };
		Assert.assertEquals(65537, Deserializer.getInt(buffer, 0));

		buffer = new byte[] { 1, 0, 0, 0 };
		Assert.assertEquals(16777216, Deserializer.getInt(buffer, 0));

		buffer = new byte[] { 1, 0, 0, 1 };
		Assert.assertEquals(16777217, Deserializer.getInt(buffer, 0));
	}

	@Test
	public void getShortTest() {
		byte[] buffer = new byte[] { 0, 0 };
		Assert.assertEquals(0, Deserializer.getShort(buffer, 0));

		buffer = new byte[] { 0, 1 };
		Assert.assertEquals(1, Deserializer.getShort(buffer, 0));

		buffer = new byte[] { 0, 127 };
		Assert.assertEquals(127, Deserializer.getShort(buffer, 0));

		buffer = new byte[] { 0, -128 };
		Assert.assertEquals(128, Deserializer.getShort(buffer, 0));

		buffer = new byte[] { 0, -127 };
		Assert.assertEquals(129, Deserializer.getShort(buffer, 0));

		buffer = new byte[] { 0, -1 };
		Assert.assertEquals(255, Deserializer.getShort(buffer, 0));

		buffer = new byte[] { 1, 0 };
		Assert.assertEquals(256, Deserializer.getShort(buffer, 0));

		buffer = new byte[] { 1, 1 };
		Assert.assertEquals(257, Deserializer.getShort(buffer, 0));
	}
}
