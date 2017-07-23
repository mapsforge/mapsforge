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

import org.mapsforge.core.graphics.Bitmap;
import org.mapsforge.core.graphics.GraphicFactory;
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

    private final GraphicFactory graphicFactory;
    private final int level;
    private final byte layer;
    private final byte minZoom;
    private final byte maxZoom;
    private final float magnitude;

    public Hillshading(byte minZoom, byte maxZoom, short magnitude, byte layer, int level, GraphicFactory graphicFactory) {
        this.graphicFactory = graphicFactory;
        this.level = level;
        this.layer = layer;
        this.minZoom = minZoom;
        this.maxZoom = maxZoom;
        this.magnitude = Math.min(Math.max(0, magnitude), 255) / 255f;
    }

    public void render(final RenderContext renderContext, HillsRenderConfig hillsRenderConfig) {
        if (hillsRenderConfig == null) {
            return;
        }
        Tile tile = renderContext.rendererJob.tile;
        byte zoomLevel = tile.zoomLevel;
        if (zoomLevel > maxZoom || zoomLevel < minZoom)
            return;

        Point origin = tile.getOrigin();
        double maptileTopLat = MercatorProjection.pixelYToLatitude((long) origin.y, tile.mapSize);
        double maptileLeftLng = MercatorProjection.pixelXToLongitude((long) origin.x, tile.mapSize);

        double maptileBottomLat = MercatorProjection.pixelYToLatitude((long) origin.y + tile.tileSize, tile.mapSize);
        double maptileRightLng = MercatorProjection.pixelXToLongitude((long) origin.x + tile.tileSize, tile.mapSize);
        if (maptileRightLng < maptileLeftLng)
            maptileRightLng += tile.mapSize;

        int shadingLngStep = 1;
        int shadingLatStep = 1;
        for (int shadingLeftLng = (int) Math.floor(maptileLeftLng); shadingLeftLng <= maptileRightLng; shadingLeftLng += shadingLngStep) {
            for (int shadingBottomLat = (int) Math.floor(maptileBottomLat); shadingBottomLat <= maptileTopLat; shadingBottomLat += shadingLatStep) {
                int shadingRightLng = shadingLeftLng + 1;
                int shadingTopLat = shadingBottomLat + 1;

                Bitmap shadingTile = null;
                try {
                    shadingTile = hillsRenderConfig.getShadingTile(shadingBottomLat, shadingLeftLng);
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, e.getMessage(), e);
                }
                if (shadingTile == null)
                    continue;

                double shadingPixelOffset = 0d;

                // shading tile subset if it fully fits inside map tile
                double shadingSubrectTop = 0;
                double shadingSubrectLeft = 0;
                double shadingSubrectRight = shadingTile.getWidth();
                double shadingSubrectBottom = shadingTile.getHeight();

                // map tile subset if it fully fits inside shading tile
                double maptileSubrectLeft = 0;
                double maptileSubrectTop = 0;
                double maptileSubrectRight = tile.tileSize;
                double maptileSubrectBottom = tile.tileSize;

                // find the intersection between map tile and shading tile in earth coordinates and determine the pixel 
                if (shadingTopLat > maptileTopLat) { // map tile ends in shading tile
                    shadingSubrectTop = shadingTile.getHeight() * ((shadingTopLat - maptileTopLat) / shadingLatStep);
                } else if (maptileTopLat > shadingTopLat) {
                    maptileSubrectTop = MercatorProjection.latitudeToPixelY(shadingTopLat + (shadingPixelOffset / shadingTile.getHeight()), tile.mapSize) - origin.y;
                }
                if (shadingBottomLat < maptileBottomLat) { // map tile ends in shading tile
                    shadingSubrectBottom = shadingTile.getHeight() - shadingTile.getHeight() * ((maptileBottomLat - shadingBottomLat) / shadingLatStep);
                } else if (maptileBottomLat < shadingBottomLat) {
                    maptileSubrectBottom = MercatorProjection.latitudeToPixelY(shadingBottomLat + (shadingPixelOffset / shadingTile.getHeight()), tile.mapSize) - origin.y;
                }
                if (shadingLeftLng < maptileLeftLng) { // map tile ends in shading tile
                    shadingSubrectLeft = shadingTile.getWidth() * ((maptileLeftLng - shadingLeftLng) / shadingLngStep);
                } else if (maptileLeftLng < shadingLeftLng) {
                    maptileSubrectLeft = MercatorProjection.longitudeToPixelX(shadingLeftLng + (shadingPixelOffset / shadingTile.getWidth()), tile.mapSize) - origin.x;
                }
                if (shadingRightLng > maptileRightLng) { // map tile ends in shading tile
                    shadingSubrectRight = shadingTile.getWidth() - shadingTile.getWidth() * ((shadingRightLng - maptileRightLng) / shadingLngStep);
                } else if (maptileRightLng > shadingRightLng) {
                    maptileSubrectRight = MercatorProjection.longitudeToPixelX(shadingRightLng + (shadingPixelOffset / shadingTile.getHeight()), tile.mapSize) - origin.x;
                }

                Rectangle hillsRect = new Rectangle(shadingSubrectLeft, shadingSubrectTop, shadingSubrectRight, shadingSubrectBottom);
                Rectangle maptileRect = new Rectangle(maptileSubrectLeft, maptileSubrectTop, maptileSubrectRight, maptileSubrectBottom);
                ShapeContainer hillShape = new HillshadingContainer(shadingTile, magnitude, hillsRect, maptileRect);

                renderContext.setDrawingLayers(layer);
                renderContext.addToCurrentDrawingLayer(level, new ShapePaintContainer(hillShape, null));
            }
        }
    }
}
