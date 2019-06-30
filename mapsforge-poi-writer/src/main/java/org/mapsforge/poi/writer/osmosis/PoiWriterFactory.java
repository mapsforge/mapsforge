/*
 * Copyright 2010, 2011 mapsforge.org
 * Copyright 2010, 2011 Karsten Groll
 * Copyright 2015-2017 devemux86
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
package org.mapsforge.poi.writer.osmosis;

import org.mapsforge.poi.writer.logging.DummyProgressManager;
import org.mapsforge.poi.writer.logging.LoggerWrapper;
import org.mapsforge.poi.writer.logging.ProgressManager;
import org.mapsforge.poi.writer.model.PoiWriterConfiguration;
import org.mapsforge.poi.writer.util.Constants;
import org.openstreetmap.osmosis.core.pipeline.common.TaskConfiguration;
import org.openstreetmap.osmosis.core.pipeline.common.TaskManager;
import org.openstreetmap.osmosis.core.pipeline.common.TaskManagerFactory;
import org.openstreetmap.osmosis.core.pipeline.v0_6.SinkManager;
import org.openstreetmap.osmosis.core.task.v0_6.Sink;

/**
 * Factory for the POI writer osmosis plugin.
 */
public class PoiWriterFactory extends TaskManagerFactory {
    private static final String PARAM_ALL_TAGS = "all-tags";
    private static final String PARAM_BBOX = "bbox";
    private static final String PARAM_COMMENT = "comment";
    private static final String PARAM_FILTER_CATEGORIES = "filter-categories";
    private static final String PARAM_GEO_TAGS = "geo-tags";
    private static final String PARAM_NAMES = "names";
    private static final String PARAM_NORMALIZE = "normalize";
    private static final String PARAM_OUTFILE = "file";
    private static final String PARAM_PREFERRED_LANGUAGE = "preferred-language";
    private static final String PARAM_PROGRESS_LOGS = "progress-logs";
    private static final String PARAM_TAG_MAPPING_FILE = "tag-conf-file";
    private static final String PARAM_WAYS = "ways";

    @Override
    protected TaskManager createTaskManagerImpl(TaskConfiguration taskConfig) {
        PoiWriterConfiguration configuration = new PoiWriterConfiguration();
        configuration.setAllTags(getBooleanArgument(taskConfig, PARAM_ALL_TAGS, true));
        configuration.addBboxConfiguration(getStringArgument(taskConfig, PARAM_BBOX, null));
        configuration.setComment(getStringArgument(taskConfig, PARAM_COMMENT, null));
        configuration.setFilterCategories(getBooleanArgument(taskConfig, PARAM_FILTER_CATEGORIES, true));
        configuration.setGeoTags(getBooleanArgument(taskConfig, PARAM_GEO_TAGS, false));
        configuration.setNames(getBooleanArgument(taskConfig, PARAM_NAMES, true));
        configuration.setNormalize(getBooleanArgument(taskConfig, PARAM_NORMALIZE, false));
        configuration.addOutputFile(getStringArgument(taskConfig, PARAM_OUTFILE, Constants.DEFAULT_PARAM_OUTFILE));
        configuration.setPreferredLanguage(getStringArgument(taskConfig, PARAM_PREFERRED_LANGUAGE, null));
        configuration.setProgressLogs(getBooleanArgument(taskConfig, PARAM_PROGRESS_LOGS, true));
        configuration.loadTagMappingFile(getStringArgument(taskConfig, PARAM_TAG_MAPPING_FILE, null));
        configuration.setWays(getBooleanArgument(taskConfig, PARAM_WAYS, true));

        // If set to true, progress messages will be forwarded to a GUI message handler
        // boolean guiMode = getBooleanArgument(taskConfig, "gui-mode", false);

        ProgressManager progressManager = new DummyProgressManager();

        // Use graphical progress manager if plugin is called from map maker GUI
        /*if (guiMode) {
            try {
                Class<?> clazz = Class.forName(GUI_PROGRESS_MANAGER_CLASS_NAME);
                Method method = clazz.getMethod("getInstance");
                Object o = method.invoke(clazz);

                System.out.println("Progress manager (plugin): " + o);
                progressManager = (ProgressManager) o;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }*/

        // Tell the logger which progress manager to use
        LoggerWrapper.setDefaultProgressManager(progressManager);

        Sink task = new PoiWriterTask(configuration, progressManager);
        return new SinkManager(taskConfig.getId(), task, taskConfig.getPipeArgs());
    }
}
