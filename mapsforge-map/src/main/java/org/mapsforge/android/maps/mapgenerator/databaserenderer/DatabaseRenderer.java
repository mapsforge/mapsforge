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
package org.mapsforge.android.maps.mapgenerator.databaserenderer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.ParserConfigurationException;

import org.mapsforge.android.maps.mapgenerator.MapGeneratorJob;
import org.mapsforge.core.model.GeoPoint;
import org.mapsforge.core.model.Point;
import org.mapsforge.core.model.Tag;
import org.mapsforge.core.model.Tile;
import org.mapsforge.core.util.MercatorProjection;
import org.mapsforge.graphics.android.AndroidGraphics;
import org.mapsforge.map.graphics.Bitmap;
import org.mapsforge.map.graphics.Paint;
import org.mapsforge.map.graphics.Style;
import org.mapsforge.map.reader.MapDatabase;
import org.mapsforge.map.reader.MapReadResult;
import org.mapsforge.map.reader.PointOfInterest;
import org.mapsforge.map.reader.Way;
import org.mapsforge.map.reader.header.MapFileInfo;
import org.mapsforge.map.rendertheme.GraphicAdapter.Color;
import org.mapsforge.map.rendertheme.RenderCallback;
import org.mapsforge.map.rendertheme.XmlRenderTheme;
import org.mapsforge.map.rendertheme.rule.RenderTheme;
import org.mapsforge.map.rendertheme.rule.RenderThemeHandler;
import org.xml.sax.SAXException;

/**
 * A DatabaseRenderer renders map tiles by reading from a {@link MapDatabase}.
 */
public class DatabaseRenderer implements RenderCallback {
	private static final Byte DEFAULT_START_ZOOM_LEVEL = Byte.valueOf((byte) 12);
	private static final byte LAYERS = 11;
	private static final Logger LOGGER = Logger.getLogger(DatabaseRenderer.class.getName());
	private static final Paint PAINT_WATER_TILE_HIGHTLIGHT = AndroidGraphics.INSTANCE.getPaint();
	private static final double STROKE_INCREASE = 1.5;
	private static final byte STROKE_MIN_ZOOM_LEVEL = 12;
	private static final Tag TAG_NATURAL_WATER = new Tag("natural", "water");
	private static final Point[][] WATER_TILE_COORDINATES = getTilePixelCoordinates();
	private static final byte ZOOM_MAX = 22;

	private static RenderTheme getRenderTheme(XmlRenderTheme jobTheme) {
		try {
			return RenderThemeHandler.getRenderTheme(AndroidGraphics.INSTANCE, jobTheme);
		} catch (ParserConfigurationException e) {
			LOGGER.log(Level.SEVERE, null, e);
		} catch (SAXException e) {
			LOGGER.log(Level.SEVERE, null, e);
		} catch (IOException e) {
			LOGGER.log(Level.SEVERE, null, e);
		}
		return null;
	}

	private static Point[][] getTilePixelCoordinates() {
		Point point1 = new Point(0, 0);
		Point point2 = new Point(Tile.TILE_SIZE, 0);
		Point point3 = new Point(Tile.TILE_SIZE, Tile.TILE_SIZE);
		Point point4 = new Point(0, Tile.TILE_SIZE);
		return new Point[][] { { point1, point2, point3, point4, point1 } };
	}

	private static byte getValidLayer(byte layer) {
		if (layer < 0) {
			return 0;
		} else if (layer >= LAYERS) {
			return LAYERS - 1;
		} else {
			return layer;
		}
	}

	private final List<PointTextContainer> areaLabels;
	private final CanvasRasterer canvasRasterer;
	private Point[][] coordinates;
	private Tile currentTile;
	private List<List<ShapePaintContainer>> drawingLayers;
	private final LabelPlacement labelPlacement;
	private final MapDatabase mapDatabase;
	private List<PointTextContainer> nodes;
	private final List<SymbolContainer> pointSymbols;
	private Point poiPosition;
	private XmlRenderTheme previousJobTheme;
	private float previousTextScale;
	private byte previousZoomLevel;
	private RenderTheme renderTheme;
	private ShapeContainer shapeContainer;
	private final List<WayTextContainer> wayNames;
	private final List<List<List<ShapePaintContainer>>> ways;
	private final List<SymbolContainer> waySymbols;

