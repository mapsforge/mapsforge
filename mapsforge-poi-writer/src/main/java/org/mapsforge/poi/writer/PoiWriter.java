/*
 * Copyright 2015-2019 devemux86
 * Copyright 2017-2018 Gustl22
 * Copyright 2019 Kamil Donoval
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

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.poi.storage.*;
import org.mapsforge.poi.writer.logging.LoggerWrapper;
import org.mapsforge.poi.writer.logging.ProgressManager;
import org.mapsforge.poi.writer.model.PoiWriterConfiguration;
import org.openstreetmap.osmosis.core.container.v0_6.EntityContainer;
import org.openstreetmap.osmosis.core.domain.v0_6.*;

import java.sql.*;
import java.text.Normalizer;
import java.text.NumberFormat;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads entities from an OSM stream and writes them to a SQLite database.
 * Entities can be filtered and grouped by categories by using an XML definition.
 */
public final class PoiWriter {
    /**
     * Creates a new instance of a {@link PoiWriter}.
     *
     * @param configuration   Configuration for the POI writer.
     * @param progressManager Object that sends progress messages to a GUI.
     * @return a new instance of a {@link PoiWriter}
     */
    public static PoiWriter newInstance(PoiWriterConfiguration configuration, ProgressManager progressManager) {
        return new PoiWriter(configuration, progressManager);
    }

    private static final Logger LOGGER = LoggerWrapper.getLogger(PoiWriter.class.getName());

    static final int BATCH_LIMIT = 1024;

    /**
     * The minimum amount of nodes required for a valid closed polygon.
     */
    private static final int MIN_NODES_POLYGON = 4;

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();

    private static final Pattern NAME_LANGUAGE_PATTERN = Pattern.compile("(name)(:)([a-zA-Z]{1,3}(?:[-_][a-zA-Z0-9]{1,8})*)");
    private static final Pattern NAME_NORMALIZE_PATTERN = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");

    final PoiWriterConfiguration configuration;
    private final ProgressManager progressManager;

    private final NumberFormat nfCounts = NumberFormat.getInstance();

    // Available categories
    private final PoiCategoryManager categoryManager;

    // Mappings
    private final TagMappingResolver tagMappingResolver;

    // Accepted categories
    private final PoiCategoryFilter categoryFilter;

    // Geo tagging
    private GeoTagger geoTagger;

    // Statistics
    private long nNodes = 0;
    private long nWays = 0;
    private long nRelations = 0;
    private long poiAdded = 0;

    // Database
    Connection conn = null;
    private PreparedStatement pStmtCatMap = null;
    private PreparedStatement pStmtData = null;
    private PreparedStatement pStmtIndex = null;
    private PreparedStatement pStmtNodesC = null;
    private PreparedStatement pStmtNodesR = null;
    private PreparedStatement pStmtWayNodesR = null;

    private PoiWriter(PoiWriterConfiguration configuration, ProgressManager progressManager) {
        this.configuration = configuration;
        this.progressManager = progressManager;

        nfCounts.setGroupingUsed(true);

        LOGGER.info("Loading categories...");

        // Get categories defined in XML
        this.categoryManager = new XMLPoiCategoryManager(this.configuration.getTagMapping());

        // Tag -> POI mapper
        this.tagMappingResolver = new TagMappingResolver(this.configuration.getTagMapping(), this.categoryManager);

        // Set accepted categories (allow all categories)
        this.categoryFilter = new WhitelistPoiCategoryFilter();
        try {
            this.categoryFilter.addCategory(this.categoryManager.getRootCategory());
        } catch (UnknownPoiCategoryException e) {
            LOGGER.warning("Could not add category to filter: " + e.getMessage());
        }

        LOGGER.info("Adding tag mappings...");

        // Create database and add categories
        try {
            prepareDatabase();
        } catch (ClassNotFoundException | SQLException | UnknownPoiCategoryException e) {
            e.printStackTrace();
        }

        LOGGER.info("Creating POI database...");
        this.progressManager.initProgressBar(0, 0);
        this.progressManager.setMessage("Creating POI database");

        // Set GeoTagger (note that database has to be created before initializing GeoTagger)
        if (this.configuration.isGeoTags()) {
            this.geoTagger = new GeoTagger(this);
        }
    }

