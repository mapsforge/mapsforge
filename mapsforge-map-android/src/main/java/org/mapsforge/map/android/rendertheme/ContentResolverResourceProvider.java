package org.mapsforge.map.android.rendertheme;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;

import org.mapsforge.map.rendertheme.XmlThemeResourceProvider;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mapsforge.core.util.IOUtils.closeQuietly;

/**
 * An Xml Theme Resource Provider resolving resources using Android Scoped Storage (Document Framework)
 *
 * Implementation note: this methods does not use DocumentFile internally but rather queries
 * for document information directly due to vastly better performance.
 * Also for better performance, this implementation caches resource Uris
 *
 * Note: this implementation requires miminum Api level 21 / LOLLIPOP
 */
public class ContentResolverResourceProvider implements XmlThemeResourceProvider {

    private static final String PREFIX_FILE = "file:";

    private final ContentResolver contentResolver;
    private final Uri relativeRootUri;

    private final Map<String, Uri> resourceUriCache = new HashMap<>();

    private static class DocumentInfo {
        public final String name;
        public final Uri uri;
        public final boolean isDirectory;

        public DocumentInfo(String name, Uri uri, boolean isDirectory) {
            this.name = name;
            this.uri = uri;
            this.isDirectory = isDirectory;
        }
    }

    public ContentResolverResourceProvider(ContentResolver contentResolver, Uri treeUri) {
        this.contentResolver = contentResolver;
        this.relativeRootUri = treeUri;
        refreshCache();

    }

    @Override
    public InputStream createInputStream(String source) throws FileNotFoundException {
        Uri docUri = this.resourceUriCache.get(source);
        if (docUri != null) {
            return contentResolver.openInputStream(docUri);
        }
        return null;
    }

    /** refreshes the uri cache by recreating it */
    private void refreshCache() {
        this.resourceUriCache.clear();
        final Uri dirUri;
        if (this.relativeRootUri != null && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            dirUri = DocumentsContract.buildDocumentUriUsingTree(this.relativeRootUri, DocumentsContract.getTreeDocumentId(this.relativeRootUri));
            buildCacheLevel(PREFIX_FILE, dirUri);
        }
    }

    /** builds uri cache foir one dir level (recursive function) */
    private void buildCacheLevel(String prefix, Uri dirUri) {
        List<DocumentInfo> docs = queryDir(dirUri);
        for(DocumentInfo doc : docs) {
            if (doc.isDirectory) {
                buildCacheLevel(prefix + doc.name + "/" , doc.uri);
            } else {
                this.resourceUriCache.put(prefix + doc.name, doc.uri);
            }
        }
    }

    /**
     * queries the content of a directory using scoped storage
     * Returns a list of arrays with infos 0:name(String), 1:uri(Uri), 2:isDir(boolean)
     */
    private List<DocumentInfo> queryDir(final Uri dirUri) {

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            final String[] columns = new String[]{
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                    DocumentsContract.Document.COLUMN_MIME_TYPE
            };

            if (dirUri == null) {
                return Collections.emptyList();
            }

            final List<DocumentInfo> result = new ArrayList<>();
            final Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(dirUri, DocumentsContract.getDocumentId(dirUri));

            Cursor c = null;
            try {
                c = contentResolver.query(childrenUri, columns, null, null, null);
                while (c.moveToNext()) {
                    final String documentId = c.getString(0);
                    final String name = c.getString(1);
                    final String mimeType = c.getString(2);
                    final boolean isDir = DocumentsContract.Document.MIME_TYPE_DIR.equals(mimeType);
                    final Uri uri = DocumentsContract.buildDocumentUriUsingTree(dirUri, documentId);
                    result.add(new DocumentInfo(name, uri, isDir));
                }
                return result;
            } finally {
                closeQuietly(c);
            }
        }

        return Collections.emptyList();
    }

}
