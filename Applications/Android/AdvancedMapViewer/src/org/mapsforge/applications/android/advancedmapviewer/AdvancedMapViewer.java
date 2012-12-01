/*
 * Copyright 2010, 2011, 2012 mapsforge.org
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
package org.mapsforge.applications.android.advancedmapviewer;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.text.DateFormat;
import java.util.Date;

import org.mapsforge.android.AndroidUtils;
import org.mapsforge.android.maps.DebugSettings;
import org.mapsforge.android.maps.MapActivity;
import org.mapsforge.android.maps.MapScaleBar;
import org.mapsforge.android.maps.MapScaleBar.TextField;
import org.mapsforge.android.maps.MapView;
import org.mapsforge.android.maps.MapViewPosition;
import org.mapsforge.android.maps.mapgenerator.TileCache;
import org.mapsforge.android.maps.overlay.Marker;
import org.mapsforge.android.maps.overlay.MyLocationOverlay;
import org.mapsforge.applications.android.advancedmapviewer.filefilter.FilterByFileExtension;
import org.mapsforge.applications.android.advancedmapviewer.filefilter.ValidMapFile;
import org.mapsforge.applications.android.advancedmapviewer.filefilter.ValidRenderTheme;
import org.mapsforge.applications.android.advancedmapviewer.filepicker.FilePicker;
import org.mapsforge.applications.android.advancedmapviewer.preferences.EditPreferences;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.GeoPoint;
import org.mapsforge.core.model.MapPosition;
import org.mapsforge.map.reader.header.MapFileInfo;
import org.mapsforge.map.rendertheme.InternalRenderTheme;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

/**
 * A map application which uses the features from the mapsforge map library. The map can be centered to the current
 * location. A simple file browser for selecting the map file is also included. Some preferences can be adjusted via the
 * {@link EditPreferences} activity and screenshots of the map may be taken in different image formats.
 */
public class AdvancedMapViewer extends MapActivity {
	/**
	 * The default number of tiles in the file system cache.
	 */
	public static final int FILE_SYSTEM_CACHE_SIZE_DEFAULT = 250;

	/**
	 * The maximum number of tiles in the file system cache.
	 */
	public static final int FILE_SYSTEM_CACHE_SIZE_MAX = 500;

	/**
	 * The default move speed factor of the map.
	 */
	public static final int MOVE_SPEED_DEFAULT = 10;

	/**
	 * The maximum move speed factor of the map.
	 */
	public static final int MOVE_SPEED_MAX = 30;

	private static final String BUNDLE_CENTER_AT_FIRST_FIX = "centerAtFirstFix";
	private static final String BUNDLE_SHOW_MY_LOCATION = "showMyLocation";
	private static final String BUNDLE_SNAP_TO_LOCATION = "snapToLocation";
	private static final int DIALOG_ENTER_COORDINATES = 0;
	private static final int DIALOG_INFO_MAP_FILE = 1;
	private static final int DIALOG_LOCATION_PROVIDER_DISABLED = 2;
	private static final FileFilter FILE_FILTER_EXTENSION_MAP = new FilterByFileExtension(".map");
	private static final FileFilter FILE_FILTER_EXTENSION_XML = new FilterByFileExtension(".xml");
	private static final int SELECT_MAP_FILE = 0;
	private static final int SELECT_RENDER_THEME_FILE = 1;

	private MyLocationOverlay myLocationOverlay;
	private ScreenshotCapturer screenshotCapturer;
	private ToggleButton snapToLocationView;
	private WakeLock wakeLock;
	MapView mapView;

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.options_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_info:
				return true;

			case R.id.menu_info_map_file:
				showDialog(DIALOG_INFO_MAP_FILE);
				return true;

			case R.id.menu_info_about:
				startActivity(new Intent(this, InfoView.class));
				return true;

			case R.id.menu_position:
				return true;

			case R.id.menu_position_my_location_enable:
				enableShowMyLocation(true);
				return true;

			case R.id.menu_position_my_location_disable:
				disableShowMyLocation();
				return true;

			case R.id.menu_position_last_known:
				gotoLastKnownPosition();
				return true;

			case R.id.menu_position_enter_coordinates:
				showDialog(DIALOG_ENTER_COORDINATES);
				return true;

			case R.id.menu_position_map_center:
				// disable GPS follow mode if it is enabled
				disableSnapToLocation(true);
				MapFileInfo mapFileInfo = this.mapView.getMapDatabase().getMapFileInfo();
				this.mapView.getMapViewPosition().setCenter(mapFileInfo.boundingBox.getCenterPoint());
				return true;

