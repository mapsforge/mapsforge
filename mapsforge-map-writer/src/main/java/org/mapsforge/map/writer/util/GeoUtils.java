/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright 2015 lincomatic
 * Copyright 2017-2019 Gustl22
 * Copyright 2018 devemux86
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
package org.mapsforge.map.writer.util;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.TopologyException;
import org.locationtech.jts.simplify.TopologyPreservingSimplifier;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.util.LatLongUtils;
import org.mapsforge.core.util.MercatorProjection;
import org.mapsforge.map.writer.model.TDNode;
import org.mapsforge.map.writer.model.TDWay;
import org.mapsforge.map.writer.model.TileCoordinate;
import org.mapsforge.map.writer.model.WayDataBlock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Provides utility functions for the maps preprocessing.
 */
public final class GeoUtils {
    /**
     * The minimum amount of coordinates (lat/lon counted separately) required for a valid closed polygon.
     */
    public static final int MIN_COORDINATES_POLYGON = 8;

    // private static final double DOUGLAS_PEUCKER_SIMPLIFICATION_TOLERANCE = 0.0000188;
    // private static final double DOUGLAS_PEUCKER_SIMPLIFICATION_TOLERANCE = 0.00003;
    /**
     * The minimum amount of nodes required for a valid closed polygon.
     */
    public static final int MIN_NODES_POLYGON = 4;

    private static final double[] EPSILON_ZERO = new double[]{0, 0};
    // JTS
    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();
    private static final Logger LOGGER = Logger.getLogger(GeoUtils.class.getName());
    private static final byte SUBTILE_ZOOMLEVEL_DIFFERENCE = 2;

    private static final int[] TILE_BITMASK_VALUES = new int[]{32768, 16384, 8192, 4096, 2048, 1024, 512, 256, 128,
            64, 32, 16, 8, 4, 2, 1};

    /**
     * Clips a geometry to a tile.
     *
     * @param way                 the way
     * @param geometry            the geometry
     * @param tileCoordinate      the tile coordinate
     * @param enlargementInMeters the bounding box buffer
     * @return the clipped geometry
     */
    public static Geometry clipToTile(TDWay way, Geometry geometry, TileCoordinate tileCoordinate,
                                      int enlargementInMeters) {
        Geometry tileBBJTS = null;
        Geometry ret = null;

        // create tile bounding box
        tileBBJTS = tileToJTSGeometry(tileCoordinate.getX(), tileCoordinate.getY(), tileCoordinate.getZoomlevel(),
                enlargementInMeters);

        // clip the geometry by intersection with the bounding box of the tile
        // may throw a TopologyException
        try {
            if (!geometry.isValid()) {
                // this should stop the problem of non-noded intersections that trigger an error when
                // clipping
                LOGGER.warning("invalid geometry prior to tile clipping, trying to repair " + way.getId());
                geometry = JTSUtils.repairInvalidPolygon(geometry);
                if (!geometry.isValid()) {
                    LOGGER.warning("invalid geometry even after attempt to fix " + way.getId());
                }
            }
            ret = tileBBJTS.intersection(geometry);
            // according to Ludwig (see issue332) valid polygons may become invalid by clipping (at least
            // in the Python shapely library
            // we need to investigate this more closely and write approriate test cases
            // for now, I check whether the resulting polygon is valid and if not try to repair it
            if ((ret instanceof Polygon || ret instanceof MultiPolygon) && !ret.isValid()) {
                LOGGER.warning("clipped way is not valid, trying to repair it: " + way.getId());
                ret = JTSUtils.repairInvalidPolygon(ret);
                if (ret == null) {
                    way.setInvalid(true);
                    LOGGER.warning("could not repair invalid polygon: " + way.getId());
                }
            }
        } catch (TopologyException e) {
            LOGGER.log(Level.WARNING, "JTS cannot clip way, not storing it in data file: " + way.getId(), e);
            way.setInvalid(true);
            return null;
        }
        return ret;
    }

