/*
 * Copyright 2010, 2011 mapsforge.org
 * Copyright 2010, 2011 Karsten Groll
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
package org.mapsforge.poi.writer.osmosis;

import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.poi.storage.DbConstants;
import org.mapsforge.poi.storage.PoiCategory;
import org.mapsforge.poi.storage.PoiCategoryFilter;
import org.mapsforge.poi.storage.PoiCategoryManager;
import org.mapsforge.poi.storage.UnknownPoiCategoryException;
import org.mapsforge.poi.storage.WhitelistPoiCategoryFilter;
import org.mapsforge.poi.writer.TagMappingResolver;
import org.mapsforge.poi.writer.XMLPoiCategoryManager;
import org.mapsforge.poi.writer.logging.LoggerWrapper;
import org.mapsforge.poi.writer.logging.ProgressManager;
import org.mapsforge.poi.writer.model.PoiWriterConfiguration;
import org.mapsforge.poi.writer.util.Constants;
import org.openstreetmap.osmosis.core.container.v0_6.EntityContainer;
import org.openstreetmap.osmosis.core.domain.v0_6.Entity;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;
import org.openstreetmap.osmosis.core.task.v0_6.Sink;

import java.io.IOException;
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
import java.util.Properties;
import java.util.Stack;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This task reads Nodes from an OSM stream and writes them to a SQLite database.
 * Nodes can be filtered and grouped by categories by using an XML definition.
 */
public class PoiWriterTask implements Sink {
	private static final Logger LOGGER = LoggerWrapper.getLogger(PoiWriterTask.class.getName());

	// For debug purposes only (at least for now)
	private static final boolean INCLUDE_META_DATA = false;

	private static final Pattern NAME_LANGUAGE_PATTERN = Pattern.compile("(name)(:)([a-zA-Z]{1,3}(?:[-_][a-zA-Z0-9]{1,8})*)");

	private final PoiWriterConfiguration configuration;
	private final ProgressManager progressManager;

	// Available categories
	private final PoiCategoryManager categoryManager;

	// Mappings
	private final TagMappingResolver tagMappingResolver;

	// Accepted categories
	private final PoiCategoryFilter categoryFilter;

	// Statistics
	private int nodesAdded = 0;

	// Database
	private Connection conn = null;
	private PreparedStatement pStmt = null;
	private PreparedStatement pStmt2 = null;

	/**
	 * This method writes all nodes that can be mapped to a specific category and whose category is
	 * in a given whitelist to a SQLite database. The category tree and tag mappings are retrieved
	 * from an XML file.
	 *
	 * @param configuration
	 *            Configuration for the POI writer.
	 * @param progressManager
	 *            Object that sends progress messages to a GUI.
	 */
	public PoiWriterTask(PoiWriterConfiguration configuration, ProgressManager progressManager) {
		this.configuration = configuration;
		this.progressManager = progressManager;

		Properties properties = new Properties();
		try {
			properties.load(PoiWriterTask.class.getClassLoader().getResourceAsStream("default.properties"));
			configuration.setWriterVersion(Constants.CREATOR_NAME + "-"
					+ properties.getProperty(Constants.PROPERTY_NAME_WRITER_VERSION));
			configuration.setFileSpecificationVersion(Integer.parseInt(properties
					.getProperty(Constants.PROPERTY_NAME_FILE_SPECIFICATION_VERSION)));

			LOGGER.info("POI writer version: " + configuration.getWriterVersion());
			LOGGER.info("POI format specification version: " + configuration.getFileSpecificationVersion());
		} catch (IOException e) {
			throw new RuntimeException("Could not find default properties", e);
		} catch (NumberFormatException e) {
			throw new RuntimeException("POI format specification version is not an integer", e);
		}

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

	/**
	 * {@inheritDoc}
	 */
	@Override
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

		LOGGER.info("Added " + nfCounts.format(this.nodesAdded) + " POIs.");
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

	/*
	 * (non-Javadoc)
	 * @see org.openstreetmap.osmosis.core.task.v0_6.Initializable#initialize(java.util.Map)
	 */
	@Override
	public void initialize(Map<String, Object> metadata) {
		// nothing to do here
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

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void process(EntityContainer entityContainer) {
		Entity e = entityContainer.getEntity();
		LOGGER.finest("Processing entity: " + e.toString());

		switch (e.getType()) {
			case Node:
				processNode((Node) e);
				break;
		}
	}

	private void processNode(Node node) {
		// Only add nodes that fall within the bounding box (if set)
		BoundingBox bb = configuration.getBboxConfiguration();
		if (bb != null && !bb.contains(node.getLatitude(), node.getLongitude())) {
			return;
		}

		// Only add nodes that have data
		if (node.getTags().isEmpty()) {
			return;
		}

		Map<String, String> tagMap = new HashMap<>(20, 1.0f);
		String tagStr = null;

		// Get node's tag and data
		for (Tag tag : node.getTags()) {
			String key = tag.getKey().toLowerCase(Locale.ENGLISH);
			if (this.tagMappingResolver.getMappingTags().contains(key)) {
				// Save this tag
				tagStr = key + "=" + tag.getValue();
			} else {
				// Save the node's data
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

			// Add node if its category matches
			if (pc != null && this.categoryFilter.isAcceptedCategory(pc)) {
				writePOI(node.getId(), node.getLatitude(), node.getLongitude(), tagMap, pc);
				++this.nodesAdded;
			}
		} catch (UnknownPoiCategoryException e) {
			LOGGER.warning("The '" + tagStr + "' tag refers to a POI that does not exist: " + e.getMessage());
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void release() {
		// do nothing here
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
		BoundingBox bb = configuration.getBboxConfiguration();
		if (bb != null) {
			pStmtMetadata.setString(2, bb.minLatitude + "," + bb.minLongitude + "," + bb.maxLatitude + "," + bb.maxLongitude);
		} else {
			pStmtMetadata.setNull(2, Types.NULL);
		}
		pStmtMetadata.addBatch();

		// Comment
		pStmtMetadata.setString(1, DbConstants.METADATA_COMMENT);
		if (configuration.getComment() != null) {
			pStmtMetadata.setString(2, configuration.getComment());
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
		if (configuration.getPreferredLanguage() != null) {
			pStmtMetadata.setString(2, configuration.getPreferredLanguage());
		} else {
			pStmtMetadata.setNull(2, Types.NULL);
		}
		pStmtMetadata.addBatch();

		// Version
		pStmtMetadata.setString(1, DbConstants.METADATA_VERSION);
		pStmtMetadata.setInt(2, configuration.getFileSpecificationVersion());
		pStmtMetadata.addBatch();

		// Writer
		pStmtMetadata.setString(1, DbConstants.METADATA_WRITER);
		pStmtMetadata.setString(2, configuration.getWriterVersion());
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

			// If all important data should be written to db
			if (INCLUDE_META_DATA) {
				this.pStmt2.setString(2, tagsToString(poiData));
			} else {
				// Use preferred language
				boolean foundPreferredLanguageName = false;
				String name = null;
				for (String key : poiData.keySet()) {
					if ("name".equals(key) && !foundPreferredLanguageName) {
						name = poiData.get(key);
					} else if (configuration.getPreferredLanguage() != null && !foundPreferredLanguageName) {
						Matcher matcher = NAME_LANGUAGE_PATTERN.matcher(key);
						if (matcher.matches()) {
							String language = matcher.group(3);
							if (language.equalsIgnoreCase(configuration.getPreferredLanguage())) {
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
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
}
