/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright 2014-2015 Ludwig M Brinckmann
 * Copyright 2014-2016 devemux86
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
import org.mapsforge.core.model.Tile;
import org.mapsforge.core.util.MercatorProjection;
import org.mapsforge.map.layer.hills.HillsRenderConfig;
import org.mapsforge.map.layer.renderer.HillshadingContainer;
import org.mapsforge.map.layer.renderer.ShapeContainer;
import org.mapsforge.map.layer.renderer.ShapePaintContainer;
import org.mapsforge.map.rendertheme.RenderContext;

import java.util.concurrent.ExecutionException;

/**
 * represents hillshading on a painter algorithm layer/level in the parsed rendertheme (but without a rule, we don't need to increase waymatching complexity here)
 */
public class Hillshading {
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
        this.magnitude = Math.min(Math.max(0,magnitude), 255)/255f;
    }

    public void render(final RenderContext renderContext, HillsRenderConfig hillsRenderConfig) {
        if(hillsRenderConfig==null) {
            return;
        }
        Tile tile = renderContext.rendererJob.tile;
        byte zoomLevel = tile.zoomLevel;
        if(zoomLevel >maxZoom || zoomLevel <minZoom) return;

        Point origin = tile.getOrigin();
        double topLeftLat = MercatorProjection.pixelYToLatitude((long) origin.y, tile.mapSize);
        double topLeftLng = MercatorProjection.pixelXToLongitude((long) origin.x, tile.mapSize);

        double botRightLat = MercatorProjection.pixelYToLatitude((long) origin.y+tile.tileSize, tile.mapSize);
        double botRightLng = MercatorProjection.pixelXToLongitude((long) origin.x+tile.tileSize, tile.mapSize);

        for(int lng = (int)topLeftLng ; lng <= botRightLng; lng++){
            for(int lat = (int)botRightLat ; lat <= topLeftLat; lat++){

                try {
                    Bitmap shadingTile = hillsRenderConfig.getShadingTile(lat, lng);
                    if(shadingTile==null) continue;

                    // scaling the full shading bitmap linearly between its corners causes quite a bit of shifting,
                    // it would be better to project the corners of the clipping region and then extrapolate the virtual corners from there

                    double shadingPixelOffset = 0.5d; // the slope information actually represents the southeast corner of the pixel

                    double shadeTopLeftX = MercatorProjection.longitudeToPixelX(lng + shadingPixelOffset / shadingTile.getWidth(), tile.mapSize) - origin.x;
                    double shadeTopLeftY = MercatorProjection.latitudeToPixelY(lat + 1 + shadingPixelOffset / shadingTile.getHeight(), tile.mapSize) - origin.y;

                    double shadeBotRightX = MercatorProjection.longitudeToPixelX(lng + 1 + shadingPixelOffset / shadingTile.getWidth(), tile.mapSize) - origin.x;
                    double shadeBotRightY = MercatorProjection.latitudeToPixelY(lat + shadingPixelOffset / shadingTile.getHeight(), tile.mapSize) - origin.y;

                    renderContext.setDrawingLayers(layer);
                    ShapeContainer hillShape = new HillshadingContainer(shadingTile, magnitude, shadeTopLeftX, shadeTopLeftY, shadeBotRightX, shadeBotRightY);
                    renderContext.addToCurrentDrawingLayer(level, new ShapePaintContainer(hillShape, null));
                } catch (ExecutionException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }
        }


    }
}
