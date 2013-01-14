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
package org.mapsforge.android.maps;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.mapsforge.android.AndroidUtils;
import org.mapsforge.android.maps.inputhandling.MapMover;
import org.mapsforge.android.maps.inputhandling.TouchEventHandler;
import org.mapsforge.android.maps.inputhandling.ZoomAnimator;
import org.mapsforge.android.maps.mapgenerator.FileSystemTileCache;
import org.mapsforge.android.maps.mapgenerator.InMemoryTileCache;
import org.mapsforge.android.maps.mapgenerator.JobParameters;
import org.mapsforge.android.maps.mapgenerator.JobQueue;
import org.mapsforge.android.maps.mapgenerator.MapGeneratorJob;
import org.mapsforge.android.maps.mapgenerator.MapWorker;
import org.mapsforge.android.maps.mapgenerator.TileCache;
import org.mapsforge.android.maps.mapgenerator.databaserenderer.DatabaseRenderer;
import org.mapsforge.android.maps.overlay.Overlay;
import org.mapsforge.android.maps.overlay.OverlayController;
import org.mapsforge.core.model.GeoPoint;
import org.mapsforge.core.model.MapPosition;
import org.mapsforge.core.model.Tile;
import org.mapsforge.core.util.MercatorProjection;
import org.mapsforge.map.reader.MapDatabase;
import org.mapsforge.map.reader.header.FileOpenResult;
import org.mapsforge.map.rendertheme.ExternalRenderTheme;
import org.mapsforge.map.rendertheme.InternalRenderTheme;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.ViewGroup;

/**
 * A MapView shows a map on the display of the device. It handles all user input and touch gestures to move and zoom the
 * map. This MapView also includes a scale bar and zoom controls. The {@link #getMapViewPosition()} method returns a
 * {@link MapViewPosition} to programmatically modify the position and zoom level of the map.
 * <p>
 * A binary database file is required which contains the map data. Map files can be stored in any folder. The current
 * map file is set by calling {@link #setMapFile(File)}. To retrieve the current {@link MapDatabase}, use the
 * {@link #getMapDatabase()} method.
 * <p>
 * {@link Overlay Overlays} can be used to display geographical data such as points and ways. To draw an overlay on top
 * of the map, add it to the list returned by {@link #getOverlays()}.
 */
public class MapView extends ViewGroup {
	/**
	 * Default render theme of the MapView.
	 */
	public static final InternalRenderTheme DEFAULT_RENDER_THEME = InternalRenderTheme.OSMARENDER;
	private static final float DEFAULT_TEXT_SCALE = 1;
	private static final int DEFAULT_TILE_CACHE_SIZE_FILE_SYSTEM = 100;
	private static final int DEFAULT_TILE_CACHE_SIZE_IN_MEMORY = 20;

	private final DatabaseRenderer databaseRenderer;
	private DebugSettings debugSettings;
	private final TileCache fileSystemTileCache;
	private final FpsCounter fpsCounter;
	private final FrameBuffer frameBuffer;
	private final TileCache inMemoryTileCache;
	private JobParameters jobParameters;
	private final JobQueue jobQueue;
	private final MapDatabase mapDatabase;
	private File mapFile;
	private final MapMover mapMover;
	private final MapScaleBar mapScaleBar;
	private final MapViewPosition mapViewPosition;
	private final MapWorker mapWorker;
	private final MapZoomControls mapZoomControls;
	private final OverlayController overlayController;
	private final List<Overlay> overlays;
	private final Projection projection;
	private final TouchEventHandler touchEventHandler;
	private final ZoomAnimator zoomAnimator;

	/**
	 * @param context
	 *            the enclosing MapActivity instance.
	 * @throws IllegalArgumentException
	 *             if the context object is not an instance of {@link MapActivity}.
	 */
	public MapView(Context context) {
		this(context, null);
	}

