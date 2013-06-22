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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.mapsforge.map.writer.model.TDNode;
import org.mapsforge.map.writer.model.TDWay;
import org.mapsforge.map.writer.util.JTSUtils;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Polygon;

//TODO could be implemented more efficiently with graphs: each line string is an edge, use an undirected graph and search for strongly connected components

class WayPolygonizer {
	class PolygonMergeException extends Exception {
		private static final long serialVersionUID = 1L;
	}

	private static final int MIN_NODES_POLYGON = 4;

	private static boolean isClosedPolygon(Deque<TDWay> currentPolygonSegments) {
		TDWay c1Start = currentPolygonSegments.getFirst();
		TDWay c1End = currentPolygonSegments.getLast();

		long startFirst = c1Start.isReversedInRelation() ? c1Start.getWayNodes()[c1Start.getWayNodes().length - 1]
				.getId() : c1Start.getWayNodes()[0].getId();

		long endLast = c1End.isReversedInRelation() ? c1End.getWayNodes()[0].getId() : c1End.getWayNodes()[c1End
				.getWayNodes().length - 1].getId();

		return startFirst == endLast;
	}

	private static boolean isClosedPolygon(TDWay way) {
		TDNode[] waynodes = way.getWayNodes();
		return waynodes[0].getId() == waynodes[waynodes.length - 1].getId();
	}

	private static Coordinate[] toCoordinates(Collection<TDWay> linestrings) {
		Coordinate[][] temp = new Coordinate[linestrings.size()][];
		int i = 0;
		int n = 0;
		for (TDWay tdWay : linestrings) {
			temp[i] = JTSUtils.toCoordinates(tdWay);
			n += temp[i].length;
			++i;
		}
		Coordinate[] res = new Coordinate[n];
		int pos = 0;
		for (i = 0; i < temp.length; i++) {
			System.arraycopy(temp[i], 0, res, pos, temp[i].length);
			pos += temp[i].length;
		}
		return res;
	}

	private List<TDWay> dangling;

	private final GeometryFactory geometryFactory = new GeometryFactory();

	private List<TDWay> illegal;

	private Map<Integer, List<Integer>> outerToInner;

	private List<Deque<TDWay>> polygons;

	List<TDWay> getDangling() {
		return this.dangling;
	}

	List<TDWay> getIllegal() {
		return this.illegal;
	}

	Map<Integer, List<Integer>> getOuterToInner() {
		return this.outerToInner;
	}

	List<Deque<TDWay>> getPolygons() {
		return this.polygons;
	}

