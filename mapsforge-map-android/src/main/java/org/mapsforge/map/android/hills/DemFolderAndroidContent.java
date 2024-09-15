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
package org.mapsforge.map.android.hills;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;

import org.mapsforge.core.util.IOUtils;
import org.mapsforge.map.layer.hills.DemFile;
import org.mapsforge.map.layer.hills.DemFolder;
import org.mapsforge.map.layer.hills.HgtCache;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class DemFolderAndroidContent implements DemFolder {

    final private Uri dirUri;
    private final Context context;
    final private ContentResolver contentResolver;

    public DemFolderAndroidContent(Uri dirUri, Context context, ContentResolver contentResolver) {
        this.dirUri = dirUri;
        this.context = context;
        this.contentResolver = contentResolver;
    }

    private List<Entry> children = null;

    @Override
    public Iterable<DemFolder> subs() {
        if (children == null) children = queryNested();
        return new TransformedIt<DemFolder>(children) {
            @Override
            boolean accept(Entry entry) {
                return entry.isDir || HgtCache.isFileNameZip(entry.name);
            }

            @Override
            DemFolder transform(Entry entry) {
                if (HgtCache.isFileNameZip(entry.name)) {
                    return new DemFolderZipAndroidContent(entry, contentResolver);
                } else {
                    return new DemFolderAndroidContent(entry.uri, context, contentResolver);
                }
            }
        };
    }

    @Override
    public Iterable<DemFile> files() {
        if (children == null) children = queryNested();
        return new TransformedIt<DemFile>(children) {
            @Override
            boolean accept(Entry entry) {
                return HgtCache.isFileNameHgt(entry.name) && false == (entry.isDir || HgtCache.isFileNameZip(entry.name));
            }

            @Override
            DemFileAndroidContent transform(Entry entry) {
                return new DemFileAndroidContent(entry, contentResolver);
            }
        };
    }

    abstract static class TransformedIt<T> implements Iterable<T> {
        protected TransformedIt(Iterable<Entry> source) {
            this.source = source;
        }

        abstract T transform(Entry entry);

        abstract boolean accept(Entry entry);

        final Iterable<Entry> source;

        /**
         * basically a guava Iterators transform(filter( implemented with lookahead
         */
        @Override
        public Iterator<T> iterator() {
            return new Iterator<T>() {
                final Iterator<Entry> it = source.iterator();
                Entry next;
                boolean endReached = false;

                {
                    returnCurrentSetNext();
                }

                Entry returnCurrentSetNext() {
                    Entry cur = next;
                    while (true) {
                        if (!it.hasNext()) {
                            next = null;
                            endReached = true;
                            break;
                        } else {
                            next = it.next();
                            if (accept(next)) {
                                break;
                            }
                        }
                    }
                    return cur;
                }

                @Override
                public boolean hasNext() {
                    return !endReached;
                }

                @Override
                public T next() {
                    return transform(returnCurrentSetNext());
                }

                @Override
                public void remove() {
                    it.remove();
                }
            };
        }
    }

    public static class Entry {
        final Uri uri;
        final String name;
        final boolean isDir;
        final long size;

        public Entry(Uri uri, String name, boolean isDir, long size) {
            this.uri = uri;
            this.name = name;
            this.isDir = isDir;
            this.size = size;
        }

        @Override
        public String toString() {
            return (isDir ? "Dir" : "File") + "{" +
                    "name='" + name + '\'' +
                    "uri=" + uri +
                    '}';
        }
    }

    private List<Entry> queryNested() {
        if (dirUri == null) {
            return Collections.emptyList();
        }

        String dirDocId = DocumentsContract.getTreeDocumentId(dirUri);
        if (DocumentsContract.isDocumentUri(context, dirUri)) {
            dirDocId = DocumentsContract.getDocumentId(dirUri);
        }

        Uri parentUri = DocumentsContract.buildDocumentUriUsingTree(dirUri, dirDocId);
        Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(parentUri, dirDocId);


        String[] columns = new String[]{
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
                DocumentsContract.Document.COLUMN_SIZE,
        };

        Cursor c = null;
        List<Entry> result = new ArrayList<>();
        try {
            c = contentResolver.query(childrenUri, columns, null, null, null);

            while (c != null && c.moveToNext()) {
                String fileDocId = c.getString(0);
                String name = c.getString(1);
                String mimeType = c.getString(2);
                long size = c.getLong(3);

                Uri uri = DocumentsContract.buildDocumentUriUsingTree(dirUri, fileDocId);
                boolean isDir = DocumentsContract.Document.MIME_TYPE_DIR.equals(mimeType);
                result.add(new Entry(uri, name, isDir, size));
            }

            return result;
        } finally {
            IOUtils.closeQuietly(c);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (!(obj instanceof DemFolderAndroidContent)) {
            return false;
        }
        return dirUri.equals(((DemFolderAndroidContent) obj).dirUri);
    }
}
