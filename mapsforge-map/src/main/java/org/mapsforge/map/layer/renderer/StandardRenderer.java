/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright 2014-2015 Ludwig M Brinckmann
 * Copyright 2014-2016 devemux86
 * Copyright 2017 usrusr
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

import org.mapsforge.core.graphics.*;
import org.mapsforge.core.mapelements.SymbolContainer;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Point;
import org.mapsforge.core.model.Rectangle;
import org.mapsforge.core.model.Tag;
import org.mapsforge.core.util.MercatorProjection;
import org.mapsforge.map.datastore.MapDataStore;
import org.mapsforge.map.datastore.MapReadResult;
import org.mapsforge.map.datastore.PointOfInterest;
import org.mapsforge.map.datastore.Way;
import org.mapsforge.map.layer.hills.HillsRenderConfig;
import org.mapsforge.map.rendertheme.RenderCallback;
import org.mapsforge.map.rendertheme.RenderContext;

import java.util.Collections;

/**
 * The DatabaseRenderer renders map tiles by reading from a {@link MapDataStore}.
 */
public class StandardRenderer implements RenderCallback {

    private static final Byte DEFAULT_START_ZOOM_LEVEL = (byte) 12;
    private static final Tag TAG_NATURAL_WATER = new Tag("natural", "water");
    private static final byte ZOOM_MAX = 22;

    public final GraphicFactory graphicFactory;
    public final HillsRenderConfig hillsRenderConfig;
    public final MapDataStore mapDataStore;
    private final boolean renderLabels;

    /**
     * Constructs a new StandardRenderer (without hillshading).
     *
     * @param mapDataStore the MapDataStore from which the map data will be read.
     */
    public StandardRenderer(MapDataStore mapDataStore,
                            GraphicFactory graphicFactory,
                            boolean renderLabels) {
        this(mapDataStore, graphicFactory, renderLabels, null);
    }

    /**
     * Constructs a new StandardRenderer.
     *
     * @param mapDataStore      the MapDataStore from which the map data will be read.
     * @param hillsRenderConfig optional relief shading support.
     */
    public StandardRenderer(MapDataStore mapDataStore,
                            GraphicFactory graphicFactory,
                            boolean renderLabels, HillsRenderConfig hillsRenderConfig) {
        this.mapDataStore = mapDataStore;
        this.graphicFactory = graphicFactory;
        this.renderLabels = renderLabels;
        this.hillsRenderConfig = hillsRenderConfig;
    }

    /**
     * @return the start point (may be null).
     */
    public LatLong getStartPosition() {
        if (this.mapDataStore != null) {
            return this.mapDataStore.startPosition();
        }
        return null;
    }

