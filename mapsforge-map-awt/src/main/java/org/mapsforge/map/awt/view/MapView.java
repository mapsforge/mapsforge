/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright 2014 Ludwig M Brinckmann
 * Copyright 2014-2018 devemux86
 * Copyright 2018 mikes222
 * Copyright 2020 Lukas Bai
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
package org.mapsforge.map.awt.view;

import org.mapsforge.core.graphics.GraphicContext;
import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.Dimension;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.map.awt.graphics.AwtGraphicFactory;
import org.mapsforge.map.awt.input.MapViewComponentListener;
import org.mapsforge.map.awt.input.MouseEventListener;
import org.mapsforge.map.controller.FrameBufferController;
import org.mapsforge.map.controller.LayerManagerController;
import org.mapsforge.map.controller.MapViewController;
import org.mapsforge.map.layer.Layer;
import org.mapsforge.map.layer.LayerManager;
import org.mapsforge.map.layer.TileLayer;
import org.mapsforge.map.layer.labels.LabelStore;
import org.mapsforge.map.layer.renderer.TileRendererLayer;
import org.mapsforge.map.model.Model;
import org.mapsforge.map.scalebar.DefaultMapScaleBar;
import org.mapsforge.map.scalebar.MapScaleBar;
import org.mapsforge.map.util.MapPositionUtil;
import org.mapsforge.map.util.MapViewProjection;
import org.mapsforge.map.view.FpsCounter;
import org.mapsforge.map.view.FrameBuffer;
import org.mapsforge.map.view.FrameBufferHA3;
import org.mapsforge.map.view.InputListener;

