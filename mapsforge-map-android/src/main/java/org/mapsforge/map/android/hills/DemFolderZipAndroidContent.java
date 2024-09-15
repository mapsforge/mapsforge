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
package org.mapsforge.map.android.hills;

import android.content.ContentResolver;

import org.mapsforge.map.layer.hills.DemFile;
import org.mapsforge.map.layer.hills.DemFolder;

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * <em>WARNING: The performance of this can be very poor. Use {@link org.mapsforge.map.layer.hills.DemFolderZipFS} instead if you can.</em>
 */
public class DemFolderZipAndroidContent implements DemFolder {

    protected final DemFolderAndroidContent.Entry contentResolverEntry;
    protected final ContentResolver contentResolver;

    /**
     * <em>WARNING: The performance of this can be very poor. Use {@link org.mapsforge.map.layer.hills.DemFolderZipFS} instead if you can.</em>
     */
    public DemFolderZipAndroidContent(DemFolderAndroidContent.Entry contentResolverEntry, ContentResolver contentResolver) {
        this.contentResolverEntry = contentResolverEntry;
        this.contentResolver = contentResolver;
    }

    @Override
    public Iterable<DemFolder> subs() {
        return Collections.emptyList();
    }

    @Override
    public Iterable<DemFile> files() {
        final List<DemFile> items = new ArrayList<>();

        ZipInputStream zipInputStream = null;
        try {
            zipInputStream = new ZipInputStream(new BufferedInputStream(contentResolver.openInputStream(contentResolverEntry.uri)));
        } catch (FileNotFoundException e) {
            LOGGER.log(Level.WARNING, e.toString());
        }

        final List<ZipEntry> zipEntries = listZipInputStreamFiles(zipInputStream);

        for (ZipEntry zipEntry : zipEntries) {
            items.add(new DemFileZipEntryAndroidContent(zipEntry, contentResolverEntry, contentResolver));
        }

        return items;
    }

    public static ZipEntry getZipEntryFile(final ZipInputStream zipInputStream, final String zipEntryName) throws IOException {
        ZipEntry output = null;

        if (zipInputStream != null && zipEntryName != null) {
            ZipEntry zipEntryTmp = null;

            while ((zipEntryTmp = zipInputStream.getNextEntry()) != null) {
                if (false == zipEntryTmp.isDirectory()) {
                    if (zipEntryName.equals(zipEntryTmp.getName())) {
                        output = zipEntryTmp;
                        break;
                    }
                }
            }
        }

        return output;
    }

    public static List<ZipEntry> listZipInputStreamFiles(final ZipInputStream zipInputStream) {
        final List<ZipEntry> output = new ArrayList<>();

        if (zipInputStream != null) {
            ZipEntry zipEntryTmp = null;

            while (true) {
                try {
                    if ((zipEntryTmp = zipInputStream.getNextEntry()) == null) break;
                } catch (IOException e) {
                    LOGGER.log(Level.WARNING, e.toString());
                    break;
                }
                if (false == zipEntryTmp.isDirectory()) {
                    output.add(zipEntryTmp);
                }
            }
        }

        return output;
    }

    /**
     * @return <code>null</code> if entry not found; <code>zipInputStream</code> otherwise
     */
    public static ZipInputStream positionZipInputStreamToEntry(final ZipInputStream zipInputStream, final String zipEntryName) {
        ZipInputStream output = null;

        if (zipInputStream != null && zipEntryName != null) {
            ZipEntry zipEntryTmp = null;

            while (true) {
                try {
                    if ((zipEntryTmp = zipInputStream.getNextEntry()) == null) break;
                } catch (IOException e) {
                    LOGGER.log(Level.WARNING, e.toString());
                    break;
                }
                if (false == zipEntryTmp.isDirectory()) {
                    if (zipEntryName.equals(zipEntryTmp.getName())) {
                        output = zipInputStream;
                        break;
                    }
                }
            }
        }

        return output;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (!(obj instanceof DemFolderZipAndroidContent)) {
            return false;
        }
        return contentResolverEntry.uri.equals(((DemFolderZipAndroidContent) obj).contentResolverEntry.uri);
    }
}
