/*
 * Copyright 2010, 2011 mapsforge.org
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

import org.openstreetmap.osmosis.core.pipeline.common.TaskManagerFactory;
import org.openstreetmap.osmosis.core.plugin.PluginLoader;

import java.util.HashMap;
import java.util.Map;

/**
 * The Osmosis PluginLoader for the poi-writer osmosis plugin.
 */
public class PoiWriterPluginLoader implements PluginLoader {
	@Override
	public Map<String, TaskManagerFactory> loadTaskFactories() {
		PoiWriterFactory poiWriterFactory = new PoiWriterFactory();
		HashMap<String, TaskManagerFactory> map = new HashMap<>();
		map.put("poi-writer", poiWriterFactory);
		map.put("pw", poiWriterFactory);
		return map;
	}
}
