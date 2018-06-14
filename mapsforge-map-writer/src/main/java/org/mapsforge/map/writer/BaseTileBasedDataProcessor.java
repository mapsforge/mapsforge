/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright 2015 lincomatic
 * Copyright 2017-2018 Gustl22
 * Copyright 2017 devemux86
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

import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.TopologyException;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.util.LatLongUtils;
import org.mapsforge.core.util.MercatorProjection;
import org.mapsforge.map.writer.model.MapWriterConfiguration;
import org.mapsforge.map.writer.model.NodeResolver;
import org.mapsforge.map.writer.model.OSMTag;
import org.mapsforge.map.writer.model.TDNode;
import org.mapsforge.map.writer.model.TDRelation;
import org.mapsforge.map.writer.model.TDWay;
import org.mapsforge.map.writer.model.TileBasedDataProcessor;
import org.mapsforge.map.writer.model.TileCoordinate;
import org.mapsforge.map.writer.model.TileData;
import org.mapsforge.map.writer.model.TileGridLayout;
import org.mapsforge.map.writer.model.TileInfo;
import org.mapsforge.map.writer.model.WayResolver;
import org.mapsforge.map.writer.model.ZoomIntervalConfiguration;
import org.mapsforge.map.writer.util.GeoUtils;
import org.mapsforge.map.writer.util.OSMUtils;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Relation;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import gnu.trove.iterator.TLongIterator;
import gnu.trove.list.array.TLongArrayList;
import gnu.trove.map.hash.TLongObjectHashMap;
import gnu.trove.map.hash.TShortIntHashMap;
import gnu.trove.procedure.TObjectProcedure;
import gnu.trove.set.TLongSet;
import gnu.trove.set.hash.TLongHashSet;

abstract class BaseTileBasedDataProcessor implements TileBasedDataProcessor, NodeResolver, WayResolver {

    protected class RelationHandler implements TObjectProcedure<TDRelation> {
        private List<Deque<TDWay>> extractedPolygons;

        private List<Integer> inner;
        private Map<Integer, List<Integer>> outerToInner;
        private final WayPolygonizer polygonizer = new WayPolygonizer();

        private int nRelations = 0;

        @Override
        public boolean execute(TDRelation relation) {
            if (relation == null) {
                return false;
            }

            if (++this.nRelations % 100 == 0) {
                System.out.print("Progress: Relations " + nfCounts.format(this.nRelations)
                        + " / " + nfCounts.format(getRelationsNumber()) + "\r");
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
                    List<TDNode> waynodeList = new ArrayList<>();
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
                    countWayTags(relation.getTags().keySet());
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
                        countWayTags(outerWay.getTags().keySet());
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
                            int contained = 0;
                            for (Entry<Short, Object> innerTag : innerWay.getTags().entrySet()) {
                                for (Entry<Short, Object> outerTag : outer.getTags().entrySet()) {
                                    if (innerTag.getKey().equals(outerTag.getKey())) {
                                        Object innerValue = innerTag.getValue();
                                        Object outerValue = outerTag.getValue();
                                        if (innerValue != null) {
                                            if (outerValue != null) {
                                                if ((innerValue instanceof Byte && outerValue instanceof Byte && innerValue.equals(outerValue))
                                                        || (innerValue instanceof Integer && outerValue instanceof Integer && innerValue.equals(outerValue))
                                                        || (innerValue instanceof Float && outerValue instanceof Float && innerValue.equals(outerValue))
                                                        || (innerValue instanceof Short && outerValue instanceof Short && innerValue.equals(outerValue))
                                                        || (innerValue instanceof String && outerValue instanceof String && innerValue.equals(outerValue))) {
                                                    contained++;
                                                }
                                            }
                                        } else if (outerValue == null) {
                                            contained++;
                                        }
                                    }
                                }
                            }
                            if (contained == innerWay.getTags().size()) {
                                BaseTileBasedDataProcessor.this.innerWaysWithoutAdditionalTags.add(innerWay.getId());
                            }
                        }
                    } else {
                        List<TDNode> waynodeList = new ArrayList<>();
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
        private int nWays = 0;

        @Override
        public boolean execute(TDWay way) {
            if (way == null) {
                return true;
            }

            if (++this.nWays % 10000 == 0) {
                System.out.print("Progress: Ways " + nfCounts.format(this.nWays)
                        + " / " + nfCounts.format(getWaysNumber()) + "\r");
            }

            // we only consider ways that have tags and which have not already
            // added as outer way of a relation
            // inner ways without additional tags are also not considered as they are processed as part of a
            // multi polygon
            if (way.isRenderRelevant() && !BaseTileBasedDataProcessor.this.outerToInnerMapping.contains(way.getId())
                    && !BaseTileBasedDataProcessor.this.innerWaysWithoutAdditionalTags.contains(way.getId())) {
                // TODO #HDstoreData: integrate all way processes from HD and RAM Processor HERE
                // e.g. outerToInnerMapping, virtualWays, associatedRelations, implicitRelations
                // or declare better that HD Processor does not store object data before writing to file
                addImplicitRelationInformation(way);
                addWayToTiles(way, BaseTileBasedDataProcessor.this.bboxEnlargement);
            }

            return true;
        }
    }

