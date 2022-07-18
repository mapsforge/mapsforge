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
        if(obj==null) return false;
        if( ! (obj instanceof DemFolderFS)) {
            return false;
        }
        return file.getAbsolutePath().equals(((DemFolderFS) obj).file.getAbsolutePath());
    }
}
