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

import gnu.trove.list.array.TLongArrayList;
import gnu.trove.map.hash.TLongObjectHashMap;
import gnu.trove.procedure.TLongProcedure;
import gnu.trove.set.hash.TLongHashSet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.map.writer.model.MapWriterConfiguration;
import org.mapsforge.map.writer.model.TDNode;
import org.mapsforge.map.writer.model.TDRelation;
import org.mapsforge.map.writer.model.TDWay;
import org.mapsforge.map.writer.model.TileCoordinate;
import org.mapsforge.map.writer.model.TileData;
import org.mapsforge.map.writer.model.TileInfo;
import org.mapsforge.map.writer.model.ZoomIntervalConfiguration;
import org.mapsforge.map.writer.util.GeoUtils;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Relation;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;

/**
 * A TileBasedDataStore that uses the RAM as storage device for temporary data structures.
 */
public final class RAMTileBasedDataProcessor extends BaseTileBasedDataProcessor {
	/**
	 * Creates a new instance of a {@link RAMTileBasedDataProcessor}.
	 * 
	 * @param configuration
	 *            the configuration
	 * @return a new instance of a {@link RAMTileBasedDataProcessor}
	 */
	public static RAMTileBasedDataProcessor newInstance(MapWriterConfiguration configuration) {
		return new RAMTileBasedDataProcessor(configuration);
	}

	final TLongObjectHashMap<TDWay> ways;
	private final TLongObjectHashMap<TDRelation> multipolygons;

	private final TLongObjectHashMap<TDNode> nodes;

	private final RAMTileData[][][] tileData;

	private RAMTileBasedDataProcessor(MapWriterConfiguration configuration) {
		super(configuration);
		this.nodes = new TLongObjectHashMap<>();
		this.ways = new TLongObjectHashMap<>();
		this.multipolygons = new TLongObjectHashMap<>();
		this.tileData = new RAMTileData[this.zoomIntervalConfiguration.getNumberOfZoomIntervals()][][];
		// compute number of tiles needed on each base zoom level
		for (int i = 0; i < this.zoomIntervalConfiguration.getNumberOfZoomIntervals(); i++) {
			this.tileData[i] = new RAMTileData[this.tileGridLayouts[i].getAmountTilesHorizontal()][this.tileGridLayouts[i]
					.getAmountTilesVertical()];
		}
	}

	@Override
	public void addNode(Node node) {
		TDNode tdNode = TDNode.fromNode(node, this.preferredLanguage);
		this.nodes.put(tdNode.getId(), tdNode);
		addPOI(tdNode);
	}

	@Override
	public void addRelation(Relation relation) {
		TDRelation tdRelation = TDRelation.fromRelation(relation, this, this.preferredLanguage);
		if (tdRelation != null) {
			this.multipolygons.put(relation.getId(), tdRelation);
		}
	}

	@Override
	public void addWay(Way way) {
		TDWay tdWay = TDWay.fromWay(way, this, this.preferredLanguage);
		if (tdWay == null) {
			return;
		}
		this.ways.put(tdWay.getId(), tdWay);
		this.maxWayID = Math.max(this.maxWayID, way.getId());

		if (tdWay.isCoastline()) {
			// find matching tiles on zoom level 12
			Set<TileCoordinate> coastLineTiles = GeoUtils.mapWayToTiles(tdWay, TileInfo.TILE_INFO_ZOOMLEVEL, 0);
			for (TileCoordinate tileCoordinate : coastLineTiles) {
				TLongHashSet coastlines = this.tilesToCoastlines.get(tileCoordinate);
				if (coastlines == null) {
					coastlines = new TLongHashSet();
					this.tilesToCoastlines.put(tileCoordinate, coastlines);
				}
				coastlines.add(tdWay.getId());
			}
		}
	}