    /**
     * @return the start zoom level (may be null).
     */
    public Byte getStartZoomLevel() {
        if (this.mapDataStore != null && null != this.mapDataStore.startZoomLevel()) {
            return this.mapDataStore.startZoomLevel();
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
    public void renderArea(final RenderContext renderContext, Paint fill, Paint stroke, int level, PolylineContainer way) {
        renderContext.addToCurrentDrawingLayer(level, new ShapePaintContainer(way, stroke));
        renderContext.addToCurrentDrawingLayer(level, new ShapePaintContainer(way, fill));
    }

    @Override
    public void renderAreaCaption(final RenderContext renderContext, Display display, int priority, String caption, float horizontalOffset, float verticalOffset, Paint fill, Paint stroke, Position position, int maxTextWidth, PolylineContainer way) {
        if (renderLabels) {
            Point centerPoint = way.getCenterAbsolute().offset(horizontalOffset, verticalOffset);
            renderContext.labels.add(this.graphicFactory.createPointTextContainer(centerPoint, display, priority, caption, fill, stroke, null, position, maxTextWidth));
        }
    }

    @Override
    public void renderAreaSymbol(final RenderContext renderContext, Display display, int priority, Bitmap symbol, PolylineContainer way) {
        if (renderLabels) {
            Point centerPosition = way.getCenterAbsolute();
            renderContext.labels.add(new SymbolContainer(centerPosition, display, priority, null, symbol));
        }
    }

    @Override
    public void renderPointOfInterestCaption(final RenderContext renderContext, Display display, int priority, String caption, float horizontalOffset, float verticalOffset, Paint fill, Paint stroke, Position position, int maxTextWidth, PointOfInterest poi) {
        if (renderLabels) {
            Point poiPosition = MercatorProjection.getPixelAbsolute(poi.position, renderContext.rendererJob.tile.mapSize);

            renderContext.labels.add(this.graphicFactory.createPointTextContainer(poiPosition.offset(horizontalOffset, verticalOffset), display, priority, caption, fill,
                    stroke, null, position, maxTextWidth));
        }
    }

    @Override
    public void renderPointOfInterestCircle(final RenderContext renderContext, float radius, Paint fill, Paint stroke, int level, PointOfInterest poi) {
        Point poiPosition = MercatorProjection.getPixelRelativeToTile(poi.position, renderContext.rendererJob.tile);
        renderContext.addToCurrentDrawingLayer(level, new ShapePaintContainer(new CircleContainer(poiPosition, radius), stroke));
        renderContext.addToCurrentDrawingLayer(level, new ShapePaintContainer(new CircleContainer(poiPosition, radius), fill));
    }

    @Override
    public void renderPointOfInterestSymbol(final RenderContext renderContext, Display display, int priority, Rectangle boundary, Bitmap symbol, PointOfInterest poi) {
        if (renderLabels) {
            Point poiPosition = MercatorProjection.getPixelAbsolute(poi.position, renderContext.rendererJob.tile.mapSize);
            renderContext.labels.add(new SymbolContainer(poiPosition, display, priority, boundary, symbol));
        }
    }

    @Override
    public void renderWay(final RenderContext renderContext, Paint stroke, float dy, int level, PolylineContainer way) {
        renderContext.addToCurrentDrawingLayer(level, new ShapePaintContainer(way, stroke, dy));
    }

    @Override
    public void renderWaySymbol(final RenderContext renderContext, Display display, int priority, Bitmap symbol, float dy, Rectangle boundary, boolean repeat, float repeatGap, float repeatStart, boolean rotate, PolylineContainer way) {
        if (renderLabels) {
            WayDecorator.renderSymbol(symbol, display, priority, dy, boundary, repeat, repeatGap,
                    repeatStart, rotate, way.getCoordinatesAbsolute(), renderContext.labels);
        }
    }

    @Override
    public void renderWayText(final RenderContext renderContext, Display display, int priority, String textKey, float dy, Paint fill, Paint stroke,
                              boolean repeat, float repeatGap, float repeatStart, boolean rotate, PolylineContainer way) {
        if (renderLabels) {
            WayDecorator.renderText(graphicFactory, way.getUpperLeft(), way.getLowerRight(), textKey, display, priority, dy, fill, stroke,
                    repeat, repeatGap, repeatStart, rotate, way.getCoordinatesAbsolute(), renderContext.labels);
        }
    }

    boolean renderBitmap(RenderContext renderContext) {
        return !renderContext.renderTheme.hasMapBackgroundOutside() || this.mapDataStore.supportsTile(renderContext.rendererJob.tile);
    }

    protected void renderPointOfInterest(final RenderContext renderContext, PointOfInterest pointOfInterest) {
        renderContext.setDrawingLayers(pointOfInterest.layer);
        renderContext.renderTheme.matchNode(this, renderContext, pointOfInterest);
    }

    protected void renderWaterBackground(final RenderContext renderContext) {
        renderContext.setDrawingLayers((byte) 0);
        Point[] coordinates = getTilePixelCoordinates(renderContext.rendererJob.tile.tileSize);
        Point tileOrigin = renderContext.rendererJob.tile.getOrigin();
        for (int i = 0; i < coordinates.length; i++) {
            coordinates[i] = coordinates[i].offset(tileOrigin.x, tileOrigin.y);
        }
        PolylineContainer way = new PolylineContainer(coordinates, renderContext.rendererJob.tile, renderContext.rendererJob.tile, Collections.singletonList(TAG_NATURAL_WATER));
        renderContext.renderTheme.matchClosedWay(this, renderContext, way);
    }

    protected void renderWay(final RenderContext renderContext, PolylineContainer way) {
        renderContext.setDrawingLayers(way.getLayer());

        if (way.isClosedWay()) {
            renderContext.renderTheme.matchClosedWay(this, renderContext, way);
        } else {
            renderContext.renderTheme.matchLinearWay(this, renderContext, way);
        }
    }

    protected void processReadMapData(final RenderContext renderContext, MapReadResult mapReadResult) {
        if (mapReadResult == null) {
            return;
        }

        for (PointOfInterest pointOfInterest : mapReadResult.pointOfInterests) {
            renderPointOfInterest(renderContext, pointOfInterest);
        }

        for (Way way : mapReadResult.ways) {
            renderWay(renderContext, new PolylineContainer(way, renderContext.rendererJob.tile, renderContext.rendererJob.tile));
        }

        if (mapReadResult.isWater) {
            renderWaterBackground(renderContext);
        }
    }

    private static Point[] getTilePixelCoordinates(int tileSize) {
        Point[] result = new Point[5];
        result[0] = new Point(0, 0);
        result[1] = new Point(tileSize, 0);
        result[2] = new Point(tileSize, tileSize);
        result[3] = new Point(0, tileSize);
        result[4] = result[0];
        return result;
    }

}
