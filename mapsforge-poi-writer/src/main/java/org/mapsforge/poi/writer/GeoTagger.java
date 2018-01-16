/*
 * Copyright 2017 Gustl22
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
package org.mapsforge.poi.writer;

import com.google.common.collect.Lists;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.poi.storage.DbConstants;
import org.mapsforge.poi.writer.logging.LoggerWrapper;
import org.openstreetmap.osmosis.core.domain.v0_6.EntityType;
import org.openstreetmap.osmosis.core.domain.v0_6.Relation;
import org.openstreetmap.osmosis.core.domain.v0_6.RelationMember;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;
import org.openstreetmap.osmosis.core.domain.v0_6.WayNode;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * GeoTagger provides location Tags from OSM-Data in Poi-Tags (especially is_in and address tags).
 */
class GeoTagger {

    private static final Logger LOGGER = LoggerWrapper.getLogger(GeoTagger.class.getName());

    private PoiWriter writer;
    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();
    private PreparedStatement pStmtInsertWayNodes = null;
    private PreparedStatement pStmtDeletePoiData = null;
    private PreparedStatement pStmtDeletePoiCategory = null;
    private PreparedStatement pStmtDeletePoiIndex = null;
    private PreparedStatement pStmtUpdateData = null;
    private PreparedStatement pStmtNodesInBox = null;
    private PreparedStatement pStmtTagsByID = null;

    //List of Administrative Boundaries Relations
    private List<List<Relation>> administrativeBoundaries;
    private List<Relation> postalBoundaries;

    GeoTagger(PoiWriter writer) {
        this.writer = writer;
        try {
            this.pStmtInsertWayNodes = writer.conn.prepareStatement(DbConstants.INSERT_WAYNODES_STATEMENT);
            this.pStmtDeletePoiData = writer.conn.prepareStatement(DbConstants.DELETE_DATA_STATEMENT);
            this.pStmtDeletePoiIndex = writer.conn.prepareStatement(DbConstants.DELETE_INDEX_STATEMENT);
            this.pStmtDeletePoiCategory = writer.conn.prepareStatement(DbConstants.DELETE_CATEGORY_MAP_STATEMENT);
            this.pStmtUpdateData = writer.conn.prepareStatement(DbConstants.UPDATE_DATA_STATEMENT);
            this.pStmtNodesInBox = writer.conn.prepareStatement(DbConstants.FIND_IN_BOX_STATEMENT);
            this.pStmtTagsByID = writer.conn.prepareStatement(DbConstants.FIND_DATA_BY_ID_STATEMENT);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        //Init Relation list
        postalBoundaries = new ArrayList<>();
        administrativeBoundaries = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            administrativeBoundaries.add(new ArrayList<Relation>());
        }
    }

    private int batchCountWays = 0;

    void storeAdministrativeBoundaries(Way way) {

        Collection<Tag> tags = way.getTags();
        for (Tag tag : tags) {
            switch (tag.getKey()) {
                case "building":
                case "highway":
                case "landuse":
                case "leisure":
                case "amenity":
                case "sport":
                case "waterway":
                case "barrier":
                case "railway":
                case "foot":
//                    //"natural is no nonBound-case"
                    return;
                //TODO Add more cases for other nonboundaries
                case "boundary":
                    // This case is simpler than exclude all non valid cases.
                    // But it will not get all bound results, some have no tags set and exist only as
                    // relation; it's better to exclude them and
                    // then add possible boundaries to database
                    storeWay(way);
                    return;
            }
        }
        //No tags occured that speek against a boundary
        storeWay(way);
    }

