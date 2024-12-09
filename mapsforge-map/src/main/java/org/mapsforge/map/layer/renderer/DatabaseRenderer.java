/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright 2014-2015 Ludwig M Brinckmann
 * Copyright 2014-2016 devemux86
 * Copyright 2017 usrusr
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
package org.mapsforge.map.layer.renderer;

import org.mapsforge.core.graphics.Color;
import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.graphics.TileBitmap;
import org.mapsforge.core.mapelements.MapElementContainer;
import org.mapsforge.core.model.Rectangle;
import org.mapsforge.core.model.Rotation;
import org.mapsforge.core.model.Tile;
import org.mapsforge.map.datastore.MapDataStore;
import org.mapsforge.map.datastore.MapReadResult;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.layer.hills.HillsRenderConfig;
import org.mapsforge.map.layer.labels.LabelStore;
import org.mapsforge.map.layer.labels.MapDataStoreLabelStore;
import org.mapsforge.map.layer.labels.TileBasedLabelStore;
import org.mapsforge.map.rendertheme.RenderContext;
import org.mapsforge.map.util.LayerUtil;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

/**
 * The DatabaseRenderer renders map tiles by reading from a {@link org.mapsforge.map.datastore.MapDataStore}.
 */
public class DatabaseRenderer extends StandardRenderer {
    protected static final Logger LOGGER = Logger.getLogger(DatabaseRenderer.class.getName());

    protected final LabelStore labelStore;
    protected final boolean renderLabels;
    protected final TileCache tileCache;
    protected final TileDependencies tileDependencies;

    /**
     * Constructs a new DatabaseRenderer.
     * There are three possible configurations:
     * 1) render labels directly onto tiles: renderLabels == true && tileCache != null
     * 2) do not render labels but cache them: renderLabels == false && labelStore != null
     * 3) do not render or cache labels: renderLabels == false && labelStore == null
     *
     * @param mapDataStore      the data source.
     * @param graphicFactory    the graphic factory.
     * @param tileCache         where tiles are cached (needed if labels are drawn directly onto tiles, otherwise null)
     * @param labelStore        where labels are cached.
     * @param renderLabels      if labels should be rendered.
     * @param cacheLabels       if labels should be cached.
     * @param hillsRenderConfig the hillshading setup to be used (can be null).
     */
    public DatabaseRenderer(MapDataStore mapDataStore, GraphicFactory graphicFactory, TileCache tileCache,
                            LabelStore labelStore, boolean renderLabels, boolean cacheLabels,
                            HillsRenderConfig hillsRenderConfig) {
        super(mapDataStore, graphicFactory, renderLabels || cacheLabels, hillsRenderConfig);
        this.tileCache = tileCache;
        this.labelStore = labelStore;
        this.renderLabels = renderLabels;
        this.tileDependencies = new TileDependencies();
    }

