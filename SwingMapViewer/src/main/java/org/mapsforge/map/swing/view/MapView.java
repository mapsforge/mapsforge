/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright 2014 Ludwig M Brinckmann
 * Copyright 2014, 2015 devemux86
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
package org.mapsforge.map.swing.view;

import java.awt.Container;
import java.awt.Graphics;

import org.mapsforge.core.graphics.GraphicContext;
import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.Dimension;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.map.awt.AwtGraphicFactory;
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
import org.mapsforge.map.view.FpsCounter;
import org.mapsforge.map.view.FrameBuffer;

public class MapView extends Container implements org.mapsforge.map.view.MapView {

	private static final GraphicFactory GRAPHIC_FACTORY = AwtGraphicFactory.INSTANCE;
	private static final long serialVersionUID = 1L;

	private final FpsCounter fpsCounter;
	private final FrameBuffer frameBuffer;
	private final FrameBufferController frameBufferController;
	private final LayerManager layerManager;
	private MapScaleBar mapScaleBar;
	private final Model model;

	public MapView() {
		super();

		this.model = new Model();

		this.fpsCounter = new FpsCounter(GRAPHIC_FACTORY, this.model.displayModel);
		this.frameBuffer = new FrameBuffer(this.model.frameBufferModel, this.model.displayModel, GRAPHIC_FACTORY);
		this.frameBufferController = FrameBufferController.create(this.frameBuffer, this.model);

		this.layerManager = new LayerManager(this, this.model.mapViewPosition, GRAPHIC_FACTORY);
		this.layerManager.start();
		LayerManagerController.create(this.layerManager, this.model);

		MapViewController.create(this, this.model);

		this.mapScaleBar = new DefaultMapScaleBar(this.model.mapViewPosition, this.model.mapViewDimension, GRAPHIC_FACTORY,
				this.model.displayModel);
	}

	@Override
	public void addLayer(Layer layer) {
		this.layerManager.getLayers().add(layer);
	}

	/**
	 * Clear map view.
	 */
	@Override
	public void destroy() {
		this.layerManager.interrupt();
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
		AwtGraphicFactory.clearResourceMemoryCache();
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
	public Model getModel() {
		return this.model;
	}

	@Override
	public void paint(Graphics graphics) {
		super.paint(graphics);

		GraphicContext graphicContext = AwtGraphicFactory.createGraphicContext(graphics);
		this.frameBuffer.draw(graphicContext);
		this.mapScaleBar.draw(graphicContext);
		this.fpsCounter.draw(graphicContext);
	}

	@Override
	public void setCenter(LatLong center) {
		this.model.mapViewPosition.setCenter(center);
	}

	@Override
	public void setMapScaleBar(MapScaleBar mapScaleBar) {
		this.mapScaleBar.destroy();
		this.mapScaleBar=mapScaleBar;
	}

	@Override
	public void setZoomLevel(byte zoomLevel) {
		this.model.mapViewPosition.setZoomLevel(zoomLevel);
	}
}
