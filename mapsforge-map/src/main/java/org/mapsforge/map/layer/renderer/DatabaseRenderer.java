/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright 2014-2015 Ludwig M Brinckmann
 * Copyright 2014, 2015 devemux86
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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.mapsforge.core.graphics.Bitmap;
import org.mapsforge.core.graphics.Color;
import org.mapsforge.core.graphics.Display;
import org.mapsforge.core.mapelements.MapElementContainer;
import org.mapsforge.core.graphics.Position;
import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.mapelements.SymbolContainer;
import org.mapsforge.core.graphics.TileBitmap;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Point;
import org.mapsforge.core.model.Rectangle;
import org.mapsforge.core.model.Tag;
import org.mapsforge.core.model.Tile;
import org.mapsforge.core.util.MercatorProjection;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.layer.labels.TileBasedLabelStore;
import org.mapsforge.map.datastore.MapDataStore;
import org.mapsforge.map.datastore.MapReadResult;
import org.mapsforge.map.datastore.PointOfInterest;
import org.mapsforge.map.datastore.Way;
import org.mapsforge.map.rendertheme.RenderCallback;
import org.mapsforge.map.rendertheme.RenderContext;
import org.mapsforge.map.rendertheme.rule.RenderTheme;
import org.mapsforge.map.util.LayerUtil;

/**
 * The DatabaseRenderer renders map tiles by reading from a {@link org.mapsforge.map.datastore.MapDataStore}.
 */
public class DatabaseRenderer implements RenderCallback {

	private static final Byte DEFAULT_START_ZOOM_LEVEL = (byte) 12;
	private static final Logger LOGGER = Logger.getLogger(DatabaseRenderer.class.getName());
	private static final Tag TAG_NATURAL_WATER = new Tag("natural", "water");
	private static final byte ZOOM_MAX = 22;

	private static Point[] getTilePixelCoordinates(int tileSize) {
		Point[] result = new Point[5];
		result[0] = new Point(0, 0);
		result[1] = new Point(tileSize, 0);
		result[2] = new Point(tileSize, tileSize);
		result[3] = new Point(0, tileSize);
		result[4] = result[0];
		return result;
	}

	private final GraphicFactory graphicFactory;
	private final TileBasedLabelStore labelStore;
	private final MapDataStore mapDatabase;
	private final boolean renderLabels;
	private final TileCache tileCache;
	private final TileDependencies tileDependencies;

	/**
	 * Constructs a new DatabaseRenderer that will not draw labels, instead it stores the label
	 * information in the labelStore for drawing by a LabelLayer.
	 * 
	 * @param mapDatabase
	 *            the MapDatabase from which the map data will be read.
	 */
	public DatabaseRenderer(MapDataStore mapDatabase,
			GraphicFactory graphicFactory, TileBasedLabelStore labelStore) {
		this.mapDatabase = mapDatabase;
		this.graphicFactory = graphicFactory;
		this.labelStore = labelStore;
		this.renderLabels = false;
		this.tileCache = null;
		this.tileDependencies = null;
	}

	/**
	 * Constructs a new DatabaseRenderer that will draw labels onto the tiles.
	 *
	 * @param mapFile
	 *            the MapDatabase from which the map data will be read.
	 */
	public DatabaseRenderer(MapDataStore mapFile,
			GraphicFactory graphicFactory, TileCache tileCache) {
		this.mapDatabase = mapFile;
		this.graphicFactory = graphicFactory;

		this.labelStore = null;
		this.renderLabels = true;
		this.tileCache = tileCache;
		this.tileDependencies = new TileDependencies();
	}