	/**
	 * @param context
	 *            the enclosing MapActivity instance.
	 * @param attributeSet
	 *            a set of attributes.
	 * @throws IllegalArgumentException
	 *             if the context object is not an instance of {@link MapActivity}.
	 */
	public MapView(Context context, AttributeSet attributeSet) {
		super(context, attributeSet);

		if (!(context instanceof MapActivity)) {
			throw new IllegalArgumentException("context is not an instance of MapActivity");
		}
		MapActivity mapActivity = (MapActivity) context;

		setBackgroundColor(FrameBuffer.MAP_VIEW_BACKGROUND);
		setDescendantFocusability(FOCUS_BLOCK_DESCENDANTS);
		setWillNotDraw(false);

		this.debugSettings = new DebugSettings(false, false, false);
		this.fileSystemTileCache = new FileSystemTileCache(DEFAULT_TILE_CACHE_SIZE_FILE_SYSTEM,
				mapActivity.getMapViewId());
		this.fpsCounter = new FpsCounter();
		this.frameBuffer = new FrameBuffer(this);
		this.inMemoryTileCache = new InMemoryTileCache(DEFAULT_TILE_CACHE_SIZE_IN_MEMORY);
		this.jobParameters = new JobParameters(DEFAULT_RENDER_THEME, DEFAULT_TEXT_SCALE);
		this.jobQueue = new JobQueue(this);
		this.mapDatabase = new MapDatabase();
		this.mapViewPosition = new MapViewPosition(this);
		this.mapScaleBar = new MapScaleBar(this);
		this.mapZoomControls = new MapZoomControls(context, this);
		this.overlays = Collections.synchronizedList(new ArrayList<Overlay>());
		this.projection = new MapViewProjection(this);
		this.touchEventHandler = new TouchEventHandler(mapActivity, this);

		this.databaseRenderer = new DatabaseRenderer(this.mapDatabase);

		this.mapWorker = new MapWorker(this);
		this.mapWorker.start();

		this.mapMover = new MapMover(this);
		this.mapWorker.setDatabaseRenderer(this.databaseRenderer);
		this.mapMover.start();

		this.zoomAnimator = new ZoomAnimator(this);
		this.zoomAnimator.start();

		this.overlayController = new OverlayController(this);
		this.overlayController.start();

		GeoPoint startPoint = this.databaseRenderer.getStartPoint();
		Byte startZoomLevel = this.databaseRenderer.getStartZoomLevel();
		if (startPoint != null) {
			this.mapViewPosition.setCenter(startPoint);
		}
		if (startZoomLevel != null) {
			this.mapViewPosition.setZoomLevel(startZoomLevel.byteValue());
		}

		mapActivity.registerMapView(this);
	}

	/**
	 * @return the currently used DatabaseRenderer (may be null).
	 */
	public DatabaseRenderer getDatabaseRenderer() {
		return this.databaseRenderer;
	}

	/**
	 * @return the debug settings which are used in this MapView.
	 */
	public DebugSettings getDebugSettings() {
		return this.debugSettings;
	}

	/**
	 * @return the file system tile cache which is used in this MapView.
	 */
	public TileCache getFileSystemTileCache() {
		return this.fileSystemTileCache;
	}

	/**
	 * @return the FPS counter which is used in this MapView.
	 */
	public FpsCounter getFpsCounter() {
		return this.fpsCounter;
	}

	/**
	 * @return the FrameBuffer which is used in this MapView.
	 */
	public FrameBuffer getFrameBuffer() {
		return this.frameBuffer;
	}

	/**
	 * @return the in-memory tile cache which is used in this MapView.
	 */
	public TileCache getInMemoryTileCache() {
		return this.inMemoryTileCache;
	}

	/**
	 * @return the job queue which is used in this MapView.
	 */
	public JobQueue getJobQueue() {
		return this.jobQueue;
	}

	/**
	 * @return the map database which is used for reading map files.
	 */
	public MapDatabase getMapDatabase() {
		return this.mapDatabase;
	}

	/**
	 * @return the currently used map file.
	 */
	public File getMapFile() {
		return this.mapFile;
	}

	/**
	 * @return the MapMover which is used by this MapView.
	 */
	public MapMover getMapMover() {
		return this.mapMover;
	}

	/**
	 * @return the scale bar which is used in this MapView.
	 */
	public MapScaleBar getMapScaleBar() {
		return this.mapScaleBar;
	}