    /**
     * A tile on zoom level <i>z</i> has exactly 16 sub tiles on zoom level <i>z+2</i>. For each of these 16 sub tiles
     * it is analyzed if the given way needs to be included. The result is represented as a 16 bit short value. Each bit
     * represents one of the 16 sub tiles. A bit is set to 1 if the sub tile needs to include the way. Representation is
     * row-wise.
     *
     * @param geometry           the geometry which is analyzed
     * @param tile               the tile which is split into 16 sub tiles
     * @param enlargementInMeter amount of pixels that is used to enlarge the bounding box of the way and the tiles in the mapping
     *                           process
     * @return a 16 bit short value that represents the information which of the sub tiles needs to include the way
     */
    public static short computeBitmask(final Geometry geometry, final TileCoordinate tile, final int enlargementInMeter) {
        List<TileCoordinate> subtiles = tile
                .translateToZoomLevel((byte) (tile.getZoomlevel() + SUBTILE_ZOOMLEVEL_DIFFERENCE));

        short bitmask = 0;
        int tileCounter = 0;
        for (TileCoordinate subtile : subtiles) {
            Geometry bbox = tileToJTSGeometry(subtile.getX(), subtile.getY(), subtile.getZoomlevel(),
                    enlargementInMeter);
            if (bbox.intersects(geometry)) {
                bitmask |= TILE_BITMASK_VALUES[tileCounter];
            }
            tileCounter++;
        }
        return bitmask;
    }

    /**
     * @param geometry the JTS {@link Geometry} object
     * @return the centroid of the given geometry
     */
    public static LatLong computeCentroid(Geometry geometry) {
        Point centroid = geometry.getCentroid();
        if (centroid != null) {
            return new LatLong(centroid.getCoordinate().y, centroid.getCoordinate().x);
        }

        return null;
    }

    /**
     * @param geometry the JTS {@link Geometry} object
     * @return the interior point of the given geometry
     */
    public static LatLong computeInteriorPoint(Geometry geometry) {
        Point interiorPoint = geometry.getInteriorPoint();
        if (interiorPoint != null) {
            return new LatLong(interiorPoint.getCoordinate().y, interiorPoint.getCoordinate().x);
        }

        return null;
    }

    // *********** PREPROCESSING OF WAYS **************

    /**
     * @param geometry           a JTS {@link Geometry} object representing the OSM entity
     * @param tile               the tile
     * @param enlargementInMeter the enlargement of the tile in meters
     * @return true, if the geometry is covered completely by this tile
     */
    public static boolean coveredByTile(final Geometry geometry, final TileCoordinate tile, final int enlargementInMeter) {
        Geometry bbox = tileToJTSGeometry(tile.getX(), tile.getY(), tile.getZoomlevel(), enlargementInMeter);
        if (bbox.covers(geometry)) {
            return true;
        }

        return false;
    }

    // **************** WAY OR POI IN TILE *****************

    /**
     * @param way the TDWay
     * @return the boundary of way
     */
    public static BoundingBox mapWayToBoundingBox(TDWay way) {
        TDNode[] wayNodes = way.getWayNodes();
        if (wayNodes.length < 3) {
            return null;
        }
        try {
            double lat = LatLongUtils.microdegreesToDegrees(wayNodes[0].getLatitude());
            double lon = LatLongUtils.microdegreesToDegrees(wayNodes[0].getLongitude());
            BoundingBox bb = new BoundingBox(lat, lon, lat, lon);
            for (int i = 1; i < wayNodes.length; i++) {
                bb = bb.extendCoordinates(
                        LatLongUtils.microdegreesToDegrees(wayNodes[i].getLatitude()),
                        LatLongUtils.microdegreesToDegrees(wayNodes[i].getLongitude()));
            }
            return bb;
        } catch (IllegalArgumentException ex) {
            LOGGER.warning("wrong coordinates on way: " + way.toString()
                    + "\nLat: " + LatLongUtils.microdegreesToDegrees(wayNodes[0].getLatitude())
                    + " Lon: " + LatLongUtils.microdegreesToDegrees(wayNodes[0].getLongitude()));
        }
        return null;
    }

