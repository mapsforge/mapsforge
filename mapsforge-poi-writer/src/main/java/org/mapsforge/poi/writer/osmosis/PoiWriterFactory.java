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

import org.mapsforge.mapmaker.logging.DummyProgressManager;
import org.mapsforge.mapmaker.logging.LoggerWrapper;
import org.mapsforge.mapmaker.logging.ProgressManager;
import org.mapsforge.poi.writer.util.Constants;
import org.openstreetmap.osmosis.core.pipeline.common.TaskConfiguration;
import org.openstreetmap.osmosis.core.pipeline.common.TaskManager;
import org.openstreetmap.osmosis.core.pipeline.common.TaskManagerFactory;
import org.openstreetmap.osmosis.core.pipeline.v0_6.SinkManager;
import org.openstreetmap.osmosis.core.task.v0_6.Sink;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * This class is used for creating {@link PoiWriterTask}s.
 */
public class PoiWriterFactory extends TaskManagerFactory {
	private static final String GUI_PROGRESS_MANAGER_CLASS_NAME = "org.mapsforge.mapmaker.gui.ProgressGUI";

	private static final String PARAM_OUTFILE = "file";
	private static final String PARAM_TAG_MAPPING_FILE = "tag-conf-file";

	/**
	 * Default constructor.
	 */
	public PoiWriterFactory() {
	}

	@Override
	protected TaskManager createTaskManagerImpl(TaskConfiguration taskConfig) {
		// Output file
		String outputFilePath = getStringArgument(taskConfig, PARAM_OUTFILE, Constants.DEFAULT_PARAM_OUTFILE);

		// XML file containing a POI category configuration
		URL configFilePath = loadTagMappingFile(getStringArgument(taskConfig, PARAM_TAG_MAPPING_FILE, null));

		// If set to true, progress messages will be forwarded to a GUI message handler
		boolean guiMode = getBooleanArgument(taskConfig, "gui-mode", false);

		ProgressManager progressManager = new DummyProgressManager();

		// Use graphical progress manager if plugin is called from map maker GUI
		if (guiMode) {
			try {
				Class<?> clazz = Class.forName(GUI_PROGRESS_MANAGER_CLASS_NAME);
				Method method = clazz.getMethod("getInstance");
				Object o = method.invoke(clazz);

				System.out.println("Progress manager (plugin): " + o);
				progressManager = (ProgressManager) o;
			} catch (ClassNotFoundException | SecurityException | IllegalAccessException | NoSuchMethodException | IllegalArgumentException | InvocationTargetException e) {
				e.printStackTrace();
			}
		}

		// Tell the logger which progress manager to use
		LoggerWrapper.setDefaultProgressManager(progressManager);

		// The creation task
		Sink task = new PoiWriterTask(outputFilePath, configFilePath, progressManager);

		return new SinkManager(taskConfig.getId(), task, taskConfig.getPipeArgs());
	}

	/**
	 * Convenience method.
	 *
	 * @param file
	 *            the path to the output file
	 */
	public URL loadTagMappingFile(String file) {
		if (file != null) {
			File f = new File(file);
			if (!f.exists()) {
				throw new IllegalArgumentException("tag mapping file parameter points to a file that does not exist");
			}
			if (f.isDirectory()) {
				throw new IllegalArgumentException("tag mapping file parameter points to a directory, must be a file");
			} else if (!f.canRead()) {
				throw new IllegalArgumentException(
						"tag mapping file parameter points to a file we have no read permissions");
			}

			try {
				return f.toURI().toURL();
			} catch (MalformedURLException e) {
				throw new RuntimeException(e);
			}
		} else {
			return PoiWriterTask.class.getClassLoader().getResource("poi-mapping.xml");
		}
	}
}
