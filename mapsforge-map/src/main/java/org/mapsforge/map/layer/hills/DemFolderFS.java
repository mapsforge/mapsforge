/*
 * Copyright 2022 usrusr
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

import java.io.File;
import java.io.FileFilter;
import java.util.Collections;
import java.util.Iterator;

public class DemFolderFS implements DemFolder {
    public final File file;

    public DemFolderFS(File file) {
        this.file = file;
    }

    @Override
    public Iterable<DemFolder> subs() {
        final File[] files = file.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.isDirectory();
            }
        });
        if (files == null) return Collections.emptyList();
        return new Iterable<DemFolder>() {
            @Override
            public Iterator<DemFolder> iterator() {
                return new Iterator<DemFolder>() {
                    int nextidx = 0;

                    @Override
                    public boolean hasNext() {
                        return nextidx < files.length;
                    }

                    @Override
                    public DemFolder next() {
                        DemFolderFS ret = new DemFolderFS(files[nextidx]);
                        nextidx++;
                        return ret;
                    }

                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
            }
        };
    }

    @Override
    public Iterable<DemFile> files() {
        final File[] files = file.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.isFile();
            }
        });
        if (files == null) return Collections.emptyList();
        return new Iterable<DemFile>() {
            @Override
            public Iterator<DemFile> iterator() {
                return new Iterator<DemFile>() {
                    int nextidx = 0;

                    @Override
                    public boolean hasNext() {
                        return nextidx < files.length;
                    }

                    @Override
                    public DemFile next() {
                        DemFileFS ret = new DemFileFS(files[nextidx]);
                        nextidx++;
                        return ret;
                    }

                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
            }
        };
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (!(obj instanceof DemFolderFS)) {
            return false;
        }
        return file.getAbsolutePath().equals(((DemFolderFS) obj).file.getAbsolutePath());
    }
}