    /**
     * @param way the TDWay
     * @return way as a polygon
     */
    public static Polygon mapWayToPolygon(TDWay way) {
        TDNode[] wayNodes = way.getWayNodes();
        if (wayNodes.length < 3) {
            return null;
        }
        Coordinate[] wayCoords = new Coordinate[wayNodes.length + 1];
        for (int i = 0; i < wayCoords.length - 1; i++) {
            wayCoords[i] = new Coordinate(wayNodes[i].getLatitude(), wayNodes[i].getLongitude());
        }
        wayCoords[wayCoords.length - 1] = wayCoords[0];
        Polygon polygon = GEOMETRY_FACTORY.createPolygon(GEOMETRY_FACTORY.createLinearRing(wayCoords), null);
        if (!polygon.isValid()) {
            return null;
        }
        return polygon;
    }

    /**
     * Computes which tiles on the given base zoom level need to include the given way (which may be a polygon).
     *
     * @param way                the way that is mapped to tiles
     * @param baseZoomLevel      the base zoom level which is used in the mapping
     * @param enlargementInMeter amount of pixels that is used to enlarge the bounding box of the way and the tiles in the mapping
     *                           process
     * @return all tiles on the given base zoom level that need to include the given way, an empty set if no tiles are
     * matched
     */
    public static Set<TileCoordinate> mapWayToTiles(final TDWay way, final byte baseZoomLevel,
                                                    final int enlargementInMeter) {
        if (way == null) {
            LOGGER.fine("way is null in mapping to tiles");
            return Collections.emptySet();
        }

        HashSet<TileCoordinate> matchedTiles = new HashSet<>();
        Geometry wayGeometry = JTSUtils.toJTSGeometry(way);
        if (wayGeometry == null) {
            way.setInvalid(true);
            LOGGER.fine("unable to create geometry from way: " + way.getId());
            return matchedTiles;
        }

        TileCoordinate[] bbox = getWayBoundingBox(way, baseZoomLevel, enlargementInMeter);
        // calculate the tile coordinates and the corresponding bounding boxes
        try {
            for (int k = bbox[0].getX(); k <= bbox[1].getX(); k++) {
                for (int l = bbox[0].getY(); l <= bbox[1].getY(); l++) {
                    Geometry bboxGeometry = tileToJTSGeometry(k, l, baseZoomLevel, enlargementInMeter);
                    if (bboxGeometry.intersects(wayGeometry)) {
                        matchedTiles.add(new TileCoordinate(k, l, baseZoomLevel));
                    }
                }
            }
        } catch (TopologyException e) {
            LOGGER.fine("encountered error during mapping of a way to corresponding tiles, way id: " + way.getId());
            return Collections.emptySet();
        }

        return matchedTiles;
    }

    /**
     * @param latLong the point
     * @param tile    the tile
     * @return true if the point is located in the given tile
     */
    public static boolean pointInTile(LatLong latLong, TileCoordinate tile) {
        if (latLong == null || tile == null) {
            return false;
        }

        double lon1 = MercatorProjection.tileXToLongitude(tile.getX(), tile.getZoomlevel());
        double lon2 = MercatorProjection.tileXToLongitude(tile.getX() + 1, tile.getZoomlevel());
        double lat1 = MercatorProjection.tileYToLatitude(tile.getY(), tile.getZoomlevel());
        double lat2 = MercatorProjection.tileYToLatitude(tile.getY() + 1, tile.getZoomlevel());
        return latLong.latitude <= lat1 && latLong.latitude >= lat2 && latLong.longitude >= lon1
                && latLong.longitude <= lon2;
    }

    /**
     * Simplifies a geometry using the Douglas Peucker algorithm.
     *
     * @param way                  the way
     * @param geometry             the geometry
     * @param zoomlevel            the zoom level
     * @param simplificationFactor the simplification factor
     * @return the simplified geometry
     */
    public static Geometry simplifyGeometry(TDWay way, Geometry geometry, byte zoomlevel, int tileSize,
                                            double simplificationFactor) {
        Geometry ret = null;

        Envelope bbox = geometry.getEnvelopeInternal();
        // compute maximal absolute latitude (so that we don't need to care if we
        // are on northern or southern hemisphere)
        double latMax = Math.max(Math.abs(bbox.getMaxY()), Math.abs(bbox.getMinY()));
        double deltaLat = deltaLat(simplificationFactor, latMax, zoomlevel, tileSize);

        try {
            ret = TopologyPreservingSimplifier.simplify(geometry, deltaLat);
        } catch (TopologyException e) {
            LOGGER.log(Level.FINE,
                    "JTS cannot simplify way due to an error, not simplifying way with id: " + way.getId(), e);
            way.setInvalid(true);
            return geometry;
        }

        return ret;
    }