	/**
	 * Called when a job needs to be executed.
	 * 
	 * @param rendererJob
	 *            the job that should be executed.
	 */
	public TileBitmap executeJob(RendererJob rendererJob) {

		RenderTheme renderTheme;
		try {
			renderTheme = rendererJob.renderThemeFuture.get();
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Error to retrieve render theme from future", e);
			return null;
		}

		RenderContext renderContext = null;
		try {
			renderContext = new RenderContext(renderTheme, rendererJob, new CanvasRasterer(graphicFactory));

			if (renderBitmap(renderContext)) {
				TileBitmap bitmap = null;

				if (this.mapDatabase != null) {
					MapReadResult mapReadResult = this.mapDatabase.readMapData(rendererJob.tile);
					processReadMapData(renderContext, mapReadResult);
				}

				if (!rendererJob.labelsOnly) {
					bitmap = this.graphicFactory.createTileBitmap(renderContext.rendererJob.tile.tileSize, renderContext.rendererJob.hasAlpha);
					bitmap.setTimestamp(rendererJob.mapDataStore.getDataTimestamp(renderContext.rendererJob.tile));
					renderContext.canvasRasterer.setCanvasBitmap(bitmap);
					if (!rendererJob.hasAlpha && rendererJob.displayModel.getBackgroundColor() != renderContext.renderTheme.getMapBackground()) {
						renderContext.canvasRasterer.fill(renderContext.renderTheme.getMapBackground());
					}
					renderContext.canvasRasterer.drawWays(renderContext);
				}

				if (renderLabels) {
					Set<MapElementContainer> labelsToDraw = processLabels(renderContext);
					// now draw the ways and the labels
					renderContext.canvasRasterer.drawMapElements(labelsToDraw, renderContext.rendererJob.tile);
				} else {
					// store elements for this tile in the label cache
					this.labelStore.storeMapItems(renderContext.rendererJob.tile, renderContext.labels);
				}

				if (!rendererJob.labelsOnly && renderContext.renderTheme.hasMapBackgroundOutside()) {
					// blank out all areas outside of map
					Rectangle insideArea = this.mapDatabase.boundingBox().getPositionRelativeToTile(renderContext.rendererJob.tile);
					if (!rendererJob.hasAlpha) {
						renderContext.canvasRasterer.fillOutsideAreas(renderContext.renderTheme.getMapBackgroundOutside(), insideArea);
					} else {
						renderContext.canvasRasterer.fillOutsideAreas(Color.TRANSPARENT, insideArea);
					}
				}
				return bitmap;
			}
			// outside of map area with background defined:
			return createBackgroundBitmap(renderContext);
		} finally {
			if (renderContext != null) {
				renderContext.destroy();
			}
		}
	}

	public MapDataStore getMapDatabase() {
		return this.mapDatabase;
	}

	/**
	 * @return the start point (may be null).
	 */
	public LatLong getStartPosition() {
		if (this.mapDatabase != null) {
			return this.mapDatabase.startPosition();
		}
		return null;
	}

	/**
	 * @return the start zoom level (may be null).
	 */
	public Byte getStartZoomLevel() {
		if (this.mapDatabase != null && null != this.mapDatabase.startZoomLevel()) {
			return this.mapDatabase.startZoomLevel();
		}
		return DEFAULT_START_ZOOM_LEVEL;
	}

	/**
	 * @return the maximum zoom level.
	 */
	public byte getZoomLevelMax() {
		return ZOOM_MAX;
	}

	void removeTileInProgress(Tile tile) {
		if (this.tileDependencies != null) {
			this.tileDependencies.removeTileInProgress(tile);
		}
	}

	@Override
	public void renderArea(final RenderContext renderContext, Paint fill, Paint stroke, int level, PolylineContainer way) {
		renderContext.addToCurrentDrawingLayer(level, new ShapePaintContainer(way, stroke));
		renderContext.addToCurrentDrawingLayer(level, new ShapePaintContainer(way, fill));
	}

	@Override
	public void renderAreaCaption(final RenderContext renderContext, Display display, int priority, String caption, float horizontalOffset, float verticalOffset, Paint fill, Paint stroke, Position position, int maxTextWidth, PolylineContainer way) {
		Point centerPoint = way.getCenterAbsolute().offset(horizontalOffset, verticalOffset);
		renderContext.labels.add(this.graphicFactory.createPointTextContainer(centerPoint, display, priority, caption, fill, stroke, null, position, maxTextWidth));
	}

	@Override
	public void renderAreaSymbol(final RenderContext renderContext, Display display, int priority, Bitmap symbol, PolylineContainer way) {
		Point centerPosition = way.getCenterAbsolute();

		renderContext.labels.add(new SymbolContainer(centerPosition, display, priority, symbol));
	}

