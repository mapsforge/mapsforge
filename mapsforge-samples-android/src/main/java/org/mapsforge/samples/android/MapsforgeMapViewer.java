/*
 * Copyright 2018-2021 devemux86
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
package org.mapsforge.samples.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.view.Menu;
import android.view.MenuItem;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.android.rendertheme.ContentRenderTheme;
import org.mapsforge.map.android.rendertheme.ContentResolverResourceProvider;
import org.mapsforge.map.android.util.AndroidUtil;
import org.mapsforge.map.android.view.MapView;
import org.mapsforge.map.datastore.MapDataStore;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.layer.renderer.TileRendererLayer;
import org.mapsforge.map.reader.MapFile;
import org.mapsforge.map.rendertheme.XmlRenderTheme;
import org.mapsforge.map.rendertheme.ZipRenderTheme;
import org.mapsforge.map.rendertheme.ZipXmlThemeResourceProvider;
import org.mapsforge.map.rendertheme.internal.MapsforgeThemes;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.zip.ZipInputStream;

/**
 * Android app example.
 */
public class MapsforgeMapViewer extends Activity {

    private static final int SELECT_MAP_FILE = 0;
    private static final int SELECT_THEME_ARCHIVE = 1;
    private static final int SELECT_THEME_DIR = 2;
    private static final int SELECT_THEME_FILE = 3;

    private MapView mapView;
    private Menu menu;
    private TileRendererLayer tileRendererLayer;
    private Uri mapFileUri, themeDirUri;

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
        int itemId = item.getItemId();
        if (itemId == R.id.theme_default) {
            loadTheme(MapsforgeThemes.DEFAULT);
            item.setChecked(true);
            return true;
        } else if (itemId == R.id.theme_osmarender) {
            loadTheme(MapsforgeThemes.OSMARENDER);
            item.setChecked(true);
            return true;
        } else if (itemId == R.id.theme_external_archive) {
            Intent intent = new Intent(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT ? Intent.ACTION_OPEN_DOCUMENT : Intent.ACTION_GET_CONTENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            startActivityForResult(intent, SELECT_THEME_ARCHIVE);
            return true;
        } else if (itemId == R.id.theme_external) {
            Intent intent;
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
                return false;
            intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivityForResult(intent, SELECT_THEME_DIR);
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
                mapFileUri = data.getData();

                mapView.getMapScaleBar().setVisible(true);
                mapView.setBuiltInZoomControls(true);

                TileCache tileCache = AndroidUtil.createTileCache(this, "mapcache",
                        mapView.getModel().displayModel.getTileSize(), 1f,
                        mapView.getModel().frameBufferModel.getOverdrawFactor());

                FileInputStream fis = (FileInputStream) getContentResolver().openInputStream(mapFileUri);
                MapDataStore mapDataStore = new MapFile(fis);
                tileRendererLayer = new TileRendererLayer(tileCache, mapDataStore,
                        mapView.getModel().mapViewPosition, AndroidGraphicFactory.INSTANCE);
                tileRendererLayer.setXmlRenderTheme(MapsforgeThemes.DEFAULT);

                mapView.getLayerManager().getLayers().add(tileRendererLayer);

                mapView.setCenter(mapDataStore.startPosition());
                mapView.setZoomLevel(mapDataStore.startZoomLevel());
            } catch (Exception e) {
                e.printStackTrace();
                finish();
            }
        } else if (requestCode == SELECT_THEME_ARCHIVE) {
            if (resultCode != Activity.RESULT_OK || data == null)
                return;

            try {
                final Uri uri = data.getData();

                final List<String> xmlThemes = ZipXmlThemeResourceProvider.scanXmlThemes(new ZipInputStream(new BufferedInputStream(getContentResolver().openInputStream(uri))));
                if (xmlThemes.isEmpty())
                    return;

                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.dialog_theme_title);
                builder.setSingleChoiceItems(xmlThemes.toArray(new String[0]), -1, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            dialog.dismiss();
                            XmlRenderTheme theme = new ZipRenderTheme(xmlThemes.get(which), new ZipXmlThemeResourceProvider(new ZipInputStream(new BufferedInputStream(getContentResolver().openInputStream(uri)))));
                            loadTheme(theme);
                            menu.findItem(R.id.theme_external_archive).setChecked(true);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });
                builder.show();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (requestCode == SELECT_THEME_DIR) {
            if (resultCode != Activity.RESULT_OK || data == null)
                return;

            themeDirUri = data.getData();

            // Now we have the directory for resources, but we need to let the user also select a theme file
            Intent intent = new Intent(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT ? Intent.ACTION_OPEN_DOCUMENT : Intent.ACTION_GET_CONTENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, themeDirUri);
            startActivityForResult(intent, SELECT_THEME_FILE);
        } else if (requestCode == SELECT_THEME_FILE) {
            if (resultCode != Activity.RESULT_OK || data == null)
                return;

            Uri uri = data.getData();
            XmlRenderTheme theme = new ContentRenderTheme(getContentResolver(), uri);
            theme.setResourceProvider(new ContentResolverResourceProvider(getContentResolver(), themeDirUri));

            loadTheme(theme);
            menu.findItem(R.id.theme_external).setChecked(true);
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

            FileInputStream fis = (FileInputStream) getContentResolver().openInputStream(mapFileUri);
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