	/**
	 * Constructs a new DatabaseRenderer.
	 * 
	 * @param mapDatabase
	 *            the MapDatabase from which the map data will be read.
	 */
	public DatabaseRenderer(MapDatabase mapDatabase) {
		this.mapDatabase = mapDatabase;
		this.canvasRasterer = new CanvasRasterer();
		this.labelPlacement = new LabelPlacement();

		this.ways = new ArrayList<List<List<ShapePaintContainer>>>(LAYERS);
		this.wayNames = new ArrayList<WayTextContainer>(64);
		this.nodes = new ArrayList<PointTextContainer>(64);
		this.areaLabels = new ArrayList<PointTextContainer>(64);
		this.waySymbols = new ArrayList<SymbolContainer>(64);
		this.pointSymbols = new ArrayList<SymbolContainer>(64);

		PAINT_WATER_TILE_HIGHTLIGHT.setStyle(Style.FILL);
		PAINT_WATER_TILE_HIGHTLIGHT.setColor(AndroidGraphics.INSTANCE.getColor(Color.CYAN));
	}

	/**
	 * Called when a job needs to be executed.
	 * 
	 * @param mapGeneratorJob
	 *            the job that should be executed.
	 * @param bitmap
	 *            the bitmap for the generated map tile.
	 * @return true if the job was executed successfully, false otherwise.
	 */
	public boolean executeJob(MapGeneratorJob mapGeneratorJob, android.graphics.Bitmap bitmap) {
		this.currentTile = mapGeneratorJob.tile;

		XmlRenderTheme jobTheme = mapGeneratorJob.jobParameters.jobTheme;
		if (!jobTheme.equals(this.previousJobTheme)) {
			if (this.renderTheme != null) {
				this.renderTheme.destroy();
			}
			this.renderTheme = getRenderTheme(jobTheme);
			if (this.renderTheme == null) {
				this.previousJobTheme = null;
				return false;
			}
			createWayLists();
			this.previousJobTheme = jobTheme;
			this.previousZoomLevel = Byte.MIN_VALUE;
		}

		byte zoomLevel = this.currentTile.zoomLevel;
		if (zoomLevel != this.previousZoomLevel) {
			setScaleStrokeWidth(zoomLevel);
			this.previousZoomLevel = zoomLevel;
		}

		float textScale = mapGeneratorJob.jobParameters.textScale;
		if (Float.compare(textScale, this.previousTextScale) != 0) {
			this.renderTheme.scaleTextSize(textScale);
			this.previousTextScale = textScale;
		}

		if (this.mapDatabase != null) {
			MapReadResult mapReadResult = this.mapDatabase.readMapData(this.currentTile);
			processReadMapData(mapReadResult);
		}

		this.nodes = this.labelPlacement.placeLabels(this.nodes, this.pointSymbols, this.areaLabels, this.currentTile);

		this.canvasRasterer.setCanvasBitmap(bitmap);
		this.canvasRasterer.fill(this.renderTheme.getMapBackground());
		this.canvasRasterer.drawWays(this.ways);
		this.canvasRasterer.drawSymbols(this.waySymbols);
		this.canvasRasterer.drawSymbols(this.pointSymbols);
		this.canvasRasterer.drawWayNames(this.wayNames);
		this.canvasRasterer.drawNodes(this.nodes);
		this.canvasRasterer.drawNodes(this.areaLabels);

		if (mapGeneratorJob.debugSettings.drawTileFrames) {
			this.canvasRasterer.drawTileFrame();
		}

		if (mapGeneratorJob.debugSettings.drawTileCoordinates) {
			this.canvasRasterer.drawTileCoordinates(this.currentTile);
		}

		clearLists();

		return true;
	}

	/**
	 * @return the start point (may be null).
	 */
	public GeoPoint getStartPoint() {
		if (this.mapDatabase != null && this.mapDatabase.hasOpenFile()) {
			MapFileInfo mapFileInfo = this.mapDatabase.getMapFileInfo();
			if (mapFileInfo.startPosition != null) {
				return mapFileInfo.startPosition;
			}
			return mapFileInfo.boundingBox.getCenterPoint();
		}

		return null;
	}