	@Override
	public void renderPointOfInterestCaption(final RenderContext renderContext, Display display, int priority, String caption, float horizontalOffset, float verticalOffset, Paint fill, Paint stroke, Position position, int maxTextWidth, PointOfInterest poi) {
		Point poiPosition = MercatorProjection.getPixelAbsolute(poi.position, renderContext.rendererJob.tile.mapSize);

		renderContext.labels.add(this.graphicFactory.createPointTextContainer(poiPosition.offset(horizontalOffset, verticalOffset), display, priority, caption, fill,
				stroke, null, position, maxTextWidth));
	}

	@Override
	public void renderPointOfInterestCircle(final RenderContext renderContext, float radius, Paint fill, Paint stroke, int level, PointOfInterest poi) {
		Point poiPosition = MercatorProjection.getPixelRelativeToTile(poi.position, renderContext.rendererJob.tile);
		renderContext.addToCurrentDrawingLayer(level, new ShapePaintContainer(new CircleContainer(poiPosition, radius), stroke));
		renderContext.addToCurrentDrawingLayer(level, new ShapePaintContainer(new CircleContainer(poiPosition, radius), fill));
	}

	@Override
	public void renderPointOfInterestSymbol(final RenderContext renderContext, Display display, int priority, Bitmap symbol, PointOfInterest poi) {
		Point poiPosition = MercatorProjection.getPixelAbsolute(poi.position, renderContext.rendererJob.tile.mapSize);
		renderContext.labels.add(new SymbolContainer(poiPosition, display, priority, symbol));
	}

	@Override
	public void renderWay(final RenderContext renderContext, Paint stroke, float dy, int level, PolylineContainer way) {
		renderContext.addToCurrentDrawingLayer(level, new ShapePaintContainer(way, stroke, dy));
	}

	@Override
	public void renderWaySymbol(final RenderContext renderContext, Display display, int priority, Bitmap symbol, float dy, boolean alignCenter, boolean repeat, float repeatGap, float repeatStart, boolean rotate, PolylineContainer way) {
		WayDecorator.renderSymbol(symbol, display, priority, dy, alignCenter, repeat, repeatGap,
				repeatStart, rotate, way.getCoordinatesAbsolute(), renderContext.labels);
	}

	@Override
	public void renderWayText(final RenderContext renderContext, Display display, int priority, String textKey, float dy, Paint fill, Paint stroke, PolylineContainer way) {
		WayDecorator.renderText(way.getTile(), textKey, display, priority, dy, fill, stroke, way.getCoordinatesAbsolute(), renderContext.labels);
	}

	boolean renderBitmap(RenderContext renderContext) {
		return !renderContext.renderTheme.hasMapBackgroundOutside() || this.mapDatabase.supportsTile(renderContext.rendererJob.tile);
	}

	/**
	 * Draws a bitmap just with outside colour, used for bitmaps outside of map area.
	 * @param renderContext the RenderContext
	 * @return bitmap drawn in single colour.
	 */
	private TileBitmap createBackgroundBitmap(RenderContext renderContext) {
		TileBitmap bitmap = this.graphicFactory.createTileBitmap(renderContext.rendererJob.tile.tileSize, renderContext.rendererJob.hasAlpha);
		renderContext.canvasRasterer.setCanvasBitmap(bitmap);
		if (!renderContext.rendererJob.hasAlpha) {
			renderContext.canvasRasterer.fill(renderContext.renderTheme.getMapBackgroundOutside());
		}
		return bitmap;

	}

