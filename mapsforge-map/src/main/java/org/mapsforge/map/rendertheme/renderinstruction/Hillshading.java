/*
 * Copyright 2017 usrusr
 * Copyright 2017 oruxman
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

import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.graphics.HillshadingBitmap;
import org.mapsforge.core.model.Point;
import org.mapsforge.core.model.Rectangle;
import org.mapsforge.core.model.Tile;
import org.mapsforge.core.util.MercatorProjection;
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

    private boolean always;
    private final int level;
    private final byte layer;
    private final byte minZoom;
    private final byte maxZoom;
    private final float magnitude;

    public Hillshading(byte minZoom, byte maxZoom, short magnitude, byte layer, boolean always, int level, GraphicFactory graphicFactory) {
        this.always = always;
        this.level = level;
        this.layer = layer;
        this.minZoom = minZoom;
        this.maxZoom = maxZoom;
        this.magnitude = magnitude;
    }

    public void render(final RenderContext renderContext, HillsRenderConfig hillsRenderConfig) {
        if (hillsRenderConfig == null) {
            if (always) {
                renderContext.setDrawingLayers(layer);
                ShapeContainer hillShape = new HillshadingContainer(null, this.magnitude, null, null);
                renderContext.addToCurrentDrawingLayer(level, new ShapePaintContainer(hillShape, null));
            }
            return;
        }
        float effectiveMagnitude = Math.min(Math.max(0f, this.magnitude * hillsRenderConfig.getMaginuteScaleFactor()), 255f) / 255f;
        Tile tile = renderContext.rendererJob.tile;
        byte zoomLevel = tile.zoomLevel;
        if (zoomLevel > maxZoom || zoomLevel < minZoom)
            return;

        Point origin = tile.getOrigin();
        double maptileTopLat = MercatorProjection.pixelYToLatitude((long) origin.y, tile.mapSize);
        double maptileLeftLng = MercatorProjection.pixelXToLongitude((long) origin.x, tile.mapSize);

        double maptileBottomLat = MercatorProjection.pixelYToLatitude((long) origin.y + tile.tileSize, tile.mapSize);
        double maptileRightLng = MercatorProjection.pixelXToLongitude((long) origin.x + tile.tileSize, tile.mapSize);

        double mapTileLatDegrees = maptileTopLat - maptileBottomLat;
        double mapTileLngDegrees = maptileRightLng - maptileLeftLng;
        double pxPerLat = (tile.tileSize / mapTileLatDegrees);
        double pxPerLng = (tile.tileSize / mapTileLngDegrees);

        if (maptileRightLng < maptileLeftLng)
            maptileRightLng += tile.mapSize;


        int shadingLngStep = 1;
        int shadingLatStep = 1;
        for (int shadingLeftLng = (int) Math.floor(maptileLeftLng); shadingLeftLng <= maptileRightLng; shadingLeftLng += shadingLngStep) {
            for (int shadingBottomLat = (int) Math.floor(maptileBottomLat); shadingBottomLat <= maptileTopLat; shadingBottomLat += shadingLatStep) {
                int shadingRightLng = shadingLeftLng + 1;
                int shadingTopLat = shadingBottomLat + 1;

                HillshadingBitmap shadingTile = null;
                try {
                    shadingTile = hillsRenderConfig.getShadingTile(shadingBottomLat, shadingLeftLng, pxPerLat, pxPerLng);
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, e.getMessage(), e);
                }
                if (shadingTile == null) {
                    if (!always) {
                        continue;
                    }
                }
                double shadingPixelOffset = 0d;


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
                    shadingInnerWidth = 1;
                    shadingInnerHeight = 1;
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
                if (shadingTopLat > maptileTopLat) { // map tile ends in shading tile
                    shadingSubrectTop = padding + shadingInnerHeight * ((shadingTopLat - maptileTopLat) / shadingLatStep);
                } else if (maptileTopLat > shadingTopLat) {
                    maptileSubrectTop = MercatorProjection.latitudeToPixelY(shadingTopLat + (shadingPixelOffset / shadingInnerHeight), tile.mapSize) - origin.y;
                }
                if (shadingBottomLat < maptileBottomLat) { // map tile ends in shading tile
                    shadingSubrectBottom = padding + shadingInnerHeight - shadingInnerHeight * ((maptileBottomLat - shadingBottomLat) / shadingLatStep);
                } else if (maptileBottomLat < shadingBottomLat) {
                    maptileSubrectBottom = MercatorProjection.latitudeToPixelY(shadingBottomLat + (shadingPixelOffset / shadingInnerHeight), tile.mapSize) - origin.y;
                }
                if (shadingLeftLng < maptileLeftLng) { // map tile ends in shading tile
                    shadingSubrectLeft = padding + shadingInnerWidth * ((maptileLeftLng - shadingLeftLng) / shadingLngStep);
                } else if (maptileLeftLng < shadingLeftLng) {
                    maptileSubrectLeft = MercatorProjection.longitudeToPixelX(shadingLeftLng + (shadingPixelOffset / shadingInnerWidth), tile.mapSize) - origin.x;
                }
                if (shadingRightLng > maptileRightLng) { // map tile ends in shading tile
                    shadingSubrectRight = padding + shadingInnerWidth - shadingInnerWidth * ((shadingRightLng - maptileRightLng) / shadingLngStep);
                } else if (maptileRightLng > shadingRightLng) {
                    maptileSubrectRight = MercatorProjection.longitudeToPixelX(shadingRightLng + (shadingPixelOffset / shadingInnerHeight), tile.mapSize) - origin.x;
                }

                Rectangle hillsRect = (shadingTile == null) ? null : new Rectangle(shadingSubrectLeft, shadingSubrectTop, shadingSubrectRight, shadingSubrectBottom);
                Rectangle maptileRect = new Rectangle(maptileSubrectLeft, maptileSubrectTop, maptileSubrectRight, maptileSubrectBottom);
                ShapeContainer hillShape = new HillshadingContainer(shadingTile, effectiveMagnitude, hillsRect, maptileRect);

                renderContext.setDrawingLayers(layer);
                renderContext.addToCurrentDrawingLayer(level, new ShapePaintContainer(hillShape, null));
            }
        }
    }
}