	/**
	 * @return the start zoom level (may be null).
	 */
	public Byte getStartZoomLevel() {
		if (this.mapDatabase != null && this.mapDatabase.hasOpenFile()) {
			MapFileInfo mapFileInfo = this.mapDatabase.getMapFileInfo();
			if (mapFileInfo.startZoomLevel != null) {
				return mapFileInfo.startZoomLevel;
			}
		}

		return DEFAULT_START_ZOOM_LEVEL;
	}

	/**
	 * @return the maximum zoom level.
	 */
	public byte getZoomLevelMax() {
		return ZOOM_MAX;
	}

	@Override
	public void renderArea(Paint fill, Paint stroke, int level) {
		List<ShapePaintContainer> list = this.drawingLayers.get(level);
		list.add(new ShapePaintContainer(this.shapeContainer, fill));
		list.add(new ShapePaintContainer(this.shapeContainer, stroke));
	}

	@Override
	public void renderAreaCaption(String caption, float verticalOffset, Paint fill, Paint stroke) {
		Point centerPosition = GeometryUtils.calculateCenterOfBoundingBox(this.coordinates[0]);
		this.areaLabels.add(new PointTextContainer(caption, centerPosition.x, centerPosition.y, fill, stroke));
	}

	@Override
	public void renderAreaSymbol(Bitmap symbol) {
		Point centerPosition = GeometryUtils.calculateCenterOfBoundingBox(this.coordinates[0]);
		int halfSymbolWidth = symbol.getWidth() / 2;
		int halfSymbolHeight = symbol.getHeight() / 2;
		double pointX = centerPosition.x - halfSymbolWidth;
		double pointY = centerPosition.y - halfSymbolHeight;
		Point shiftedCenterPosition = new Point(pointX, pointY);
		this.pointSymbols.add(new SymbolContainer(symbol, shiftedCenterPosition));
	}

	@Override
	public void renderPointOfInterestCaption(String caption, float verticalOffset, Paint fill, Paint stroke) {
		this.nodes.add(new PointTextContainer(caption, this.poiPosition.x, this.poiPosition.y + verticalOffset, fill,
				stroke));
	}

	@Override
	public void renderPointOfInterestCircle(float radius, Paint fill, Paint stroke, int level) {
		List<ShapePaintContainer> list = this.drawingLayers.get(level);
		list.add(new ShapePaintContainer(new CircleContainer(this.poiPosition, radius), fill));
		list.add(new ShapePaintContainer(new CircleContainer(this.poiPosition, radius), stroke));
	}

	@Override
	public void renderPointOfInterestSymbol(Bitmap symbol) {
		int halfSymbolWidth = symbol.getWidth() / 2;
		int halfSymbolHeight = symbol.getHeight() / 2;
		double pointX = this.poiPosition.x - halfSymbolWidth;
		double pointY = this.poiPosition.y - halfSymbolHeight;
		Point shiftedCenterPosition = new Point(pointX, pointY);
		this.pointSymbols.add(new SymbolContainer(symbol, shiftedCenterPosition));
	}

	@Override
	public void renderWay(Paint stroke, int level) {
		this.drawingLayers.get(level).add(new ShapePaintContainer(this.shapeContainer, stroke));
	}

	@Override
	public void renderWaySymbol(Bitmap symbolBitmap, boolean alignCenter, boolean repeatSymbol) {
		WayDecorator.renderSymbol(symbolBitmap, alignCenter, repeatSymbol, this.coordinates, this.waySymbols);
	}

	@Override
	public void renderWayText(String textKey, Paint fill, Paint stroke) {
		WayDecorator.renderText(textKey, fill, stroke, this.coordinates, this.wayNames);
	}