	private Set<MapElementContainer> processLabels(RenderContext renderContext) {
		// if we are drawing the labels per tile, we need to establish which tile-overlapping
		// elements need to be drawn.

		Set<MapElementContainer> labelsToDraw = new HashSet<MapElementContainer>();

		synchronized (tileDependencies) {
			// first we need to get the labels from the adjacent tiles if they have already been drawn
			// as those overlapping items must also be drawn on the current tile. They must be drawn regardless
			// of priority clashes as a part of them has alread been drawn.
			Set<Tile> neighbours = renderContext.rendererJob.tile.getNeighbours();
			Iterator<Tile> tileIterator = neighbours.iterator();
			Set<MapElementContainer> undrawableElements = new HashSet<MapElementContainer>();

			tileDependencies.addTileInProgress(renderContext.rendererJob.tile);
			while (tileIterator.hasNext()) {
				Tile neighbour = tileIterator.next();

				if (tileDependencies.isTileInProgress(neighbour) || tileCache.containsKey(renderContext.rendererJob.otherTile(neighbour))) {
					// if a tile has already been drawn, the elements drawn that overlap onto the
					// current tile should be in the tile dependencies, we add them to the labels that
					// need to be drawn onto this tile. For the multi-threaded renderer we also need to take
					// those tiles into account that are not yet in the TileCache: this is taken care of by the
					// set of tilesInProgress inside the TileDependencies.
					labelsToDraw.addAll(tileDependencies.getOverlappingElements(neighbour, renderContext.rendererJob.tile));

					// but we need to remove the labels for this tile that overlap onto a tile that has been drawn
					for (MapElementContainer current : renderContext.labels) {
						if (current.intersects(neighbour.getBoundaryAbsolute())) {
							undrawableElements.add(current);
						}
					}
					// since we already have the data from that tile, we do not need to get the data for
					// it, so remove it from the neighbours list.
					tileIterator.remove();
				} else {
					tileDependencies.removeTileData(neighbour);
				}
			}

			// now we remove the elements that overlap onto a drawn tile from the list of labels
			// for this tile
			renderContext.labels.removeAll(undrawableElements);

			// at this point we have two lists: one is the list of labels that must be drawn because
			// they already overlap from other tiles. The second one is currentLabels that contains
			// the elements on this tile that do not overlap onto a drawn tile. Now we sort this list and
			// remove those elements that clash in this list already.
			List<MapElementContainer> currentElementsOrdered = LayerUtil.collisionFreeOrdered(renderContext.labels);

			// now we go through this list, ordered by priority, to see which can be drawn without clashing.
			Iterator<MapElementContainer> currentMapElementsIterator = currentElementsOrdered.iterator();
			while (currentMapElementsIterator.hasNext()) {
				MapElementContainer current = currentMapElementsIterator.next();
				for (MapElementContainer label : labelsToDraw) {
					if (label.clashesWith(current)) {
						currentMapElementsIterator.remove();
						break;
					}
				}
			}

			labelsToDraw.addAll(currentElementsOrdered);

			// update dependencies, add to the dependencies list all the elements that overlap to the
			// neighbouring tiles, first clearing out the cache for this relation.
			for (Tile tile : neighbours) {
				tileDependencies.removeTileData(renderContext.rendererJob.tile, tile);
				for (MapElementContainer element : labelsToDraw) {
					if (element.intersects(tile.getBoundaryAbsolute())) {
						tileDependencies.addOverlappingElement(renderContext.rendererJob.tile, tile, element);
					}
				}
			}
		}
		return labelsToDraw;
	}

	private void processReadMapData(final RenderContext renderContext, MapReadResult mapReadResult) {
		if (mapReadResult == null) {
			return;
		}

		for (PointOfInterest pointOfInterest : mapReadResult.pointOfInterests) {
			renderPointOfInterest(renderContext, pointOfInterest);
		}

		for (Way way : mapReadResult.ways) {
			renderWay(renderContext, new PolylineContainer(way, renderContext.rendererJob.tile));
		}

		if (mapReadResult.isWater) {
			renderWaterBackground(renderContext);
		}
	}

	private void renderPointOfInterest(final RenderContext renderContext, PointOfInterest pointOfInterest) {
		renderContext.setDrawingLayers(pointOfInterest.layer);
		renderContext.renderTheme.matchNode(this, renderContext, pointOfInterest);
	}

	private void renderWaterBackground(final RenderContext renderContext) {
		renderContext.setDrawingLayers((byte) 0);
		Point[] coordinates = getTilePixelCoordinates(renderContext.rendererJob.tile.tileSize);
		PolylineContainer way = new PolylineContainer(coordinates, renderContext.rendererJob.tile, Arrays.asList(TAG_NATURAL_WATER));
		renderContext.renderTheme.matchClosedWay(this, renderContext, way);
	}

	private void renderWay(final RenderContext renderContext, PolylineContainer way) {
		renderContext.setDrawingLayers(way.getLayer());

		if (way.isClosedWay()) {
			renderContext.renderTheme.matchClosedWay(this, renderContext, way);
		} else {
			renderContext.renderTheme.matchLinearWay(this, renderContext, way);
		}
	}


}
