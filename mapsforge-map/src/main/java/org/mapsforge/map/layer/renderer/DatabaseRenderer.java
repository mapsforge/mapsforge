/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright © 2014 Ludwig M Brinckmann
 * Copyright © 2014 devemux86
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
package org.mapsforge.map.layer.renderer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.ParserConfigurationException;

import org.mapsforge.core.graphics.Bitmap;
import org.mapsforge.core.mapelements.MapElementContainer;
import org.mapsforge.core.graphics.Position;
import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.mapelements.SymbolContainer;
import org.mapsforge.core.graphics.TileBitmap;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Point;
import org.mapsforge.core.model.Tag;
import org.mapsforge.core.model.Tile;
import org.mapsforge.core.util.MercatorProjection;
import org.mapsforge.map.layer.labels.TileBasedLabelStore;
import org.mapsforge.map.model.DisplayModel;
import org.mapsforge.map.reader.MapDatabase;
import org.mapsforge.map.reader.MapReadResult;
import org.mapsforge.map.reader.PointOfInterest;
import org.mapsforge.map.reader.Way;
import org.mapsforge.map.reader.header.MapFileInfo;
import org.mapsforge.map.rendertheme.RenderCallback;
import org.mapsforge.map.rendertheme.XmlRenderTheme;
import org.mapsforge.map.rendertheme.rule.RenderTheme;
import org.mapsforge.map.rendertheme.rule.RenderThemeHandler;
import org.mapsforge.map.util.LayerUtil;
import org.xml.sax.SAXException;

/**
 * The DatabaseRenderer renders map tiles by reading from a {@link MapDatabase}.
 *
 * Up to version 0.4.x the DatabaseRenderer was responsible for rendering ways, areas as
 * well as labels. However, the label placement algorithm suffered from multiple problems,
 * such as clipped labels at tile bounds.
 *
 *
 */
public class DatabaseRenderer implements RenderCallback {

	private static final Byte DEFAULT_START_ZOOM_LEVEL = Byte.valueOf((byte) 12);
	private static final byte LAYERS = 11;
	private static final Logger LOGGER = Logger.getLogger(DatabaseRenderer.class.getName());
	private static final double STROKE_INCREASE = 1.5;
	private static final byte STROKE_MIN_ZOOM_LEVEL = 12;
	private static final Tag TAG_NATURAL_WATER = new Tag("natural", "water");
	private static final byte ZOOM_MAX = 22;

