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
 * represents hillshading on a painter algorithm level in the parsed rendertheme (but without a rule)
 */
public class Hillshading
//        extends RenderInstruction
{
    private final GraphicFactory graphicFactory;
    private final int level;
    private final byte minZoom;
    private final byte maxZoom;
    private final float magnitude;
    public Hillshading(GraphicFactory graphicFactory, int level, byte minZoom, byte maxZoom, int magnitude) {
        this.graphicFactory = graphicFactory;
        //       super(graphicFactory, displayModel);
        this.level = level;
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

//        double topLeftLat = MercatorProjection.tileYToLatitude((long) origin.y, zoomLevel);
//        double topLeftLng = MercatorProjection.tileXToLongitude((long) origin.x, zoomLevel);
//
//        double botRightLat = MercatorProjection.tileYToLatitude((long) origin.y+tile.tileSize, zoomLevel);
//        double botRightLng = MercatorProjection.tileXToLongitude((long) origin.x+tile.tileSize, zoomLevel);

        double topLeftLat = MercatorProjection.pixelYToLatitude((long) origin.y, tile.mapSize);
        double topLeftLng = MercatorProjection.pixelXToLongitude((long) origin.x, tile.mapSize);

        double botRightLat = MercatorProjection.pixelYToLatitude((long) origin.y+tile.tileSize, tile.mapSize);
        double botRightLng = MercatorProjection.pixelXToLongitude((long) origin.x+tile.tileSize, tile.mapSize);

        System.out.println("renderHillshading: [[" + zoomLevel + "]] " + tile.tileSize + "  "+origin+ " " + topLeftLat + " " + topLeftLng + " to " + botRightLat + " " + botRightLng + "");

        for(int lng = (int)topLeftLng ; lng <= botRightLng; lng++){
            for(int lat = (int)botRightLat ; lat <= topLeftLat; lat++){

                try {
                    Bitmap shadingTile = hillsRenderConfig.getShadingTile(lat, lng);
                    if(shadingTile==null) continue;


                    double shadeTopLeftX = MercatorProjection.longitudeToPixelX(lng - 0.5d / shadingTile.getWidth(), tile.mapSize) - origin.x;
                    double shadeTopLeftY = MercatorProjection.latitudeToPixelY(lat + 1 - 0.5d / shadingTile.getHeight(), tile.mapSize) - origin.y;

                    double shadeBotRightX = MercatorProjection.longitudeToPixelX(lng + 1 - 0.5d / shadingTile.getWidth(), tile.mapSize) - origin.x;
                    double shadeBotRightY = MercatorProjection.latitudeToPixelY(lat - 0.5d / shadingTile.getHeight(), tile.mapSize) - origin.y;

//                    Matrix matrix = graphicFactory.createMatrix();
//                    float scaleX = (float) (shadeBotRightX - shadeTopLeftX) / shadingTile.getWidth();
//                    float scaleY = (float) (shadeBotRightY - shadeTopLeftY) / shadingTile.getHeight();
//                    matrix.scale(
//                            scaleX,
//                            scaleY
//                    );
//                    matrix.translate(shadeTopLeftX, shadeTopLeftY);
//
//                    System.out.println("renderHillshading: "+lat+"/"+lng+" : "+matrix);

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

//    @Override
//    public void destroy() {
//        // no-op
//    }
//
//
//    @Override
//    public void renderNode(RenderCallback renderCallback, final RenderContext renderContext, PointOfInterest poi) {
//        // do nothing
//    }
//
//    @Override
//    public void renderWay(RenderCallback renderCallback, final RenderContext renderContext, PolylineContainer way) {
//        HillsContext hillsContext = renderContext.hillsContext;
//        if( ! hillsContext.hillsActive(renderContext)) {
//            return;
//        }
////        if(hillsContext.level <= level) {
////            return;
////        }
//        Tile tile = renderContext.rendererJob.tile;
//
//
//        renderCallback.renderHillshading(renderContext, , magnitude);
//    }
//
//    @Override
//    public void scaleStrokeWidth(float scaleFactor, byte zoomLevel) {
//
//    }
//
//    @Override
//    public void scaleTextSize(float scaleFactor, byte zoomLevel) {
//        // do nothing
//    }
}
