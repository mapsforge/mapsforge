/*
 * Copyright 2018-2021 devemux86
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
package org.mapsforge.samples.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.provider.OpenableColumns;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.android.rendertheme.ContentRenderTheme;
import org.mapsforge.map.android.rendertheme.ContentResolverResourceProvider;
import org.mapsforge.map.android.util.AndroidUtil;
import org.mapsforge.map.android.view.MapView;
import org.mapsforge.map.datastore.MapDataStore;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.layer.renderer.TileRendererLayer;
import org.mapsforge.map.reader.MapFile;
import org.mapsforge.map.rendertheme.InternalRenderTheme;
import org.mapsforge.map.rendertheme.StreamRenderTheme;
import org.mapsforge.map.rendertheme.XmlRenderTheme;
import org.mapsforge.map.rendertheme.XmlThemeResourceProvider;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.mapsforge.core.util.IOUtils.closeQuietly;

/**
 * Android app example.
 */
public class MapsforgeMapViewer extends Activity {

    private static final int SELECT_MAP_FILE = 0;
    private static final int SELECT_THEME_FILE = 1;
    private static final int SELECT_THEMES_DIRECTORY = 2;
    private static final int SELECT_THEMES_DIRECTORY_FILE = 3;
    private static final int SELECT_THEME_FILE_CUSTOMRESOURCE = 4;

    private MapView mapView;
    private Menu menu;
    private TileRendererLayer tileRendererLayer;
    private Uri uri;

    private Uri directoryUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AndroidGraphicFactory.createInstance(getApplication());

        mapView = new MapView(this);
        setContentView(mapView);

        Intent intent = new Intent(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT ? Intent.ACTION_OPEN_DOCUMENT : Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        startActivityForResult(intent, SELECT_MAP_FILE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.theme_menu, menu);
        this.menu = menu;
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent = null;
        final int itemId = item.getItemId();
        if (itemId == R.id.theme_default) {
            loadTheme(InternalRenderTheme.DEFAULT);
            item.setChecked(true);
            return true;
        } else if (itemId == R.id.theme_osmarender) {
            loadTheme(InternalRenderTheme.OSMARENDER);
            item.setChecked(true);
            return true;
        } else if (itemId == R.id.theme_external_no_resources) {
            intent = new Intent(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT ? Intent.ACTION_OPEN_DOCUMENT : Intent.ACTION_GET_CONTENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            startActivityForResult(intent, SELECT_THEME_FILE);
            return true;
        } else if (itemId == R.id.theme_external_with_resources && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivityForResult(intent, SELECT_THEMES_DIRECTORY);
            return true;
        } else if (itemId == R.id.theme_external_custom_resources) {
                intent = new Intent(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT ? Intent.ACTION_OPEN_DOCUMENT : Intent.ACTION_GET_CONTENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*");
                startActivityForResult(intent, SELECT_THEME_FILE_CUSTOMRESOURCE);
                return true;
        }
        return false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == SELECT_MAP_FILE) {
            if (resultCode != Activity.RESULT_OK || data == null) {
                finish();
                return;
            }

            try {
                uri = data.getData();

                mapView.getMapScaleBar().setVisible(true);
                mapView.setBuiltInZoomControls(true);

                TileCache tileCache = AndroidUtil.createTileCache(this, "mapcache",
                        mapView.getModel().displayModel.getTileSize(), 1f,
                        mapView.getModel().frameBufferModel.getOverdrawFactor());

                FileInputStream fis = (FileInputStream) getContentResolver().openInputStream(uri);
                MapDataStore mapDataStore = new MapFile(fis);
                tileRendererLayer = new TileRendererLayer(tileCache, mapDataStore,
                        mapView.getModel().mapViewPosition, AndroidGraphicFactory.INSTANCE);
                tileRendererLayer.setXmlRenderTheme(InternalRenderTheme.DEFAULT);

                mapView.getLayerManager().getLayers().add(tileRendererLayer);

                mapView.setCenter(mapDataStore.startPosition());
                mapView.setZoomLevel(mapDataStore.startZoomLevel());
            } catch (Exception e) {
                e.printStackTrace();
                finish();
            }
        } else if (requestCode == SELECT_THEME_FILE) {
            if (resultCode != Activity.RESULT_OK || data == null)
                return;

            Uri uri = data.getData();
            XmlRenderTheme theme = new ContentRenderTheme(getContentResolver(), "", uri);

            loadTheme(theme);
            menu.findItem(R.id.theme_external_no_resources).setChecked(true);
        } else if (requestCode == SELECT_THEMES_DIRECTORY) {
            if (resultCode != Activity.RESULT_OK || data == null)
                return;

            this.directoryUri = data.getData();

            //Now we have the directory for resouces, but we need to let the user also select a theme file
            Intent intent = new Intent(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT ? Intent.ACTION_OPEN_DOCUMENT : Intent.ACTION_GET_CONTENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, this.directoryUri);
            startActivityForResult(intent, SELECT_THEMES_DIRECTORY_FILE);


        } else if (requestCode == SELECT_THEMES_DIRECTORY_FILE) {
            if (resultCode != Activity.RESULT_OK || data == null)
                return;

            Uri uri = data.getData();

            XmlRenderTheme theme = new ContentRenderTheme(getContentResolver(), "", uri, this.directoryUri);

            loadTheme(theme);
            menu.findItem(R.id.theme_external_with_resources).setChecked(true);
        } else if (requestCode == SELECT_THEME_FILE_CUSTOMRESOURCE) {
            if (resultCode != Activity.RESULT_OK || data == null)
                return;

            Uri uri = data.getData();
            try {
                XmlRenderTheme theme = new StreamRenderTheme("", getContentResolver().openInputStream(uri), null, new XmlThemeResourceProvider() {
                    @Override
                    public InputStream createInputStream(String source) {
                        //just an example: deliver a "blue star" svg as icon for everything
                        if (source.endsWith(".svg")) {
                            return getResources().openRawResource(R.raw.blue_star);
                        }
                        return null;
                    }
                });
                loadTheme(theme);
            } catch (FileNotFoundException fnfe) {
                //ignore in this example
            }

            menu.findItem(R.id.theme_external_custom_resources).setChecked(true);
        }
    }

    @Override
    protected void onDestroy() {
        mapView.destroyAll();
        AndroidGraphicFactory.clearResourceMemoryCache();
        super.onDestroy();
    }

    private void loadTheme(final XmlRenderTheme theme) {
        try {
            mapView.getLayerManager().getLayers().remove(tileRendererLayer);
            tileRendererLayer.onDestroy();
            tileRendererLayer.getTileCache().purge();

            TileCache tileCache = AndroidUtil.createTileCache(this, "mapcache",
                    mapView.getModel().displayModel.getTileSize(), 1f,
                    mapView.getModel().frameBufferModel.getOverdrawFactor());

            FileInputStream fis = (FileInputStream) getContentResolver().openInputStream(uri);
            MapDataStore mapDataStore = new MapFile(fis);
            tileRendererLayer = new TileRendererLayer(tileCache, mapDataStore,
                    mapView.getModel().mapViewPosition, AndroidGraphicFactory.INSTANCE);
            tileRendererLayer.setXmlRenderTheme(theme);

            mapView.getLayerManager().getLayers().add(tileRendererLayer);
            mapView.getLayerManager().redrawLayers();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
