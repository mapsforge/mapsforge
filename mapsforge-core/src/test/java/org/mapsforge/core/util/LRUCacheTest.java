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
package org.mapsforge.core.util;

import org.junit.Assert;
import org.junit.Test;

public class LRUCacheTest {
	private static final String KEY1 = "foo1";
	private static final String KEY2 = "foo2";
	private static final String KEY3 = "foo3";
	private static final String VALUE1 = "bar1";
	private static final String VALUE2 = "bar2";
	private static final String VALUE3 = "bar3";

	private static LRUCache<String, String> createLRUCache(int capacity) {
		LRUCache<String, String> lruCache = new LRUCache<String, String>(capacity);
		Assert.assertEquals(capacity, lruCache.capacity);

		return lruCache;
	}

	private static void verifyInvalidCapacity(int capacity) {
		try {
			createLRUCache(capacity);
			Assert.fail("capacity: " + capacity);
		} catch (IllegalArgumentException e) {
			Assert.assertTrue(true);
		}
	}

	@Test
	public void lruCacheTest() {
		LRUCache<String, String> lruCache = createLRUCache(2);

		lruCache.put(KEY1, VALUE1);
		Assert.assertEquals(VALUE1, lruCache.get(KEY1));
		Assert.assertFalse(lruCache.containsKey(KEY2));
		Assert.assertFalse(lruCache.containsKey(KEY3));

		lruCache.put(KEY2, VALUE2);
		Assert.assertEquals(VALUE1, lruCache.get(KEY1));
		Assert.assertEquals(VALUE2, lruCache.get(KEY2));
		Assert.assertFalse(lruCache.containsKey(KEY3));

		lruCache.put(KEY3, VALUE3);
		Assert.assertFalse(lruCache.containsKey(KEY1));
		Assert.assertEquals(VALUE2, lruCache.get(KEY2));
		Assert.assertEquals(VALUE3, lruCache.get(KEY3));

		lruCache.put(KEY1, VALUE1);
		Assert.assertEquals(VALUE1, lruCache.get(KEY1));
		Assert.assertFalse(lruCache.containsKey(KEY2));
		Assert.assertEquals(VALUE3, lruCache.get(KEY3));
	}

	@Test
	public void lruCacheWithCapacityZeroTest() {
		LRUCache<String, String> lruCache = createLRUCache(0);
		lruCache.put(KEY1, VALUE1);
		Assert.assertFalse(lruCache.containsKey(KEY1));
	}

	@Test
	public void lruCacheWithNegativeCapacityTest() {
		verifyInvalidCapacity(-1);
	}
}
