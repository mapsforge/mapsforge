/*
 * Copyright 2010, 2011 mapsforge.org
 * Copyright 2010, 2011 Karsten Groll
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
import org.openstreetmap.osmosis.core.pipeline.common.TaskConfiguration;
import org.openstreetmap.osmosis.core.pipeline.common.TaskManager;
import org.openstreetmap.osmosis.core.pipeline.common.TaskManagerFactory;
import org.openstreetmap.osmosis.core.pipeline.v0_6.SinkManager;
import org.openstreetmap.osmosis.core.task.v0_6.Sink;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * This class is used for creating {@link POIWriterTask}s.
 */
public class POIWriterFactory extends TaskManagerFactory {
	private static final String GUI_PROGRESS_MANAGER_CLASS_NAME = "org.mapsforge.mapmaker.gui.ProgressGUI";

	/**
	 * Default constructor.
	 */
	public POIWriterFactory() {
	}

	@Override
	protected TaskManager createTaskManagerImpl(TaskConfiguration taskConfig) {
		// Output file
		String outputFilePath = getDefaultStringArgument(taskConfig, System.getProperty("user.home") + "/map.pbf");

		// XML-file containing a POI category configuration
		String categoryConfigFilePath = getStringArgument(taskConfig, "categoryConfigPath", "POICategoriesOsmosis.xml");

		// If set to true, progress messages will forwarded to a GUI message handler
		boolean guiMode = getBooleanArgument(taskConfig, "gui-mode", false);

		ProgressManager progressManager = new DummyProgressManager();

		// Use graphical progress manager if plugin is called from map maker GUI
		if (guiMode) {
			try {
				Class<?> clazz = Class.forName(GUI_PROGRESS_MANAGER_CLASS_NAME);
				Method method = clazz.getMethod("getInstance", new Class[0]);
				Object o = method.invoke(clazz, new Object[0]);

				System.out.println("Progress manager (plugin): " + o);
				progressManager = (ProgressManager) o;
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} catch (SecurityException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (NoSuchMethodException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		// Tell the logger which progress manager to use
		LoggerWrapper.setDefaultProgressManager(progressManager);

		// The creation task
		Sink task = new POIWriterTask(outputFilePath, categoryConfigFilePath, progressManager);

		return new SinkManager(taskConfig.getId(), task, taskConfig.getPipeArgs());
	}
}
