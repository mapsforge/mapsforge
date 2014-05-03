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
package org.mapsforge.map.writer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.mapsforge.map.writer.model.Encoding;
import org.mapsforge.map.writer.model.WayDataBlock;

/**
 * Provides delta or double delta encoding of lists of integers.
 */
public final class DeltaEncoder {
	/**
	 * Encodes a list of WayDataBlock objects with the given encoding scheme.
	 * 
	 * @param blocks
	 *            List of WayDataBlock objects to be encoded.
	 * @param encoding
	 *            The Encoding which is used.
	 * @return A new list of new WayDataBlock objects encoded with the given encoding. The original list is returned in
	 *         case the encoding equals NONE.
	 */
	public static List<WayDataBlock> encode(List<WayDataBlock> blocks, Encoding encoding) {
		if (blocks == null) {
			return null;
		}

		if (encoding == Encoding.NONE) {
			return blocks;
		}

		List<WayDataBlock> results = new ArrayList<>();

		for (WayDataBlock wayDataBlock : blocks) {
			List<Integer> outer = mEncode(wayDataBlock.getOuterWay(), encoding);
			List<List<Integer>> inner = null;
			if (wayDataBlock.getInnerWays() != null) {
				inner = new ArrayList<>();
				for (List<Integer> list : wayDataBlock.getInnerWays()) {
					inner.add(mEncode(list, encoding));
				}
			}
			results.add(new WayDataBlock(outer, inner, encoding));
		}

		return results;
	}

	/**
	 * Computes the size in bytes for storing a list of WayDataBlock objects as unsigned var-bytes.
	 * 
	 * @param blocks
	 *            the blocks which should be encoded
	 * @return the number of bytes needed for the encoding
	 */
	public static int simulateSerialization(List<WayDataBlock> blocks) {
		int sum = 0;
		for (WayDataBlock wayDataBlock : blocks) {
			sum += mSimulateSerialization(wayDataBlock.getOuterWay());
			if (wayDataBlock.getInnerWays() != null) {
				for (List<Integer> list : wayDataBlock.getInnerWays()) {
					sum += mSimulateSerialization(list);
				}
			}
		}
		return sum;
	}

	static List<Integer> deltaEncode(List<Integer> list) {
		if (list == null) {
			return null;
		}
		ArrayList<Integer> result = new ArrayList<>();

		if (list.isEmpty()) {
			return result;
		}

		Iterator<Integer> it = list.iterator();
		// add the first way node to the result list
		Integer prevLat = it.next();
		Integer prevLon = it.next();

		result.add(prevLat);
		result.add(prevLon);

		while (it.hasNext()) {
			Integer currentLat = it.next();
			Integer currentLon = it.next();
			result.add(Integer.valueOf(currentLat.intValue() - prevLat.intValue()));
			result.add(Integer.valueOf(currentLon.intValue() - prevLon.intValue()));

			prevLat = currentLat;
			prevLon = currentLon;
		}

		return result;
	}

	static List<Integer> doubleDeltaEncode(List<Integer> list) {
		if (list == null) {
			return null;
		}

		ArrayList<Integer> result = new ArrayList<>();
		if (list.isEmpty()) {
			return result;
		}

		Iterator<Integer> it = list.iterator();
		// add the first way node to the result list
		Integer prevLat = it.next();
		Integer prevLon = it.next();

		Integer prevLatDelta = Integer.valueOf(0);
		Integer prevLonDelta = Integer.valueOf(0);

		result.add(prevLat);
		result.add(prevLon);

		while (it.hasNext()) {
			Integer currentLat = it.next();
			Integer currentLon = it.next();
			Integer deltaLat = Integer.valueOf(currentLat.intValue() - prevLat.intValue());
			Integer deltaLon = Integer.valueOf(currentLon.intValue() - prevLon.intValue());

			result.add(Integer.valueOf(deltaLat.intValue() - prevLatDelta.intValue()));
			result.add(Integer.valueOf(deltaLon.intValue() - prevLonDelta.intValue()));

			prevLat = currentLat;
			prevLon = currentLon;
			prevLatDelta = deltaLat;
			prevLonDelta = deltaLon;
		}

		return result;
	}

	private static List<Integer> mEncode(List<Integer> list, Encoding encoding) {
		switch (encoding) {
			case DELTA:
				return deltaEncode(list);
			case DOUBLE_DELTA:
				return doubleDeltaEncode(list);
			case NONE:
				return list;
		}

		throw new IllegalArgumentException("unknown encoding value: " + encoding);
	}

	private static int mSimulateSerialization(List<Integer> list) {
		int sum = 0;
		for (Integer coordinate : list) {
			sum += Serializer.getVariableByteSigned(coordinate.intValue()).length;
		}
		return sum;
	}

	private DeltaEncoder() {
	}
}
