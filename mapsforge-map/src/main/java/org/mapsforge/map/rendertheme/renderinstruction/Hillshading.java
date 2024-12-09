/*
 * Copyright 2017 usrusr
 * Copyright 2017 oruxman
 * Copyright 2024 Sublimis
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
package org.mapsforge.map.rendertheme.renderinstruction;

import org.mapsforge.core.graphics.Canvas;
import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.graphics.HillshadingBitmap;
import org.mapsforge.core.model.Point;
import org.mapsforge.core.model.Rectangle;
import org.mapsforge.core.model.Tile;
import org.mapsforge.core.util.MercatorProjection;
import org.mapsforge.map.layer.hills.HgtCache;
import org.mapsforge.map.layer.hills.HillsRenderConfig;
import org.mapsforge.map.layer.renderer.HillshadingContainer;
import org.mapsforge.map.layer.renderer.ShapeContainer;
import org.mapsforge.map.layer.renderer.ShapePaintContainer;
import org.mapsforge.map.rendertheme.RenderContext;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents hillshading on a painter algorithm layer/level in the parsed rendertheme
 * (but without a rule, we don't need to increase waymatching complexity here)
 */
public class Hillshading {

    private static final Logger LOGGER = Logger.getLogger(Hillshading.class.getName());

    public static final int ShadingLatStep = 1;
    public static final int ShadingLonStep = 1;

    private final boolean always;
    private final int level;
    private final byte layer;
    private final byte minZoom;
    private final byte maxZoom;
    private final float magnitude;
    private final GraphicFactory mGraphicFactory;

    public Hillshading(byte minZoom, byte maxZoom, short magnitude, byte layer, boolean always, int level, GraphicFactory graphicFactory) {
        this.always = always;
        this.level = level;
        this.layer = layer;
        this.minZoom = minZoom;
        this.maxZoom = maxZoom;
        this.magnitude = magnitude;
        this.mGraphicFactory = graphicFactory;
    }

