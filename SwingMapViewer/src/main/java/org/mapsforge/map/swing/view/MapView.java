/*
 * Copyright 2010, 2011, 2012 mapsforge.org
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
import java.awt.Graphics2D;

import org.mapsforge.core.graphics.Canvas;
import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.map.awt.AwtGraphicFactory;
import org.mapsforge.map.controller.FrameBufferController;
import org.mapsforge.map.controller.LayerManagerController;
import org.mapsforge.map.controller.MapViewController;
import org.mapsforge.map.layer.LayerManager;
import org.mapsforge.map.model.Model;
import org.mapsforge.map.view.FpsCounter;
import org.mapsforge.map.view.FrameBuffer;

public class MapView extends Container implements org.mapsforge.map.view.MapView {
	private static final GraphicFactory GRAPHIC_FACTORY = AwtGraphicFactory.INSTANCE;
	private static final long serialVersionUID = 1L;

	private final FpsCounter fpsCounter;
	private final FrameBuffer frameBuffer;
	private final LayerManager layerManager;

	public MapView(Model model) {
		super();

		this.fpsCounter = new FpsCounter(GRAPHIC_FACTORY);
		this.frameBuffer = new FrameBuffer(model.frameBufferModel, GRAPHIC_FACTORY);
		new FrameBufferController(this.frameBuffer, model);

		this.layerManager = new LayerManager(this, model.mapViewPosition, GRAPHIC_FACTORY);
		this.layerManager.start();
		new LayerManagerController(this.layerManager, model);

		new MapViewController(this, model);
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
	public void paint(Graphics graphics) {
		super.paint(graphics);

		Canvas canvas = AwtGraphicFactory.createCanvas((Graphics2D) graphics);
		this.frameBuffer.draw(canvas);
		this.fpsCounter.draw(canvas);
	}
}
