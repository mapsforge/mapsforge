/*
 * Copyright 2015 Ludwig M Brinckmann
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
package org.mapsforge.map.rendertheme;

import org.mapsforge.core.mapelements.MapElementContainer;
import org.mapsforge.core.model.Tile;
import org.mapsforge.map.layer.renderer.CanvasRasterer;
import org.mapsforge.map.layer.renderer.RendererJob;
import org.mapsforge.map.layer.renderer.ShapePaintContainer;
import org.mapsforge.map.rendertheme.rule.RenderTheme;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * A RenderContext contains all the information and data to render a map area, it is passed between
 * calls in order to avoid local data stored in the DatabaseRenderer.
 */
public class RenderContext {

    private static final byte LAYERS = 11;

    private static final double STROKE_INCREASE = 1.5;
    private static final byte STROKE_MIN_ZOOM_LEVEL = 12;
    public final RendererJob rendererJob;
    public final RenderTheme renderTheme;

    // Configuration that drives the rendering
    public final CanvasRasterer canvasRasterer;

    // Data generated for the rendering process
    private List<List<ShapePaintContainer>> drawingLayers;
    public final List<MapElementContainer> labels;
    public final List<List<List<ShapePaintContainer>>> ways;


    public RenderContext(RendererJob rendererJob, CanvasRasterer canvasRasterer) throws InterruptedException, ExecutionException {
        this.rendererJob = rendererJob;
        this.labels = new LinkedList<>();
        this.canvasRasterer = canvasRasterer;
        this.renderTheme = rendererJob.renderThemeFuture.get();
        this.renderTheme.scaleTextSize(rendererJob.textScale, rendererJob.tile.zoomLevel);
        this.ways = createWayLists();
        setScaleStrokeWidth(this.rendererJob.tile.zoomLevel);
    }

    public void destroy() {
        this.canvasRasterer.destroy();
    }

    public void setDrawingLayers(byte layer) {
        if (layer < 0) {
            layer = 0;
        } else if (layer >= RenderContext.LAYERS) {
            layer = RenderContext.LAYERS - 1;
        }
        this.drawingLayers = ways.get(layer);
    }

    public void addToCurrentDrawingLayer(int level, ShapePaintContainer element) {
        this.drawingLayers.get(level).add(element);
    }

    /**
     * Just a way of generating a hash key for a tile if only the RendererJob is known.
     *
     * @param tile the tile that changes
     * @return a RendererJob based on the current one, only tile changes
     */
    public RendererJob otherTile(Tile tile) {
        return new RendererJob(tile, this.rendererJob.mapDataStore, this.rendererJob.renderThemeFuture, this.rendererJob.displayModel,
                this.rendererJob.textScale, this.rendererJob.hasAlpha, this.rendererJob.labelsOnly);
    }

    private List<List<List<ShapePaintContainer>>> createWayLists() {
        List<List<List<ShapePaintContainer>>> result = new ArrayList<>(LAYERS);
        int levels = this.renderTheme.getLevels();

        for (byte i = LAYERS - 1; i >= 0; --i) {
            List<List<ShapePaintContainer>> innerWayList = new ArrayList<>(levels);
            for (int j = levels - 1; j >= 0; --j) {
                innerWayList.add(new ArrayList<ShapePaintContainer>(0));
            }
            result.add(innerWayList);
        }
        return result;
    }

    /**
     * Sets the scale stroke factor for the given zoom level.
     *
     * @param zoomLevel the zoom level for which the scale stroke factor should be set.
     */
    private void setScaleStrokeWidth(byte zoomLevel) {
        int zoomLevelDiff = Math.max(zoomLevel - STROKE_MIN_ZOOM_LEVEL, 0);
        this.renderTheme.scaleStrokeWidth((float) Math.pow(STROKE_INCREASE, zoomLevelDiff), this.rendererJob.tile.zoomLevel);
    }

}