	private void clearLists() {
		for (int i = this.ways.size() - 1; i >= 0; --i) {
			List<List<ShapePaintContainer>> innerWayList = this.ways.get(i);
			for (int j = innerWayList.size() - 1; j >= 0; --j) {
				innerWayList.get(j).clear();
			}
		}

		this.areaLabels.clear();
		this.nodes.clear();
		this.pointSymbols.clear();
		this.wayNames.clear();
		this.waySymbols.clear();
	}

	private void createWayLists() {
		int levels = this.renderTheme.getLevels();
		this.ways.clear();

		for (byte i = LAYERS - 1; i >= 0; --i) {
			List<List<ShapePaintContainer>> innerWayList = new ArrayList<List<ShapePaintContainer>>(levels);
			for (int j = levels - 1; j >= 0; --j) {
				innerWayList.add(new ArrayList<ShapePaintContainer>(0));
			}
			this.ways.add(innerWayList);
		}
	}

	private void processReadMapData(MapReadResult mapReadResult) {
		if (mapReadResult == null) {
			return;
		}

		for (PointOfInterest pointOfInterest : mapReadResult.pointOfInterests) {
			renderPointOfInterest(pointOfInterest);
		}

		for (Way way : mapReadResult.ways) {
			renderWay(way);
		}

		if (mapReadResult.isWater) {
			renderWaterBackground();
		}
	}

	private void renderPointOfInterest(PointOfInterest pointOfInterest) {
		this.drawingLayers = this.ways.get(getValidLayer(pointOfInterest.layer));
		this.poiPosition = scaleGeoPoint(pointOfInterest.position);
		this.renderTheme.matchNode(this, pointOfInterest.tags, this.currentTile.zoomLevel);
	}

	private void renderWaterBackground() {
		this.drawingLayers = this.ways.get(0);
		this.coordinates = WATER_TILE_COORDINATES;
		this.shapeContainer = new WayContainer(this.coordinates);
		this.renderTheme.matchClosedWay(this, Arrays.asList(TAG_NATURAL_WATER), this.currentTile.zoomLevel);
	}

	private void renderWay(Way way) {
		this.drawingLayers = this.ways.get(getValidLayer(way.layer));
		// TODO what about the label position?

		GeoPoint[][] geoPoints = way.geoPoints;
		this.coordinates = new Point[geoPoints.length][];
		for (int i = 0; i < this.coordinates.length; ++i) {
			this.coordinates[i] = new Point[geoPoints[i].length];

			for (int j = 0; j < this.coordinates[i].length; ++j) {
				this.coordinates[i][j] = scaleGeoPoint(geoPoints[i][j]);
			}
		}
		this.shapeContainer = new WayContainer(this.coordinates);

		if (GeometryUtils.isClosedWay(this.coordinates[0])) {
			this.renderTheme.matchClosedWay(this, way.tags, this.currentTile.zoomLevel);
		} else {
			this.renderTheme.matchLinearWay(this, way.tags, this.currentTile.zoomLevel);
		}
	}

	/**
	 * Converts the given GeoPoint into XY coordinates on the current tile.
	 * 
	 * @param geoPoint
	 *            the GeoPoint to convert.
	 * @return the XY coordinates on the current tile.
	 */
	private Point scaleGeoPoint(GeoPoint geoPoint) {
		double pixelX = MercatorProjection.longitudeToPixelX(geoPoint.longitude, this.currentTile.zoomLevel)
				- this.currentTile.getPixelX();
		double pixelY = MercatorProjection.latitudeToPixelY(geoPoint.latitude, this.currentTile.zoomLevel)
				- this.currentTile.getPixelY();

		return new Point((float) pixelX, (float) pixelY);
	}

	/**
	 * Sets the scale stroke factor for the given zoom level.
	 * 
	 * @param zoomLevel
	 *            the zoom level for which the scale stroke factor should be set.
	 */
	private void setScaleStrokeWidth(byte zoomLevel) {
		int zoomLevelDiff = Math.max(zoomLevel - STROKE_MIN_ZOOM_LEVEL, 0);
		this.renderTheme.scaleStrokeWidth((float) Math.pow(STROKE_INCREASE, zoomLevelDiff));
	}

	public void destroy() {
		if (this.renderTheme != null) {
			this.renderTheme.destroy();
			this.renderTheme = null;
		}
	}
}