    private void storeWay(Way way) {
        int i = 0;
        try {
            for (WayNode wayNode : way.getWayNodes()) {
                pStmtInsertWayNodes.setLong(1, way.getId());
                pStmtInsertWayNodes.setLong(2, wayNode.getNodeId());
                pStmtInsertWayNodes.setInt(3, i);
                pStmtInsertWayNodes.addBatch();

                i++;

                batchCountWays++;
                if (batchCountWays % PoiWriter.BATCH_LIMIT == 0) {
                    pStmtInsertWayNodes.executeBatch();
                    pStmtInsertWayNodes.clearBatch();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    void filterBoundaries(Relation relation) {
        if ("boundary".equalsIgnoreCase(writer.getTagValue(relation.getTags(), "type"))) {
            String boundaryCategory = writer.getTagValue(relation.getTags(), "boundary");
            if ("administrative".equalsIgnoreCase(boundaryCategory)) {
                String adminLevelValue = writer.getTagValue(relation.getTags(), "admin_level");
                if (adminLevelValue == null) return;
                switch (adminLevelValue.trim()) {
                    //TODO Specify cultural/regional diffs for admin_levels,
                    // which should be the lowest level of administrative boundary, tagged with is_in
                    case "7":
                        administrativeBoundaries.get(6).add(relation);
                        return;
                    case "8":
                        administrativeBoundaries.get(7).add(relation);
                        return;
                    case "9":
                        administrativeBoundaries.get(8).add(relation);
                        return;
                    case "10":
                        administrativeBoundaries.get(9).add(relation);
                        return;
                    default:
                }
            } else if ("postal_code".equalsIgnoreCase(boundaryCategory)) {
                postalBoundaries.add(relation);
            }
        }
    }

    void processBoundaries() {
        int nPostalBounds = 0;
        for (Relation postalBoundary : postalBoundaries) {
            if (nPostalBounds % 10 == 0) {
                System.out.printf("Progress: PostalBounds " + nPostalBounds + "/"
                        + postalBoundaries.size() + " \r");
            }
            processBoundary(postalBoundary, true);
            nPostalBounds++;
        }
        commit();

        for (int i = administrativeBoundaries.size() - 1; i >= 0; i--) {
            int nAdminBounds = 0;
            List<Relation> administrativeBoundary = administrativeBoundaries.get(i);
            for (Relation relation : administrativeBoundary) {
                if (nAdminBounds % 10 == 0) {
                    System.out.printf("Progress: AdminLevel " + i + ": " + nAdminBounds + "/"
                            + administrativeBoundary.size() + " \r");
                }
                processBoundary(relation, false);
                nAdminBounds++;
            }
            commit();
        }
    }

    private void processBoundary(Relation relation, boolean isPostCode) {
        Coordinate[] coordinates = mergeBoundary(relation);
        if (coordinates == null) return;

        LOGGER.finer("Polygon created; ");
        Polygon polygon = GEOMETRY_FACTORY.createPolygon(GEOMETRY_FACTORY.createLinearRing(coordinates), null);

        //Get pois in bounds
        Map<Poi, Map<String, String>> pois = getPoisInsidePolygon(polygon);

        if (isPostCode) {
            //Remove doubles dependent on postcode
            pois = removeDoublePois(pois);

            //Tag entries
            String postcode = writer.getTagValue(relation.getTags(), "postal_code");
            if (postcode != null && !postcode.isEmpty()) {
                updateTagData(pois, "addr:postcode", postcode);

                //Add addr:city tag, if its available from note tag
                String city = writer.getTagValue(relation.getTags(), "note");
                if (city != null && !city.isEmpty()) {
                    int i = city.indexOf(postcode);
                    if (i >= 0) {
                        city = city.substring(0, i) + city.substring(postcode.length(), city.length()).trim();
                        updateTagData(pois, "addr:city", city);
                    }
                }
            }
        } else {
            Map.Entry<Poi, Map<String, String>> adminArea = null;
            for (Map.Entry<Poi, Map<String, String>> entry : pois.entrySet()) {

                Map<String, String> tagmap = entry.getValue();
                if (!tagmap.keySet().contains("place") || !tagmap.keySet().contains("is_in")) {
                    continue;
                }

                switch (tagmap.get("place")) {
                    case "town":
                    case "village":
                    case "hamlet":
                    case "isolated_dwelling":
                    case "allotments":
                    case "suburb":
                        break;
                    default:
                        continue;
                }

                if (adminArea == null) {
                    //If adminArea not set
                    adminArea = new HashMap.SimpleEntry<>(entry.getKey(), entry.getValue());
                } else {
                    //If one area is closer to center than the set one (this case would occur rarely)
                    Point center = polygon.getCentroid();
                    LatLong centroid = new LatLong(center.getY(), center.getX());
                    double disOld = centroid.sphericalDistance(new LatLong(adminArea.getKey().lat, adminArea.getKey().lon));
                    double disNew = centroid.sphericalDistance(new LatLong(entry.getKey().lat, entry.getKey().lon));
                    if (disNew < disOld) {
                        adminArea = new HashMap.SimpleEntry<>(entry.getKey(), entry.getValue());
                    }
                }
            }
            String isInTagName = null;
            if (adminArea != null) {
                isInTagName = adminArea.getValue().get("is_in");
            }
            String relationName = writer.getTagValue(relation.getTags(), "name");

            String value = relationName + (isInTagName != null ? "," + isInTagName : "");
            updateTagData(pois, "is_in", value);

            LOGGER.fine(relationName + ": #Pois found: " + pois.size() + "; Is_in tags set");
        }
    }

    private Map<Poi, Map<String, String>> removeDoublePois(Map<Poi, Map<String, String>> pois) {
        Map<String, Poi> uniqueHighways = new HashMap<>();

        Iterator it = pois.entrySet().iterator();
        while (it.hasNext()) {
            @SuppressWarnings("unchecked")
            Map.Entry<Poi, Map<String, String>> entry = (Map.Entry<Poi, Map<String, String>>) it.next();

            Map<String, String> tags = entry.getValue();
            //Only double highways are removed, you can remove second part if you want.
            if (tags.containsKey("name") && tags.containsKey("highway")) {
                String name = tags.get("name");
                if (uniqueHighways.containsKey(name)) {
                    //Write surrounding area as parent in "is_in" tag.
                    try {
                        long id = entry.getKey().id;
                        this.pStmtDeletePoiData.setLong(1, id);
                        this.pStmtDeletePoiIndex.setLong(1, id);
                        this.pStmtDeletePoiCategory.setLong(1, id);

                        this.pStmtDeletePoiData.addBatch();
                        this.pStmtDeletePoiIndex.addBatch();
                        this.pStmtDeletePoiCategory.addBatch();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                    it.remove();
                } else {
                    uniqueHighways.put(name, entry.getKey());
                }
            }
        }
        try {
            pStmtDeletePoiCategory.executeBatch();
            pStmtDeletePoiCategory.clearBatch();
            pStmtDeletePoiData.executeBatch();
            pStmtDeletePoiData.clearBatch();
            pStmtDeletePoiIndex.executeBatch();
            pStmtDeletePoiIndex.clearBatch();
            writer.conn.commit();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return pois;
    }

    private int batchCountRelation = 0;

    /**
     * Updates the tags of a POI-List with key and value.
     *
     * @param pois  Pois, which should be tagged
     * @param key   The tag key
     * @param value The tag value
     */
    private void updateTagData(Map<Poi, Map<String, String>> pois, String key, String value) {
        for (Map.Entry<Poi, Map<String, String>> entry : pois.entrySet()) {
            Poi poi = entry.getKey();
            String tmpValue = value;

            Map<String, String> tagmap = entry.getValue();
            if (!tagmap.keySet().contains("name")) {
                continue;
            }

            if (tagmap.keySet().contains(key)) {
                // Process is_in tags
                if (!key.equals("is_in")) {
                    continue;
                }
                String prev = tagmap.get(key);
                // Continue if tag already set correctly
                if (prev.contains(",") || prev.contains(";")) {
                    continue;
                }
                // If string is a correct value, append it to existent value;
                if (tmpValue.contains(",") || tmpValue.contains(";")) {
                    tmpValue = (prev + "," + tmpValue);
                }
            }

            //Write surrounding area as parent in "is_in" tag.
            tagmap.put(key, tmpValue);
            try {
                this.pStmtUpdateData.setLong(2, poi.id);
                this.pStmtUpdateData.setString(1, writer.tagsToString(tagmap));

                this.pStmtUpdateData.addBatch();

                batchCountRelation++;
                if (batchCountRelation % PoiWriter.BATCH_LIMIT == 0) {
                    pStmtUpdateData.executeBatch();
                    pStmtUpdateData.clearBatch();
                    writer.conn.commit();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Merges Boundaries of a relation
     *
     * @param relation The relation of boundary
     * @return A List of Coordinates for the Boundary, first and last element are the same.
     */
    private Coordinate[] mergeBoundary(Relation relation) {
        List<List<Long>> bounds = new ArrayList<>();
        for (RelationMember relationMember : relation.getMembers()) {
            if (relationMember.getMemberType().equals(EntityType.Way)
                    && "outer".equalsIgnoreCase(relationMember.getMemberRole())) {
                List<Long> waynodes = writer.findWayNodesByWayID(relationMember.getMemberId());
                if (waynodes != null && !waynodes.isEmpty()) {
                    bounds.add(waynodes);
                    continue;
                }
                LOGGER.finer("\n---> Member not found | Membertype= " + relationMember.getMemberType().name()
                        + ", Memberrole= " + relationMember.getMemberRole() + "\n");
                continue;
            }
            LOGGER.finer("\n---> Member not accepted | Membertype= " + relationMember.getMemberType().name()
                    + ", Memberrole= " + relationMember.getMemberRole() + "\n");
        }
        //Merge bound nodes (There's may a simpler method)
        if (bounds.isEmpty()) {
            return null;
        }
        LOGGER.fine("Administrative: " + writer.getTagValue(relation.getTags(), "name")
                + " #Members: " + relation.getMembers().size()
                + " #Segments: " + bounds.size());

        //Iterate through given list
        final double threshold = 20; //In meters;
        for (int i = 1; i < bounds.size(); i++) {
            //Create second list, to compare values
            List<Long> I = bounds.get(i);
            List<Long> check = I;
            long nIa = I.get(0);
            long nIb = I.get(I.size() - 1);
            LatLong Ia = writer.findNodeByID(I.get(0));
            LatLong Ib = writer.findNodeByID(I.get(I.size() - 1));
            for (int j = 0; j < bounds.size(); j++) {
                if (i == j) continue;
                //LOGGER.info("j: "+ j+"; i: "+i);
                List<Long> J = bounds.get(j);
                long nJa = J.get(0);
                long nJb = J.get(J.size() - 1);
                LatLong Ja = writer.findNodeByID(J.get(0));
                LatLong Jb = writer.findNodeByID(J.get(J.size() - 1));
                if (Ia == null || Ib == null || Ja == null || Jb == null) return null;
                //If first of I and last of J matches: merge
                if (Ia.sphericalDistance(Jb) < threshold) {
                    I.remove(0);
                    J.addAll(I);
                    bounds.set(j, J);
                    bounds.remove(i);
                    i--;
                    LOGGER.finest("matches: " + Ia.latitude + ", " + Ia.longitude
                            + "; Ids: " + nIa + ", " + nJb);
                    break;
                } else if (Ib.sphericalDistance(Jb) < threshold) {
                    //If list I is reversed
                    I = Lists.reverse(I);
                    I.remove(0);
                    J.addAll(I);
                    bounds.set(j, J);
                    bounds.remove(i);
                    i--;
                    LOGGER.finest("reverse I matches: " + Ib.latitude + ", " + Ib.longitude
                            + "; Ids: " + nIb + ", " + nJb);
                    break;
                } else if (Ia.sphericalDistance(Ja) < threshold) {
                    //If list J is reversed
                    J = Lists.reverse(J);
                    I.remove(0);
                    J.addAll(I);
                    bounds.set(j, J);
                    bounds.remove(i);
                    i--;
                    LOGGER.finest("reverse J matches: " + Ia.latitude + ", " + Ia.longitude
                            + "; Ids: " + nIa + ", " + nJb);
                    break;
                } else if (Ib.sphericalDistance(Ja) < threshold) {
                    //If both are reversed
                    J = Lists.reverse(J);
                    I = Lists.reverse(I);
                    I.remove(0);
                    J.addAll(I);
                    bounds.set(j, J);
                    bounds.remove(i);
                    i--;
                    LOGGER.finest("reverse Both matches: " + Ib.latitude + ", " + Ib.longitude
                            + "; Ids: " + nIb + ", " + nJa);
                    break;
                }
            }
            //One path does'not match, so return
            if (bounds.contains(check) && bounds.size() > 1) {
                LOGGER.finer("Merging failed");
                return null; //continue if you want to add all calculatated boundary parts.
            }
        }
        LOGGER.finer("Bound merging finished; Size= " + bounds.size());
        //Check the result
        //TODO Calculate areas which have more than 1 circle bound. (Simple cover func.)
        if (bounds.size() != 1) {
            return null;
        }
        LOGGER.finer("Bound has right size; ");

        Long[] area = bounds.get(0).toArray(new Long[bounds.get(0).size()]);
        LatLong node = writer.findNodeByID(area[0]);
        if (node == null || node.sphericalDistance(writer.findNodeByID(area[area.length - 1])) >= threshold) {
            return null;
        }
        area[0] = area[area.length - 1];
        LOGGER.finer("Last node is first node; ");
        // Convert the way to a JTS polygon
        Coordinate[] coordinates = new Coordinate[area.length];
        for (int j = 0; j < area.length; j++) {
            LatLong wayNode = writer.findNodeByID(area[j]);
            if (wayNode == null) return null;
            coordinates[j] = new Coordinate(wayNode.longitude, wayNode.latitude);
        }

        return coordinates;
    }

    private class Poi {
        long id;
        double lat;
        double lon;

        Poi(long id, double lat, double lon) {
            this.id = id;
            this.lat = lat;
            this.lon = lon;
        }

        @Override
        public boolean equals(Object obj) {
            return obj != null && obj instanceof Poi && ((Poi) obj).id == this.id;
        }

        @Override
        public int hashCode() {
            return ((Long) id).hashCode();
        }
    }

    private Map<Poi, Map<String, String>> getPoisInsidePolygon(Polygon polygon) {
        Coordinate[] coordinates = polygon.getBoundary().getCoordinates();
        double minLat = coordinates[0].y;
        double minLon = coordinates[0].x;
        BoundingBox bbox = new BoundingBox(minLat, minLon, minLat, minLon);
        for (Coordinate coord : coordinates) {
            bbox = bbox.extendCoordinates(coord.y, coord.x);
        }

        Map<Poi, Map<String, String>> pois = new HashMap<>();
        LOGGER.finer("Bbox: minLat: " + bbox.minLatitude + "; minLon: " + bbox.minLongitude
                + "; maxLat: " + bbox.maxLatitude + "; maxLon: " + bbox.maxLongitude + ";");
        try {

            this.pStmtNodesInBox.setDouble(1, bbox.maxLatitude); //poi_index.minLat <= ?
            this.pStmtNodesInBox.setDouble(2, bbox.maxLongitude); //poi_index.minLon <= ?
            this.pStmtNodesInBox.setDouble(3, bbox.minLatitude); //poi_index.minLat >= ?
            this.pStmtNodesInBox.setDouble(4, bbox.minLongitude); //poi_index.minLon >= ?

            ResultSet rs = this.pStmtNodesInBox.executeQuery();
            while (rs.next()) {
                //poi_index.id, poi_index.minLat, poi_index.minLon, poi_data.data, poi_categories.name
                long id = rs.getLong(1);
                double lat = rs.getDouble(2);
                double lon = rs.getDouble(3);

                Point point = GEOMETRY_FACTORY.createPoint(new Coordinate(lon, lat));
                if (!point.within(polygon)) {
                    continue;
                }

                //Handle Tags
                this.pStmtTagsByID.setLong(1, id);
                ResultSet rsTags = pStmtTagsByID.executeQuery();
                Map<String, String> tagmap = new HashMap<>();
                while (rsTags.next()) {
                    tagmap.putAll(writer.stringToTags(rsTags.getString(2)));
                }
                rsTags.close();

                pois.put(new Poi(id, lat, lon), tagmap);
                LOGGER.finest("Bbox: InnerNode-Id: " + id + "; Lat: " + lat + "; Lon: " + lon + ";");
//                if (categname.equals("Cities") || categname.equals("Towns")
//                        || categname.equals("Villages")
//                        || categname.equals("Hamlets")) {
//                    places.put(id, tagmap);
//                }
            }
            rs.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return pois;
    }

    void commit() {
        try {
            pStmtInsertWayNodes.executeBatch();
            pStmtUpdateData.executeBatch();
            pStmtInsertWayNodes.clearBatch();
            pStmtUpdateData.clearBatch();
            writer.conn.commit();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