	/**
	 * @return the current position and zoom level of this MapView.
	 */
	public MapViewPosition getMapViewPosition() {
		return this.mapViewPosition;
	}

	/**
	 * @return the zoom controls instance which is used in this MapView.
	 */
	public MapZoomControls getMapZoomControls() {
		return this.mapZoomControls;
	}

	public OverlayController getOverlayController() {
		return this.overlayController;
	}

	/**
	 * Returns a thread-safe list of overlays for this MapView. It is necessary to manually synchronize on this list
	 * when iterating over it.
	 * 
	 * @return the overlay list.
	 */
	public List<Overlay> getOverlays() {
		return this.overlays;
	}

	/**
	 * @return the currently used projection of the map. Do not keep this object for a longer time.
	 */
	public Projection getProjection() {
		return this.projection;
	}

	public ZoomAnimator getZoomAnimator() {
		return this.zoomAnimator;
	}

	/**
	 * Calls either {@link #invalidate()} or {@link #postInvalidate()}, depending on the current thread.
	 */
	public void invalidateOnUiThread() {
		if (AndroidUtils.currentThreadIsUiThread()) {
			invalidate();
		} else {
			postInvalidate();
		}
	}

	/**
	 * @return true if the ZoomAnimator is currently running, false otherwise.
	 */
	public boolean isZoomAnimatorRunning() {
		return this.zoomAnimator.isExecuting();
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent keyEvent) {
		return this.mapMover.onKeyDown(keyCode, keyEvent);
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent keyEvent) {
		return this.mapMover.onKeyUp(keyCode, keyEvent);
	}

	@Override
	public boolean onTouchEvent(MotionEvent motionEvent) {
		int action = TouchEventHandler.getAction(motionEvent);
		this.mapZoomControls.onMapViewTouchEvent(action);

		if (!isClickable()) {
			return true;
		}
		return this.touchEventHandler.onTouchEvent(motionEvent);
	}

	@Override
	public boolean onTrackballEvent(MotionEvent motionEvent) {
		return this.mapMover.onTrackballEvent(motionEvent);
	}

	/**
	 * Triggers a redraw process of the map.
	 */
	public void redraw() {
		if (this.getWidth() <= 0 || this.getHeight() <= 0 || isZoomAnimatorRunning()) {
			return;
		}

		MapPosition mapPosition = this.mapViewPosition.getMapPosition();

		if (this.mapFile != null) {
			GeoPoint geoPoint = mapPosition.geoPoint;
			double pixelLeft = MercatorProjection.longitudeToPixelX(geoPoint.longitude, mapPosition.zoomLevel);
			double pixelTop = MercatorProjection.latitudeToPixelY(geoPoint.latitude, mapPosition.zoomLevel);
			pixelLeft -= getWidth() >> 1;
			pixelTop -= getHeight() >> 1;

			long tileLeft = MercatorProjection.pixelXToTileX(pixelLeft, mapPosition.zoomLevel);
			long tileTop = MercatorProjection.pixelYToTileY(pixelTop, mapPosition.zoomLevel);
			long tileRight = MercatorProjection.pixelXToTileX(pixelLeft + getWidth(), mapPosition.zoomLevel);
			long tileBottom = MercatorProjection.pixelYToTileY(pixelTop + getHeight(), mapPosition.zoomLevel);

			for (long tileY = tileTop; tileY <= tileBottom; ++tileY) {
				for (long tileX = tileLeft; tileX <= tileRight; ++tileX) {
					Tile tile = new Tile(tileX, tileY, mapPosition.zoomLevel);
					MapGeneratorJob mapGeneratorJob = new MapGeneratorJob(tile, this.mapFile, this.jobParameters,
							this.debugSettings);

					if (this.inMemoryTileCache.containsKey(mapGeneratorJob)) {
						Bitmap bitmap = this.inMemoryTileCache.get(mapGeneratorJob);
						this.frameBuffer.drawBitmap(mapGeneratorJob.tile, bitmap);
					} else if (this.fileSystemTileCache.containsKey(mapGeneratorJob)) {
						Bitmap bitmap = this.fileSystemTileCache.get(mapGeneratorJob);

						if (bitmap != null) {
							this.frameBuffer.drawBitmap(mapGeneratorJob.tile, bitmap);
							this.inMemoryTileCache.put(mapGeneratorJob, bitmap);
						} else {
							// the image data could not be read from the cache
							this.jobQueue.addJob(mapGeneratorJob);
						}
					} else {
						// cache miss
						this.jobQueue.addJob(mapGeneratorJob);
					}
				}
			}

			this.jobQueue.requestSchedule();
			synchronized (this.mapWorker) {
				this.mapWorker.notify();
			}
		}

		this.overlayController.redrawOverlays();

		if (this.mapScaleBar.isShowMapScaleBar()) {
			this.mapScaleBar.redrawScaleBar();
		}

		invalidateOnUiThread();
	}

