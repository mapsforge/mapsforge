/*
 * Copyright 2016 devemux86
 * Copyright 2025 Sublimis
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

import java.util.*;

public final class Utils {

    /**
     * Null safe equals.
     */
    public static boolean equals(Object o1, Object o2) {
        return (o1 == o2) || (o1 != null && o1.equals(o2));
    }

    private Utils() {
        throw new IllegalStateException();
    }

    // UTILS FOR RULES

    public static int hashTagParameter(String keyValue) {
        return keyValue.hashCode();
    }

    public static int[] convertListString(List<String> list) {
        int[] values = new int[list.size()];
        for (int i = 0, m = list.size(); i < m; i++) {
            values[i] = hashTagParameter(list.get(i));
        }
        return values;
    }

    public static boolean contains(int[] data, int item) {
        //noinspection ForLoopReplaceableByForEach
        for (int i = 0; i < data.length; i++) {
            if (data[i] == item) {
                return true;
            }
        }
        return false;
    }

    /**
     * Deduplicate a collection by first building a sorted list, then removing the duplicates
     * from the original collection without changing the order of the remaining elements.
     * <p>
     * The strategy is efficient for unsorted collections with fast find-and-remove operations, like
     * {@link HashSet}, and/or for collections containing small fraction of duplicates.
     *
     * @param collection A {@link Collection} with fast find-and-remove operations and/or expected small fraction of duplicates. {@code null}-s are permitted.
     * @param <T>        A type implementing the {@link Comparable} interface.
     * @return The original collection without duplicate elements. Order of the remaining elements is not changed.
     */
    public static <T extends Comparable<? super T>> Collection<T> deduplicate(Collection<T> collection) {
        if (!collection.isEmpty()) {

            final List<T> sortedList;
            {
                sortedList = new ArrayList<>(collection);
                Collections.sort(sortedList);
            }

            T pivot = sortedList.get(0);

            for (int i = 1; i < sortedList.size(); i++) {
                final T item = sortedList.get(i);
                if (pivot == item || (pivot != null && pivot.compareTo(item) == 0)) {
                    // We're removing duplicates instead of building a new list from non-duplicates
                    // because the expected number of duplicates is small and removals are fast.
                    collection.remove(item);
                    continue;
                }

                pivot = item;
            }
        }

        return collection;
    }

    /**
     * Deduplicate a sorted collection by removing the duplicates
     * from the original collection without changing the order of the remaining elements.
     * <p>
     * The strategy is efficient for collections with fast removal operations, like {@link LinkedList} or
     * {@link LinkedHashSet}, and/or for collections containing small fraction of duplicates.
     * <p>
     * The collection is iterated sequentially, so the performance of the find operation is of no importance.
     *
     * @param sortedCollection A sorted collection with fast removals and/or expected small fraction of duplicates. {@code null}-s are permitted.
     * @param <T>              A type implementing the {@link Comparable} interface.
     * @return The original collection without duplicate elements. Order of the remaining elements is not changed.
     */
    public static <T extends Comparable<? super T>> Collection<T> deduplicateSorted(Collection<T> sortedCollection) {
        if (!sortedCollection.isEmpty()) {
            final Iterator<T> sortedIterator = sortedCollection.iterator();

            T pivot = sortedIterator.next();

            while (sortedIterator.hasNext()) {
                final T item = sortedIterator.next();
                if (pivot == item || (pivot != null && pivot.compareTo(item) == 0)) {
                    // We're removing duplicates instead of building a new list from non-duplicates
                    // because the expected number of duplicates is small and removals are fast.
                    sortedIterator.remove();
                    continue;
                }

                pivot = item;
            }
        }

        return sortedCollection;
    }
}
