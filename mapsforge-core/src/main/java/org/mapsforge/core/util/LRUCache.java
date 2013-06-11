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

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * An LRUCache with a fixed size and an access-order policy. Old mappings are automatically removed from the cache when
 * new mappings are added. This implementation uses an {@link LinkedHashMap} internally.
 * 
 * @param <K>
 *            the type of the map key, see {@link Map}.
 * @param <V>
 *            the type of the map value, see {@link Map}.
 */
public class LRUCache<K, V> extends LinkedHashMap<K, V> {
	private static final float LOAD_FACTOR = 0.6f;
	private static final long serialVersionUID = 1L;

	private static int calculateInitialCapacity(int capacity) {
		if (capacity < 0) {
			throw new IllegalArgumentException("capacity must not be negative: " + capacity);
		}
		return (int) (capacity / LOAD_FACTOR) + 2;
	}

	public final int capacity;

	/**
	 * @param capacity
	 *            the maximum capacity of this cache.
	 * @throws IllegalArgumentException
	 *             if the capacity is negative.
	 */
	public LRUCache(int capacity) {
		super(calculateInitialCapacity(capacity), LOAD_FACTOR, true);
		this.capacity = capacity;
	}

	@Override
	protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
		return size() > this.capacity;
	}
}