	/**
	 * Tries to merge ways to closed polygons. The ordering of waynodes is preserved during the merge process.
	 * 
	 * @param ways
	 *            An array of ways that should be merged. Ways may be given in any order and may already be closed.
	 */
	void mergePolygons(TDWay[] ways) {
		this.polygons = new ArrayList<>();
		this.dangling = new ArrayList<>();
		this.illegal = new ArrayList<>();

		Deque<TDWay> ungroupedWays = new ArrayDeque<>();

		// initially all ways are ungrouped
		for (TDWay tdWay : ways) {
			// reset reversed flag, may already be set when way is part of another relation
			tdWay.setReversedInRelation(false);

			// first extract all way that are closed polygons in their own right
			if (isClosedPolygon(tdWay)) {
				if (tdWay.getWayNodes().length < MIN_NODES_POLYGON) {
					this.illegal.add(tdWay);
				} else {
					Deque<TDWay> cluster = new ArrayDeque<>();
					cluster.add(tdWay);
					this.polygons.add(cluster);
				}
			} else {
				ungroupedWays.add(tdWay);
			}
		}

		// all ways have been polygons, nice!
		if (ungroupedWays.isEmpty()) {
			return;
		}

		if (ungroupedWays.size() == 1) {
			this.dangling.add(ungroupedWays.getFirst());
			return;
		}

		boolean startNewPolygon = true;

		while (true) {
			boolean merge = false;
			if (startNewPolygon) {
				// we start a new polygon either during first iteration or when
				// the previous iterations merged ways to a closed polygon and there
				// are still ungrouped ways left
				Deque<TDWay> cluster = new ArrayDeque<>();
				// get the first way of the yet ungrouped ways and form a new group
				cluster.add(ungroupedWays.removeFirst());
				this.polygons.add(cluster);
				startNewPolygon = false;
			}

			// test if we can merge the current polygon with an ungrouped way
			Iterator<TDWay> it = ungroupedWays.iterator();
			while (it.hasNext()) {
				TDWay current = it.next();

				Deque<TDWay> currentPolygonSegments = this.polygons.get(this.polygons.size() - 1);
				// first way in current polygon
				TDWay c1Start = currentPolygonSegments.getFirst();
				// last way in current polygon
				TDWay c1End = currentPolygonSegments.getLast();

				long startFirst = c1Start.isReversedInRelation() ? c1Start.getWayNodes()[c1Start.getWayNodes().length - 1]
						.getId() : c1Start.getWayNodes()[0].getId();

				long endLast = c1End.isReversedInRelation() ? c1End.getWayNodes()[0].getId()
						: c1End.getWayNodes()[c1End.getWayNodes().length - 1].getId();

				long currentFirst = current.getWayNodes()[0].getId();
				long currentLast = current.getWayNodes()[current.getWayNodes().length - 1].getId();

				// current way end connects to the start of the current polygon (correct direction)
				if (startFirst == currentLast) {
					merge = true;
					it.remove();
					// add way to start of current polygon
					currentPolygonSegments.offerFirst(current);
				}
				// // current way start connects to the start of the current polygon (reversed
				// direction)
				else if (startFirst == currentFirst) {
					current.setReversedInRelation(true);
					merge = true;
					it.remove();
					currentPolygonSegments.offerFirst(current);
				}
				// current way start connects to the end of the current polygon (correct direction)
				else if (endLast == currentFirst) {
					merge = true;
					it.remove();
					// add way to end of current polygon
					currentPolygonSegments.offerLast(current);
				}
				// // current way end connects to the end of the current polygon (reversed direction)
				else if (endLast == currentLast) {
					current.setReversedInRelation(true);
					merge = true;
					it.remove();
					// add way to end of current polygon
					currentPolygonSegments.offerLast(current);
				}
			}

			Deque<TDWay> currentCluster = this.polygons.get(this.polygons.size() - 1);
			boolean closed = isClosedPolygon(currentCluster);
			// not a closed polygon and no more ways to merge
			if (!closed) {
				if (ungroupedWays.isEmpty() || !merge) {
					this.dangling.addAll(this.polygons.get(this.polygons.size() - 1));
					// may be a non operation when ungrouped is empty
					this.dangling.addAll(ungroupedWays);
					this.polygons.remove(this.polygons.size() - 1);
					return;
				}
			} else {
				// built a closed polygon and no more ways left --> we are finished
				if (ungroupedWays.isEmpty()) {
					return;
				}

				startNewPolygon = true;
			}

			// if we are here, the polygon is not yet closed, but there are also some ungrouped ways
			// which may be merge-able in the next iteration
		}
	}

	void polygonizeAndRelate(TDWay[] ways) {
		mergePolygons(ways);
		relatePolygons();
	}

	void relatePolygons() {
		this.outerToInner = new HashMap<>();
		if (this.polygons.isEmpty()) {
			return;
		}

		Polygon[] polygonGeometries = new Polygon[this.polygons.size()];
		int i = 0;
		for (Deque<TDWay> polygon : this.polygons) {
			polygonGeometries[i++] = this.geometryFactory.createPolygon(
					this.geometryFactory.createLinearRing(toCoordinates(polygon)), null);
		}

		this.outerToInner = new HashMap<>();
		HashSet<Integer> inner = new HashSet<>();
		for (int k = 0; k < polygonGeometries.length; k++) {
			if (inner.contains(Integer.valueOf(k))) {
				continue;
			}
			for (int l = k + 1; l < polygonGeometries.length; l++) {
				if (inner.contains(Integer.valueOf(l))) {
					continue;
				}

				if (polygonGeometries[k].covers(polygonGeometries[l])) {
					List<Integer> inners = this.outerToInner.get(Integer.valueOf(k));
					if (inners == null) {
						inners = new ArrayList<>();
						this.outerToInner.put(Integer.valueOf(k), inners);
					}
					inners.add(Integer.valueOf(l));
					inner.add(Integer.valueOf(l));
				} else if (!this.outerToInner.containsKey(Integer.valueOf(k))
						&& polygonGeometries[l].covers(polygonGeometries[k])) {
					List<Integer> inners = this.outerToInner.get(Integer.valueOf(l));
					if (inners == null) {
						inners = new ArrayList<>();
						this.outerToInner.put(Integer.valueOf(l), inners);
					}
					inners.add(Integer.valueOf(k));
					inner.add(Integer.valueOf(k));
				}
			}

			// single polygon without any inner polygons
			if (!this.outerToInner.containsKey(Integer.valueOf(k)) && !inner.contains(Integer.valueOf(k))) {
				this.outerToInner.put(Integer.valueOf(k), null);
			}
		}
	}
}
