/*
 * Copyright 2021 eddiemuc
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
package org.mapsforge.map.android.rendertheme;

import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.DocumentsContract;
import org.mapsforge.core.util.IOUtils;
import org.mapsforge.map.rendertheme.XmlThemeResourceProvider;
import org.mapsforge.map.rendertheme.XmlUtils;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.*;

/**
 * An xml theme resource provider resolving resources using Android scoped storage (document framework).
 * <p>
 * Implementation note: these methods do not use DocumentFile internally,
 * but query directly for document info due to vastly better performance.
 * Also for better performance, this implementation caches resource uris.
 * <p>
 * Note: this implementation requires minimum Android 5.0 (API 21)
 */
public class ContentResolverResourceProvider implements XmlThemeResourceProvider {

    private final ContentResolver contentResolver;
    private final Uri relativeRootUri;
    private final boolean isDocumentUri;

    private final Map<String, Uri> resourceUriCache = new HashMap<>();

    private static class DocumentInfo {
        private final String name;
        private final Uri uri;
        private final boolean isDirectory;

        private DocumentInfo(String name, Uri uri, boolean isDirectory) {
            this.name = name;
            this.uri = uri;
            this.isDirectory = isDirectory;
        }
    }

    /**
     * Creates a new content resolver resource provider.
     *
     * @param contentResolver content resolver used to read content.
     * @param relativeRootUri uri pointing to a directory.
     *                        Uri is assumed to be a pure tree Uri (as e.g. returned by {@link Intent#ACTION_OPEN_DOCUMENT_TREE}).
     */
    public ContentResolverResourceProvider(ContentResolver contentResolver, Uri relativeRootUri) {
        this(contentResolver, relativeRootUri, false);
    }

    /**
     * Creates a new content resolver resource provider.
     *
     * @param contentResolver content resolver used to read content.
     * @param relativeRootUri uri pointing to a directory.
     * @param isDocumentUri   Uris as returned e.g. by {@link Intent#ACTION_OPEN_DOCUMENT_TREE}) cannot directly be used to scan directories and read content.
     *                        They must be converted to document uris first using {@link DocumentsContract#buildChildDocumentsUriUsingTree(Uri, String)}.
     *                        However, in some situations this conversion was done previously by caller (e.g. if root dir should be subdirectory of a directory returned by {@link Intent#ACTION_OPEN_DOCUMENT_TREE}).
     *                        In these cases, converted Uri will point to original root directory which is not always the wanted behaviour.
     *                        Thus, this parameter allows caller to control whether conversion should be done or not.
     *                        If set to true, then given Uri is considered to be a document uri already and no conversion is done.
     *                        If set to false, uri is considered to be a pure tree uri as returned e.g. by {@link Intent#ACTION_OPEN_DOCUMENT_TREE}) and it is converted.
     */
    public ContentResolverResourceProvider(ContentResolver contentResolver, Uri relativeRootUri, boolean isDocumentUri) {
        this.contentResolver = contentResolver;
        this.relativeRootUri = relativeRootUri;
        this.isDocumentUri = isDocumentUri;

        refreshCache();
    }

    /**
     * Build uri cache for one dir level (recursive function).
     */
    private void buildCacheLevel(String prefix, Uri dirUri) {
        List<DocumentInfo> docs = queryDir(dirUri);
        for (DocumentInfo doc : docs) {
            if (doc.isDirectory) {
                buildCacheLevel(prefix + doc.name + "/", doc.uri);
            } else {
                // Store both relative urls and absolute urls
                resourceUriCache.put(XmlUtils.PREFIX_FILE + prefix + doc.name, doc.uri);
                resourceUriCache.put(XmlUtils.PREFIX_FILE + "/" + prefix + doc.name, doc.uri);
            }
        }
    }

    @Override
    public InputStream createInputStream(String relativePath, String source) throws FileNotFoundException {
        Uri docUri = resourceUriCache.get(source);
        if (docUri != null) {
            return contentResolver.openInputStream(docUri);
        }
        return null;
    }

    /**
     * Query the content of a directory using scoped storage.
     *
     * @return a list of arrays with info [0: name (String), 1: uri (Uri), 2: isDir (boolean)]
     */
    private List<DocumentInfo> queryDir(Uri dirUri) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return Collections.emptyList();
        }
        if (dirUri == null) {
            return Collections.emptyList();
        }

        List<DocumentInfo> result = new ArrayList<>();
        Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(dirUri, DocumentsContract.getDocumentId(dirUri));

        String[] columns = new String[]{
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE
        };

        Cursor c = null;
        try {
            c = contentResolver.query(childrenUri, columns, null, null, null);

            while (c.moveToNext()) {
                String documentId = c.getString(0);
                String name = c.getString(1);
                String mimeType = c.getString(2);

                Uri uri = DocumentsContract.buildDocumentUriUsingTree(dirUri, documentId);
                boolean isDir = DocumentsContract.Document.MIME_TYPE_DIR.equals(mimeType);
                result.add(new DocumentInfo(name, uri, isDir));
            }

            return result;
        } finally {
            IOUtils.closeQuietly(c);
        }
    }

    /**
     * Refresh the uri cache by recreating it.
     */
    private void refreshCache() {
        resourceUriCache.clear();

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return;
        }
        if (relativeRootUri == null) {
            return;
        }

        Uri dirUri = relativeRootUri;
        if (!isDocumentUri) {
            // Convert "tree uri" to a "document uri"
            dirUri = DocumentsContract.buildDocumentUriUsingTree(dirUri, DocumentsContract.getTreeDocumentId(dirUri));
        }
        buildCacheLevel("", dirUri);
    }
}