	private static List<Point> getTilePixelCoordinates(int tileSize) {
		List result = new ArrayList(5);
		result.add(new Point(0, 0));
		result.add(new Point(tileSize, 0));
		result.add(new Point(tileSize, tileSize));
		result.add(new Point(0, tileSize));
		result.add(result.get(0));
		return result;
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

	private final CanvasRasterer canvasRasterer;
	private List<MapElementContainer> currentMapElementContainers;
	private List<List<ShapePaintContainer>> drawingLayers;
	private final GraphicFactory graphicFactory;
	private final TileBasedLabelStore labelStore;
	private final MapDatabase mapDatabase;
	private XmlRenderTheme previousJobTheme;
	private RenderTheme renderTheme;
	private List<List<List<ShapePaintContainer>>> ways;

	/**
	 * Constructs a new DatabaseRenderer.
	 * 
	 * @param mapDatabase
	 *            the MapDatabase from which the map data will be read.
	 */
	public DatabaseRenderer(MapDatabase mapDatabase, GraphicFactory graphicFactory, TileBasedLabelStore labelStore) {
		this.mapDatabase = mapDatabase;
		this.graphicFactory = graphicFactory;

		this.canvasRasterer = new CanvasRasterer(graphicFactory);
		this.labelStore = labelStore;
	}

	public void destroy() {
		this.canvasRasterer.destroy();
		// there is a chance that the renderer is being destroyed from the
		// DestroyThread before the rendertheme has been completely created
		// and assigned. If that happens bitmap memory held by the
		// RenderThemeHandler
		// will be leaked
		if (this.renderTheme != null) {
			this.renderTheme.destroy();
		} else {
			LOGGER.log(Level.SEVERE, "RENDERTHEME Could not destroy RenderTheme");
		}
	}

	/**
	 * Called when a job needs to be executed.
	 * 
	 * @param rendererJob
	 *            the job that should be executed.
	 */
	public TileBitmap executeJob(RendererJob rendererJob) {
		final int tileSize = rendererJob.tile.tileSize;
		final byte zoomLevel = rendererJob.tile.zoomLevel;

		this.currentMapElementContainers = new LinkedList<MapElementContainer>();

		XmlRenderTheme jobTheme = rendererJob.xmlRenderTheme;
		if (!jobTheme.equals(this.previousJobTheme)) {
			this.renderTheme = getRenderTheme(jobTheme, rendererJob.displayModel);
			if (this.renderTheme == null) {
				this.previousJobTheme = null;
				return null;
			}
			this.ways = createWayLists();
			this.previousJobTheme = jobTheme;
		}

		setScaleStrokeWidth(zoomLevel);
		this.renderTheme.scaleTextSize(rendererJob.textScale);

		if (this.mapDatabase != null) {
			MapReadResult mapReadResult = this.mapDatabase.readMapData(rendererJob.tile);
			processReadMapData(ways, mapReadResult, rendererJob.tile);
		}

		TileBitmap bitmap = null;
		if (!rendererJob.labelsOnly) {
			bitmap = this.graphicFactory.createTileBitmap(tileSize,
					rendererJob.hasAlpha);
			this.canvasRasterer.setCanvasBitmap(bitmap);
			if (rendererJob.displayModel.getBackgroundColor() != this.renderTheme.getMapBackground()) {
				this.canvasRasterer.fill(rendererJob.hasAlpha ? 0 : this.renderTheme.getMapBackground());
			}
			this.canvasRasterer.drawWays(ways, rendererJob.tile);
		}



		if (labelStore != null) {
			this.labelStore.storeMapItems(rendererJob.tile, LayerUtil.collisionFreeOrdered(this.currentMapElementContainers));
		}

		// clear way list
		for (int i = this.ways.size() - 1; i >= 0; --i) {
			List<List<ShapePaintContainer>> innerWayList = this.ways.get(i);
			for (int j = innerWayList.size() - 1; j >= 0; --j) {
				innerWayList.get(j).clear();
			}
		}

		return bitmap;
	}

	public MapDatabase getMapDatabase() {
		return this.mapDatabase;
	}

	/**
	 * @return the start point (may be null).
	 */
	public LatLong getStartPoint() {
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
	public void renderArea(PolylineContainer way, Paint fill, Paint stroke, int level) {
		List<ShapePaintContainer> list = this.drawingLayers.get(level);
		list.add(new ShapePaintContainer(way, stroke));
		list.add(new ShapePaintContainer(way, fill));
	}

	@Override
	public void renderAreaCaption(PolylineContainer way, int priority, String caption, float horizontalOffset, float verticalOffset,
	                              Paint fill, Paint stroke, Position position, int maxTextWidth) {
		Point centerPoint = way.getCenterAbsolute().offset(horizontalOffset, verticalOffset);
		this.currentMapElementContainers.add(this.graphicFactory.createPointTextContainer(centerPoint, priority, caption, fill, stroke, null, position, maxTextWidth));
	}

	@Override
	public void renderAreaSymbol(PolylineContainer way, int priority, Bitmap symbol) {
		Point centerPosition = way.getCenterAbsolute();

		this.currentMapElementContainers.add(new SymbolContainer(centerPosition, priority, symbol));
	}

	@Override
	public void renderPointOfInterestCaption(PointOfInterest poi, int priority, String caption, float horizontalOffset, float verticalOffset,
	                                         Paint fill, Paint stroke, Position position, int maxTextWidth, Tile tile) {
		Point poiPosition = MercatorProjection.getPixelAbsolute(poi.position, tile.zoomLevel, tile.tileSize);

		this.currentMapElementContainers.add(this.graphicFactory.createPointTextContainer(poiPosition.offset(horizontalOffset, verticalOffset), priority, caption, fill,
				stroke, null, position, maxTextWidth));
	}

	@Override
	public void renderPointOfInterestCircle(PointOfInterest poi, float radius, Paint fill, Paint stroke, int level, Tile tile) {
		List<ShapePaintContainer> list = this.drawingLayers.get(level);
		Point poiPosition = MercatorProjection.getPixelRelativeToTile(poi.position, tile);
		list.add(new ShapePaintContainer(new CircleContainer(poiPosition, radius), stroke));
		list.add(new ShapePaintContainer(new CircleContainer(poiPosition, radius), fill));
	}

	@Override
	public void renderPointOfInterestSymbol(PointOfInterest poi, int priority, Bitmap symbol, Tile tile) {
		Point poiPosition = MercatorProjection.getPixelAbsolute(poi.position, tile.zoomLevel, tile.tileSize);
		this.currentMapElementContainers.add(new SymbolContainer(poiPosition, priority, symbol));
	}

	@Override
	public void renderWay(PolylineContainer way, Paint stroke, float dy, int level) {
		this.drawingLayers.get(level).add(new ShapePaintContainer(way, stroke, dy));
	}

	@Override
	public void renderWaySymbol(PolylineContainer way, int priority, Bitmap symbol, float dy, boolean alignCenter, boolean repeat,
	                     float repeatGap, float repeatStart, boolean rotate) {
		WayDecorator.renderSymbol(symbol, priority, dy, alignCenter, repeat, repeatGap,
				repeatStart, rotate, way.getCoordinatesAbsolute(), this.currentMapElementContainers);
	}

	@Override
	public void renderWayText(PolylineContainer way, int priority, String textKey, float dy, Paint fill, Paint stroke) {
		WayDecorator.renderText(textKey, priority, dy, fill, stroke, way.getCoordinatesAbsolute(), this.currentMapElementContainers);
	}


	private List<List<List<ShapePaintContainer>>> createWayLists() {
		List<List<List<ShapePaintContainer>>> ways = new ArrayList<List<List<ShapePaintContainer>>>(LAYERS);
		int levels = this.renderTheme.getLevels();

		for (byte i = LAYERS - 1; i >= 0; --i) {
			List<List<ShapePaintContainer>> innerWayList = new ArrayList<List<ShapePaintContainer>>(levels);
			for (int j = levels - 1; j >= 0; --j) {
				innerWayList.add(new ArrayList<ShapePaintContainer>(0));
			}
			ways.add(innerWayList);
		}
		return ways;
	}

	private RenderTheme getRenderTheme(XmlRenderTheme jobTheme, DisplayModel displayModel) {
		try {
			return RenderThemeHandler.getRenderTheme(this.graphicFactory, displayModel, jobTheme);
		} catch (ParserConfigurationException e) {
			LOGGER.log(Level.SEVERE, null, e);
		} catch (SAXException e) {
			LOGGER.log(Level.SEVERE, null, e);
		} catch (IOException e) {
			LOGGER.log(Level.SEVERE, null, e);
		}
		return null;
	}

	private void processReadMapData(final List<List<List<ShapePaintContainer>>> ways, MapReadResult mapReadResult, Tile tile) {
		if (mapReadResult == null) {
			return;
		}

		for (PointOfInterest pointOfInterest : mapReadResult.pointOfInterests) {
			renderPointOfInterest(ways, pointOfInterest, tile);
		}

		for (Way way : mapReadResult.ways) {
			renderWay(ways, new PolylineContainer(way, tile));
		}

		if (mapReadResult.isWater) {
			renderWaterBackground(ways, tile);
		}
	}

	private void renderPointOfInterest(final List<List<List<ShapePaintContainer>>> ways, PointOfInterest pointOfInterest, Tile tile) {
		this.drawingLayers = ways.get(getValidLayer(pointOfInterest.layer));
		this.renderTheme.matchNode(this, pointOfInterest, tile);
	}

	private void renderWaterBackground(final List<List<List<ShapePaintContainer>>> ways, Tile tile) {
		this.drawingLayers = ways.get(0);
		List<Point> coordinates = getTilePixelCoordinates(tile.tileSize);
		PolylineContainer way = new PolylineContainer(coordinates, tile, Arrays.asList(TAG_NATURAL_WATER));
		this.renderTheme.matchClosedWay(this, way);
	}

	private void renderWay(final List<List<List<ShapePaintContainer>>> ways, PolylineContainer way) {
		this.drawingLayers = ways.get(getValidLayer(way.getLayer()));

		if (way.isClosedWay()) {
			this.renderTheme.matchClosedWay(this, way);
		} else {
			this.renderTheme.matchLinearWay(this, way);
		}
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

}
