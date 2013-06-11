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
package org.mapsforge.core.model;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

public class TagTest {
	private static final String KEY = "foo";
	private static final String TAG_TO_STRING = "key=foo, value=bar";
	private static final String VALUE = "bar";

	@Test
	public void constructorTest() {
		Tag tag1 = new Tag(KEY + '=' + VALUE);
		Tag tag2 = new Tag(KEY, VALUE);

		TestUtils.equalsTest(tag1, tag2);
	}

	@Test
	public void equalsTest() {
		Tag tag1 = new Tag(KEY, VALUE);
		Tag tag2 = new Tag(KEY, VALUE);
		Tag tag3 = new Tag(KEY, KEY);
		Tag tag4 = new Tag(VALUE, VALUE);

		TestUtils.equalsTest(tag1, tag2);

		TestUtils.notEqualsTest(tag1, tag3);
		TestUtils.notEqualsTest(tag1, tag4);
		TestUtils.notEqualsTest(tag1, new Object());
		TestUtils.notEqualsTest(tag1, null);
	}

	@Test
	public void fieldTest() {
		Tag tag = new Tag(KEY, VALUE);

		Assert.assertEquals(KEY, tag.key);
		Assert.assertEquals(VALUE, tag.value);
	}

	@Test
	public void serializeTest() throws IOException, ClassNotFoundException {
		Tag tag = new Tag(KEY, VALUE);
		TestUtils.serializeTest(tag);
	}

	@Test
	public void toStringTest() {
		Tag tag = new Tag(KEY, VALUE);
		Assert.assertEquals(TAG_TO_STRING, tag.toString());
	}
}