	@Override
	public void complete() {
		// Polygonize multipolygon
		RelationHandler relationHandler = new RelationHandler();
		this.multipolygons.forEachValue(relationHandler);

		WayHandler wayHandler = new WayHandler();
		this.ways.forEachValue(wayHandler);

		OSMTagMapping.getInstance().optimizePoiOrdering(this.histogramPoiTags);
		OSMTagMapping.getInstance().optimizeWayOrdering(this.histogramWayTags);
	}

	@Override
	public BoundingBox getBoundingBox() {
		return this.boundingbox;
	}

	@Override
	public Set<TDWay> getCoastLines(TileCoordinate tc) {
		if (tc.getZoomlevel() <= TileInfo.TILE_INFO_ZOOMLEVEL) {
			return Collections.emptySet();
		}
		TileCoordinate correspondingOceanTile = tc.translateToZoomLevel(TileInfo.TILE_INFO_ZOOMLEVEL).get(0);
		TLongHashSet coastlines = this.tilesToCoastlines.get(correspondingOceanTile);
		if (coastlines == null) {
			return Collections.emptySet();
		}

		final Set<TDWay> res = new HashSet<>();
		coastlines.forEach(new TLongProcedure() {
			@Override
			public boolean execute(long id) {
				TDWay way = RAMTileBasedDataProcessor.this.ways.get(id);
				if (way != null) {
					res.add(way);
					return true;
				}
				return false;
			}
		});
		return res;
	}

	@Override
	public List<TDWay> getInnerWaysOfMultipolygon(long outerWayID) {
		TLongArrayList innerwayIDs = this.outerToInnerMapping.get(outerWayID);
		if (innerwayIDs == null) {
			return null;
		}
		return getInnerWaysOfMultipolygon(innerwayIDs.toArray());
	}

	@Override
	public TDNode getNode(long id) {
		return this.nodes.get(id);
	}

	@Override
	public TileData getTile(int zoom, int tileX, int tileY) {
		return getTileImpl(zoom, tileX, tileY);
	}

	@Override
	public TDWay getWay(long id) {
		return this.ways.get(id);
	}

	@Override
	public ZoomIntervalConfiguration getZoomIntervalConfiguration() {
		return this.zoomIntervalConfiguration;
	}

	@Override
	public void release() {
		// nothing to do here
	}

	@Override
	protected RAMTileData getTileImpl(int zoom, int tileX, int tileY) {
		int tileCoordinateXIndex = tileX - this.tileGridLayouts[zoom].getUpperLeft().getX();
		int tileCoordinateYIndex = tileY - this.tileGridLayouts[zoom].getUpperLeft().getY();
		// check for valid range
		if (tileCoordinateXIndex < 0 || tileCoordinateYIndex < 0 || this.tileData[zoom].length <= tileCoordinateXIndex
				|| this.tileData[zoom][tileCoordinateXIndex].length <= tileCoordinateYIndex) {
			return null;
		}

		RAMTileData td = this.tileData[zoom][tileCoordinateXIndex][tileCoordinateYIndex];
		if (td == null) {
			td = new RAMTileData();
			this.tileData[zoom][tileCoordinateXIndex][tileCoordinateYIndex] = td;
		}

		return td;
	}

	@Override
	protected void handleAdditionalRelationTags(TDWay virtualWay, TDRelation relation) {
		// nothing to do here
	}

	@Override
	protected void handleVirtualInnerWay(TDWay virtualWay) {
		this.ways.put(virtualWay.getId(), virtualWay);
	}

	@Override
	protected void handleVirtualOuterWay(TDWay virtualWay) {
		// nothing to do here
	}

	private List<TDWay> getInnerWaysOfMultipolygon(long[] innerWayIDs) {
		if (innerWayIDs == null) {
			return Collections.emptyList();
		}
		List<TDWay> res = new ArrayList<>();
		for (long id : innerWayIDs) {
			TDWay current = this.ways.get(id);
			if (current == null) {
				continue;
			}
			res.add(current);
		}

		return res;
	}
}
