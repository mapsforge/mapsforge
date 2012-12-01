/*
 * Copyright 2010, 2011, 2012 mapsforge.org
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
import gnu.trove.map.hash.TShortIntHashMap;
import gnu.trove.procedure.TObjectProcedure;
import gnu.trove.set.TLongSet;
import gnu.trove.set.hash.TLongHashSet;

import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.CoordinatesUtil;
import org.mapsforge.core.util.MercatorProjection;
import org.mapsforge.map.writer.model.MapWriterConfiguration;
import org.mapsforge.map.writer.model.NodeResolver;
import org.mapsforge.map.writer.model.TDNode;
import org.mapsforge.map.writer.model.TDRelation;
import org.mapsforge.map.writer.model.TDWay;
import org.mapsforge.map.writer.model.TileBasedDataProcessor;
import org.mapsforge.map.writer.model.TileCoordinate;
import org.mapsforge.map.writer.model.TileData;
import org.mapsforge.map.writer.model.TileGridLayout;
import org.mapsforge.map.writer.model.WayResolver;
import org.mapsforge.map.writer.model.ZoomIntervalConfiguration;
import org.mapsforge.map.writer.util.GeoUtils;

import com.vividsolutions.jts.geom.TopologyException;

abstract class BaseTileBasedDataProcessor implements TileBasedDataProcessor, NodeResolver, WayResolver {
	protected static final Logger LOGGER = Logger.getLogger(BaseTileBasedDataProcessor.class.getName());

	protected final org.mapsforge.core.model.BoundingBox boundingbox;
	protected TileGridLayout[] tileGridLayouts;
	protected final ZoomIntervalConfiguration zoomIntervalConfiguration;
	protected final int bboxEnlargement;
	protected final String preferredLanguage;
	protected final boolean skipInvalidRelations;

	protected final TLongObjectHashMap<TLongArrayList> outerToInnerMapping;
	protected final TLongSet innerWaysWithoutAdditionalTags;
	protected final Map<TileCoordinate, TLongHashSet> tilesToCoastlines;

	// accounting
	protected float[] countWays;
	protected float[] countWayTileFactor;

	protected final TShortIntHashMap histogramPoiTags;
	protected final TShortIntHashMap histogramWayTags;
	protected long maxWayID = Long.MIN_VALUE;

	// public BaseTileBasedDataProcessor(double minLat, double maxLat, double minLon, double maxLon,
	// ZoomIntervalConfiguration zoomIntervalConfiguration, int bboxEnlargement, String preferredLanguage) {
	// this(new Rect(minLon, maxLon, minLat, maxLat), zoomIntervalConfiguration, bboxEnlargement,
	// preferredLanguage);
	//
	// }

	public BaseTileBasedDataProcessor(MapWriterConfiguration configuration) {
		super();
		this.boundingbox = configuration.getBboxConfiguration();
		this.zoomIntervalConfiguration = configuration.getZoomIntervalConfiguration();
		this.tileGridLayouts = new TileGridLayout[this.zoomIntervalConfiguration.getNumberOfZoomIntervals()];
		this.bboxEnlargement = configuration.getBboxEnlargement();
		this.preferredLanguage = configuration.getPreferredLanguage();
		this.skipInvalidRelations = configuration.isSkipInvalidRelations();

		this.outerToInnerMapping = new TLongObjectHashMap<TLongArrayList>();
		this.innerWaysWithoutAdditionalTags = new TLongHashSet();
		this.tilesToCoastlines = new HashMap<TileCoordinate, TLongHashSet>();

		this.countWays = new float[this.zoomIntervalConfiguration.getNumberOfZoomIntervals()];
		this.countWayTileFactor = new float[this.zoomIntervalConfiguration.getNumberOfZoomIntervals()];

		this.histogramPoiTags = new TShortIntHashMap();
		this.histogramWayTags = new TShortIntHashMap();

		// compute horizontal and vertical tile coordinate offsets for all
		// base zoom levels
		for (int i = 0; i < this.zoomIntervalConfiguration.getNumberOfZoomIntervals(); i++) {
			TileCoordinate upperLeft = new TileCoordinate((int) MercatorProjection.longitudeToTileX(
					this.boundingbox.minLongitude, this.zoomIntervalConfiguration.getBaseZoom(i)),
					(int) MercatorProjection.latitudeToTileY(this.boundingbox.maxLatitude,
							this.zoomIntervalConfiguration.getBaseZoom(i)),
					this.zoomIntervalConfiguration.getBaseZoom(i));
			this.tileGridLayouts[i] = new TileGridLayout(upperLeft, computeNumberOfHorizontalTiles(i),
					computeNumberOfVerticalTiles(i));
		}
	}

	@Override
	public BoundingBox getBoundingBox() {
		return this.boundingbox;
	}

	@Override
	public TileGridLayout getTileGridLayout(int zoomIntervalIndex) {
		return this.tileGridLayouts[zoomIntervalIndex];
	}

	@Override
	public ZoomIntervalConfiguration getZoomIntervalConfiguration() {
		return this.zoomIntervalConfiguration;
	}

	@Override
	public long cumulatedNumberOfTiles() {
		long cumulated = 0;
		for (int i = 0; i < this.zoomIntervalConfiguration.getNumberOfZoomIntervals(); i++) {
			cumulated += this.tileGridLayouts[i].getAmountTilesHorizontal()
					* this.tileGridLayouts[i].getAmountTilesVertical();
		}
		return cumulated;
	}

	protected void countPoiTags(TDNode poi) {
		if (poi == null || poi.getTags() == null) {
			return;
		}
		for (short tag : poi.getTags()) {
			this.histogramPoiTags.adjustOrPutValue(tag, 1, 1);
		}
	}

	protected void countWayTags(TDWay way) {
		if (way == null || way.getTags() == null) {
			return;
		}
		for (short tag : way.getTags()) {
			this.histogramWayTags.adjustOrPutValue(tag, 1, 1);
		}
	}

	protected void countWayTags(short[] tags) {
		if (tags == null) {
			return;
		}
		for (short tag : tags) {
			this.histogramWayTags.adjustOrPutValue(tag, 1, 1);
		}
	}

	protected void addPOI(TDNode poi) {
		if (!poi.isPOI()) {
			return;
		}

		byte minZoomLevel = poi.getZoomAppear();
		for (int i = 0; i < this.zoomIntervalConfiguration.getNumberOfZoomIntervals(); i++) {
			// is POI seen in a zoom interval?
			if (minZoomLevel <= this.zoomIntervalConfiguration.getMaxZoom(i)) {
				long tileCoordinateX = MercatorProjection.longitudeToTileX(
						CoordinatesUtil.microdegreesToDegrees(poi.getLongitude()),
						this.zoomIntervalConfiguration.getBaseZoom(i));
				long tileCoordinateY = MercatorProjection.latitudeToTileY(
						CoordinatesUtil.microdegreesToDegrees(poi.getLatitude()),
						this.zoomIntervalConfiguration.getBaseZoom(i));
				TileData tileData = getTileImpl(i, (int) tileCoordinateX, (int) tileCoordinateY);
				if (tileData != null) {
					tileData.addPOI(poi);
					countPoiTags(poi);
				}
			}
		}
	}

	protected void addWayToTiles(TDWay way, int enlargement) {
		int bboxEnlargementLocal = enlargement;
		byte minZoomLevel = way.getMinimumZoomLevel();
		for (int i = 0; i < this.zoomIntervalConfiguration.getNumberOfZoomIntervals(); i++) {
			// is way seen in a zoom interval?
			if (minZoomLevel <= this.zoomIntervalConfiguration.getMaxZoom(i)) {
				Set<TileCoordinate> matchedTiles = GeoUtils.mapWayToTiles(way,
						this.zoomIntervalConfiguration.getBaseZoom(i), bboxEnlargementLocal);
				boolean added = false;
				for (TileCoordinate matchedTile : matchedTiles) {
					TileData td = getTileImpl(i, matchedTile.getX(), matchedTile.getY());
					if (td != null) {
						countWayTags(way);
						this.countWayTileFactor[i]++;
						added = true;
						td.addWay(way);
					}
				}
				if (added) {
					this.countWays[i]++;
				}
			}
		}
	}

	protected abstract TileData getTileImpl(int zoom, int tileX, int tileY);

	protected abstract void handleVirtualOuterWay(TDWay virtualWay);

	protected abstract void handleAdditionalRelationTags(TDWay virtualWay, TDRelation relation);

	protected abstract void handleVirtualInnerWay(TDWay virtualWay);

	private int computeNumberOfHorizontalTiles(int zoomIntervalIndex) {
		long tileCoordinateLeft = MercatorProjection.longitudeToTileX(this.boundingbox.minLongitude,
				this.zoomIntervalConfiguration.getBaseZoom(zoomIntervalIndex));

		long tileCoordinateRight = MercatorProjection.longitudeToTileX(this.boundingbox.maxLongitude,
				this.zoomIntervalConfiguration.getBaseZoom(zoomIntervalIndex));

		assert tileCoordinateLeft <= tileCoordinateRight;
		assert tileCoordinateLeft - tileCoordinateRight + 1 < Integer.MAX_VALUE;

		LOGGER.finer("basezoom: " + this.zoomIntervalConfiguration.getBaseZoom(zoomIntervalIndex) + "\t+n_horizontal: "
				+ (tileCoordinateRight - tileCoordinateLeft + 1));

		return (int) (tileCoordinateRight - tileCoordinateLeft + 1);
	}

	private int computeNumberOfVerticalTiles(int zoomIntervalIndex) {
		long tileCoordinateBottom = MercatorProjection.latitudeToTileY(this.boundingbox.minLatitude,
				this.zoomIntervalConfiguration.getBaseZoom(zoomIntervalIndex));

		long tileCoordinateTop = MercatorProjection.latitudeToTileY(this.boundingbox.maxLatitude,

		this.zoomIntervalConfiguration.getBaseZoom(zoomIntervalIndex));

		assert tileCoordinateBottom >= tileCoordinateTop;
		assert tileCoordinateBottom - tileCoordinateTop + 1 <= Integer.MAX_VALUE;

		LOGGER.finer("basezoom: " + this.zoomIntervalConfiguration.getBaseZoom(zoomIntervalIndex) + "\t+n_vertical: "
				+ (tileCoordinateBottom - tileCoordinateTop + 1));

		return (int) (tileCoordinateBottom - tileCoordinateTop + 1);
	}

	protected class RelationHandler implements TObjectProcedure<TDRelation> {
		private final WayPolygonizer polygonizer = new WayPolygonizer();

		private List<Integer> inner;
		private List<Deque<TDWay>> extractedPolygons;
		private Map<Integer, List<Integer>> outerToInner;

		@Override
		public boolean execute(TDRelation relation) {
			if (relation == null) {
				return false;
			}

			this.extractedPolygons = null;
			this.outerToInner = null;

			TDWay[] members = relation.getMemberWays();
			try {
				this.polygonizer.polygonizeAndRelate(members);
			} catch (TopologyException e) {
				LOGGER.log(Level.FINE,
						"cannot relate extracted polygons to each other for relation: " + relation.getId(), e);
			}

			// skip invalid relations
			if (!this.polygonizer.getDangling().isEmpty()) {
				if (BaseTileBasedDataProcessor.this.skipInvalidRelations) {
					LOGGER.fine("skipping relation that contains dangling ways which could not be merged to polygons: "
							+ relation.getId());
					return true;
				}
				LOGGER.fine("relation contains dangling ways which could not be merged to polygons: "
						+ relation.getId());

			} else if (!this.polygonizer.getIllegal().isEmpty()) {
				if (BaseTileBasedDataProcessor.this.skipInvalidRelations) {
					LOGGER.fine("skipping relation contains illegal closed ways with fewer than 4 nodes: "
							+ relation.getId());
					return true;
				}
				LOGGER.fine("relation contains illegal closed ways with fewer than 4 nodes: " + relation.getId());
			}

			this.extractedPolygons = this.polygonizer.getPolygons();
			this.outerToInner = this.polygonizer.getOuterToInner();

			for (Entry<Integer, List<Integer>> entry : this.outerToInner.entrySet()) {
				Deque<TDWay> outerPolygon = this.extractedPolygons.get(entry.getKey().intValue());
				this.inner = null;
				this.inner = entry.getValue();
				byte shape = TDWay.SIMPLE_POLYGON;
				// does it contain inner ways?
				if (this.inner != null && !this.inner.isEmpty()) {
					shape = TDWay.MULTI_POLYGON;
				}

				TDWay outerWay = null;
				if (outerPolygon.size() > 1) {
					// we need to create a new way from a set of ways
					// collect the way nodes and use the tags of the relation
					// if one of the ways has its own tags, we should ignore them,
					// ways with relevant tags will be added separately later
					if (!relation.isRenderRelevant()) {
						LOGGER.fine("constructed outer polygon in relation has no known tags: " + relation.getId());
						continue;
					}
					// merge way nodes from outer way segments
					List<TDNode> waynodeList = new ArrayList<TDNode>();
					for (TDWay outerSegment : outerPolygon) {
						if (outerSegment.isReversedInRelation()) {
							for (int i = outerSegment.getWayNodes().length - 1; i >= 0; i--) {
								waynodeList.add(outerSegment.getWayNodes()[i]);
							}
						} else {
							for (TDNode tdNode : outerSegment.getWayNodes()) {
								waynodeList.add(tdNode);
							}
						}
					}
					TDNode[] waynodes = waynodeList.toArray(new TDNode[waynodeList.size()]);

					// create new virtual way which represents the outer way
					// use maxWayID counter to create unique id
					outerWay = new TDWay(++BaseTileBasedDataProcessor.this.maxWayID, relation.getLayer(),
							relation.getName(), relation.getHouseNumber(), relation.getRef(), relation.getTags(),
							shape, waynodes);

					// add the newly created way to matching tiles
					addWayToTiles(outerWay, BaseTileBasedDataProcessor.this.bboxEnlargement);
					handleVirtualOuterWay(outerWay);
					// adjust tag statistics, cannot be omitted!!!
					countWayTags(relation.getTags());
				}

				// the outer way consists of only one segment
				else {
					outerWay = outerPolygon.getFirst();

					// is it a polygon that we have seen already and which was
					// identified as a polgyon containing inner ways?
					if (BaseTileBasedDataProcessor.this.outerToInnerMapping.contains(outerWay.getId())) {
						shape = TDWay.MULTI_POLYGON;
					}
					outerWay.setShape(shape);

					// we merge the name, ref, tag information of the relation to the outer way
					// TODO is this true?
					// a relation that addresses an already closed way, is normally used to add
					// additional information to the way
					outerWay.mergeRelationInformation(relation);
					// only consider the way, if it has tags, otherwise the renderer cannot interpret
					// the way
					if (outerWay.isRenderRelevant()) {
						// handle relation tags
						handleAdditionalRelationTags(outerWay, relation);
						addWayToTiles(outerWay, BaseTileBasedDataProcessor.this.bboxEnlargement);
						countWayTags(outerWay.getTags());
					}
				}

				// relate inner ways to outer way
				addInnerWays(outerWay);
			}
			return true;
		}

		private void addInnerWays(TDWay outer) {
			if (this.inner != null && !this.inner.isEmpty()) {
				TLongArrayList innerList = BaseTileBasedDataProcessor.this.outerToInnerMapping.get(outer.getId());
				if (innerList == null) {
					innerList = new TLongArrayList();
					BaseTileBasedDataProcessor.this.outerToInnerMapping.put(outer.getId(), innerList);
				}

				for (Integer innerIndex : this.inner) {
					Deque<TDWay> innerSegments = this.extractedPolygons.get(innerIndex.intValue());
					TDWay innerWay = null;

					if (innerSegments.size() == 1) {
						innerWay = innerSegments.getFirst();
						if (innerWay.hasTags() && outer.hasTags()) {
							short[] iTags = innerWay.getTags();
							short[] oTags = outer.getTags();
							int contained = 0;
							for (short iTagID : iTags) {
								for (short oTagID : oTags) {
									if (iTagID == oTagID) {
										contained++;
									}
								}
							}
							if (contained == iTags.length) {
								BaseTileBasedDataProcessor.this.innerWaysWithoutAdditionalTags.add(innerWay.getId());
							}
						}
					} else {
						List<TDNode> waynodeList = new ArrayList<TDNode>();
						for (TDWay innerSegment : innerSegments) {
							if (innerSegment.isReversedInRelation()) {
								for (int i = innerSegment.getWayNodes().length - 1; i >= 0; i--) {
									waynodeList.add(innerSegment.getWayNodes()[i]);
								}
							} else {
								for (TDNode tdNode : innerSegment.getWayNodes()) {
									waynodeList.add(tdNode);
								}
							}
						}
						TDNode[] waynodes = waynodeList.toArray(new TDNode[waynodeList.size()]);
						// TODO which layer?
						innerWay = new TDWay(++BaseTileBasedDataProcessor.this.maxWayID, (byte) 0, null, null, null,
								waynodes);
						handleVirtualInnerWay(innerWay);
						// does not need to be added to corresponding tiles
						// virtual inner ways do not have any tags, they are holes in the outer polygon
					}
					innerList.add(innerWay.getId());
				}
			}
		}
	}

	protected class WayHandler implements TObjectProcedure<TDWay> {
		@Override
		public boolean execute(TDWay way) {
			if (way == null) {
				return true;
			}
			// we only consider ways that have tags and which have not already
			// added as outer way of a relation
			// inner ways without additional tags are also not considered as they are processed as part of a
			// multi polygon
			if (way.isRenderRelevant() && !BaseTileBasedDataProcessor.this.outerToInnerMapping.contains(way.getId())
					&& !BaseTileBasedDataProcessor.this.innerWaysWithoutAdditionalTags.contains(way.getId())) {
				addWayToTiles(way, BaseTileBasedDataProcessor.this.bboxEnlargement);
			}

			return true;
		}
	}
}
