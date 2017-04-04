package org.mapsforge.poi.writer;

import com.google.common.collect.Lists;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.poi.storage.DbConstants;
import org.openstreetmap.osmosis.core.domain.v0_6.EntityType;
import org.openstreetmap.osmosis.core.domain.v0_6.Relation;
import org.openstreetmap.osmosis.core.domain.v0_6.RelationMember;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;
import org.openstreetmap.osmosis.core.domain.v0_6.WayNode;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mapsforge.poi.writer.PoiWriter.LOGGER;

/**
 * GeoTagger provides location Tags from OSM-Data in Poi-Tags (especially is_in and address tags)
 * Created by gustl on 03.04.17.
 */

class GeoTagger {
    private PoiWriter writer;
    private Connection conn = null;
    private final int BATCH_LIMIT;
    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();
    private PreparedStatement pStmtInsertWayNodes = null;
    private PreparedStatement pStmtUpdateData = null;
    private PreparedStatement pStmtNodesInBox = null;

    //List of Administrative Boundaries Relations
    private List<List<Relation>> administrativeBoundaries;
    private List<Relation> postalBoundaries;

    GeoTagger(PoiWriter writer) {
        this.writer = writer;
        conn = writer.getConnection();
        BATCH_LIMIT = PoiWriter.BATCH_LIMIT;
        try {
            this.pStmtInsertWayNodes = conn.prepareStatement(DbConstants.INSERT_WAYNODES_STATEMENT);
            this.pStmtUpdateData = this.conn.prepareStatement(DbConstants.UPDATE_DATA_STATEMENT);
            this.pStmtNodesInBox = this.conn.prepareStatement(DbConstants.FIND_IN_BOX_STATEMENT);
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
            }
            batchCountWays++;
            if (batchCountWays % BATCH_LIMIT == 0) {
                pStmtInsertWayNodes.executeBatch();
                pStmtInsertWayNodes.clearBatch();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    void filterBoundaries(Relation relation) {
        if ("boundary".equalsIgnoreCase(writer.getValueOfTag("type", relation.getTags()))) {
            String boundaryCategory = writer.getValueOfTag("boundary", relation.getTags());
            if ("administrative".equalsIgnoreCase(boundaryCategory)) {
                String adminLevelValue = writer.getValueOfTag("admin_level", relation.getTags());
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
        for (Relation postalBoundary : postalBoundaries) {
            processBoundary(postalBoundary, true);
        }
        commit();

        for (int i = administrativeBoundaries.size() - 1; i >= 0; i--) {
            List<Relation> administrativeBoundary = administrativeBoundaries.get(i);
            for (Relation relation : administrativeBoundary) {
                processBoundary(relation, false);
            }
            commit();
        }
    }

    private void processBoundary(Relation relation, boolean isPostCode) {
        Coordinate[] coordinates = mergeBoundary(relation);
        if(coordinates == null) return;

        LOGGER.finer("Polygon created; ");
        Polygon polygon = GEOMETRY_FACTORY.createPolygon(GEOMETRY_FACTORY.createLinearRing(coordinates), null);
        double minLat = coordinates[0].y;
        double minLon = coordinates[0].x;
        BoundingBox bbox = new BoundingBox(minLat, minLon, minLat, minLon);
        for (Coordinate coord : coordinates) {
            bbox = bbox.extendCoordinates(coord.y, coord.x);
        }

        //Get pois in bounds
        Map<Poi, Map<String, String>> pois = getPoisInsideBounds(bbox);

        if(isPostCode){
            String value = writer.getValueOfTag("postal_code", relation.getTags());
            if(value != null || !value.isEmpty()){
                updateTagData(pois, polygon,"addr:postcode", value);
            }
        } else {
            Map.Entry<Poi, Map<String, String>> adminArea = null;
            for (Map.Entry<Poi, Map<String, String>> entry : pois.entrySet()) {
                Poi poi = entry.getKey();

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
                Point point = GEOMETRY_FACTORY.createPoint(new Coordinate(poi.lon, poi.lat));
                if (!point.within(polygon)) {
                    continue;
                }
                if (adminArea == null) {
                    adminArea = new HashMap.SimpleEntry<>(entry.getKey(), entry.getValue());
                } else {
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
            String relationName = writer.getValueOfTag("name", relation.getTags());

            String value = relationName + (isInTagName != null ? "," + isInTagName : "");
            updateTagData(pois, polygon,"is_in", value);

            LOGGER.fine(relationName + ": #Pois found: " + pois.size()+"; Is_in tags set");
        }
    }

    private int batchCountRelation = 0;
    /**
     * Updates the tags of a POI-List with key and value.
     * @param pois Pois, which should be tagged
     * @param polygon Boundary, where the Pois should inherited
     * @param key The tag key
     * @param value The tag value
     */
    private void updateTagData(Map<Poi, Map<String, String>> pois,Polygon polygon, String key, String value){
        for (Map.Entry<Poi, Map<String, String>> entry : pois.entrySet()) {
            Poi poi = entry.getKey();
            String tmpValue = value;

            Map<String, String> tagmap = entry.getValue();
            if (tagmap.keySet().contains(key)) {
                if(!key.equals("is_in")){
                    continue;
                }
                String prev = tagmap.get(key);
                if(prev.contains(",") || prev.contains(";")){
                    continue;
                }
                if(tmpValue.contains(",") || tmpValue.contains(";")){
                    tmpValue = (prev + "," + tmpValue);
                }
            }

            if (!tagmap.keySet().contains("name")) {
                continue;
            }

            Point point = GEOMETRY_FACTORY.createPoint(new Coordinate(poi.lon, poi.lat));
            if (!point.within(polygon)) {
                continue;
            }

            //Write surrounding area as parent in "is_in" tag.
            tagmap.put(key, tmpValue);
            batchCountRelation++;
            try {
                this.pStmtUpdateData.setString(1, writer.tagsToString(tagmap));
                this.pStmtUpdateData.setLong(2, poi.id);

                this.pStmtUpdateData.addBatch();

                if (batchCountRelation % BATCH_LIMIT == 0) {
                    pStmtUpdateData.executeBatch();
                    pStmtUpdateData.clearBatch();
                    conn.commit();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Merges Boundaries of a relation
     * @param relation The relation of boundary
     * @return A List of Coordinates for the Boundary, first and last element are the same.
     */
    private Coordinate[] mergeBoundary(Relation relation){
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
        LOGGER.fine("Administrative: " + writer.getValueOfTag("name", relation.getTags())
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

    private Map<Poi, Map<String, String>> getPoisInsideBounds(BoundingBox bbox) {
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
                Map<String, String> tagmap = writer.stringToTags(rs.getString(4));
                int categ = rs.getInt(5);
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
            conn.commit();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
