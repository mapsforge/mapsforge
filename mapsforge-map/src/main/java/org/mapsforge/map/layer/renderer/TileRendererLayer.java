/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright 2014-2015 Ludwig M Brinckmann
 * Copyright 2014 Christian Pesch
 * Copyright 2016 devemux86
 * Copyright 2016 ksaihtam
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

import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.graphics.TileBitmap;
import org.mapsforge.core.model.Tile;
import org.mapsforge.map.datastore.MapDataStore;
import org.mapsforge.map.layer.TileLayer;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.layer.hills.HillsRenderConfig;
import org.mapsforge.map.layer.labels.LabelStore;
import org.mapsforge.map.layer.labels.TileBasedLabelStore;
import org.mapsforge.map.model.DisplayModel;
import org.mapsforge.map.model.MapViewPosition;
import org.mapsforge.map.model.common.Observer;
import org.mapsforge.map.rendertheme.XmlRenderTheme;
import org.mapsforge.map.rendertheme.rule.RenderThemeFuture;

public class TileRendererLayer extends TileLayer<RendererJob> implements Observer {
    private final DatabaseRenderer databaseRenderer;
    private final GraphicFactory graphicFactory;
    private final MapDataStore mapDataStore;
    private MapWorkerPool mapWorkerPool;
    private RenderThemeFuture renderThemeFuture;
    private float textScale;
    private final TileBasedLabelStore tileBasedLabelStore;
    private XmlRenderTheme xmlRenderTheme;

    /**
     * Creates a TileRendererLayer.<br/>
     * - Tiles will not have alpha/transparency<br/>
     * - Labels will be rendered onto tiles<br/>
     * - Labels will not be cached in a LabelStore
     *  @param tileCache       cache where tiles are stored
     * @param mapDataStore    the mapsforge map file
     * @param mapViewPosition the mapViewPosition to know which tiles to render
     * @param graphicFactory  the graphicFactory to carry out platform specific operations
     * @param hillsRenderConfig the hillshading setup to be used (can be null)
     */
    public TileRendererLayer(TileCache tileCache, MapDataStore mapDataStore, MapViewPosition mapViewPosition,
                             GraphicFactory graphicFactory, HillsRenderConfig hillsRenderConfig) {
        this(tileCache, mapDataStore, mapViewPosition, false, true, false, graphicFactory, hillsRenderConfig);
    }

    /**
     * Creates a TileRendererLayer.
     *  @param tileCache       cache where tiles are stored
     * @param mapDataStore    the mapsforge map file
     * @param mapViewPosition the mapViewPosition to know which tiles to render
     * @param isTransparent   true if the tile should have an alpha/transparency
     * @param renderLabels    true if labels should be rendered onto tiles
     * @param cacheLabels     true if labels should be cached in a LabelStore
     * @param graphicFactory  the graphicFactory to carry out platform specific operations
     * @param hillsRenderConfig the hillshading setup to be used (can be null)
     */
    public TileRendererLayer(TileCache tileCache, MapDataStore mapDataStore, MapViewPosition mapViewPosition,
                             boolean isTransparent, boolean renderLabels, boolean cacheLabels,
                             GraphicFactory graphicFactory, HillsRenderConfig hillsRenderConfig) {
        super(tileCache, mapViewPosition, graphicFactory.createMatrix(), isTransparent);

        this.graphicFactory = graphicFactory;
        this.mapDataStore = mapDataStore;
        if (cacheLabels) {
            this.tileBasedLabelStore = new TileBasedLabelStore(tileCache.getCapacityFirstLevel());
        } else {
            this.tileBasedLabelStore = null;
        }
        this.databaseRenderer = new DatabaseRenderer(this.mapDataStore, graphicFactory, tileCache, tileBasedLabelStore, renderLabels, cacheLabels, hillsRenderConfig);
        this.textScale = 1;
    }
    /**
     * Creates a TileRendererLayer.
     *  @param tileCache       cache where tiles are stored
     * @param mapDataStore    the mapsforge map file
     * @param mapViewPosition the mapViewPosition to know which tiles to render
     * @param isTransparent   true if the tile should have an alpha/transparency
     * @param renderLabels    true if labels should be rendered onto tiles
     * @param cacheLabels     true if labels should be cached in a LabelStore
     * @param graphicFactory  the graphicFactory to carry out platform specific operations
     */
    public TileRendererLayer(TileCache tileCache, MapDataStore mapDataStore, MapViewPosition mapViewPosition,
                             boolean isTransparent, boolean renderLabels, boolean cacheLabels,
                             GraphicFactory graphicFactory){
        this(tileCache, mapDataStore, mapViewPosition, isTransparent, renderLabels, cacheLabels,graphicFactory, null);
    }

