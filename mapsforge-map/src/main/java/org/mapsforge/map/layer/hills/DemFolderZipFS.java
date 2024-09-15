/*
 * Copyright 2024 Sublimis
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
package org.mapsforge.map.layer.hills;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class DemFolderZipFS implements DemFolder {
    protected final ZipFile zipFile;

    public DemFolderZipFS(ZipFile zipFile) {
        this.zipFile = zipFile;
    }

    @Override
    public Iterable<DemFolder> subs() {
        return Collections.emptyList();
    }

    @Override
    public Iterable<DemFile> files() {
        final Enumeration<? extends ZipEntry> entries = zipFile.entries();

        final List<DemFile> items = new ArrayList<>();

        ZipEntry zipEntry;

        while (entries.hasMoreElements() && (zipEntry = entries.nextElement()) != null) {
            if (false == zipEntry.isDirectory()) {
                items.add(new DemFileZipEntryFS(zipFile, zipEntry));
            }
        }

        return items;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (!(obj instanceof DemFolderZipFS)) {
            return false;
        }
        return zipFile.equals(((DemFolderZipFS) obj).zipFile);
    }
}
