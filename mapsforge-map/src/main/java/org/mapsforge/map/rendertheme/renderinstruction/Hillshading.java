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
import org.mapsforge.map.layer.hills.AThreadedHillShading;
import org.mapsforge.map.layer.hills.HgtCache;
import org.mapsforge.map.layer.hills.HillShadingUtils.SilentFutureTask;
import org.mapsforge.map.layer.hills.HillShadingUtils.HillShadingThreadPool;
import org.mapsforge.map.layer.hills.HillsRenderConfig;
import org.mapsforge.map.layer.renderer.HillshadingContainer;
import org.mapsforge.map.layer.renderer.ShapeContainer;
import org.mapsforge.map.layer.renderer.ShapePaintContainer;
import org.mapsforge.map.rendertheme.RenderContext;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents hillshading on a painter algorithm layer/level in the parsed rendertheme
 * (but without a rule, we don't need to increase waymatching complexity here)
 */
public class Hillshading {

    private static final Logger LOGGER = Logger.getLogger(Hillshading.class.getName());

    /**
     * Default name prefix for additional reading threads created and used by hill shading. A numbered suffix will be appended.
     */
    public static final String ThreadPoolName = "MapsforgeHillShading";

    public static final int ShadingLatStep = 1;
    public static final int ShadingLonStep = 1;

    private final int level;
    private final byte layer;
    private final byte minZoom;
    private final byte maxZoom;
    private final float magnitude;
    private final int color;
    private final GraphicFactory graphicFactory;

    /**
     * Static thread pool shared by all tasks.
     */
    protected static final AtomicReference<HillShadingThreadPool> ThreadPool = new AtomicReference<>(null);
    protected final Object RenderSync = new Object();

    public Hillshading(byte minZoom, byte maxZoom, int magnitude, int color, byte layer, int level, GraphicFactory graphicFactory) {
        this.level = level;
        this.layer = layer;
        this.minZoom = minZoom;
        this.maxZoom = maxZoom;
        this.magnitude = magnitude;
        this.color = color;
        this.graphicFactory = graphicFactory;
    }

    public void render(final RenderContext renderContext, HillsRenderConfig hillsRenderConfig) {
        if (hillsRenderConfig == null) {
            return;
        }

        final Tile tile = renderContext.rendererJob.tile;
        final byte zoomLevel = tile.zoomLevel >= 0 ? tile.zoomLevel : 0;

        if (checkZoomLevelCoarse(zoomLevel, hillsRenderConfig)) {

            final Point origin = tile.getOrigin();

            final double maptileLeftLon = MercatorProjection.pixelXToLongitude(origin.x, tile.mapSize);
            double maptileRightLon = MercatorProjection.pixelXToLongitude(origin.x + tile.tileSize, tile.mapSize);
            if (maptileRightLon < maptileLeftLon)
                maptileRightLon += tile.mapSize;

            final double maptileTopLat = MercatorProjection.pixelYToLatitude(origin.y, tile.mapSize);
            final double maptileBottomLat = MercatorProjection.pixelYToLatitude(origin.y + tile.tileSize, tile.mapSize);

            final float effectiveMagnitude = Math.min(Math.max(0f, this.magnitude * hillsRenderConfig.getMagnitudeScaleFactor()), 255f) / 255f;
            final int effectiveColor = getEffectiveColor(hillsRenderConfig);

            createThreadPoolsMaybe();

            final Deque<SilentFutureTask> deque = new ArrayDeque<>();

            for (int shadingLeftLon = (int) Math.floor(maptileLeftLon); shadingLeftLon <= maptileRightLon; shadingLeftLon += ShadingLonStep) {
                final SilentFutureTask code = renderLatStrip(renderContext, hillsRenderConfig, shadingLeftLon, zoomLevel, tile, maptileBottomLat, maptileTopLat, maptileLeftLon, maptileRightLon, effectiveMagnitude, effectiveColor);
                deque.addLast(code);
            }

            while (false == deque.isEmpty()) {
                deque.pollFirst().get();
            }
        }
    }

    protected int getEffectiveColor(HillsRenderConfig hillsRenderConfig) {
        int retVal = hillsRenderConfig.getColor();

        if (retVal == 0) {
            retVal = color;
        }

        return retVal;
    }

    protected SilentFutureTask renderLatStrip(final RenderContext renderContext, final HillsRenderConfig hillsRenderConfig, final int shadingLeftLon, final byte zoomLevel, final Tile tile, final double maptileBottomLat, final double maptileTopLat, final double maptileLeftLon, final double maptileRightLon, final float effectiveMagnitude, int effectiveColor) {
        Callable<Boolean> runnable = new Callable<Boolean>() {
            public Boolean call() {
                final int shadingRightLon = shadingLeftLon + ShadingLonStep;
                final double leftX = MercatorProjection.longitudeToPixelX(shadingLeftLon, tile.mapSize);
                final double rightX = MercatorProjection.longitudeToPixelX(shadingRightLon, tile.mapSize);
                final double pxPerLon = (rightX - leftX) / ShadingLonStep;

                for (int shadingBottomLat = (int) Math.floor(maptileBottomLat); shadingBottomLat <= maptileTopLat; shadingBottomLat += ShadingLatStep) {
                    final int shadingTopLat = shadingBottomLat + ShadingLatStep;

                    final double topY = MercatorProjection.latitudeToPixelY(shadingTopLat, tile.mapSize);
                    final double bottomY = MercatorProjection.latitudeToPixelY(shadingBottomLat, tile.mapSize);
                    final double pxPerLat = (bottomY - topY) / ShadingLatStep;

                    HillshadingBitmap shadingTile = null;

                    if (checkZoomLevelFine(zoomLevel, hillsRenderConfig, shadingBottomLat, shadingLeftLon)) {
                        try {
                            shadingTile = hillsRenderConfig.getShadingTile(shadingBottomLat, shadingLeftLon, zoomLevel, pxPerLat, pxPerLon, effectiveColor);
                        } catch (Exception e) {
                            LOGGER.log(Level.SEVERE, e.toString(), e);
                        }
                    }

                    if (shadingTile == null) {
                        continue;
                    }

                    final int padding;
                    final int shadingInnerWidth;
                    final int shadingInnerHeight;
                    if (shadingTile != null) {
                        padding = shadingTile.getPadding();
                        shadingInnerWidth = shadingTile.getWidth() - 2 * padding;
                        shadingInnerHeight = shadingTile.getHeight() - 2 * padding;
                    } else {
                        padding = shadingInnerWidth = shadingInnerHeight = 0;
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

                    final Point origin = tile.getOrigin();

                    // find the intersection between map tile and shading tile in earth coordinates and determine the pixel

                    if (shadingBottomLat > maptileBottomLat) {
                        // Shading tile ends in map tile
                        maptileSubrectBottom = Math.round(MercatorProjection.latitudeToPixelY(shadingBottomLat, tile.mapSize)) - origin.y;
                        mergeNeighbor(shadingTile, padding, hillsRenderConfig, HillshadingBitmap.Border.SOUTH, shadingBottomLat, shadingLeftLon, zoomLevel, pxPerLat, pxPerLon, effectiveColor);
                    } else if (shadingBottomLat < maptileBottomLat) {
                        // Map tile ends in shading tile
                        shadingSubrectBottom -= shadingInnerHeight * ((maptileBottomLat - shadingBottomLat) / ShadingLatStep);
                    }

                    if (shadingTopLat < maptileTopLat) {
                        // Shading tile ends in map tile
                        maptileSubrectTop = Math.round(MercatorProjection.latitudeToPixelY(shadingTopLat, tile.mapSize)) - origin.y;
                        mergeNeighbor(shadingTile, padding, hillsRenderConfig, HillshadingBitmap.Border.NORTH, shadingBottomLat, shadingLeftLon, zoomLevel, pxPerLat, pxPerLon, effectiveColor);
                    } else if (shadingTopLat > maptileTopLat) {
                        // Map tile ends in shading tile
                        shadingSubrectTop += shadingInnerHeight * ((shadingTopLat - maptileTopLat) / ShadingLatStep);
                    }

                    if (shadingLeftLon > maptileLeftLon) {
                        // Shading tile ends in map tile
                        maptileSubrectLeft = Math.round(MercatorProjection.longitudeToPixelX(shadingLeftLon, tile.mapSize)) - origin.x;
                        mergeNeighbor(shadingTile, padding, hillsRenderConfig, HillshadingBitmap.Border.WEST, shadingBottomLat, shadingLeftLon, zoomLevel, pxPerLat, pxPerLon, effectiveColor);
                    } else if (shadingLeftLon < maptileLeftLon) {
                        // Map tile ends in shading tile
                        shadingSubrectLeft += shadingInnerWidth * ((maptileLeftLon - shadingLeftLon) / ShadingLonStep);
                    }

                    if (shadingRightLon < maptileRightLon) {
                        // Shading tile ends in map tile
                        maptileSubrectRight = Math.round(MercatorProjection.longitudeToPixelX(shadingRightLon, tile.mapSize)) - origin.x;
                        mergeNeighbor(shadingTile, padding, hillsRenderConfig, HillshadingBitmap.Border.EAST, shadingBottomLat, shadingLeftLon, zoomLevel, pxPerLat, pxPerLon, effectiveColor);
                    } else if (shadingRightLon > maptileRightLon) {
                        // Map tile ends in shading tile
                        shadingSubrectRight -= shadingInnerWidth * ((shadingRightLon - maptileRightLon) / ShadingLonStep);
                    }

                    final Rectangle hillsRect = (shadingTile == null) ? null : new Rectangle(shadingSubrectLeft, shadingSubrectTop, shadingSubrectRight, shadingSubrectBottom);
                    final Rectangle maptileRect = new Rectangle(maptileSubrectLeft, maptileSubrectTop, maptileSubrectRight, maptileSubrectBottom);
                    final ShapeContainer hillShape = new HillshadingContainer(shadingTile, effectiveMagnitude, effectiveColor, hillsRect, maptileRect);

                    final ShapePaintContainer newContainer = new ShapePaintContainer(hillShape, null);

                    // There's no synchronization in RenderContext, so we must do it here
                    synchronized (RenderSync) {
                        renderContext.setDrawingLayer(layer);
                        renderContext.addToCurrentDrawingLayer(level, newContainer);
                    }
                }

                return true;
            }
        };

        final SilentFutureTask code = new SilentFutureTask(runnable);

        postToThreadPoolOrRun(code);

        return code;
    }

    protected static void postToThreadPoolOrRun(final Runnable code) {
        final HillShadingThreadPool threadPool = ThreadPool.get();

        if (threadPool != null) {
            threadPool.executeOrRun(code);
        }
    }

    protected static void createThreadPoolsMaybe() {
        final AtomicReference<HillShadingThreadPool> threadPoolReference = ThreadPool;

        if (threadPoolReference.get() == null) {
            synchronized (threadPoolReference) {
                if (threadPoolReference.get() == null) {
                    threadPoolReference.set(createThreadPool());
                }
            }
        }
    }

    protected static HillShadingThreadPool createThreadPool() {
        final int threadCount = AThreadedHillShading.ReadingThreadsCountDefault;
        final int queueSize = Integer.MAX_VALUE;
        return new HillShadingThreadPool(threadCount, threadCount, queueSize, 5, ThreadPoolName).start();
    }

    protected boolean checkZoomLevelCoarse(int zoomLevel, HillsRenderConfig hillsRenderConfig) {
        boolean retVal = true;

        if (hillsRenderConfig.isAdaptiveZoomEnabled()) {
            // Pass, wide zoom range algorithms will use finer granulation zoom level support check later
        } else {
            if (zoomLevel > maxZoom) {
                retVal = false;
            } else if (zoomLevel < minZoom) {
                retVal = false;
            }
        }

        return retVal;
    }

    protected boolean checkZoomLevelFine(int zoomLevel, HillsRenderConfig hillsRenderConfig, int shadingBottomLat, int shadingLeftLon) {
        boolean retVal = true;

        if (hillsRenderConfig.isAdaptiveZoomEnabled()) {
            retVal = hillsRenderConfig.isZoomLevelSupported(zoomLevel, shadingBottomLat, shadingLeftLon);
        }

        return retVal;
    }

    protected HillshadingBitmap getNeighbor(HillsRenderConfig hillsRenderConfig, HillshadingBitmap.Border border, int shadingBottomLat, int shadingLeftLon, int zoomLevel, double pxPerLat, double pxPerLon, int effectiveColor) {

        HillshadingBitmap neighbor = null;
        try {
            switch (border) {
                case NORTH:
                    neighbor = hillsRenderConfig.getShadingTile(shadingBottomLat + ShadingLatStep, shadingLeftLon, zoomLevel, pxPerLat, pxPerLon, effectiveColor);
                    break;
                case SOUTH:
                    neighbor = hillsRenderConfig.getShadingTile(shadingBottomLat - ShadingLatStep, shadingLeftLon, zoomLevel, pxPerLat, pxPerLon, effectiveColor);
                    break;
                case EAST:
                    neighbor = hillsRenderConfig.getShadingTile(shadingBottomLat, shadingLeftLon + ShadingLonStep, zoomLevel, pxPerLat, pxPerLon, effectiveColor);
                    break;
                case WEST:
                    neighbor = hillsRenderConfig.getShadingTile(shadingBottomLat, shadingLeftLon - ShadingLonStep, zoomLevel, pxPerLat, pxPerLon, effectiveColor);
                    break;
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, e.toString(), e);
        }

        return neighbor;
    }

    protected void mergePaddingOnBitmap(HillshadingBitmap center, HillshadingBitmap neighbor, HillshadingBitmap.Border border, int padding) {
        if (neighbor != null && padding > 0) {
            final Canvas copyCanvas = graphicFactory.createCanvas();

            HgtCache.mergeSameSized(center, neighbor, border, padding, copyCanvas);
        }
    }

    protected void mergeNeighbor(HillshadingBitmap monoBitmap, int padding, HillsRenderConfig hillsRenderConfig, HillshadingBitmap.Border border, int shadingBottomLat, int shadingLeftLon, int zoomLevel, double pxPerLat, double pxPerLon, int effectiveColor) {
        if (monoBitmap != null && padding > 0) {
            final HillshadingBitmap neighbor = getNeighbor(hillsRenderConfig, border, shadingBottomLat, shadingLeftLon, zoomLevel, pxPerLat, pxPerLon, effectiveColor);
            mergePaddingOnBitmap(monoBitmap, neighbor, border, padding);
        }
    }
}