	/**
	 * Sets the visibility of the zoom controls.
	 * 
	 * @param showZoomControls
	 *            true if the zoom controls should be visible, false otherwise.
	 */
	public void setBuiltInZoomControls(boolean showZoomControls) {
		this.mapZoomControls.setShowMapZoomControls(showZoomControls);
	}

	/**
	 * @param debugSettings
	 *            the new DebugSettings for this MapView.
	 */
	public void setDebugSettings(DebugSettings debugSettings) {
		this.debugSettings = debugSettings;
		clearAndRedrawMapView();
	}

	/**
	 * Sets the map file for this MapView.
	 * 
	 * @param mapFile
	 *            the map file.
	 * @return a FileOpenResult to describe whether the operation returned successfully.
	 * @throws IllegalArgumentException
	 *             if the supplied mapFile is null.
	 */
	public FileOpenResult setMapFile(File mapFile) {
		if (mapFile == null) {
			throw new IllegalArgumentException("mapFile must not be null");
		} else if (mapFile.equals(this.mapFile)) {
			// same map file as before
			return FileOpenResult.SUCCESS;
		}

		this.zoomAnimator.pause();
		this.mapWorker.pause();
		this.mapMover.pause();

		this.zoomAnimator.awaitPausing();
		this.mapMover.awaitPausing();
		this.mapWorker.awaitPausing();

		this.mapMover.stopMove();
		this.jobQueue.clear();

		this.zoomAnimator.proceed();
		this.mapWorker.proceed();
		this.mapMover.proceed();

		this.mapDatabase.closeFile();
		FileOpenResult fileOpenResult = this.mapDatabase.openFile(mapFile);
		if (fileOpenResult.isSuccess()) {
			this.mapFile = mapFile;

			GeoPoint startPoint = this.databaseRenderer.getStartPoint();
			if (startPoint != null) {
				this.mapViewPosition.setCenter(startPoint);
			}

			Byte startZoomLevel = this.databaseRenderer.getStartZoomLevel();
			if (startZoomLevel != null) {
				this.mapViewPosition.setZoomLevel(startZoomLevel.byteValue());
			}

			clearAndRedrawMapView();
			return FileOpenResult.SUCCESS;
		}
		this.mapFile = null;
		clearAndRedrawMapView();
		return fileOpenResult;
	}

	/**
	 * Sets the XML file which is used for rendering the map.
	 * 
	 * @param renderThemeFile
	 *            the XML file which defines the rendering theme.
	 * @throws IllegalArgumentException
	 *             if the supplied internalRenderTheme is null.
	 * @throws FileNotFoundException
	 *             if the supplied file does not exist, is a directory or cannot be read.
	 */
	public void setRenderTheme(File renderThemeFile) throws FileNotFoundException {
		if (renderThemeFile == null) {
			throw new IllegalArgumentException("render theme file must not be null");
		}

		org.mapsforge.map.rendertheme.XmlRenderTheme jobTheme = new ExternalRenderTheme(renderThemeFile);
		this.jobParameters = new JobParameters(jobTheme, this.jobParameters.textScale);
		clearAndRedrawMapView();
	}

