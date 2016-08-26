/*
 * Copyright 2014 Ludwig M Brinckmann
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

import java.util.Set;

/**
 * Cache that maintains a working set of elements in the cache, given to it by
 * setWorkingSet(Set<K>) in addition to other elements which are kept on a LRU
 * basis.
 *
 * @param <K> the type of the map key, see {@link java.util.Map}.
 * @param <V> the type of the map value, see {@link java.util.Map}.
 */
public class WorkingSetCache<K, V> extends LRUCache<K, V> {
    private static final long serialVersionUID = 1L;

    public WorkingSetCache(int capacity) {
        super(capacity);
    }

    /**
     * Sets the current working set, ensuring that elements in this working set
     * will not be ejected in the near future.
     *
     * @param workingSet set of K that makes up the current working set.
     */
    public void setWorkingSet(Set<K> workingSet) {
        synchronized(workingSet) {
            for (K key : workingSet) {
                this.get(key);
            }
        }
    }
}
