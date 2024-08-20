/*
 * Copyright 2022 usrusr
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

import org.mapsforge.core.util.IOUtils;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class DemFileFS implements DemFile {
    private static final Logger LOGGER = Logger.getLogger(AbsShadingAlgorithmDefaults.class.getName());


    final protected File file;

    public DemFileFS(File file) {
        this.file = file;
    }

    @Override
    public String getName() {
        return file.getName();
    }

    @Override
    public InputStream openInputStream() throws FileNotFoundException {
        return new BufferedInputStream(new FileInputStream(file));
    }

    @Override
    public long getSize() {
        return file.length();
    }


    public static InputStream tryZippedSingleHgt(String name, InputStream inputStream) throws IOException {
        ZipInputStream zipInputStream = null;
        try {
            zipInputStream = new ZipInputStream(inputStream);
            ZipEntry entry;
            String nameLowerCase = name.toLowerCase();
            String expectedNameLC = nameLowerCase.substring(0, nameLowerCase.length() - 4) + ".hgt";
            while (null != (entry = zipInputStream.getNextEntry())) {
                if (!expectedNameLC.equals(entry.getName().toLowerCase())) continue;

                int todo = (int) entry.getSize();
                return streamReadPart(name, zipInputStream, todo);
            }
        } finally {
            IOUtils.closeQuietly(zipInputStream);
        }
        return null;
    }

    /**
     * does *not* close the stream!
     */
    public static InputStream streamReadPart(String name, InputStream stream, int todo) throws IOException {
        final byte[] bytes = new byte[todo];

        int done = 0;
        while (todo > 0) {
            int read = stream.read(bytes, done, todo);
            if (read == 0) {
                LOGGER.log(Level.SEVERE, "failed to read entire .hgt in " + name + " " + done + " of " + todo + " done");
                return null;
            }
            done += read;
            todo -= read;
        }

        return new ByteArrayInputStream(bytes);
    }

    @Override
    public InputStream asStream() throws IOException {
        String nameLowerCase = file.getName().toLowerCase();
        if (nameLowerCase.endsWith(".zip")) {
            return tryZippedSingleHgt(file.getName(), new FileInputStream(file));
        } else {
            return openInputStream();
        }
    }
}
