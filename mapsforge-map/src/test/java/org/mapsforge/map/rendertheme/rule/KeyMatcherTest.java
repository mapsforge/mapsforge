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
package org.mapsforge.map.rendertheme.rule;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;
import org.mapsforge.core.model.Tag;

public class KeyMatcherTest {
	private static final String KEY1 = "foo";
	private static final String KEY2 = "bar";

	@Test
	public void isCoveredByTest() {
		AttributeMatcher attributeMatcher1 = new KeyMatcher(Arrays.asList(KEY1));
		AttributeMatcher attributeMatcher2 = new KeyMatcher(Arrays.asList(KEY1));

		Assert.assertTrue(attributeMatcher1.isCoveredBy(attributeMatcher1));
		Assert.assertTrue(attributeMatcher1.isCoveredBy(attributeMatcher2));
		Assert.assertTrue(attributeMatcher1.isCoveredBy(AnyMatcher.INSTANCE));

		Assert.assertFalse(AnyMatcher.INSTANCE.isCoveredBy(attributeMatcher1));
	}

	@Test
	public void matchesTest() {
		Tag tag1 = new Tag(KEY1, null);
		Tag tag2 = new Tag(KEY2, null);
		AttributeMatcher attributeMatcher = new KeyMatcher(Arrays.asList(KEY1));

		Assert.assertTrue(attributeMatcher.matches(Arrays.asList(tag1)));
		Assert.assertFalse(attributeMatcher.matches(Arrays.asList(tag2)));
	}
}