    /**
     * Called when a job needs to be executed.
     *
     * @param rendererJob the job that should be executed.
     */
    public TileBitmap executeJob(RendererJob rendererJob) {
        TileBitmap output = null;
        RenderContext renderContext = null;
        try {
            renderContext = new RenderContext(rendererJob, graphicFactory);

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

                    renderContext.drawWays();
                }

                if (this.renderLabels) {
                    processLabels(renderContext);
                }
                if (this.labelStore instanceof TileBasedLabelStore) {
                    // store elements for this tile in the label cache
                    ((TileBasedLabelStore) this.labelStore).storeMapItems(rendererJob.tile, renderContext.getLabels());
                }

                if (!rendererJob.labelsOnly && renderContext.renderTheme.hasMapBackgroundOutside() && this.mapDataStore != null) {
                    // blank out all areas outside of map
                    Rectangle insideArea = this.mapDataStore.boundingBox().getPositionRelativeToTile(rendererJob.tile);
                    if (!rendererJob.hasAlpha) {
                        renderContext.canvasRasterer.fillOutsideAreas(renderContext.renderTheme.getMapBackgroundOutside(), insideArea);
                    } else {
                        renderContext.canvasRasterer.fillOutsideAreas(Color.TRANSPARENT, insideArea);
                    }
                }

                output = bitmap;
            } else {
                // outside of map area with background defined:
                output = createBackgroundBitmap(renderContext);
            }
        } catch (Exception e) {
            LOGGER.warning(e.toString());
        } finally {
            if (renderContext != null) {
                renderContext.destroy();
            }
            if (!rendererJob.labelsOnly && output != null) {
                if (tileCache != null) {
                    tileCache.put(rendererJob, output);
                }
                removeTileInProgress(rendererJob.tile);
            }
        }

        return output;
    }

    public MapDataStore getMapDatabase() {
        return this.mapDataStore;
    }

    protected void removeTileInProgress(Tile tile) {
        this.tileDependencies.removeTileInProgress(tile);
    }

    /**
     * Draws a bitmap just with outside colour, used for bitmaps outside of map area.
     *
     * @param renderContext the RenderContext
     * @return bitmap drawn in single colour.
     */
    protected TileBitmap createBackgroundBitmap(RenderContext renderContext) {
        TileBitmap bitmap = this.graphicFactory.createTileBitmap(renderContext.rendererJob.tile.tileSize, renderContext.rendererJob.hasAlpha);
        renderContext.canvasRasterer.setCanvasBitmap(bitmap);
        if (!renderContext.rendererJob.hasAlpha) {
            renderContext.canvasRasterer.fill(renderContext.renderTheme.getMapBackgroundOutside());
        }
        return bitmap;
    }

    protected void processLabels(RenderContext renderContext) {
        // if we are drawing the labels per tile, we need to establish which tile-overlapping
        // elements need to be drawn.

        final List<MapElementContainer> labelsToDraw;
        {
            Set<MapElementContainer> activeLabels = new HashSet<>();

            if (this.labelStore instanceof MapDataStoreLabelStore) {
                // Required to prevent non-deterministic labels and label tearing when drawing labels on tiles.
                // This is similar to what LabelLayer does in its draw() method.
                activeLabels.addAll(this.labelStore.getVisibleItems(renderContext.rendererJob.tile.getAboveLeft(), renderContext.rendererJob.tile.getBelowRight()));

                LayerUtil.removeInvisibleItems(activeLabels);

                labelsToDraw = LayerUtil.collisionAndContestingFreeOrdered(new ArrayList<>(activeLabels), renderContext.rendererJob.tile, Rotation.NULL_ROTATION);
            } else {
                synchronized (tileDependencies) {
                    // first we need to get the labels from the adjacent tiles if they have already been drawn
                    // as those overlapping items must also be drawn on the current tile. They must be drawn regardless
                    // of priority clashes as a part of them has alread been drawn.
                    Set<Tile> neighbours = renderContext.rendererJob.tile.getNeighbours();
                    Iterator<Tile> tileIterator = neighbours.iterator();
                    Set<MapElementContainer> undrawableElements = new HashSet<>();

                    tileDependencies.addTileInProgress(renderContext.rendererJob.tile);
                    while (tileIterator.hasNext()) {
                        Tile neighbour = tileIterator.next();

                        if (tileDependencies.isTileInProgress(neighbour) || tileCache.containsKey(renderContext.rendererJob.otherTile(neighbour))) {
                            // if a neighbour tile has already been drawn, the elements drawn that overlap onto the
                            // current tile should be in the tile dependencies, we add them to the labels that
                            // need to be drawn onto this tile. For the multi-threaded renderer we also need to take
                            // those tiles into account that are not yet in the TileCache: this is taken care of by the
                            // set of tilesInProgress inside the TileDependencies.
                            activeLabels.addAll(tileDependencies.getOverlappingElements(neighbour, renderContext.rendererJob.tile));

                            // but we need to remove the labels for this tile that overlap onto a tile that has been drawn
                            for (MapElementContainer current : renderContext.getLabels()) {
                                if (current.intersects(neighbour.getBoundaryAbsolute(), Rotation.NULL_ROTATION)) {
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
                    renderContext.clearLabels(undrawableElements);

                    // at this point we have two lists: one is the list of labels that must be drawn because
                    // they already overlap from other tiles. The second one is currentLabels that contains
                    // the elements on this tile that do not overlap onto a drawn tile. Now we sort this list and
                    // remove those elements that clash in this list already.
                    List<MapElementContainer> currentElementsOrdered = LayerUtil.collisionFreeOrdered(renderContext.getLabels(), Rotation.NULL_ROTATION, false);

                    // now we go through this list, ordered by priority, to see which can be drawn without clashing.
                    Iterator<MapElementContainer> currentMapElementsIterator = currentElementsOrdered.iterator();
                    while (currentMapElementsIterator.hasNext()) {
                        MapElementContainer current = currentMapElementsIterator.next();
                        for (MapElementContainer label : activeLabels) {
                            if (label.clashesWith(current, Rotation.NULL_ROTATION)) {
                                currentMapElementsIterator.remove();
                                break;
                            }
                        }
                    }

                    activeLabels.addAll(currentElementsOrdered);

                    // update dependencies, add to the dependencies list all the elements that overlap to the
                    // neighbouring tiles, first clearing out the cache for this relation.
                    for (Tile tile : neighbours) {
                        tileDependencies.removeTileData(renderContext.rendererJob.tile, tile);
                        for (MapElementContainer element : activeLabels) {
                            if (element.intersects(tile.getBoundaryAbsolute(), Rotation.NULL_ROTATION)) {
                                tileDependencies.addOverlappingElement(renderContext.rendererJob.tile, tile, element);
                            }
                        }
                    }
                }

                LayerUtil.removeInvisibleItems(activeLabels);

                labelsToDraw = LayerUtil.collisionFreeOrdered(new ArrayList<>(activeLabels), Rotation.NULL_ROTATION);
            }
        }

        // We need to draw elements in order of ascending priority: lower priority first, so more important
        // elements will be drawn on top (in case of display=true) items.
        renderContext.canvasRasterer.drawMapElements(labelsToDraw, renderContext.rendererJob.tile);
    }
}
