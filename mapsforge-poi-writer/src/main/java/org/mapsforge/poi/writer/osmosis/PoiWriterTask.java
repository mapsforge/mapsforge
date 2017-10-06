/*
 * Copyright 2010, 2011 mapsforge.org
 * Copyright 2010, 2011 Karsten Groll
 * Copyright 2015-2017 devemux86
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

import org.mapsforge.poi.writer.PoiWriter;
import org.mapsforge.poi.writer.logging.LoggerWrapper;
import org.mapsforge.poi.writer.logging.ProgressManager;
import org.mapsforge.poi.writer.model.PoiWriterConfiguration;
import org.mapsforge.poi.writer.util.Constants;
import org.openstreetmap.osmosis.core.container.v0_6.EntityContainer;
import org.openstreetmap.osmosis.core.task.v0_6.Sink;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * This task reads entities from an OSM stream and writes them to a SQLite database.
 * Entities can be filtered and grouped by categories by using an XML definition.
 */
public class PoiWriterTask implements Sink {
    private static final Logger LOGGER = LoggerWrapper.getLogger(PoiWriterTask.class.getName());

    private final PoiWriter poiWriter;

    /**
     * Writes all entities that can be mapped to a specific category and which category is in a
     * given whitelist to a SQLite database. The category tree and tag mappings are retrieved from
     * an XML file.
     *
     * @param configuration   Configuration for the POI writer.
     * @param progressManager Object that sends progress messages to a GUI.
     */
    public PoiWriterTask(PoiWriterConfiguration configuration, ProgressManager progressManager) {
        Properties properties = new Properties();
        try {
            properties.load(PoiWriterTask.class.getClassLoader().getResourceAsStream("mapsforge-poi.properties"));
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

        this.poiWriter = PoiWriter.newInstance(configuration, progressManager);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {
        // do nothing here
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void complete() {
        this.poiWriter.complete();
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
    public void process(EntityContainer entityContainer) {
        this.poiWriter.process(entityContainer);
    }
}
