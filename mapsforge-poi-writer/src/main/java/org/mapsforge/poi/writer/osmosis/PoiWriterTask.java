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

import org.mapsforge.mapmaker.logging.LoggerWrapper;
import org.mapsforge.mapmaker.logging.ProgressManager;
import org.mapsforge.poi.writer.util.Constants;
import org.mapsforge.storage.poi.PoiCategory;
import org.mapsforge.storage.poi.PoiCategoryFilter;
import org.mapsforge.storage.poi.PoiCategoryManager;
import org.mapsforge.storage.poi.UnknownPoiCategoryException;
import org.mapsforge.storage.poi.WhitelistPoiCategoryFilter;
import org.openstreetmap.osmosis.core.container.v0_6.EntityContainer;
import org.openstreetmap.osmosis.core.domain.v0_6.Entity;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;
import org.openstreetmap.osmosis.core.task.v0_6.Sink;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This task reads Nodes from an OSM stream and writes them to a SQLite3 database. Nodes can be filtered and grouped by
 * categories by using an XML definition.
 */
public class PoiWriterTask implements Sink {
	private static final Logger LOGGER = LoggerWrapper.getLogger(PoiWriterTask.class.getName());
	private final ProgressManager progressManager;

	// For debug purposes only (at least for now)
	private static final boolean INCLUDE_META_DATA = false;

	// Temporary variables
	String[] data;

	// Available categories
	private final PoiCategoryManager cm;

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
	private PreparedStatement pStmt3 = null;
	private Statement stmt = null;

	/**
	 * This method writes all nodes that can be mapped to a specific category and whose category is in a given whitelist
	 * to a SQLite3 database. The category tree and tag mappings are retrieved from an XML file.
	 *
	 * @param outputFilePath
	 *            Path to the database file that should be written. The file name should end with ".poi".
	 * @param categoryConfigPath
	 *            The XML configuration file containing the category tree and tag mappings. You can use
	 *            "POICategoriesOsmosis.xml" from the mapsforge library here.
	 * @param progressManager
	 *            Object that sends progress messages to a GUI.
	 */
	public PoiWriterTask(String outputFilePath, String categoryConfigPath, ProgressManager progressManager) {
		Properties properties = new Properties();
		try {
			properties.load(PoiWriterTask.class.getClassLoader().getResourceAsStream("default.properties"));
			String writerVersion = Constants.CREATOR_NAME + "-"
					+ properties.getProperty(Constants.PROPERTY_NAME_WRITER_VERSION);

			LOGGER.info("poi-writer version: " + writerVersion);
		} catch (IOException e) {
			throw new RuntimeException("could not find default properties", e);
		}
		LOGGER.setLevel(Level.FINE);

		this.progressManager = progressManager;

		// Get categories defined in XML
		this.cm = new XMLPoiCategoryManager(categoryConfigPath);

		// Get tag -> POI mapper
		this.tagMappingResolver = new TagMappingResolver(categoryConfigPath, this.cm);

		// Set accepted categories (Allow all categories)
		this.categoryFilter = new WhitelistPoiCategoryFilter();
		try {
			this.categoryFilter.addCategory(this.cm.getRootCategory());
		} catch (UnknownPoiCategoryException e) {
			LOGGER.warning("Could not add category to filter: " + e.getMessage());
		}

		// Create database and add categories
		try {
			prepareDatabase(outputFilePath);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (UnknownPoiCategoryException e) {
			e.printStackTrace();
		}

		this.progressManager.initProgressBar(0, 0);
		this.progressManager.setMessage("Creating POI database");
	}

	private void prepareDatabase(String path) throws ClassNotFoundException, SQLException, UnknownPoiCategoryException {
		Class.forName("org.sqlite.JDBC");
		this.conn = DriverManager.getConnection("jdbc:sqlite:" + path);
		this.conn.setAutoCommit(false);

		this.stmt = this.conn.createStatement();

		// CREATE TABLES
		this.stmt.executeUpdate("DROP TABLE IF EXISTS poi_index;");
		this.stmt.executeUpdate("DROP TABLE IF EXISTS poi_data;");
		this.stmt.executeUpdate("DROP TABLE IF EXISTS poi_categories;");
		// stmt.executeUpdate("DROP INDEX IF EXISTS poi_categories_index;");
		this.stmt.executeUpdate("CREATE VIRTUAL TABLE poi_index USING rtree(id, minLat, maxLat, minLon, maxLon);");
		this.stmt.executeUpdate("CREATE TABLE poi_data (id LONG, data BLOB, category INT, PRIMARY KEY (id));");
		this.stmt
				.executeUpdate("CREATE TABLE poi_categories (id INTEGER, name VARCHAR, parent INTEGER, PRIMARY KEY (id));");
		// stmt.executeUpdate("CREATE INDEX poi_categories_index ON poi_categories (id);");

		this.pStmt = this.conn.prepareStatement("INSERT INTO poi_index VALUES (?, ?, ?, ?, ?);");
		this.pStmt2 = this.conn.prepareStatement("INSERT INTO poi_data VALUES (?, ?, ?);");
		this.pStmt3 = this.conn.prepareStatement("INSERT INTO poi_categories VALUES (?, ?, ?);");

		// INSERT CATEGORIES
		PoiCategory root = this.cm.getRootCategory();
		this.pStmt3.setLong(1, root.getID());
		this.pStmt3.setBytes(2, root.getTitle().getBytes());
		this.pStmt3.setNull(3, 0);
		this.pStmt3.addBatch();

		Stack<PoiCategory> children = new Stack<PoiCategory>();
		children.push(root);
		while (!children.isEmpty()) {
			for (PoiCategory c : children.pop().getChildren()) {
				this.pStmt3.setLong(1, c.getID());
				this.pStmt3.setBytes(2, c.getTitle().getBytes());
				this.pStmt3.setInt(3, c.getParent().getID());
				this.pStmt3.addBatch();
				children.push(c);
			}
		}
		this.pStmt3.executeBatch();
		this.conn.commit();

	}

	private void writePOI(long id, double latitude, double longitude, HashMap<String, String> poiData,
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
			// TODO Use PBF here
			if (INCLUDE_META_DATA) {
				this.pStmt2.setBytes(2, tagsToString(poiData).getBytes());
			} else {

				// If name tag is set
				if (poiData.get("name") != null) {
					this.pStmt2.setBytes(2, poiData.get("name").getBytes());
				} else {
					this.pStmt2.setNull(2, 0);
				}
			}

			this.pStmt2.setInt(3, category.getID());

			this.pStmt.addBatch();
			this.pStmt2.addBatch();
		} catch (SQLException e) {
			e.printStackTrace();
		}

	}

