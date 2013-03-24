package org.mapsforge.rewrite;

import java.io.File;
import java.util.List;

import org.mapsforge.core.graphics.Bitmap;
import org.mapsforge.core.graphics.Color;
import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.graphics.Style;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.map.android.AndroidPreferences;
import org.mapsforge.map.android.graphics.AndroidGraphics;
import org.mapsforge.map.android.view.MapView;
import org.mapsforge.map.layer.Layer;
import org.mapsforge.map.layer.LayerManager;
import org.mapsforge.map.layer.cache.FileSystemTileCache;
import org.mapsforge.map.layer.cache.InMemoryTileCache;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.layer.cache.TwoLevelTileCache;
import org.mapsforge.map.layer.overlay.Circle;
import org.mapsforge.map.layer.overlay.Marker;
import org.mapsforge.map.layer.overlay.Polyline;
import org.mapsforge.map.layer.renderer.TileRendererLayer;
import org.mapsforge.map.model.MapViewPosition;
import org.mapsforge.map.model.Model;
import org.mapsforge.map.model.common.PreferencesFacade;
import org.mapsforge.map.rendertheme.InternalRenderTheme;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.preference.PreferenceManager;

public class Rewrite extends Activity {
	private static final GraphicFactory GRAPHIC_FACTORY = AndroidGraphics.INSTANCE;

	private static Paint createPaint(int color, int strokeWidth, Style style) {
		Paint paint = AndroidGraphics.INSTANCE.createPaint();
		paint.setColor(color);
		paint.setStrokeWidth(strokeWidth);
		paint.setStyle(style);
		return paint;
	}

	private static Layer createTileRendererLayer(TileCache tileCache, MapViewPosition mapViewPosition,
			LayerManager layerManager) {
		TileRendererLayer tileRendererLayer = new TileRendererLayer(tileCache, mapViewPosition, layerManager,
				GRAPHIC_FACTORY);
		tileRendererLayer.setMapFile(new File(Environment.getExternalStorageDirectory(), "germany.map"));
		tileRendererLayer.setXmlRenderTheme(InternalRenderTheme.OSMARENDER);
		tileRendererLayer.setTextScale(1.5f);
		return tileRendererLayer;
	}

	private MapView mapView;
	private final Model model = new Model();
	private PreferencesFacade preferencesFacade;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().detectAll().penaltyLog().build());
		StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder().detectAll().penaltyLog().build());
		super.onCreate(savedInstanceState);

		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		this.preferencesFacade = new AndroidPreferences(sharedPreferences);
		this.model.init(this.preferencesFacade);
		this.mapView = new MapView(this, this.model);
		this.mapView.setClickable(true);
		this.mapView.getFpsCounter().setVisible(true);
		this.mapView.getMapScaleBar().setVisible(true);

		LayerManager layerManager = this.mapView.getLayerManager();
		List<Layer> layers = layerManager.getLayers();

		TileCache tileCache = createTileCache();
		MapViewPosition mapViewPosition = this.model.mapViewPosition;

		layers.add(createTileRendererLayer(tileCache, mapViewPosition, layerManager));

		// layers.add(new TileDownloadLayer(tileCache, mapViewPosition, OpenCycleMap.INSTANCE, layerManager,
		// GRAPHIC_FACTORY));
		// layers.add(new TileDownloadLayer(tileCache, mapViewPosition, OpenStreetMapMapnik.INSTANCE, layerManager,
		// GRAPHIC_FACTORY));
		// layers.add(new TileGridLayer(GRAPHIC_FACTORY));
		// layers.add(new TileCoordinatesLayer(GRAPHIC_FACTORY));
		// addOverlayLayers(layers);

		setContentView(this.mapView);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		this.mapView.destroy();
	}

	@Override
	protected void onPause() {
		super.onPause();

		this.model.save(this.preferencesFacade);
		this.preferencesFacade.save();
	}

	private void addOverlayLayers(List<Layer> layers) {
		LatLong latLong1 = new LatLong(0, 0);
		LatLong latLong2 = new LatLong(52, 13);
		LatLong latLong3 = new LatLong(10, 10);

		Polyline polyline = new Polyline(createPaint(AndroidGraphics.INSTANCE.createColor(Color.BLUE), 8, Style.STROKE));
		List<LatLong> latLongs = polyline.getLatLongs();
		latLongs.add(latLong1);
		latLongs.add(latLong2);
		latLongs.add(latLong3);

		Marker marker1 = createMarker(R.drawable.marker_red, latLong1);

		Circle circle = new Circle(latLong3, 30000, createPaint(AndroidGraphics.INSTANCE.createColor(Color.WHITE), 0,
				Style.FILL), null);

		layers.add(polyline);
		layers.add(circle);
		layers.add(marker1);
	}

	private Marker createMarker(int resourceIdentifier, LatLong latLong) {
		Drawable drawable = getResources().getDrawable(resourceIdentifier);
		Bitmap bitmap = AndroidGraphics.convertToBitmap(drawable);
		return new Marker(latLong, bitmap, 0, -bitmap.getHeight() / 2);
	}

	private TileCache createTileCache() {
		TileCache firstLevelTileCache = new InMemoryTileCache(32);
		File cacheDirectory = getDir("tile_cache", MODE_PRIVATE);
		TileCache secondLevelTileCache = new FileSystemTileCache(1024, cacheDirectory, AndroidGraphics.INSTANCE);
		return new TwoLevelTileCache(firstLevelTileCache, secondLevelTileCache);
	}
}