    protected static final Logger LOGGER = Logger.getLogger(BaseTileBasedDataProcessor.class.getName());
    protected final int bboxEnlargement;
    protected final org.mapsforge.core.model.BoundingBox boundingbox;
    // accounting
    protected float[] countWays;
    protected float[] countWayTileFactor;

    protected final TShortIntHashMap histogramPoiTags;
    protected final TShortIntHashMap histogramWayTags;
    protected final TLongSet innerWaysWithoutAdditionalTags;

    protected long maxWayID = Long.MIN_VALUE;
    protected final TLongObjectHashMap<TLongArrayList> outerToInnerMapping;

    protected final List<String> preferredLanguages;
    protected final boolean skipInvalidRelations;
    protected final boolean tagValues;
    protected TileGridLayout[] tileGridLayouts;

    protected final Map<TileCoordinate, TLongHashSet> tilesToCoastlines;
    protected final Map<TileCoordinate, TLongHashSet> tilesToPartElements;
    protected final Map<TileCoordinate, TLongHashSet> tilesToRootElements;
    protected final Map<Long, Long> partRootRelations;

    // Accounting
    private int amountOfNodesProcessed = 0;
    private int amountOfRelationsProcessed = 0;
    private int amountOfWaysProcessed = 0;
    protected final NumberFormat nfCounts = NumberFormat.getInstance();

    protected final ZoomIntervalConfiguration zoomIntervalConfiguration;

