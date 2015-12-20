/*
 * Copyright 2015 devemux86
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

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.poi.storage.DbConstants;
import org.mapsforge.poi.storage.PoiCategory;
import org.mapsforge.poi.storage.PoiCategoryFilter;
import org.mapsforge.poi.storage.PoiCategoryManager;
import org.mapsforge.poi.storage.UnknownPoiCategoryException;
import org.mapsforge.poi.storage.WhitelistPoiCategoryFilter;
import org.mapsforge.poi.writer.logging.LoggerWrapper;
import org.mapsforge.poi.writer.logging.ProgressManager;
import org.mapsforge.poi.writer.model.PoiWriterConfiguration;
import org.openstreetmap.osmosis.core.container.v0_6.EntityContainer;
import org.openstreetmap.osmosis.core.domain.v0_6.Entity;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;
import org.openstreetmap.osmosis.core.domain.v0_6.WayNode;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Stack;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import gnu.trove.map.hash.TLongObjectHashMap;

/**
 * Reads entities from an OSM stream and writes them to a SQLite database.
 * Entities can be filtered and grouped by categories by using an XML definition.
 */
public final class PoiWriter {
	/**
	 * Creates a new instance of a {@link PoiWriter}.
	 *
	 * @param configuration
	 *			Configuration for the POI writer.
	 * @param progressManager
	 *			Object that sends progress messages to a GUI.
	 * @return a new instance of a {@link PoiWriter}
	 */
	public static PoiWriter newInstance(PoiWriterConfiguration configuration, ProgressManager progressManager) {
		return new PoiWriter(configuration, progressManager);
	}

	private static final Logger LOGGER = LoggerWrapper.getLogger(PoiWriter.class.getName());

	// For debug purposes only (at least for now)
	private static final boolean INCLUDE_META_DATA = false;

	private static final int BATCH_LIMIT = 1000;

	/**
	 * The minimum amount of nodes required for a valid closed polygon.
	 */
	private static final int MIN_NODES_POLYGON = 4;

	private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();

	private static final Pattern NAME_LANGUAGE_PATTERN = Pattern.compile("(name)(:)([a-zA-Z]{1,3}(?:[-_][a-zA-Z0-9]{1,8})*)");

	private final PoiWriterConfiguration configuration;
	private final ProgressManager progressManager;

	private final TLongObjectHashMap<Node> nodes;

	// Available categories
	private final PoiCategoryManager categoryManager;

	// Mappings
	private final TagMappingResolver tagMappingResolver;

	// Accepted categories
	private final PoiCategoryFilter categoryFilter;

	// Statistics
	private int poiAdded = 0;

	// Database
	private Connection conn = null;
	private PreparedStatement pStmt = null;
	private PreparedStatement pStmt2 = null;

	private PoiWriter(PoiWriterConfiguration configuration, ProgressManager progressManager) {
		this.configuration = configuration;
		this.progressManager = progressManager;

		this.nodes = new TLongObjectHashMap<>();

		LOGGER.info("Loading POI categories from XML...");

		// Get categories defined in XML
		this.categoryManager = new XMLPoiCategoryManager(configuration.getTagMapping());

		// Tag -> POI mapper
		this.tagMappingResolver = new TagMappingResolver(configuration.getTagMapping(), this.categoryManager);

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
			prepareDatabase(configuration.getOutputFile().getAbsolutePath());
		} catch (ClassNotFoundException | SQLException | UnknownPoiCategoryException e) {
			e.printStackTrace();
		}