    public void render(final RenderContext renderContext, HillsRenderConfig hillsRenderConfig) {
        if (hillsRenderConfig == null) {
            if (always) {
                renderContext.setDrawingLayer(layer);
                ShapeContainer hillShape = new HillshadingContainer(null, this.magnitude, null, null);
                renderContext.addToCurrentDrawingLayer(level, new ShapePaintContainer(hillShape, null));
            }
            return;
        }

        final float effectiveMagnitude = Math.min(Math.max(0f, this.magnitude * hillsRenderConfig.getMagnitudeScaleFactor()), 255f) / 255f;
        final Tile tile = renderContext.rendererJob.tile;
        final byte zoomLevel = tile.zoomLevel;

        if (zoomLevel > maxZoom || zoomLevel < minZoom)
            return;

        final Point origin = tile.getOrigin();
        final double maptileTopLat = MercatorProjection.pixelYToLatitude(origin.y, tile.mapSize);
        final double maptileLeftLon = MercatorProjection.pixelXToLongitude(origin.x, tile.mapSize);
        final double maptileBottomLat = MercatorProjection.pixelYToLatitude(origin.y + tile.tileSize, tile.mapSize);
        double maptileRightLon = MercatorProjection.pixelXToLongitude(origin.x + tile.tileSize, tile.mapSize);
        if (maptileRightLon < maptileLeftLon)
            maptileRightLon += tile.mapSize;

        final double mapTileLatDegrees = maptileTopLat - maptileBottomLat;
        final double mapTileLonDegrees = maptileRightLon - maptileLeftLon;
        final double pxPerLat = (tile.tileSize / mapTileLatDegrees);
        final double pxPerLon = (tile.tileSize / mapTileLonDegrees);

        for (int shadingLeftLon = (int) Math.floor(maptileLeftLon); shadingLeftLon <= maptileRightLon; shadingLeftLon += ShadingLonStep) {
            for (int shadingBottomLat = (int) Math.floor(maptileBottomLat); shadingBottomLat <= maptileTopLat; shadingBottomLat += ShadingLatStep) {
                final int shadingRightLon = shadingLeftLon + ShadingLonStep;
                final int shadingTopLat = shadingBottomLat + ShadingLatStep;

                HillshadingBitmap shadingTile = null;
                try {
                    shadingTile = hillsRenderConfig.getShadingTile(shadingBottomLat, shadingLeftLon, pxPerLat, pxPerLon);
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, e.toString(), e);
                }
                if (shadingTile == null) {
                    if (!always) {
                        continue;
                    }
                }

                final int padding;
                final int shadingInnerWidth;
                final int shadingInnerHeight;
                if (shadingTile != null) {
                    padding = shadingTile.getPadding();
                    shadingInnerWidth = shadingTile.getWidth() - 2 * padding;
                    shadingInnerHeight = shadingTile.getHeight() - 2 * padding;
                } else {
                    // dummy values to not confuse the maptile calculations
                    padding = 0;
                    shadingInnerWidth = 0;
                    shadingInnerHeight = 0;
                }

                // shading tile subset if it fully fits inside map tile
                double shadingSubrectTop = padding;
                double shadingSubrectLeft = padding;
                double shadingSubrectRight = shadingSubrectLeft + shadingInnerWidth;
                double shadingSubrectBottom = shadingSubrectTop + shadingInnerHeight;

                // map tile subset if it fully fits inside shading tile
                double maptileSubrectLeft = 0;
                double maptileSubrectTop = 0;
                double maptileSubrectRight = tile.tileSize;
                double maptileSubrectBottom = tile.tileSize;

                // find the intersection between map tile and shading tile in earth coordinates and determine the pixel

                if (shadingBottomLat > maptileBottomLat) {
                    // Shading tile ends in map tile
                    maptileSubrectBottom = Math.round(MercatorProjection.latitudeToPixelY(shadingBottomLat, tile.mapSize)) - origin.y;
                    mergeNeighbor(shadingTile, padding, hillsRenderConfig, HillshadingBitmap.Border.SOUTH, shadingBottomLat, shadingLeftLon, pxPerLat, pxPerLon);
                } else if (shadingBottomLat < maptileBottomLat) {
                    // Map tile ends in shading tile
                    shadingSubrectBottom -= shadingInnerHeight * ((maptileBottomLat - shadingBottomLat) / ShadingLatStep);
                }

                if (shadingTopLat < maptileTopLat) {
                    // Shading tile ends in map tile
                    maptileSubrectTop = Math.round(MercatorProjection.latitudeToPixelY(shadingTopLat, tile.mapSize)) - origin.y;
                    mergeNeighbor(shadingTile, padding, hillsRenderConfig, HillshadingBitmap.Border.NORTH, shadingBottomLat, shadingLeftLon, pxPerLat, pxPerLon);
                } else if (shadingTopLat > maptileTopLat) {
                    // Map tile ends in shading tile
                    shadingSubrectTop += shadingInnerHeight * ((shadingTopLat - maptileTopLat) / ShadingLatStep);
                }

                if (shadingLeftLon > maptileLeftLon) {
                    // Shading tile ends in map tile
                    maptileSubrectLeft = Math.round(MercatorProjection.longitudeToPixelX(shadingLeftLon, tile.mapSize)) - origin.x;
                    mergeNeighbor(shadingTile, padding, hillsRenderConfig, HillshadingBitmap.Border.WEST, shadingBottomLat, shadingLeftLon, pxPerLat, pxPerLon);
                } else if (shadingLeftLon < maptileLeftLon) {
                    // Map tile ends in shading tile
                    shadingSubrectLeft += shadingInnerWidth * ((maptileLeftLon - shadingLeftLon) / ShadingLonStep);
                }

                if (shadingRightLon < maptileRightLon) {
                    // Shading tile ends in map tile
                    maptileSubrectRight = Math.round(MercatorProjection.longitudeToPixelX(shadingRightLon, tile.mapSize)) - origin.x;
                    mergeNeighbor(shadingTile, padding, hillsRenderConfig, HillshadingBitmap.Border.EAST, shadingBottomLat, shadingLeftLon, pxPerLat, pxPerLon);
                } else if (shadingRightLon > maptileRightLon) {
                    // Map tile ends in shading tile
                    shadingSubrectRight -= shadingInnerWidth * ((shadingRightLon - maptileRightLon) / ShadingLonStep);
                }

                final Rectangle hillsRect = (shadingTile == null) ? null : new Rectangle(shadingSubrectLeft, shadingSubrectTop, shadingSubrectRight, shadingSubrectBottom);
                final Rectangle maptileRect = new Rectangle(maptileSubrectLeft, maptileSubrectTop, maptileSubrectRight, maptileSubrectBottom);
                final ShapeContainer hillShape = new HillshadingContainer(shadingTile, effectiveMagnitude, hillsRect, maptileRect);

                renderContext.setDrawingLayer(layer);
                renderContext.addToCurrentDrawingLayer(level, new ShapePaintContainer(hillShape, null));
            }
        }
    }

    protected HillshadingBitmap getNeighbor(HillsRenderConfig hillsRenderConfig, HillshadingBitmap.Border border, int shadingBottomLat, int shadingLeftLon, double pxPerLat, double pxPerLon) {

        HillshadingBitmap neighbor = null;
        try {
            switch (border) {
                case NORTH:
                    neighbor = hillsRenderConfig.getShadingTile(shadingBottomLat + ShadingLatStep, shadingLeftLon, pxPerLat, pxPerLon);
                    break;
                case SOUTH:
                    neighbor = hillsRenderConfig.getShadingTile(shadingBottomLat - ShadingLatStep, shadingLeftLon, pxPerLat, pxPerLon);
                    break;
                case EAST:
                    neighbor = hillsRenderConfig.getShadingTile(shadingBottomLat, shadingLeftLon + ShadingLonStep, pxPerLat, pxPerLon);
                    break;
                case WEST:
                    neighbor = hillsRenderConfig.getShadingTile(shadingBottomLat, shadingLeftLon - ShadingLonStep, pxPerLat, pxPerLon);
                    break;
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, e.toString(), e);
        }

        return neighbor;
    }

    protected synchronized void mergePaddingOnBitmap(HillshadingBitmap center, HillshadingBitmap neighbor, HillshadingBitmap.Border border, int padding) {
        if (neighbor != null && padding > 0) {
            final Canvas copyCanvas = mGraphicFactory.createCanvas();

            HgtCache.mergeSameSized(center, neighbor, border, padding, copyCanvas);
        }
    }

    protected void mergeNeighbor(HillshadingBitmap monoBitmap, int padding, HillsRenderConfig hillsRenderConfig, HillshadingBitmap.Border border, int shadingBottomLat, int shadingLeftLon, double pxPerLat, double pxPerLon) {
        if (monoBitmap != null && padding > 0) {
            final HillshadingBitmap neighbor = getNeighbor(hillsRenderConfig, border, shadingBottomLat, shadingLeftLon, pxPerLat, pxPerLon);
            mergePaddingOnBitmap(monoBitmap, neighbor, border, padding);
        }
    }
}
