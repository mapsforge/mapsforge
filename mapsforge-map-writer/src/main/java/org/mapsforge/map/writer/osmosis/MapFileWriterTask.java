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
package org.mapsforge.map.writer.osmosis;

import java.io.IOException;
import java.text.NumberFormat;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.map.writer.HDTileBasedDataProcessor;
import org.mapsforge.map.writer.MapFileWriter;
import org.mapsforge.map.writer.RAMTileBasedDataProcessor;
import org.mapsforge.map.writer.model.MapWriterConfiguration;
import org.mapsforge.map.writer.model.TileBasedDataProcessor;
import org.mapsforge.map.writer.util.Constants;
import org.openstreetmap.osmosis.core.container.v0_6.EntityContainer;
import org.openstreetmap.osmosis.core.domain.v0_6.Bound;
import org.openstreetmap.osmosis.core.domain.v0_6.Entity;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Relation;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;
import org.openstreetmap.osmosis.core.task.v0_6.Sink;

/**
 * An Osmosis plugin that reads OpenStreetMap data and converts it to a mapsforge binary file.
 * 
 * @author bross
 */
public class MapFileWriterTask implements Sink {
	private static final Logger LOGGER = Logger.getLogger(MapFileWriterTask.class.getName());

	// Accounting
	private int amountOfNodesProcessed = 0;
	private int amountOfWaysProcessed = 0;
	private int amountOfRelationsProcessed = 0;

	private final MapWriterConfiguration configuration;
	private TileBasedDataProcessor tileBasedGeoObjectStore;

	MapFileWriterTask(MapWriterConfiguration configuration) {
		this.configuration = configuration;

		Properties properties = new Properties();
		try {
			properties.load(MapFileWriterTask.class.getClassLoader().getResourceAsStream("default.properties"));
			configuration.setWriterVersion(Constants.CREATOR_NAME + "-"
					+ properties.getProperty(Constants.PROPERTY_NAME_WRITER_VERSION));
			configuration.setFileSpecificationVersion(Integer.parseInt(properties
					.getProperty(Constants.PROPERTY_NAME_FILE_SPECIFICATION_VERSION)));

			LOGGER.info("mapfile-writer version: " + configuration.getWriterVersion());
			LOGGER.info("mapfile format specification version: " + configuration.getFileSpecificationVersion());
		} catch (IOException e) {
			throw new RuntimeException("could not find default properties", e);
		} catch (NumberFormatException e) {
			throw new RuntimeException("map file specification version is not an integer", e);
		}

		// CREATE DATASTORE IF BBOX IS DEFINED
		if (this.configuration.getBboxConfiguration() != null) {
			if ("ram".equalsIgnoreCase(configuration.getDataProcessorType())) {
				this.tileBasedGeoObjectStore = RAMTileBasedDataProcessor.newInstance(configuration);
			} else {
				this.tileBasedGeoObjectStore = HDTileBasedDataProcessor.newInstance(configuration);
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

	@Override
	public final void complete() {
		NumberFormat nfMegabyte = NumberFormat.getInstance();
		NumberFormat nfCounts = NumberFormat.getInstance();
		nfCounts.setGroupingUsed(true);
		nfMegabyte.setMaximumFractionDigits(2);

		LOGGER.info("completing read...");
		this.tileBasedGeoObjectStore.complete();

		LOGGER.info("start writing file...");

		try {
			if (this.configuration.getOutputFile().exists()) {
				LOGGER.info("overwriting file " + this.configuration.getOutputFile().getAbsolutePath());
				this.configuration.getOutputFile().delete();
			}
			MapFileWriter.writeFile(this.configuration, this.tileBasedGeoObjectStore);
		} catch (IOException e) {
			LOGGER.log(Level.SEVERE, "error while writing file", e);
		}

		LOGGER.info("finished...");
		LOGGER.fine("total processed nodes: " + nfCounts.format(this.amountOfNodesProcessed));
		LOGGER.fine("total processed ways: " + nfCounts.format(this.amountOfWaysProcessed));
		LOGGER.fine("total processed relations: " + nfCounts.format(this.amountOfRelationsProcessed));

		LOGGER.info("estimated memory consumption: "
				+ nfMegabyte.format(+((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / Math
						.pow(1024, 2))) + "MB");
	}

	@Override
	public final void release() {
		if (this.tileBasedGeoObjectStore != null) {
			this.tileBasedGeoObjectStore.release();
		}
	}

	@Override
	public final void process(EntityContainer entityContainer) {
		Entity entity = entityContainer.getEntity();

		switch (entity.getType()) {
			case Bound:
				Bound bound = (Bound) entity;
				if (this.configuration.getBboxConfiguration() == null) {
					BoundingBox bbox = new BoundingBox(bound.getBottom(), bound.getLeft(), bound.getTop(),
							bound.getRight());
					this.configuration.setBboxConfiguration(bbox);
					this.configuration.validate();
					if ("ram".equals(this.configuration.getDataProcessorType())) {
						this.tileBasedGeoObjectStore = RAMTileBasedDataProcessor.newInstance(this.configuration);
					} else {
						this.tileBasedGeoObjectStore = HDTileBasedDataProcessor.newInstance(this.configuration);
					}
				}
				LOGGER.info("start reading data...");
				break;

			// *******************************************************
			// ****************** NODE PROCESSING*********************
			// *******************************************************
			case Node:

				if (this.tileBasedGeoObjectStore == null) {
					LOGGER.severe("No valid bounding box found in input data.\n"
							+ "Please provide valid bounding box via command "
							+ "line parameter 'bbox=minLat,minLon,maxLat,maxLon'.\n"
							+ "Tile based data store not initialized. Aborting...");
					throw new IllegalStateException("tile based data store not initialized, missing bounding "
							+ "box information in input data");
				}
				this.tileBasedGeoObjectStore.addNode((Node) entity);
				// hint to GC
				entity = null;
				this.amountOfNodesProcessed++;
				break;

			// *******************************************************
			// ******************* WAY PROCESSING*********************
			// *******************************************************
			case Way:
				this.tileBasedGeoObjectStore.addWay((Way) entity);
				entity = null;
				this.amountOfWaysProcessed++;
				break;

			// *******************************************************
			// ****************** RELATION PROCESSING*********************
			// *******************************************************
			case Relation:
				Relation currentRelation = (Relation) entity;
				this.tileBasedGeoObjectStore.addRelation(currentRelation);
				this.amountOfRelationsProcessed++;
				entity = null;
				break;
		}
	}
}