			case R.id.menu_screenshot:
				return true;

			case R.id.menu_screenshot_jpeg:
				this.screenshotCapturer.captureScreenshot(CompressFormat.JPEG);
				return true;

			case R.id.menu_screenshot_png:
				this.screenshotCapturer.captureScreenshot(CompressFormat.PNG);
				return true;

			case R.id.menu_preferences:
				startActivity(new Intent(this, EditPreferences.class));
				return true;

			case R.id.menu_render_theme:
				return true;

			case R.id.menu_render_theme_osmarender:
				this.mapView.setRenderTheme(InternalRenderTheme.OSMARENDER);
				return true;

			case R.id.menu_render_theme_select_file:
				startRenderThemePicker();
				return true;

			case R.id.menu_mapfile:
				startMapFilePicker();
				return true;

			default:
				return false;
		}
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		if (this.myLocationOverlay.isMyLocationEnabled()) {
			menu.findItem(R.id.menu_position_my_location_enable).setVisible(false);
			menu.findItem(R.id.menu_position_my_location_enable).setEnabled(false);
			menu.findItem(R.id.menu_position_my_location_disable).setVisible(true);
			menu.findItem(R.id.menu_position_my_location_disable).setEnabled(true);
		} else {
			menu.findItem(R.id.menu_position_my_location_enable).setVisible(true);
			menu.findItem(R.id.menu_position_my_location_enable).setEnabled(true);
			menu.findItem(R.id.menu_position_my_location_disable).setVisible(false);
			menu.findItem(R.id.menu_position_my_location_disable).setEnabled(false);
		}

		return true;
	}

	@Override
	public boolean onTrackballEvent(MotionEvent event) {
		// forward the event to the MapView
		return this.mapView.onTrackballEvent(event);
	}

	private void configureMapView() {
		this.mapView.setBuiltInZoomControls(true);
		this.mapView.setClickable(true);
		this.mapView.setFocusable(true);

		MapScaleBar mapScaleBar = this.mapView.getMapScaleBar();
		mapScaleBar.setText(TextField.KILOMETER, getString(R.string.unit_symbol_kilometer));
		mapScaleBar.setText(TextField.METER, getString(R.string.unit_symbol_meter));
	}

	private void disableShowMyLocation() {
		if (this.myLocationOverlay.isMyLocationEnabled()) {
			this.myLocationOverlay.disableMyLocation();
		}
	}

	/**
	 * Enables the "show my location" mode.
	 * 
	 * @param centerAtFirstFix
	 *            defines whether the map should be centered to the first fix.
	 */
	private void enableShowMyLocation(boolean centerAtFirstFix) {
		if (!this.myLocationOverlay.isMyLocationEnabled()) {
			if (!this.myLocationOverlay.enableMyLocation(centerAtFirstFix)) {
				showDialog(DIALOG_LOCATION_PROVIDER_DISABLED);
				return;
			}

			this.mapView.getOverlays().add(this.myLocationOverlay);
			this.snapToLocationView.setVisibility(View.VISIBLE);
		}
	}

	/**
	 * Centers the map to the last known position as reported by the most accurate location provider. If the last
	 * location is unknown, a toast message is displayed instead.
	 */
	private void gotoLastKnownPosition() {
		Location bestLocation = null;
		LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		for (String provider : locationManager.getProviders(true)) {
			Location currentLocation = locationManager.getLastKnownLocation(provider);
			if (bestLocation == null || bestLocation.getAccuracy() > currentLocation.getAccuracy()) {
				bestLocation = currentLocation;
			}
		}

		// check if a location has been found
		if (bestLocation != null) {
			GeoPoint geoPoint = new GeoPoint(bestLocation.getLatitude(), bestLocation.getLongitude());
			this.mapView.getMapViewPosition().setCenter(geoPoint);
		} else {
			showToastOnUiThread(getString(R.string.error_last_location_unknown));
		}
	}

	/**
	 * Sets all file filters and starts the FilePicker to select a map file.
	 */
	private void startMapFilePicker() {
		FilePicker.setFileDisplayFilter(FILE_FILTER_EXTENSION_MAP);
		FilePicker.setFileSelectFilter(new ValidMapFile());
		startActivityForResult(new Intent(this, FilePicker.class), SELECT_MAP_FILE);
	}

	/**
	 * Sets all file filters and starts the FilePicker to select an XML file.
	 */
	private void startRenderThemePicker() {
		FilePicker.setFileDisplayFilter(FILE_FILTER_EXTENSION_XML);
		FilePicker.setFileSelectFilter(new ValidRenderTheme());
		startActivityForResult(new Intent(this, FilePicker.class), SELECT_RENDER_THEME_FILE);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		if (requestCode == SELECT_MAP_FILE) {
			if (resultCode == RESULT_OK) {
				disableSnapToLocation(true);
				if (intent != null && intent.getStringExtra(FilePicker.SELECTED_FILE) != null) {
					this.mapView.setMapFile(new File(intent.getStringExtra(FilePicker.SELECTED_FILE)));
				}
			} else if (resultCode == RESULT_CANCELED && this.mapView.getMapFile() == null) {
				finish();
			}
		} else if (requestCode == SELECT_RENDER_THEME_FILE && resultCode == RESULT_OK && intent != null
				&& intent.getStringExtra(FilePicker.SELECTED_FILE) != null) {
			try {
				this.mapView.setRenderTheme(new File(intent.getStringExtra(FilePicker.SELECTED_FILE)));
			} catch (FileNotFoundException e) {
				showToastOnUiThread(e.getLocalizedMessage());
			}
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		this.screenshotCapturer = new ScreenshotCapturer(this);
		this.screenshotCapturer.start();

		setContentView(R.layout.activity_advanced_map_viewer);
		this.mapView = (MapView) findViewById(R.id.mapView);
		configureMapView();

		this.snapToLocationView = (ToggleButton) findViewById(R.id.snapToLocationView);
		this.snapToLocationView.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				invertSnapToLocation();
			}
		});

		Drawable drawable = getResources().getDrawable(R.drawable.ic_maps_indicator_current_position_anim1);
		drawable = Marker.boundCenter(drawable);
		this.myLocationOverlay = new MyLocationOverlay(this, this.mapView, drawable);

		PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
		this.wakeLock = powerManager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "AMV");

		if (savedInstanceState != null && savedInstanceState.getBoolean(BUNDLE_SHOW_MY_LOCATION)) {
			enableShowMyLocation(savedInstanceState.getBoolean(BUNDLE_CENTER_AT_FIRST_FIX));
			if (savedInstanceState.getBoolean(BUNDLE_SNAP_TO_LOCATION)) {
				enableSnapToLocation(false);
			}
		}
	}

	@Deprecated
	@Override
	protected Dialog onCreateDialog(int id) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		if (id == DIALOG_ENTER_COORDINATES) {
			builder.setIcon(android.R.drawable.ic_menu_mylocation);
			builder.setTitle(R.string.menu_position_enter_coordinates);
			LayoutInflater factory = LayoutInflater.from(this);
			final View view = factory.inflate(R.layout.dialog_enter_coordinates, null);
			builder.setView(view);
			builder.setPositiveButton(R.string.go_to_position, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					// disable GPS follow mode if it is enabled
					disableSnapToLocation(true);

					// set the map center and zoom level
					EditText latitudeView = (EditText) view.findViewById(R.id.latitude);
					EditText longitudeView = (EditText) view.findViewById(R.id.longitude);
					double latitude = Double.parseDouble(latitudeView.getText().toString());
					double longitude = Double.parseDouble(longitudeView.getText().toString());
					GeoPoint geoPoint = new GeoPoint(latitude, longitude);
					SeekBar zoomLevelView = (SeekBar) view.findViewById(R.id.zoomLevel);
					MapPosition newMapPosition = new MapPosition(geoPoint, (byte) zoomLevelView.getProgress());
					AdvancedMapViewer.this.mapView.getMapViewPosition().setMapPosition(newMapPosition);
				}
			});
			builder.setNegativeButton(R.string.cancel, null);
			return builder.create();
		} else if (id == DIALOG_LOCATION_PROVIDER_DISABLED) {
			builder.setIcon(android.R.drawable.ic_menu_info_details);
			builder.setTitle(R.string.error);
			builder.setMessage(R.string.no_location_provider_available);
			builder.setPositiveButton(R.string.ok, null);
			return builder.create();
		} else if (id == DIALOG_INFO_MAP_FILE) {
			builder.setIcon(android.R.drawable.ic_menu_info_details);
			builder.setTitle(R.string.menu_info_map_file);
			LayoutInflater factory = LayoutInflater.from(this);
			builder.setView(factory.inflate(R.layout.dialog_info_map_file, null));
			builder.setPositiveButton(R.string.ok, null);
			return builder.create();
		} else {
			// do dialog will be created
			return null;
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		this.screenshotCapturer.interrupt();
		disableShowMyLocation();
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (this.wakeLock.isHeld()) {
			this.wakeLock.release();
		}
	}

	@Deprecated
	@Override
	protected void onPrepareDialog(int id, final Dialog dialog) {
		if (id == DIALOG_ENTER_COORDINATES) {
			// latitude
			EditText editText = (EditText) dialog.findViewById(R.id.latitude);
			MapViewPosition mapViewPosition = this.mapView.getMapViewPosition();
			GeoPoint mapCenter = mapViewPosition.getCenter();
			editText.setText(Double.toString(mapCenter.latitude));

			// longitude
			editText = (EditText) dialog.findViewById(R.id.longitude);
			editText.setText(Double.toString(mapCenter.longitude));

			// zoom level
			SeekBar zoomlevel = (SeekBar) dialog.findViewById(R.id.zoomLevel);
			zoomlevel.setMax(this.mapView.getDatabaseRenderer().getZoomLevelMax());
			zoomlevel.setProgress(mapViewPosition.getZoomLevel());

			// zoom level value
			final TextView textView = (TextView) dialog.findViewById(R.id.zoomlevelValue);
			textView.setText(String.valueOf(zoomlevel.getProgress()));
			zoomlevel.setOnSeekBarChangeListener(new SeekBarChangeListener(textView));
		} else if (id == DIALOG_INFO_MAP_FILE) {
			MapFileInfo mapFileInfo = this.mapView.getMapDatabase().getMapFileInfo();

			// map file name
			TextView textView = (TextView) dialog.findViewById(R.id.infoMapFileViewName);
			textView.setText(this.mapView.getMapFile().getAbsolutePath());

			// map file size
			textView = (TextView) dialog.findViewById(R.id.infoMapFileViewSize);
			textView.setText(FileUtils.formatFileSize(mapFileInfo.fileSize, getResources()));

			// map file version
			textView = (TextView) dialog.findViewById(R.id.infoMapFileViewVersion);
			textView.setText(String.valueOf(mapFileInfo.fileVersion));

			// map file debug
			textView = (TextView) dialog.findViewById(R.id.infoMapFileViewDebug);
			if (mapFileInfo.debugFile) {
				textView.setText(R.string.info_map_file_debug_yes);
			} else {
				textView.setText(R.string.info_map_file_debug_no);
			}

			// map file date
			textView = (TextView) dialog.findViewById(R.id.infoMapFileViewDate);
			Date date = new Date(mapFileInfo.mapDate);
			textView.setText(DateFormat.getDateTimeInstance().format(date));

			// map file area
			textView = (TextView) dialog.findViewById(R.id.infoMapFileViewArea);
			BoundingBox boundingBox = mapFileInfo.boundingBox;
			textView.setText(boundingBox.minLatitude + ", " + boundingBox.minLongitude + " – \n"
					+ boundingBox.maxLatitude + ", " + boundingBox.maxLongitude);

			// map file start position
			textView = (TextView) dialog.findViewById(R.id.infoMapFileViewStartPosition);
			GeoPoint startPosition = mapFileInfo.startPosition;
			if (startPosition == null) {
				textView.setText(null);
			} else {
				textView.setText(startPosition.latitude + ", " + startPosition.longitude);
			}

			// map file start zoom level
			textView = (TextView) dialog.findViewById(R.id.infoMapFileViewStartZoomLevel);
			Byte startZoomLevel = mapFileInfo.startZoomLevel;
			if (startZoomLevel == null) {
				textView.setText(null);
			} else {
				textView.setText(startZoomLevel.toString());
			}

			// map file language preference
			textView = (TextView) dialog.findViewById(R.id.infoMapFileViewLanguagePreference);
			textView.setText(mapFileInfo.languagePreference);

			// map file comment text
			textView = (TextView) dialog.findViewById(R.id.infoMapFileViewComment);
			textView.setText(mapFileInfo.comment);

			// map file created by text
			textView = (TextView) dialog.findViewById(R.id.infoMapFileViewCreatedBy);
			textView.setText(mapFileInfo.createdBy);
		} else {
			super.onPrepareDialog(id, dialog);
		}
	}

	@Override
	protected void onResume() {
		super.onResume();

		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

		MapScaleBar mapScaleBar = this.mapView.getMapScaleBar();
		mapScaleBar.setShowMapScaleBar(sharedPreferences.getBoolean("showScaleBar", false));
		String scaleBarUnitDefault = getString(R.string.preferences_scale_bar_unit_default);
		String scaleBarUnit = sharedPreferences.getString("scaleBarUnit", scaleBarUnitDefault);
		mapScaleBar.setImperialUnits(scaleBarUnit.equals("imperial"));

		try {
			String textScaleDefault = getString(R.string.preferences_text_scale_default);
			this.mapView.setTextScale(Float.parseFloat(sharedPreferences.getString("textScale", textScaleDefault)));
		} catch (NumberFormatException e) {
			this.mapView.setTextScale(1);
		}

		if (sharedPreferences.getBoolean("fullscreen", false)) {
			getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
			getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
		} else {
			getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
			getWindow().addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
		}
		if (sharedPreferences.getBoolean("wakeLock", false) && !this.wakeLock.isHeld()) {
			this.wakeLock.acquire();
		}

		boolean persistent = sharedPreferences.getBoolean("cachePersistence", false);
		int capacity = Math.min(sharedPreferences.getInt("cacheSize", FILE_SYSTEM_CACHE_SIZE_DEFAULT),
				FILE_SYSTEM_CACHE_SIZE_MAX);
		TileCache fileSystemTileCache = this.mapView.getFileSystemTileCache();
		fileSystemTileCache.setPersistent(persistent);
		fileSystemTileCache.setCapacity(capacity);

		float moveSpeedFactor = Math.min(sharedPreferences.getInt("moveSpeed", MOVE_SPEED_DEFAULT), MOVE_SPEED_MAX) / 10f;
		this.mapView.getMapMover().setMoveSpeedFactor(moveSpeedFactor);

		this.mapView.getFpsCounter().setFpsCounter(sharedPreferences.getBoolean("showFpsCounter", false));

		boolean drawTileFrames = sharedPreferences.getBoolean("drawTileFrames", false);
		boolean drawTileCoordinates = sharedPreferences.getBoolean("drawTileCoordinates", false);
		boolean highlightWaterTiles = sharedPreferences.getBoolean("highlightWaterTiles", false);
		DebugSettings debugSettings = new DebugSettings(drawTileCoordinates, drawTileFrames, highlightWaterTiles);
		this.mapView.setDebugSettings(debugSettings);

		if (this.mapView.getMapFile() == null) {
			startMapFilePicker();
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBoolean(BUNDLE_SHOW_MY_LOCATION, this.myLocationOverlay.isMyLocationEnabled());
		outState.putBoolean(BUNDLE_CENTER_AT_FIRST_FIX, this.myLocationOverlay.isCenterAtNextFix());
		outState.putBoolean(BUNDLE_SNAP_TO_LOCATION, this.myLocationOverlay.isSnapToLocationEnabled());
	}

	/**
	 * @param showToast
	 *            defines whether a toast message is displayed or not.
	 */
	void disableSnapToLocation(boolean showToast) {
		if (this.myLocationOverlay.isSnapToLocationEnabled()) {
			this.myLocationOverlay.setSnapToLocationEnabled(false);
			this.snapToLocationView.setChecked(false);
			this.mapView.setClickable(true);
			if (showToast) {
				showToastOnUiThread(getString(R.string.snap_to_location_disabled));
			}
		}
	}

	/**
	 * @param showToast
	 *            defines whether a toast message is displayed or not.
	 */
	void enableSnapToLocation(boolean showToast) {
		if (!this.myLocationOverlay.isSnapToLocationEnabled()) {
			this.myLocationOverlay.setSnapToLocationEnabled(true);
			this.mapView.setClickable(false);
			if (showToast) {
				showToastOnUiThread(getString(R.string.snap_to_location_enabled));
			}
		}
	}

	void invertSnapToLocation() {
		if (this.myLocationOverlay.isSnapToLocationEnabled()) {
			disableSnapToLocation(true);
		} else {
			enableSnapToLocation(true);
		}
	}

	/**
	 * Uses the UI thread to display the given text message as toast notification.
	 * 
	 * @param text
	 *            the text message to display
	 */
	void showToastOnUiThread(final String text) {
		if (AndroidUtils.currentThreadIsUiThread()) {
			Toast toast = Toast.makeText(this, text, Toast.LENGTH_LONG);
			toast.show();
		} else {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					Toast toast = Toast.makeText(AdvancedMapViewer.this, text, Toast.LENGTH_LONG);
					toast.show();
				}
			});
		}
	}
}