    /**
     * Commit changes.
     */
    private void commit() throws SQLException {
        LOGGER.info("Committing...");
        this.progressManager.setMessage("Committing...");
        this.pStmtIndex.executeBatch();
        this.pStmtData.executeBatch();
        this.pStmtCatMap.executeBatch();
        if (this.configuration.isGeoTags()) {
            this.geoTagger.commit();
        }
        this.conn.commit();
    }

    /**
     * Complete task.
     */
    public void complete() {
        if (this.configuration.isGeoTags()) {
            this.geoTagger.processBoundaries();
        }
        NumberFormat nfMegabyte = NumberFormat.getInstance();
        nfMegabyte.setMaximumFractionDigits(2);

        try {
            commit();
            if (this.configuration.isFilterCategories()) {
                filterCategories();
            }
            writeMetadata();
            this.conn.close();

            postProcess();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        LOGGER.info("Added " + nfCounts.format(this.poiAdded) + " POIs.");
        this.progressManager.setMessage("Done.");

        LOGGER.info("Estimated memory consumption: "
                + nfMegabyte.format(+((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / Math
                .pow(1024, 2))) + "MB");
    }

    /**
     * Filter categories, i.e. without POIs and children.
     */
    private void filterCategories() throws SQLException {
        LOGGER.info("Filtering categories...");
        PreparedStatement pStmtChildren = this.conn.prepareStatement("SELECT COUNT(*) FROM poi_categories WHERE parent = ?;");
        PreparedStatement pStmtPoi = this.conn.prepareStatement("SELECT COUNT(*) FROM poi_category_map WHERE category = ?;");
        PreparedStatement pStmtDel = this.conn.prepareStatement("DELETE FROM poi_categories WHERE id = ?;");
        Statement stmt = this.conn.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT id FROM poi_categories ORDER BY id;");
        while (rs.next()) {
            int id = rs.getInt(1);
            pStmtChildren.setInt(1, id);
            ResultSet rsChildren = pStmtChildren.executeQuery();
            if (rsChildren.next()) {
                long nChildren = rsChildren.getLong(1);
                if (nChildren == 0) {
                    pStmtPoi.setInt(1, id);
                    ResultSet rsPoi = pStmtPoi.executeQuery();
                    if (rsPoi.next()) {
                        long nPoi = rsPoi.getLong(1);
                        // If category not have POI, delete it from DB
                        if (nPoi == 0) {
                            pStmtDel.setInt(1, id);
                            pStmtDel.executeUpdate();
                        }
                    }
                }
            }
        }
    }

    /**
     * Find a <code>Node</code> by its ID.
     */
    LatLong findNodeByID(long id) {
        try {
            this.pStmtNodesR.setLong(1, id);

            ResultSet rs = this.pStmtNodesR.executeQuery();
            if (rs.next()) {
                double lat = rs.getDouble(1);
                double lon = rs.getDouble(2);

                return new LatLong(lat, lon);
            }
            rs.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Find way nodes by its ID.
     */
    List<Long> findWayNodesByWayID(long id) {
        try {
            this.pStmtWayNodesR.setLong(1, id);

            ResultSet rs = this.pStmtWayNodesR.executeQuery();
            Map<Integer, Long> nodeList = new TreeMap<>();
            while (rs.next()) {
                // way, node, position
                Long nodeID = rs.getLong(1);
                Integer pos = rs.getInt(2);
                nodeList.put(pos, nodeID);
            }
            rs.close();
            return new ArrayList<>(nodeList.values());
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Returns value of given tag in a set of tags.
     *
     * @param tags collection of tags
     * @param key  tag key
     * @return Tag value or null if not exists
     */
    String getTagValue(Collection<Tag> tags, String key) {
        for (Tag tag : tags) {
            if (tag.getKey().toLowerCase(Locale.ENGLISH).equals(key.toLowerCase(Locale.ENGLISH))) {
                return tag.getValue();
            }
        }
        return null;
    }

    /**
     * Normalize / remove accents.
     *
     * @param str string with accents
     * @return string without accents
     */
    private String normalize(String str) {
        String normalizedString = Normalizer.normalize(str, Normalizer.Form.NFD);
        return NAME_NORMALIZE_PATTERN.matcher(normalizedString).replaceAll("");
    }

    /**
     * Post-process.
     */
    private void postProcess() throws SQLException {
        LOGGER.info("Post-processing...");

        this.conn = DriverManager.getConnection("jdbc:sqlite:" + this.configuration.getOutputFile().getAbsolutePath());
        this.conn.createStatement().execute(DbConstants.DROP_NODES_STATEMENT);
        this.conn.createStatement().execute(DbConstants.DROP_WAYNODES_STATEMENT);
        this.conn.close();

        this.conn = DriverManager.getConnection("jdbc:sqlite:" + this.configuration.getOutputFile().getAbsolutePath());
        this.conn.createStatement().execute("VACUUM;");
        this.conn.close();
    }

    /**
     * Prepare database.
     */
    private void prepareDatabase() throws ClassNotFoundException, SQLException, UnknownPoiCategoryException {
        Class.forName("org.sqlite.JDBC");
        this.conn = DriverManager.getConnection("jdbc:sqlite:" + this.configuration.getOutputFile().getAbsolutePath());
        this.conn.setAutoCommit(false);

        Statement stmt = this.conn.createStatement();

        // Create tables
        stmt.execute(DbConstants.DROP_WAYNODES_STATEMENT);
        stmt.execute(DbConstants.DROP_NODES_STATEMENT);
        stmt.execute(DbConstants.DROP_METADATA_STATEMENT);
        stmt.execute(DbConstants.DROP_INDEX_STATEMENT);
        stmt.execute(DbConstants.DROP_CATEGORY_MAP_STATEMENT);
        stmt.execute(DbConstants.DROP_DATA_STATEMENT);
        stmt.execute(DbConstants.DROP_CATEGORIES_STATEMENT);
        stmt.execute(DbConstants.CREATE_CATEGORIES_STATEMENT);
        stmt.execute(DbConstants.CREATE_DATA_STATEMENT);
        stmt.execute(DbConstants.CREATE_CATEGORY_MAP_STATEMENT);
        stmt.execute(DbConstants.CREATE_INDEX_STATEMENT);
        stmt.execute(DbConstants.CREATE_METADATA_STATEMENT);
        stmt.execute(DbConstants.CREATE_NODES_STATEMENT);
        stmt.execute(DbConstants.CREATE_WAYNODES_STATEMENT);

        this.pStmtCatMap = this.conn.prepareStatement(DbConstants.INSERT_CATEGORY_MAP_STATEMENT);
        this.pStmtData = this.conn.prepareStatement(DbConstants.INSERT_DATA_STATEMENT);
        this.pStmtIndex = this.conn.prepareStatement(DbConstants.INSERT_INDEX_STATEMENT);

        this.pStmtNodesC = this.conn.prepareStatement(DbConstants.INSERT_NODES_STATEMENT);
        this.pStmtNodesR = this.conn.prepareStatement(DbConstants.FIND_NODES_STATEMENT);
        this.pStmtWayNodesR = this.conn.prepareStatement(DbConstants.FIND_WAYNODES_BY_ID_STATEMENT);

        // Insert categories
        PreparedStatement pStmt = this.conn.prepareStatement(DbConstants.INSERT_CATEGORIES_STATEMENT);
        PoiCategory root = this.categoryManager.getRootCategory();
        pStmt.setLong(1, root.getID());
        pStmt.setString(2, root.getTitle());
        pStmt.setNull(3, 0);
        pStmt.addBatch();

        Stack<PoiCategory> children = new Stack<>();
        children.push(root);
        while (!children.isEmpty()) {
            for (PoiCategory c : children.pop().getChildren()) {
                pStmt.setLong(1, c.getID());
                pStmt.setString(2, c.getTitle());
                pStmt.setInt(3, c.getParent().getID());
                pStmt.addBatch();
                children.push(c);
            }
        }
        pStmt.executeBatch();
        this.conn.commit();
    }

    /**
     * Process task.
     */
    public void process(EntityContainer entityContainer) {
        Entity entity = entityContainer.getEntity();
        LOGGER.finest("Processing entity: " + entity.toString());

        switch (entity.getType()) {
            case Node:
                Node node = (Node) entity;
                if (this.nNodes == 0) {
                    LOGGER.info("Processing nodes...");
                }
                if (this.configuration.isProgressLogs()) {
                    if (nNodes % 100000 == 0) {
                        System.out.print("Progress: Nodes " + nfCounts.format(nNodes) + " \r");
                    }
                }
                ++this.nNodes;
                if (this.configuration.isWays()) {
                    writeNode(node);
                }
                processEntity(node, node.getLatitude(), node.getLongitude());
                break;
            case Way:
                if (this.configuration.isWays()) {
                    Way way = (Way) entity;
                    if (this.nWays == 0) {
                        LOGGER.info("Processing ways...");
                        try {
                            // Write rest nodes
                            this.pStmtNodesC.executeBatch();
                            this.pStmtNodesC.clearBatch();
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                    }
                    if (this.configuration.isProgressLogs()) {
                        if (nWays % 10000 == 0) {
                            System.out.print("Progress: Ways " + nfCounts.format(nWays) + " \r");
                        }
                    }
                    ++this.nWays;
                    processWay(way);
                }
                break;
            case Relation:
                if (this.configuration.isGeoTags() && this.configuration.isWays()) {
                    Relation relation = (Relation) entity;
                    if (this.nRelations == 0) {
                        LOGGER.info("Processing relations...");
                        this.geoTagger.commit();
                    }
                    this.geoTagger.filterBoundaries(relation);
                    if (this.configuration.isProgressLogs()) {
                        if (nRelations % 10000 == 0) {
                            System.out.print("Progress: Relations " + nfCounts.format(nRelations) + " \r");
                        }
                    }
                    ++this.nRelations;
                }
                break;
            default:
                break;
        }

        // Hint to GC
        entity = null;
    }

    /**
     * Process an <code>Entity</code> using the given coordinates.
     */
    private void processEntity(Entity entity, double latitude, double longitude) {
        // Only add entities that fall within the bounding box (if set)
        BoundingBox bb = this.configuration.getBboxConfiguration();
        if (bb != null && !bb.contains(latitude, longitude)) {
            return;
        }

        // Only add entities that have data
        if (entity.getTags().isEmpty()) {
            return;
        }

        // Only add named entities (if set)
        if (this.configuration.isNames()) {
            String name = getTagValue(entity.getTags(), "name");
            if (name == null || name.isEmpty()) {
                return;
            }
        }

        // Process entity
        Map<String, String> tagMap = new TreeMap<>();
        Set<PoiCategory> categories = new HashSet<>();
        for (Tag tag : entity.getTags()) {
            String key = tag.getKey().toLowerCase(Locale.ENGLISH);
            if (this.tagMappingResolver.getMappingTags().contains(key)) {
                // Check if there is a POI category for this tag and add POI to DB
                String tagStr = key + "=" + tag.getValue();
                try {
                    // Get categories from tag
                    List<PoiCategory> pcs = this.tagMappingResolver.getCategoriesFromTag(tagStr);

                    // Get categories from key, if tag wasn't matched
                    // Note: key categories should be parents of their value categories
                    if (pcs == null) {
                        pcs = this.tagMappingResolver.getCategoriesFromTag(key);
                    }

                    if (pcs != null) {
                        for (PoiCategory pc : pcs) {
                            // Add entity if its category matches
                            if (this.categoryFilter.isAcceptedCategory(pc)) {
                                // Collect the POI tags in a sorted manner (once)
                                if (tagMap.isEmpty()) {
                                    for (Tag t : entity.getTags()) {
                                        tagMap.put(t.getKey().toLowerCase(Locale.ENGLISH), t.getValue());

                                        // If normalize is enabled and key == name
                                        if (configuration.isNormalize() && t.getKey().toLowerCase(Locale.ENGLISH).equals("name")) {
                                            String normalizedValue = normalize(t.getValue().toLowerCase(Locale.ROOT));
                                            tagMap.put("normalized_name", normalizedValue);
                                        }
                                    }
                                }
                                categories.add(pc);
                            }
                        }
                    }
                } catch (UnknownPoiCategoryException e) {
                    LOGGER.warning("The '" + tagStr + "' tag refers to a POI that does not exist: " + e.getMessage());
                }
            }
        }

        // Store POI
        if (!tagMap.isEmpty()) {
            writePOI(this.poiAdded++, latitude, longitude, tagMap, categories);
        }
    }

    /**
     * Process a <code>Way</code>.
     */
    private void processWay(Way way) {
        // The way has at least 2 way nodes
        if (way.getWayNodes().size() < 2) {
            LOGGER.finer("Found way with fewer than 2 way nodes. Way id: " + way.getId());
            return;
        }

        // Geo tagging
        if (this.configuration.isGeoTags()) {
            this.geoTagger.storeAdministrativeBoundaries(way);
        }

        // Retrieve way nodes
        boolean validWay = true;
        LatLong[] wayNodes = new LatLong[way.getWayNodes().size()];
        int i = 0;
        for (WayNode wayNode : way.getWayNodes()) {
            wayNodes[i] = findNodeByID(wayNode.getNodeId());
            if (wayNodes[i] == null) {
                validWay = false;
                LOGGER.finer("Unknown way node " + wayNode.getNodeId() + " in way " + way.getId());
                break;
            }
            i++;
        }

        // For a valid way all way nodes must be existent in the input data
        if (!validWay) {
            return;
        }

        //Calculate center of way dependent on its form
        LatLong centroid;
        if (way.isClosed() && way.getWayNodes().size() >= MIN_NODES_POLYGON) {
            // Closed way
            // Convert the way to a JTS polygon
            Coordinate[] coordinates = new Coordinate[wayNodes.length];
            for (int j = 0; j < wayNodes.length; j++) {
                LatLong wayNode = wayNodes[j];
                coordinates[j] = new Coordinate(wayNode.longitude, wayNode.latitude);
            }
            Polygon polygon = GEOMETRY_FACTORY.createPolygon(GEOMETRY_FACTORY.createLinearRing(coordinates), null);
            if (!polygon.isValid()) {
                return;
            }

            // Compute the centroid of the polygon
            Point center = polygon.getCentroid();
            if (center == null) {
                return;
            }
            centroid = new LatLong(center.getY(), center.getX());
        } else {
            // Non-closed way
            centroid = wayNodes[(wayNodes.length - 1) / 2];
        }

        // Process the way
        processEntity(way, centroid.getLatitude(), centroid.getLongitude());
    }

    /**
     * Convert string representation back to tags map.
     */
    Map<String, String> stringToTags(String tagsmapstring) {
        String[] sb = tagsmapstring.split("\\r");
        Map<String, String> map = new HashMap<>();
        for (String set : sb){
            if (set.contains("=")){
                String key = set.split("=")[0];
                String value = set.substring(key.length() + 1);
                map.put(key, value);
            }
        }
        return map;
    }

    /**
     * Convert tags to a string representation using '\r' delimiter.
     */
    String tagsToString(Map<String, String> tagMap) {
        StringBuilder sb = new StringBuilder();
        for (String key : tagMap.keySet()) {
            // Skip some tags
            if (key.equalsIgnoreCase("created_by")) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append('\r');
            }
            sb.append(key).append('=').append(tagMap.get(key));
        }
        return sb.toString();
    }

    /**
     * Write the metadata to the database.
     */
    private void writeMetadata() throws SQLException {
        LOGGER.info("Writing metadata...");
        PreparedStatement pStmtMetadata = this.conn.prepareStatement(DbConstants.INSERT_METADATA_STATEMENT);

        // Bounds
        pStmtMetadata.setString(1, DbConstants.METADATA_BOUNDS);
        BoundingBox bb = this.configuration.getBboxConfiguration();
        if (bb == null) {
            // Calculate bounding box from poi coordinates
            Statement stmt = this.conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT MIN(minLat), MIN(minLon), MAX(maxLat), MAX(maxLon) FROM poi_index;");
            rs.next();
            bb = new BoundingBox(rs.getDouble(1), rs.getDouble(2), rs.getDouble(3), rs.getDouble(4));
        }
        pStmtMetadata.setString(2, bb.minLatitude + "," + bb.minLongitude + "," + bb.maxLatitude + "," + bb.maxLongitude);
        pStmtMetadata.addBatch();

        // Comment
        pStmtMetadata.setString(1, DbConstants.METADATA_COMMENT);
        if (this.configuration.getComment() != null) {
            pStmtMetadata.setString(2, this.configuration.getComment());
        } else {
            pStmtMetadata.setNull(2, Types.NULL);
        }
        pStmtMetadata.addBatch();

        // Date
        pStmtMetadata.setString(1, DbConstants.METADATA_DATE);
        pStmtMetadata.setLong(2, System.currentTimeMillis());
        pStmtMetadata.addBatch();

        // Language
        pStmtMetadata.setString(1, DbConstants.METADATA_LANGUAGE);
        if (!this.configuration.isAllTags() && this.configuration.getPreferredLanguage() != null) {
            pStmtMetadata.setString(2, this.configuration.getPreferredLanguage());
        } else {
            pStmtMetadata.setNull(2, Types.NULL);
        }
        pStmtMetadata.addBatch();

        // Version
        pStmtMetadata.setString(1, DbConstants.METADATA_VERSION);
        pStmtMetadata.setInt(2, this.configuration.getFileSpecificationVersion());
        pStmtMetadata.addBatch();

        // Ways
        pStmtMetadata.setString(1, DbConstants.METADATA_WAYS);
        pStmtMetadata.setString(2, Boolean.toString(this.configuration.isWays()));
        pStmtMetadata.addBatch();

        // Writer
        pStmtMetadata.setString(1, DbConstants.METADATA_WRITER);
        pStmtMetadata.setString(2, this.configuration.getWriterVersion());
        pStmtMetadata.addBatch();

        pStmtMetadata.executeBatch();
        this.conn.commit();
    }

    /**
     * Write a <code>Node</code> to the database.
     */
    private void writeNode(Node node) {
        try {
            this.pStmtNodesC.setLong(1, node.getId());
            this.pStmtNodesC.setDouble(2, node.getLatitude());
            this.pStmtNodesC.setDouble(3, node.getLongitude());

            this.pStmtNodesC.addBatch();

            if (this.nNodes % BATCH_LIMIT == 0) {
                this.pStmtNodesC.executeBatch();

                this.pStmtNodesC.clearBatch();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Write a POI to the database.
     */
    private void writePOI(long id, double latitude, double longitude, Map<String, String> poiData, Set<PoiCategory> categories) {
        try {
            // Index data
            this.pStmtIndex.setLong(1, id);
            this.pStmtIndex.setDouble(2, latitude);
            this.pStmtIndex.setDouble(3, latitude);
            this.pStmtIndex.setDouble(4, longitude);
            this.pStmtIndex.setDouble(5, longitude);
            this.pStmtIndex.addBatch();

            // POI data
            this.pStmtData.setLong(1, id);
            if (this.configuration.isAllTags()) {
                // All tags should be written to DB
                this.pStmtData.setString(2, tagsToString(poiData));
            } else {
                // Use preferred language
                boolean foundPreferredLanguageName = false;
                String name = null;
                for (String key : poiData.keySet()) {
                    if ("name".equals(key) && !foundPreferredLanguageName) {
                        name = poiData.get(key);
                    } else if (this.configuration.getPreferredLanguage() != null && !foundPreferredLanguageName) {
                        Matcher matcher = NAME_LANGUAGE_PATTERN.matcher(key);
                        if (matcher.matches()) {
                            String language = matcher.group(3);
                            if (language.equalsIgnoreCase(this.configuration.getPreferredLanguage())) {
                                name = poiData.get(key);
                                foundPreferredLanguageName = true;
                            }
                        }
                    }
                }
                // If name tag is set
                if (name != null) {
                    this.pStmtData.setString(2, name);
                } else {
                    this.pStmtData.setNull(2, Types.NULL);
                }
            }
            this.pStmtData.addBatch();

            // Categories data
            this.pStmtCatMap.setLong(1, id);
            for (PoiCategory category : categories) {
                this.pStmtCatMap.setInt(2, category.getID());
                this.pStmtCatMap.addBatch();
            }


            if (this.poiAdded % BATCH_LIMIT == 0) {
                this.pStmtIndex.executeBatch();
                this.pStmtData.executeBatch();
                this.pStmtCatMap.executeBatch();

                this.pStmtIndex.clearBatch();
                this.pStmtData.clearBatch();
                this.pStmtCatMap.clearBatch();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