    /**
     * Convert a JTS Geometry to a WayDataBlock list.
     *
     * @param geometry a geometry object which should be converted
     * @return a list of WayBlocks which you can use to save the way.
     */
    public static List<WayDataBlock> toWayDataBlockList(Geometry geometry) {
        List<WayDataBlock> res = new ArrayList<>();
        if (geometry instanceof MultiPolygon) {
            MultiPolygon mp = (MultiPolygon) geometry;
            for (int i = 0; i < mp.getNumGeometries(); i++) {
                Polygon p = (Polygon) mp.getGeometryN(i);
                List<Integer> outer = toCoordinateList(p.getExteriorRing());
                if (outer.size() / 2 > 0) {
                    List<List<Integer>> inner = new ArrayList<>();
                    for (int j = 0; j < p.getNumInteriorRing(); j++) {
                        List<Integer> innr = toCoordinateList(p.getInteriorRingN(j));
                        if (innr.size() / 2 > 0) {
                            inner.add(innr);
                        }
                    }
                    res.add(new WayDataBlock(outer, inner));
                }
            }
        } else if (geometry instanceof Polygon) {
            Polygon p = (Polygon) geometry;
            List<Integer> outer = toCoordinateList(p.getExteriorRing());
            if (outer.size() / 2 > 0) {
                List<List<Integer>> inner = new ArrayList<>();
                for (int i = 0; i < p.getNumInteriorRing(); i++) {
                    List<Integer> innr = toCoordinateList(p.getInteriorRingN(i));
                    if (innr.size() / 2 > 0) {
                        inner.add(innr);
                    }
                }
                res.add(new WayDataBlock(outer, inner));
            }
        } else if (geometry instanceof MultiLineString) {
            MultiLineString ml = (MultiLineString) geometry;
            for (int i = 0; i < ml.getNumGeometries(); i++) {
                LineString l = (LineString) ml.getGeometryN(i);
                List<Integer> outer = toCoordinateList(l);
                if (outer.size() / 2 > 0) {
                    res.add(new WayDataBlock(outer, null));
                }
            }
        } else if (geometry instanceof LinearRing || geometry instanceof LineString) {
            List<Integer> outer = toCoordinateList(geometry);
            if (outer.size() / 2 > 0) {
                res.add(new WayDataBlock(outer, null));
            }
        } else if (geometry instanceof GeometryCollection) {
            GeometryCollection gc = (GeometryCollection) geometry;
            for (int i = 0; i < gc.getNumGeometries(); i++) {
                List<WayDataBlock> recursiveResult = toWayDataBlockList(gc.getGeometryN(i));
                for (WayDataBlock wayDataBlock : recursiveResult) {
                    List<Integer> outer = wayDataBlock.getOuterWay();
                    if (outer.size() / 2 > 0) {
                        res.add(wayDataBlock);
                    }
                }
            }
        }

        return res;
    }

    private static double[] bufferInDegrees(long tileY, byte zoom, int enlargementInMeter) {
        if (enlargementInMeter == 0) {
            return EPSILON_ZERO;
        }

        double[] epsilons = new double[2];
        double lat = MercatorProjection.tileYToLatitude(tileY, zoom);
        epsilons[0] = LatLongUtils.latitudeDistance(enlargementInMeter);
        epsilons[1] = LatLongUtils.longitudeDistance(enlargementInMeter, lat);

        return epsilons;
    }

    // **************** JTS CONVERSIONS *********************