import java.awt.Container;
import java.awt.Graphics;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class MapView extends Container implements org.mapsforge.map.view.MapView {

    private static final GraphicFactory GRAPHIC_FACTORY = AwtGraphicFactory.INSTANCE;
    private static final long serialVersionUID = 1L;

    private final FpsCounter fpsCounter;
    private final FrameBuffer frameBuffer;
    private final FrameBufferController frameBufferController;
    private final List<InputListener> inputListeners = new CopyOnWriteArrayList<>();
    private final LayerManager layerManager;
    private MapScaleBar mapScaleBar;
    private final MapViewProjection mapViewProjection;
    private final Model model;

    public MapView() {
        super();

        this.model = new Model();

        this.fpsCounter = new FpsCounter(GRAPHIC_FACTORY, this.model.displayModel);

        this.frameBuffer = new FrameBufferHA3(this.model.frameBufferModel, this.model.displayModel, GRAPHIC_FACTORY);

        this.frameBufferController = FrameBufferController.create(this.frameBuffer, this.model);

        this.layerManager = new LayerManager(this, this.model.mapViewPosition, GRAPHIC_FACTORY);
        this.layerManager.start();
        LayerManagerController.create(this.layerManager, this.model);

        MapViewController.create(this, this.model);

        this.mapScaleBar = new DefaultMapScaleBar(this.model.mapViewPosition, this.model.mapViewDimension, GRAPHIC_FACTORY,
                this.model.displayModel);

        this.mapViewProjection = new MapViewProjection(this);

        addListeners();
    }

    public void addInputListener(InputListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("listener must not be null");
        } else if (this.inputListeners.contains(listener)) {
            throw new IllegalArgumentException("listener is already registered: " + listener);
        }
        this.inputListeners.add(listener);
    }

    @Override
    public void addLayer(Layer layer) {
        this.layerManager.getLayers().add(layer);
    }

    public void addListeners() {
        addComponentListener(new MapViewComponentListener(this));

        MouseEventListener mouseEventListener = new MouseEventListener(this);
        addMouseListener(mouseEventListener);
        addMouseMotionListener(mouseEventListener);
        addMouseWheelListener(mouseEventListener);
    }

    /**
     * Clear map view.
     */
    @Override
    public void destroy() {
        this.layerManager.finish();
        this.frameBufferController.destroy();
        this.frameBuffer.destroy();
        if (this.mapScaleBar != null) {
            this.mapScaleBar.destroy();
        }
        this.getModel().mapViewPosition.destroy();
    }

    /**
     * Clear all map view elements.<br/>
     * i.e. layers, tile cache, label store, map view, resources, etc.
     */
    @Override
    public void destroyAll() {
        for (Layer layer : this.layerManager.getLayers()) {
            this.layerManager.getLayers().remove(layer);
            layer.onDestroy();
            if (layer instanceof TileLayer) {
                ((TileLayer<?>) layer).getTileCache().destroy();
            }
            if (layer instanceof TileRendererLayer) {
                LabelStore labelStore = ((TileRendererLayer) layer).getLabelStore();
                if (labelStore != null) {
                    labelStore.clear();
                }
            }
        }
        destroy();
    }

    @Override
    public BoundingBox getBoundingBox() {
        return MapPositionUtil.getBoundingBox(this.model.mapViewPosition.getMapPosition(),
                getDimension(), this.model.displayModel.getTileSize());
    }

    @Override
    public Dimension getDimension() {
        return new Dimension(getWidth(), getHeight());
    }

    @Override
    public FpsCounter getFpsCounter() {
        return this.fpsCounter;
    }

    @Override
    public FrameBuffer getFrameBuffer() {
        return this.frameBuffer;
    }

    @Override
    public LayerManager getLayerManager() {
        return this.layerManager;
    }

    @Override
    public MapScaleBar getMapScaleBar() {
        return this.mapScaleBar;
    }

    @Override
    public MapViewProjection getMapViewProjection() {
        return this.mapViewProjection;
    }

    @Override
    public Model getModel() {
        return this.model;
    }

    /**
     * This method is called by internal programs only. The underlying mapView implementation will
     * notify registered {@link InputListener} about the start of a manual move.
     * Note that this method may be called multiple times while the move has been started.
     * Also note that only manual moves get notified.
     */
    public void onMoveEvent() {
        for (InputListener listener : inputListeners) {
            listener.onMoveEvent();
        }
    }

    /**
     * This method is called by internal programs only. The underlying mapView implementation will
     * notify registered {@link InputListener} about the start of a manual zoom.
     * Note that this method may be called multiple times while the zoom has been started.
     * Also note that only manual zooms get notified.
     */
    public void onZoomEvent() {
        for (InputListener listener : inputListeners) {
            listener.onZoomEvent();
        }
    }

    @Override
    public void paint(Graphics graphics) {
        super.paint(graphics);

        GraphicContext graphicContext = AwtGraphicFactory.createGraphicContext(graphics);
        this.frameBuffer.draw(graphicContext);
        if (this.mapScaleBar != null) {
            this.mapScaleBar.draw(graphicContext);
        }
        this.fpsCounter.draw(graphicContext);
    }

    public void removeInputListener(InputListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("listener must not be null");
        } else if (!this.inputListeners.contains(listener)) {
            throw new IllegalArgumentException("listener is not registered: " + listener);
        }
        this.inputListeners.remove(listener);
    }

    @Override
    public void setCenter(LatLong center) {
        this.model.mapViewPosition.setCenter(center);
    }

    @Override
    public void setMapScaleBar(MapScaleBar mapScaleBar) {
        if (this.mapScaleBar != null) {
            this.mapScaleBar.destroy();
        }
        this.mapScaleBar = mapScaleBar;
    }

    @Override
    public void setZoomLevel(byte zoomLevel) {
        this.model.mapViewPosition.setZoomLevel(zoomLevel);
    }

    @Override
    public void setZoomLevelMax(byte zoomLevelMax) {
        this.model.mapViewPosition.setZoomLevelMax(zoomLevelMax);
    }

    @Override
    public void setZoomLevelMin(byte zoomLevelMin) {
        this.model.mapViewPosition.setZoomLevelMin(zoomLevelMin);
    }
}
