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
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.android.rendertheme.ContentRenderTheme;
import org.mapsforge.map.android.util.AndroidUtil;
import org.mapsforge.map.android.view.MapView;
import org.mapsforge.map.datastore.MapDataStore;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.layer.renderer.TileRendererLayer;
import org.mapsforge.map.reader.MapFile;
import org.mapsforge.map.rendertheme.InternalRenderTheme;
import org.mapsforge.map.rendertheme.XmlRenderTheme;

import java.io.FileInputStream;

/**
 * Android app example.
 */
public class MapsforgeMapViewer extends Activity {

    private static final int SELECT_MAP_FILE = 0;
    private static final int SELECT_THEME_FILE = 1;

    private MapView mapView;
    private Menu menu;
    private TileRendererLayer tileRendererLayer;
    private Uri uri;

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
        switch (item.getItemId()) {
            case R.id.theme_default:
                loadTheme(InternalRenderTheme.DEFAULT);
                item.setChecked(true);
                return true;
            case R.id.theme_osmarender:
                loadTheme(InternalRenderTheme.OSMARENDER);
                item.setChecked(true);
                return true;
            case R.id.theme_external:
                Intent intent = new Intent(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT ? Intent.ACTION_OPEN_DOCUMENT : Intent.ACTION_GET_CONTENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*");
                startActivityForResult(intent, SELECT_THEME_FILE);
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