    private static double[] computeTileEnlargement(double lat, int enlargementInMeter) {
        if (enlargementInMeter == 0) {
            return EPSILON_ZERO;
        }

        double[] epsilons = new double[2];

        epsilons[0] = LatLongUtils.latitudeDistance(enlargementInMeter);
        epsilons[1] = LatLongUtils.longitudeDistance(enlargementInMeter, lat);

        return epsilons;
    }

    // Computes the amount of latitude degrees for a given distance in pixel at a given zoom level.
    private static double deltaLat(double deltaPixel, double lat, byte zoom, int tileSize) {
        long mapSize = MercatorProjection.getMapSize(zoom, tileSize);
        double pixelY = MercatorProjection.latitudeToPixelY(lat, mapSize);
        double lat2 = MercatorProjection.pixelYToLatitude(pixelY + deltaPixel, mapSize);

        return Math.abs(lat2 - lat);
    }

    private static TileCoordinate[] getWayBoundingBox(final TDWay way, byte zoomlevel, int enlargementInMeter) {
        double maxx = Double.NEGATIVE_INFINITY, maxy = Double.NEGATIVE_INFINITY, minx = Double.POSITIVE_INFINITY, miny = Double.POSITIVE_INFINITY;
        for (TDNode coordinate : way.getWayNodes()) {
            maxy = Math.max(maxy, LatLongUtils.microdegreesToDegrees(coordinate.getLatitude()));
            miny = Math.min(miny, LatLongUtils.microdegreesToDegrees(coordinate.getLatitude()));
            maxx = Math.max(maxx, LatLongUtils.microdegreesToDegrees(coordinate.getLongitude()));
            minx = Math.min(minx, LatLongUtils.microdegreesToDegrees(coordinate.getLongitude()));
        }

        double[] epsilonsTopLeft = computeTileEnlargement(maxy, enlargementInMeter);
        double[] epsilonsBottomRight = computeTileEnlargement(miny, enlargementInMeter);

        TileCoordinate[] bbox = new TileCoordinate[2];
        bbox[0] = new TileCoordinate((int) MercatorProjection.longitudeToTileX(minx - epsilonsTopLeft[1], zoomlevel),
                (int) MercatorProjection.latitudeToTileY(maxy + epsilonsTopLeft[0], zoomlevel), zoomlevel);
        bbox[1] = new TileCoordinate(
                (int) MercatorProjection.longitudeToTileX(maxx + epsilonsBottomRight[1], zoomlevel),
                (int) MercatorProjection.latitudeToTileY(miny - epsilonsBottomRight[0], zoomlevel), zoomlevel);

        return bbox;
    }

    private static Geometry tileToJTSGeometry(long tileX, long tileY, byte zoom, int enlargementInMeter) {
        double minLat = MercatorProjection.tileYToLatitude(tileY + 1, zoom);
        double maxLat = MercatorProjection.tileYToLatitude(tileY, zoom);
        double minLon = MercatorProjection.tileXToLongitude(tileX, zoom);
        double maxLon = MercatorProjection.tileXToLongitude(tileX + 1, zoom);

        double[] epsilons = bufferInDegrees(tileY, zoom, enlargementInMeter);

        minLon -= epsilons[1];
        minLat -= epsilons[0];
        maxLon += epsilons[1];
        maxLat += epsilons[0];

        Coordinate bottomLeft = new Coordinate(minLon, minLat);
        Coordinate topRight = new Coordinate(maxLon, maxLat);

        return GEOMETRY_FACTORY.createLineString(new Coordinate[]{bottomLeft, topRight}).getEnvelope();
    }

    private static List<Integer> toCoordinateList(Geometry jtsGeometry) {
        Coordinate[] jtsCoords = jtsGeometry.getCoordinates();

        ArrayList<Integer> result = new ArrayList<>();

        for (int j = 0; j < jtsCoords.length; j++) {
            LatLong latLong = new LatLong(jtsCoords[j].y, jtsCoords[j].x);
            result.add(Integer.valueOf(LatLongUtils.degreesToMicrodegrees(latLong.latitude)));
            result.add(Integer.valueOf(LatLongUtils.degreesToMicrodegrees(latLong.longitude)));
        }

        return result;
    }

    private GeoUtils() {
    }
}