    public BaseTileBasedDataProcessor(MapWriterConfiguration configuration) {
        super();
        this.boundingbox = configuration.getBboxConfiguration();
        this.zoomIntervalConfiguration = configuration.getZoomIntervalConfiguration();
        this.tileGridLayouts = new TileGridLayout[this.zoomIntervalConfiguration.getNumberOfZoomIntervals()];
        this.bboxEnlargement = configuration.getBboxEnlargement();
        this.preferredLanguages = configuration.getPreferredLanguages();
        this.skipInvalidRelations = configuration.isSkipInvalidRelations();
        this.tagValues = configuration.isTagValues();

        this.outerToInnerMapping = new TLongObjectHashMap<>();
        this.innerWaysWithoutAdditionalTags = new TLongHashSet();
        this.tilesToCoastlines = new HashMap<>();
        this.tilesToPartElements = new HashMap<>();
        this.tilesToRootElements = new HashMap<>();
        this.partRootRelations = new HashMap<>();

        this.nfCounts.setGroupingUsed(true);

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
    public long cumulatedNumberOfTiles() {
        long cumulated = 0;
        for (int i = 0; i < this.zoomIntervalConfiguration.getNumberOfZoomIntervals(); i++) {
            cumulated += this.tileGridLayouts[i].getAmountTilesHorizontal()
                    * this.tileGridLayouts[i].getAmountTilesVertical();
        }
        return cumulated;
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
    public void addNode(Node node) {
        if (++this.amountOfNodesProcessed % 1000000 == 0) {
            System.out.print("\33[2KAdd nodes: " + nfCounts.format(this.amountOfNodesProcessed) + "\r");
        }
    }

    @Override
    public void addRelation(Relation relation) {
        if (++this.amountOfRelationsProcessed % 10000 == 0) {
            System.out.print("\33[2KAdd relations: " + nfCounts.format(this.amountOfRelationsProcessed) + "\r");
        }
    }

    @Override
    public void addWay(Way way) {
        if (++this.amountOfWaysProcessed % 50000 == 0) {
            // "\33[2K" sequence is needed to clear the line, to overwrite larger previous numbers
            System.out.print("\33[2KAdd ways: " + nfCounts.format(this.amountOfWaysProcessed) + "\r");
        }
    }

    @Override
    public int getNodesNumber() {
        return amountOfNodesProcessed;
    }

    @Override
    public int getRelationsNumber() {
        return amountOfRelationsProcessed;
    }

    @Override
    public int getWaysNumber() {
        return amountOfWaysProcessed;
    }

    /**
     * Add tags to ways which describe the geo-inheritance.
     *
     * @param tdWay the way which should get the tags
     */
    protected void addImplicitRelationInformation(TDWay tdWay) {
        if (!this.tagValues) {
            return;
        }

        // FEATURE Remove this to add id to all elements (increase of space around 1/8)
        if (!this.partRootRelations.containsKey(tdWay.getId())) {
            return;
        }

        Long rootId = this.partRootRelations.get(tdWay.getId());
        if (rootId == null) {
            OSMTagMapping mapping = OSMTagMapping.getInstance();

            // Remove accidentally written ids in OSM
            for (Short aShort : tdWay.getTags().keySet()) {
                if (mapping.getWayTag(aShort).getKey().equals("id")) {
                    tdWay.getTags().remove(aShort);
                    break;
                }
            }

            // Add id to root element (respecting tag-mapping file)
            OSMTag idTag = mapping.getWayTag("id", String.valueOf(tdWay.getId()));
            assert idTag != null;
            tdWay.getTags().put(idTag.getId(), OSMUtils.getObjectFromWildcardAndValue(idTag.getValue(), String.valueOf(tdWay.getId())));
        } else {
            // Add reference id to part element
            tdWay.setRef(String.valueOf(rootId));
        }
        // #HDstoreData: Removing not possible, because HD' tiles need them multiple times (for count and processing)
        // partRootRelations.remove(tdWay.getId());
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
                        LatLongUtils.microdegreesToDegrees(poi.getLongitude()),
                        this.zoomIntervalConfiguration.getBaseZoom(i));
                long tileCoordinateY = MercatorProjection.latitudeToTileY(
                        LatLongUtils.microdegreesToDegrees(poi.getLatitude()),
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

    /**
     * Count tags to optimize order of POI tag data.
     *
     * @param poi the poi which contains tags
     */
    protected void countPoiTags(TDNode poi) {
        if (poi == null || poi.getTags() == null) {
            return;
        }
        for (short tag : poi.getTags().keySet()) {
            this.histogramPoiTags.adjustOrPutValue(tag, 1, 1);
        }
    }

    protected void countWayTags(Set<Short> tags) {
        if (tags != null) {
            for (short tag : tags) {
                this.histogramWayTags.adjustOrPutValue(tag, 1, 1);
            }
        }
    }

    /**
     * Count tags to optimize order of Way tag data.
     *
     * @param way the way which contains tags
     */
    protected void countWayTags(TDWay way) {
        if (way != null) {
            countWayTags(way.getTags().keySet());
        }
    }

    protected abstract TileData getTileImpl(int zoom, int tileX, int tileY);

    protected abstract void handleAdditionalRelationTags(TDWay virtualWay, TDRelation relation);

    /**
     * Calculate ids of ways whose polygon inherits part elements.
     */
    protected void handleImplicitWayRelations() {
        if (!this.tagValues) {
            return;
        }

        /*int progressImplicitRelations = 0;
        float limitImplicitRelations = this.tilesToPartElements.entrySet().size();*/

        // Iterate through tiles which contain parts
        for (Map.Entry<TileCoordinate, TLongHashSet> tilePartElementEntry : this.tilesToPartElements.entrySet()) {
            TLongHashSet tileRootElementSet = this.tilesToRootElements.get(tilePartElementEntry.getKey());

            // Continue if no part or no roots are in list; maybe unnecessary
            if (tileRootElementSet == null || tileRootElementSet.isEmpty()
                    || tilePartElementEntry.getValue().isEmpty()) {
                continue;
            }

            // Log
            /*String wayRelLog = "Progress: Implicit relations "
                    + ((int) ((progressImplicitRelations / limitImplicitRelations) * 100))
                    + "%% - Tile (" + tilePartElementEntry.getKey().getX()
                    + ", " + tilePartElementEntry.getKey().getY() + ")";
            progressImplicitRelations++;
            int nRootElements = 0;*/

            // Load parts only once in cache
            List<TDWay> pElems = new ArrayList<>();
            TLongIterator tilePartElementIterator = tilePartElementEntry.getValue().iterator();
            while (tilePartElementIterator.hasNext()) {
                TDWay pElem = getWay(tilePartElementIterator.next());
                pElems.add(pElem);
            }

            // Iterate through potential roots
            TLongIterator tileRootElementIterator = tileRootElementSet.iterator();
            while (tileRootElementIterator.hasNext()) {
                /*if (++nRootElements % 1000 == 0) {
                    System.out.printf((wayRelLog + " - Elements " + nfCounts.format(nRootElements) + "\r"));
                }*/

                // Init root element
                TDWay rElem = getWay(tileRootElementIterator.next());
                BoundingBox rBox = GeoUtils.mapWayToBoundingBox(rElem);
                if (rBox == null) {
                    continue;
                }
                Polygon rPolygon = null; // Lazy initialization, because root may not be needed

                // Iterate through parts
                for (int i = pElems.size() - 1; i >= 0; i--) {
                    TDWay pElem = pElems.get(i);

                    // Exclude most elements with bounding box
                    if (pElem.getWayNodes().length < 3) {
                        continue;
                    }
                    if (!rBox.contains(LatLongUtils.microdegreesToDegrees(pElem.getWayNodes()[0].getLatitude()),
                            LatLongUtils.microdegreesToDegrees(pElem.getWayNodes()[0].getLongitude()))) {
                        continue;
                    }

                    // Now calculate exact polygons
                    Polygon pPolygon = GeoUtils.mapWayToPolygon(pElem);
                    if (pPolygon == null) {
                        continue;
                    }

                    if (rPolygon == null) {
                        rPolygon = GeoUtils.mapWayToPolygon(rElem);
                        if (rPolygon == null) {
                            continue;
                        }
                    }

                    // Memorize multi-polygons
                    if (rPolygon.contains(pPolygon)) {
                        if (!this.partRootRelations.containsKey(rElem.getId())) {
                            this.partRootRelations.put(rElem.getId(), null); // Add root element
                        }
                        this.partRootRelations.put(pElem.getId(), rElem.getId()); // Add part element

                        // Remove part which is already referenced
                        pElems.remove(pElem);
                        tilePartElementEntry.getValue().remove(pElem.getId());
                    }
                }
            }
        }
        LOGGER.info("calculated " + nfCounts.format(partRootRelations.size()) + " implicit relations");
    }

    protected abstract void handleVirtualInnerWay(TDWay virtualWay);

    protected abstract void handleVirtualOuterWay(TDWay virtualWay);

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

    /**
     * Prepare relations which aren't written as relation and only exist as geo-inheritance.
     * Root and part elements will be assigned to a tile (not to be confused with later tile processing).
     * This facilitates to find matching parts and accelerates the process.
     * <p>
     * Coastlines are handled too, for simplicity reasons.
     *
     * @param tdWay the way, which should be prepared
     */
    protected void prepareImplicitWayRelations(TDWay tdWay) {
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
        } else if (this.tagValues) {
            if (tdWay.isRootElement()) {
                Set<TileCoordinate> rootTiles = GeoUtils.mapWayToTiles(tdWay, TileInfo.TILE_INFO_ZOOMLEVEL, 0);
                for (TileCoordinate tileCoordinate : rootTiles) {
                    TLongHashSet roots = this.tilesToRootElements.get(tileCoordinate);
                    if (roots == null) {
                        roots = new TLongHashSet();
                        this.tilesToRootElements.put(tileCoordinate, roots);
                    }
                    roots.add(tdWay.getId());
                }
            } else if (tdWay.isPartElement()) {
                Set<TileCoordinate> partTiles = GeoUtils.mapWayToTiles(tdWay, TileInfo.TILE_INFO_ZOOMLEVEL, 0);
                for (TileCoordinate tileCoordinate : partTiles) {
                    TLongHashSet parts = this.tilesToPartElements.get(tileCoordinate);
                    if (parts == null) {
                        parts = new TLongHashSet();
                        this.tilesToPartElements.put(tileCoordinate, parts);
                    }
                    parts.add(tdWay.getId());
                }
            }
        }
    }
}