		LOGGER.info("Creating POI database...");
		this.progressManager.initProgressBar(0, 0);
		this.progressManager.setMessage("Creating POI database");
	}

	private void commit() throws SQLException {
		LOGGER.info("Committing...");
		this.progressManager.setMessage("Committing...");
		this.pStmt.executeBatch();
		this.pStmt2.executeBatch();
		this.conn.commit();
	}

	public void complete() {
		NumberFormat nfMegabyte = NumberFormat.getInstance();
		NumberFormat nfCounts = NumberFormat.getInstance();
		nfCounts.setGroupingUsed(true);
		nfMegabyte.setMaximumFractionDigits(2);

		try {
			commit();
			finalizeDatabase();
			writeMetadata();
			this.conn.close();
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
	 * Finalize database, i.e. remove categories without POIs and children.
	 */
	private void finalizeDatabase() throws SQLException {
		LOGGER.info("Finalizing database...");
		this.conn.setAutoCommit(true);
		PreparedStatement pStmtChildren = this.conn.prepareStatement("SELECT COUNT(*) FROM poi_categories WHERE parent = ?;");
		PreparedStatement pStmtPoi = this.conn.prepareStatement("SELECT COUNT(*) FROM poi_data WHERE category = ?;");
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
						if (nPoi == 0) {
							pStmtDel.setInt(1, id);
							pStmtDel.executeUpdate();
						}
					}
				}
			}
		}
	}

	private void prepareDatabase(String path) throws ClassNotFoundException, SQLException, UnknownPoiCategoryException {
		Class.forName("org.sqlite.JDBC");
		this.conn = DriverManager.getConnection("jdbc:sqlite:" + path);
		this.conn.setAutoCommit(false);

		Statement stmt = this.conn.createStatement();

		// CREATE TABLES
		stmt.execute(DbConstants.DROP_METADATA_STATEMENT);
		stmt.execute(DbConstants.DROP_INDEX_STATEMENT);
		stmt.execute(DbConstants.DROP_DATA_STATEMENT);
		stmt.execute(DbConstants.DROP_CATEGORIES_STATEMENT);
		stmt.execute(DbConstants.CREATE_CATEGORIES_STATEMENT);
		stmt.execute(DbConstants.CREATE_DATA_STATEMENT);
		stmt.execute(DbConstants.CREATE_INDEX_STATEMENT);
		stmt.execute(DbConstants.CREATE_METADATA_STATEMENT);

		this.pStmt = this.conn.prepareStatement(DbConstants.INSERT_INDEX_STATEMENT);
		this.pStmt2 = this.conn.prepareStatement(DbConstants.INSERT_DATA_STATEMENT);
		PreparedStatement pStmt3 = this.conn.prepareStatement(DbConstants.INSERT_CATEGORIES_STATEMENT);

		// INSERT CATEGORIES
		PoiCategory root = this.categoryManager.getRootCategory();
		pStmt3.setLong(1, root.getID());
		pStmt3.setString(2, root.getTitle());
		pStmt3.setNull(3, 0);
		pStmt3.addBatch();

		Stack<PoiCategory> children = new Stack<>();
		children.push(root);
		while (!children.isEmpty()) {
			for (PoiCategory c : children.pop().getChildren()) {
				pStmt3.setLong(1, c.getID());
				pStmt3.setString(2, c.getTitle());
				pStmt3.setInt(3, c.getParent().getID());
				pStmt3.addBatch();
				children.push(c);
			}
		}
		pStmt3.executeBatch();
		this.conn.commit();
	}

	public void process(EntityContainer entityContainer) {
		Entity entity = entityContainer.getEntity();
		LOGGER.finest("Processing entity: " + entity.toString());

		switch (entity.getType()) {
			case Node:
				Node node = (Node) entity;
				if (this.configuration.isWays()) {
					this.nodes.put(node.getId(), node);
				}
				processEntity(node, node.getLatitude(), node.getLongitude());
				break;
			case Way:
				if (this.configuration.isWays()) {
					processWay((Way) entity);
				}
				break;
		}

		// Hint to GC
		entity = null;
	}

	/**
	 * Process a generic entity using the given location coordinates.
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

		Map<String, String> tagMap = new HashMap<>(20, 1.0f);
		String tagStr = null;

		// Get entity's tag and data
		for (Tag tag : entity.getTags()) {
			String key = tag.getKey().toLowerCase(Locale.ENGLISH);
			if (this.tagMappingResolver.getMappingTags().contains(key)) {
				// Save this tag
				tagStr = key + "=" + tag.getValue();
			} else {
				// Save the entity's data
				tagMap.put(key, tag.getValue());
			}
		}

		// Check if there is a POI category for this tag and add POI to DB
		try {
			// Get category from tag
			PoiCategory pc = null;
			if (tagStr != null) {
				pc = this.tagMappingResolver.getCategoryFromTag(tagStr);
			}

			// Add entity if its category matches
			if (pc != null && this.categoryFilter.isAcceptedCategory(pc)) {
				++this.poiAdded;
				writePOI(this.poiAdded, latitude, longitude, tagMap, pc);
			}
		} catch (UnknownPoiCategoryException e) {
			LOGGER.warning("The '" + tagStr + "' tag refers to a POI that does not exist: " + e.getMessage());
		}
	}

	/**
	 * Only process ways that are polygons.
	 */
	private void processWay(Way way) {
		// The first and the last way node are the same
		if (!way.isClosed()) {
			return;
		}

		// The way has at least 4 way nodes
		if (way.getWayNodes().size() < MIN_NODES_POLYGON) {
			LOGGER.finer("Found closed polygon with fewer than 4 way nodes. Way id: " + way.getId());
			return;
		}

		// Retrieve way nodes
		boolean validWay = true;
		Node[] wayNodes = new Node[way.getWayNodes().size()];
		int i = 0;
		for (WayNode wayNode : way.getWayNodes()) {
			wayNodes[i] = this.nodes.get(wayNode.getNodeId());
			if (wayNodes[i] == null) {
				validWay = false;
				LOGGER.finer("Unknown way node " + wayNode.getNodeId() + " in way " + way.getId());
			}
			i++;
		}

		// For a valid way all way nodes must be existent in the input data
		if (!validWay) {
			return;
		}

		// Convert the way to a JTS polygon
		Coordinate[] coordinates = new Coordinate[wayNodes.length];
		for (int j = 0; j < wayNodes.length; j++) {
			Node wayNode = wayNodes[j];
			coordinates[j] = new Coordinate(wayNode.getLongitude(), wayNode.getLatitude());
		}
		Polygon polygon = GEOMETRY_FACTORY.createPolygon(GEOMETRY_FACTORY.createLinearRing(coordinates), null);

		// Compute the centroid of the polygon
		Point centroid = polygon.getCentroid();

		// Process the way
		processEntity(way, centroid.getY(), centroid.getX());
	}

	private String tagsToString(Map<String, String> tagMap) {
		StringBuilder sb = new StringBuilder();

		for (String key : tagMap.keySet()) {
			// Skip some tags
			if (key.equalsIgnoreCase("created_by")) {
				continue;
			}

			sb.append(key).append('=').append(tagMap.get(key)).append(';');
		}

		return sb.toString();
	}

	private void writeMetadata() throws SQLException {
		LOGGER.info("Writing metadata...");
		this.conn.setAutoCommit(false);
		PreparedStatement pStmtMetadata = this.conn.prepareStatement(DbConstants.INSERT_METADATA_STATEMENT);

		// Bounds
		pStmtMetadata.setString(1, DbConstants.METADATA_BOUNDS);
		BoundingBox bb = this.configuration.getBboxConfiguration();
		if (bb != null) {
			pStmtMetadata.setString(2, bb.minLatitude + "," + bb.minLongitude + "," + bb.maxLatitude + "," + bb.maxLongitude);
		} else {
			pStmtMetadata.setNull(2, Types.NULL);
		}
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
		if (this.configuration.getPreferredLanguage() != null) {
			pStmtMetadata.setString(2, this.configuration.getPreferredLanguage());
		} else {
			pStmtMetadata.setNull(2, Types.NULL);
		}
		pStmtMetadata.addBatch();

		// Version
		pStmtMetadata.setString(1, DbConstants.METADATA_VERSION);
		pStmtMetadata.setInt(2, this.configuration.getFileSpecificationVersion());
		pStmtMetadata.addBatch();

		// Writer
		pStmtMetadata.setString(1, DbConstants.METADATA_WRITER);
		pStmtMetadata.setString(2, this.configuration.getWriterVersion());
		pStmtMetadata.addBatch();

		pStmtMetadata.executeBatch();
		this.conn.commit();
	}

	private void writePOI(long id, double latitude, double longitude, Map<String, String> poiData,
						  PoiCategory category) {
		try {
			// Index data
			this.pStmt.setLong(1, id);
			this.pStmt.setDouble(2, latitude);
			this.pStmt.setDouble(3, latitude);
			this.pStmt.setDouble(4, longitude);
			this.pStmt.setDouble(5, longitude);

			// POI data
			this.pStmt2.setLong(1, id);

			// If all important data should be written to DB
			if (INCLUDE_META_DATA) {
				this.pStmt2.setString(2, tagsToString(poiData));
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
					this.pStmt2.setString(2, name);
				} else {
					this.pStmt2.setNull(2, Types.NULL);
				}
			}

			this.pStmt2.setInt(3, category.getID());

			this.pStmt.addBatch();
			this.pStmt2.addBatch();

			if (this.poiAdded % BATCH_LIMIT == 0) {
				this.pStmt.executeBatch();
				this.pStmt2.executeBatch();

				this.pStmt.clearBatch();
				this.pStmt2.clearBatch();
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
}
