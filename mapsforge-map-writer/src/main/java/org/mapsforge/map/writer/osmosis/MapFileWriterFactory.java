/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
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

import org.mapsforge.map.writer.model.MapWriterConfiguration;
import org.mapsforge.map.writer.util.Constants;
import org.openstreetmap.osmosis.core.pipeline.common.TaskConfiguration;
import org.openstreetmap.osmosis.core.pipeline.common.TaskManager;
import org.openstreetmap.osmosis.core.pipeline.common.TaskManagerFactory;
import org.openstreetmap.osmosis.core.pipeline.v0_6.SinkManager;

/**
 * Factory for the mapfile writer osmosis plugin.
 */
class MapFileWriterFactory extends TaskManagerFactory {
	private static final String PARAM_BBOX = "bbox";
	private static final String PARAM_BBOX_ENLARGEMENT = "bbox-enlargement";
	private static final String PARAM_COMMENT = "comment";
	private static final String PARAM_DEBUG_INFO = "debug-file";
	private static final String PARAM_ENCODING = "encoding";
	private static final String PARAM_LABEL_POSITION = "label-position";
	private static final String PARAM_MAP_START_POSITION = "map-start-position";
	private static final String PARAM_MAP_START_ZOOM = "map-start-zoom";
	private static final String PARAM_OUTFILE = "file";
	private static final String PARAM_POLYGON_CLIPPING = "polygon-clipping";
	private static final String PARAM_PREFERRED_LANGUAGE = "preferred-language";
	// private static final String PARAM_WAYNODE_COMPRESSION = "waynode-compression";
	private static final String PARAM_SIMPLIFICATION_FACTOR = "simplification-factor";
	private static final String PARAM_SKIP_INVALID_RELATIONS = "skip-invalid-relations";
	private static final String PARAM_TAG_MAPPING_FILE = "tag-conf-file";
	private static final String PARAM_TYPE = "type";
	private static final String PARAM_WAY_CLIPPING = "way-clipping";
	private static final String PARAM_ZOOMINTERVAL_CONFIG = "zoom-interval-conf";

	@Override
	protected TaskManager createTaskManagerImpl(TaskConfiguration taskConfig) {
		MapWriterConfiguration configuration = new MapWriterConfiguration();
		configuration.addOutputFile(getStringArgument(taskConfig, PARAM_OUTFILE, Constants.DEFAULT_PARAM_OUTFILE));
		configuration.loadTagMappingFile(getStringArgument(taskConfig, PARAM_TAG_MAPPING_FILE, null));

		configuration.addMapStartPosition(getStringArgument(taskConfig, PARAM_MAP_START_POSITION, null));
		configuration.addMapStartZoom(getStringArgument(taskConfig, PARAM_MAP_START_ZOOM, null));
		configuration.addBboxConfiguration(getStringArgument(taskConfig, PARAM_BBOX, null));
		configuration.addZoomIntervalConfiguration(getStringArgument(taskConfig, PARAM_ZOOMINTERVAL_CONFIG, null));

		configuration.setComment(getStringArgument(taskConfig, PARAM_COMMENT, null));
		configuration.setDebugStrings(getBooleanArgument(taskConfig, PARAM_DEBUG_INFO, false));
		configuration.setPolygonClipping(getBooleanArgument(taskConfig, PARAM_POLYGON_CLIPPING, true));
		configuration.setWayClipping(getBooleanArgument(taskConfig, PARAM_WAY_CLIPPING, true));
		configuration.setLabelPosition(getBooleanArgument(taskConfig, PARAM_LABEL_POSITION, false));
		// boolean waynodeCompression = getBooleanArgument(taskConfig, PARAM_WAYNODE_COMPRESSION,
		// true);
		configuration.setSimplification(getDoubleArgument(taskConfig, PARAM_SIMPLIFICATION_FACTOR,
				Constants.DEFAULT_SIMPLIFICATION_FACTOR));
		configuration.setSkipInvalidRelations(getBooleanArgument(taskConfig, PARAM_SKIP_INVALID_RELATIONS, false));

		configuration.setDataProcessorType(getStringArgument(taskConfig, PARAM_TYPE, Constants.DEFAULT_PARAM_TYPE));
		configuration.setBboxEnlargement(getIntegerArgument(taskConfig, PARAM_BBOX_ENLARGEMENT,
				Constants.DEFAULT_PARAM_BBOX_ENLARGEMENT));

		configuration.setPreferredLanguage(getStringArgument(taskConfig, PARAM_PREFERRED_LANGUAGE, null));
		configuration
				.addEncodingChoice(getStringArgument(taskConfig, PARAM_ENCODING, Constants.DEFAULT_PARAM_ENCODING));

		configuration.validate();

		MapFileWriterTask task = new MapFileWriterTask(configuration);
		return new SinkManager(taskConfig.getId(), task, taskConfig.getPipeArgs());
	}
}
