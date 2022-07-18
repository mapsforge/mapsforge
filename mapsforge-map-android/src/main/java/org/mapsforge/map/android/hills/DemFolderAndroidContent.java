package org.mapsforge.map.android.hills;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.DocumentsContract;
import org.mapsforge.core.util.IOUtils;
import org.mapsforge.map.layer.hills.DemFile;
import org.mapsforge.map.layer.hills.DemFolder;

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
        if(children==null) children = queryNested();
        return new TransformedIt<DemFolder>(children) {
            @Override boolean accept(Entry entry) {
                return entry.isDir;
            }
            @Override
            DemFolder transform(Entry entry) {
                return new DemFolderAndroidContent(entry.uri, context, contentResolver);
            }
        };
    }

    @Override
    public Iterable<DemFile> files() {
        if(children==null) children = queryNested();
        return new TransformedIt<DemFile>(children) {
            @Override boolean accept(Entry entry) {
                return ! entry.isDir;
            }
            @Override
            DemFileAndroidContent transform(Entry entry) {
                return new DemFileAndroidContent(entry, contentResolver);
            }
        };
    }

    abstract class TransformedIt<T> implements Iterable<T>{
        protected TransformedIt(Iterable<Entry> source) {
            this.source = source;
        }
        abstract T transform(Entry entry);
        abstract boolean accept(Entry entry);
        final Iterable<Entry> source;

        /** basically a guava Iterators transform(filter( implemented with lookahead */
        @Override public Iterator<T> iterator() {
            return new Iterator<T>() {
                final Iterator<Entry> it = source.iterator();
                Entry next;
                boolean endReached = false;
                {
                    returnCurrentSetNext();
                }

                Entry returnCurrentSetNext() {
                    Entry cur = next;
                    while(true) {
                        if( ! it.hasNext()) {
                            next = null;
                            endReached = true;
                            break;
                        } else {
                            next = it.next();
                            if(accept(next)) {
                                break;
                            }
                        }
                    }
                    return cur;
                }
                @Override public boolean hasNext() {
                    return ! endReached;
                }
                @Override public T next() {
                    return transform(returnCurrentSetNext());
                }
                @Override public void remove() {
                    it.remove();
                }
            };
        }
    }

    static class Entry {
        final Uri uri;
        final String name;
        final boolean isDir;
        final long size;

        private Entry(Uri uri, String name, boolean isDir, long size) {
            this.uri = uri;
            this.name = name;
            this.isDir = isDir;
            this.size = size;
        }

        @Override
        public String toString() {
            return (isDir ? "Dir":"File")+"{" +
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
        try {
            c = contentResolver.query(childrenUri, columns, null, null, null);

            List<Entry> result = new ArrayList<>();
            while (c.moveToNext()) {
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
        if(obj==null) return false;
        if( ! (obj instanceof DemFolderAndroidContent)) {
            return false;
        }
        return dirUri.equals(((DemFolderAndroidContent) obj).dirUri);
    }
}