	/**
	 * Sets the internal theme which is used for rendering the map.
	 * 
	 * @param internalRenderTheme
	 *            the internal rendering theme.
	 * @throws IllegalArgumentException
	 *             if the supplied internalRenderTheme is null.
	 */
	public void setRenderTheme(InternalRenderTheme internalRenderTheme) {
		if (internalRenderTheme == null) {
			throw new IllegalArgumentException("render theme must not be null");
		}

		this.jobParameters = new JobParameters(internalRenderTheme, this.jobParameters.textScale);
		clearAndRedrawMapView();
	}

	/**
	 * Sets the text scale for the map rendering. Has no effect in downloading mode.
	 * 
	 * @param textScale
	 *            the new text scale for the map rendering.
	 */
	public void setTextScale(float textScale) {
		this.jobParameters = new JobParameters(this.jobParameters.jobTheme, textScale);
		clearAndRedrawMapView();
	}

	/**
	 * Takes a screenshot of the currently visible map and saves it as a compressed image. Zoom buttons, scale bar, FPS
	 * counter, overlays, menus and the title bar are not included in the screenshot.
	 * 
	 * @param outputFile
	 *            the image file. If the file already exists, it will be overwritten.
	 * @param compressFormat
	 *            the file format of the compressed image.
	 * @param quality
	 *            value from 0 (low) to 100 (high). Has no effect on some formats like PNG.
	 * @return true if the image was saved successfully, false otherwise.
	 * @throws IOException
	 *             if an error occurs while writing the image file.
	 */
	public boolean takeScreenshot(CompressFormat compressFormat, int quality, File outputFile) throws IOException {
		FileOutputStream outputStream = new FileOutputStream(outputFile);
		boolean success = this.frameBuffer.compress(compressFormat, quality, outputStream);
		outputStream.close();
		return success;
	}

	@Override
	protected void onDraw(Canvas canvas) {
		this.frameBuffer.draw(canvas);
		this.overlayController.draw(canvas);

		if (this.mapScaleBar.isShowMapScaleBar()) {
			this.mapScaleBar.draw(canvas);
		}

		if (this.fpsCounter.isShowFpsCounter()) {
			this.fpsCounter.draw(canvas);
		}
	}

	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		this.mapZoomControls.onLayout(changed, left, top, right, bottom);
	}

	@Override
	protected final void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		// find out how big the zoom controls should be
		this.mapZoomControls.measure(
				MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.AT_MOST),
				MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(heightMeasureSpec), MeasureSpec.AT_MOST));

		// make sure that MapView is big enough to display the zoom controls
		setMeasuredDimension(Math.max(MeasureSpec.getSize(widthMeasureSpec), this.mapZoomControls.getMeasuredWidth()),
				Math.max(MeasureSpec.getSize(heightMeasureSpec), this.mapZoomControls.getMeasuredHeight()));
	}

	@Override
	protected synchronized void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
		this.frameBuffer.destroy();

		if (width > 0 && height > 0) {
			this.frameBuffer.onSizeChanged();
			this.overlayController.onSizeChanged();
			redraw();
		}
	}

	void clearAndRedrawMapView() {
		this.jobQueue.clear();
		this.frameBuffer.clear();
		redraw();
	}

	void destroy() {
		this.overlayController.interrupt();
		this.mapMover.interrupt();
		this.mapWorker.interrupt();
		this.zoomAnimator.interrupt();

		try {
			this.mapWorker.join();
		} catch (InterruptedException e) {
			// restore the interrupted status
			Thread.currentThread().interrupt();
		}

		this.frameBuffer.destroy();
		this.mapScaleBar.destroy();
		this.inMemoryTileCache.destroy();
		this.fileSystemTileCache.destroy();
		this.databaseRenderer.destroy();

		this.mapDatabase.closeFile();
	}

	/**
	 * @return the maximum possible zoom level.
	 */
	byte getZoomLevelMax() {
		return (byte) Math.min(this.mapZoomControls.getZoomLevelMax(), this.databaseRenderer.getZoomLevelMax());
	}

	void onPause() {
		this.mapWorker.pause();
		this.mapMover.pause();
		this.zoomAnimator.pause();
	}

	void onResume() {
		this.mapWorker.proceed();
		this.mapMover.proceed();
		this.zoomAnimator.proceed();
	}
}
