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
package org.mapsforge.applications.android.advancedmapviewer.filefilter;

import java.io.File;

import org.mapsforge.map.reader.MapDatabase;
import org.mapsforge.map.reader.header.FileOpenResult;

/**
 * Accepts all valid map files.
 */
public final class ValidMapFile implements ValidFileFilter {
	private FileOpenResult fileOpenResult;

	@Override
	public boolean accept(File mapFile) {
		MapDatabase mapDatabase = new MapDatabase();
		this.fileOpenResult = mapDatabase.openFile(mapFile);
		mapDatabase.closeFile();
		return this.fileOpenResult.isSuccess();
	}

	@Override
	public FileOpenResult getFileOpenResult() {
		return this.fileOpenResult;
	}
}