	private void commit() {
		this.progressManager.setMessage("Committing...");
		try {
			this.pStmt.executeBatch();
			this.pStmt2.executeBatch();
			this.conn.commit();
			this.conn.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void complete() {
		commit();
		LOGGER.info("Added " + this.nodesAdded + " POIs.");
		this.progressManager.setMessage("Done.");
	}

	/*
	 * (non-Javadoc)
	 * @see org.openstreetmap.osmosis.core.task.v0_6.Initializable#initialize(java.util.Map)
	 */
	@Override
	public void initialize(Map<String, Object> metadata) {
		// nothing to do here
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void release() {
		// do nothing here
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void process(EntityContainer entityContainer) {
		Entity e = entityContainer.getEntity();
		LOGGER.finest("Processing entity: " + e.toString());

		if (e instanceof Node) {
			processNode((Node) e);
		}
	}

	private void processNode(Node n) {
		// Only add nodes that have data
		if (n.getTags().size() != 0) {

			Tag t = null;
			PoiCategory pc = null;
			HashMap<String, String> tagMap = new HashMap<String, String>(20, 1.0f);
			String tag = null;

			// Get nodes tag and name / URL
			for (Iterator<Tag> it = n.getTags().iterator(); it.hasNext();) {
				t = it.next();

				// Save this tag
				if (this.tagMappingResolver.getMappingTags().contains(t.getKey())) {
					tag = t.getKey() + "=" + t.getValue();
				} else {
					// Save the nodes data
					tagMap.put(t.getKey(), t.getValue());
				}
			}

			// Check if there is a POI category for this tag and add POI to DB
			try {
				// Get category from tag
				if (tag != null) {
					pc = this.tagMappingResolver.getCategoryFromTag(tag);
				}

				// Add node if its category matches
				if (pc != null && this.categoryFilter.isAcceptedCategory(pc)) {
					writePOI(n.getId(), n.getLatitude(), n.getLongitude(), tagMap, pc);
					++this.nodesAdded;
				}
			} catch (UnknownPoiCategoryException e) {
				LOGGER.warning("The '" + tag + "' tag refers to a POI that does not exist: " + e.getMessage());
			}
		}
	}

	private static String tagsToString(HashMap<String, String> tagMap) {
		StringBuilder sb = new StringBuilder();

		for (String key : tagMap.keySet()) {

			// Skip some tags
			if (key.equalsIgnoreCase("amenity") || key.equalsIgnoreCase("created_by")) {
				continue;
			}

			sb.append(key).append('=').append(tagMap.get(key)).append(';');
		}

		return sb.toString();
	}
}
