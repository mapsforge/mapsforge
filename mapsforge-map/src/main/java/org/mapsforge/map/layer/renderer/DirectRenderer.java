/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright 2014-2015 Ludwig M Brinckmann
 * Copyright 2014-2016 devemux86
 * Copyright 2017 usrusr
 * Copyright 2018 Fabrice Fontaine
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

import org.mapsforge.core.graphics.Color;
import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.graphics.TileBitmap;
import org.mapsforge.core.mapelements.MapElementContainer;
import org.mapsforge.core.model.Rectangle;
import org.mapsforge.core.model.Tile;
import org.mapsforge.map.datastore.MapDataStore;
import org.mapsforge.map.datastore.MapReadResult;
import org.mapsforge.map.layer.hills.HillsRenderConfig;
import org.mapsforge.map.rendertheme.RenderContext;
import org.mapsforge.map.util.LayerUtil;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

/**
 * The DirectRenderer renders map tiles by reading from a {@link MapDataStore}.
 * Just rendering the tiles without any memory of what happened before.
 *
 * @see <a href="https://github.com/mapsforge/mapsforge/issues/1085">mapsforge/mapsforge#1085</a>
 */
public class DirectRenderer extends StandardRenderer {
    private static final Logger LOGGER = Logger.getLogger(DirectRenderer.class.getName());

    private final boolean renderLabels;
    private final TileDependencies tileDependencies;

    /**
     * Constructs a new DirectRenderer.
     *
     * @param mapDataStore      the data source.
     * @param graphicFactory    the graphic factory.
     * @param renderLabels      if labels should be rendered.
     * @param hillsRenderConfig the hillshading setup to be used (can be null).
     */
    public DirectRenderer(MapDataStore mapDataStore, GraphicFactory graphicFactory,
                          boolean renderLabels, HillsRenderConfig hillsRenderConfig) {
        super(mapDataStore, graphicFactory, renderLabels, hillsRenderConfig);
        this.renderLabels = renderLabels;
        this.tileDependencies = new TileDependencies();
    }

    /**
     * Called when a job needs to be executed.
     *
     * @param rendererJob the job that should be executed.
     */
    public TileBitmap executeJob(RendererJob rendererJob) {
        RenderContext renderContext = null;
        try {
            renderContext = new RenderContext(rendererJob, new CanvasRasterer(graphicFactory));

            if (renderBitmap(renderContext)) {
                TileBitmap bitmap = null;

                if (this.mapDataStore != null) {
                    MapReadResult mapReadResult = this.mapDataStore.readMapData(rendererJob.tile);
                    processReadMapData(renderContext, mapReadResult);
                }

                if (!rendererJob.labelsOnly) {
                    renderContext.renderTheme.matchHillShadings(this, renderContext);
                    bitmap = this.graphicFactory.createTileBitmap(rendererJob.tile.tileSize, rendererJob.hasAlpha);
                    bitmap.setTimestamp(rendererJob.mapDataStore.getDataTimestamp(rendererJob.tile));
                    renderContext.canvasRasterer.setCanvasBitmap(bitmap);
                    if (!rendererJob.hasAlpha && rendererJob.displayModel.getBackgroundColor() != renderContext.renderTheme.getMapBackground()) {
                        renderContext.canvasRasterer.fill(renderContext.renderTheme.getMapBackground());
                    }
                    renderContext.canvasRasterer.drawWays(renderContext);
                }

                if (this.renderLabels) {
                    Set<MapElementContainer> labelsToDraw = processLabels(renderContext);
                    // now draw the ways and the labels
                    renderContext.canvasRasterer.drawMapElements(labelsToDraw, rendererJob.tile);
                }

                if (!rendererJob.labelsOnly && renderContext.renderTheme.hasMapBackgroundOutside()) {
                    // blank out all areas outside of map
                    Rectangle insideArea = this.mapDataStore.boundingBox().getPositionRelativeToTile(rendererJob.tile);
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
        } catch (Exception e) {
            // #1049: message can be null?
            LOGGER.warning("Exception: " + e.getMessage());
            return null;
        } finally {
            if (renderContext != null) {
                renderContext.destroy();
            }
        }
    }

    public MapDataStore getMapDatabase() {
        return this.mapDataStore;
    }

    void removeTileInProgress(Tile tile) {
        if (this.tileDependencies != null) {
            this.tileDependencies.removeTileInProgress(tile);
        }
    }

    /**
     * Draws a bitmap just with outside colour, used for bitmaps outside of map area.
     *
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
        Set<MapElementContainer> labelsToDraw = new HashSet<>();

        Set<Tile> neighbours = renderContext.rendererJob.tile.getNeighbours();
        for (Tile neighbour : neighbours) {
            labelsToDraw.addAll(tileDependencies.getOverlappingElements(neighbour, renderContext.rendererJob.tile));
        }

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
        return labelsToDraw;
    }
}