    /**
     * Labels can be stored in a LabelStore for rendering on a separate Layer.
     *
     * @return the LabelStore used for storing labels, null otherwise.
     */
    public LabelStore getLabelStore() {
        return tileBasedLabelStore;
    }

    public MapDataStore getMapDataStore() {
        return mapDataStore;
    }

    public float getTextScale() {
        return this.textScale;
    }

    @Override
    public void onDestroy() {
        if (this.renderThemeFuture != null) {
            this.renderThemeFuture.decrementRefCount();
        }
        this.mapDataStore.close();
        super.onDestroy();
    }

    @Override
    public synchronized void setDisplayModel(DisplayModel displayModel) {
        super.setDisplayModel(displayModel);
        if (displayModel != null) {
            compileRenderTheme();
            if (this.mapWorkerPool == null) {
                this.mapWorkerPool = new MapWorkerPool(this.tileCache, this.jobQueue, this.databaseRenderer, this);
            }
            this.mapWorkerPool.start();
        } else {
            // if we do not have a displayModel any more we can stop rendering.
            if (this.mapWorkerPool != null) {
                this.mapWorkerPool.stop();
            }
        }
    }

    public void setTextScale(float textScale) {
        this.textScale = textScale;
    }

    public void setXmlRenderTheme(XmlRenderTheme xmlRenderTheme) {
        this.xmlRenderTheme = xmlRenderTheme;
        compileRenderTheme();
    }

    protected void compileRenderTheme() {
        this.renderThemeFuture = new RenderThemeFuture(this.graphicFactory, this.xmlRenderTheme, this.displayModel);
        new Thread(this.renderThemeFuture).start();
    }

    public RenderThemeFuture getRenderThemeFuture() {
        return this.renderThemeFuture;
    }

    @Override
    protected RendererJob createJob(Tile tile) {
        return new RendererJob(tile, this.mapDataStore, this.renderThemeFuture, this.displayModel, this.textScale,
                this.isTransparent, false);
    }

    /**
     * Whether the tile is stale and should be refreshed.
     * <p/>
     * This method is called from {@link #draw(org.mapsforge.core.model.BoundingBox, byte, org.mapsforge.core.graphics.Canvas, org.mapsforge.core.model.Point)} to determine whether the tile needs to
     * be refreshed.
     * <p/>
     * A tile is considered stale if the timestamp of the layer's {@link #mapDataStore} is more recent than the
     * {@code bitmap}'s {@link org.mapsforge.core.graphics.TileBitmap#getTimestamp()}.
     * <p/>
     * When a tile has become stale, the layer will first display the tile referenced by {@code bitmap} and attempt to
     * obtain a fresh copy in the background. When a fresh copy becomes available, the layer will replace is and update
     * the cache. If a fresh copy cannot be obtained for whatever reason, the stale tile will continue to be used until
     * another {@code #draw(BoundingBox, byte, Canvas, Point)} operation requests it again.
     *
     * @param tile   A tile.
     * @param bitmap The bitmap for {@code tile} currently held in the layer's cache.
     */
    @Override
    protected boolean isTileStale(Tile tile, TileBitmap bitmap) {
        return this.mapDataStore.getDataTimestamp(tile) > bitmap.getTimestamp();
    }

    @Override
    protected void onAdd() {
        this.mapWorkerPool.start();
        if (tileCache != null) {
            tileCache.addObserver(this);
        }

        super.onAdd();
    }

    @Override
    protected void onRemove() {
        this.mapWorkerPool.stop();
        if (tileCache != null) {
            tileCache.removeObserver(this);
        }
        super.onRemove();
    }

    @Override
    protected void retrieveLabelsOnly(RendererJob job) {
        if (this.hasJobQueue && this.tileBasedLabelStore != null && this.tileBasedLabelStore.requiresTile(job.tile)) {
            job.setRetrieveLabelsOnly();
            this.jobQueue.add(job);
        }
    }

    @Override
    public void onChange() {
        this.requestRedraw();
    }
}
