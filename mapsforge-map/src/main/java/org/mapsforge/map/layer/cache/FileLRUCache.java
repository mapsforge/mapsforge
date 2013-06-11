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
package org.mapsforge.map.layer.cache;

import java.io.File;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.mapsforge.core.util.LRUCache;

class FileLRUCache<T> extends LRUCache<T, File> {
	private static final Logger LOGGER = Logger.getLogger(FileLRUCache.class.getName());
	private static final long serialVersionUID = 1L;

	FileLRUCache(int capacity) {
		super(capacity);
	}

	@Override
	protected boolean removeEldestEntry(Map.Entry<T, File> eldest) {
		if (size() > this.capacity) {
			remove(eldest.getKey());
			File file = eldest.getValue();
			if (file.exists() && !file.delete()) {
				LOGGER.log(Level.SEVERE, "could not delete file: " + file);
			}
		}

		return false;
	}
}
