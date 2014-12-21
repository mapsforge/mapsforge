/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright © 2014 Ludwig M Brinckmann
 * Copyright © 2014 Christian Pesch
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
import org.mapsforge.core.model.Tile;
import org.mapsforge.map.layer.TileLayer;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.layer.labels.LabelStore;
import org.mapsforge.map.layer.labels.TileBasedLabelStore;
import org.mapsforge.map.model.DisplayModel;
import org.mapsforge.map.model.MapViewPosition;
import org.mapsforge.map.reader.MapDataStore;
import org.mapsforge.map.rendertheme.XmlRenderTheme;
import org.mapsforge.map.rendertheme.rule.RenderTheme;
import org.mapsforge.map.rendertheme.rule.RenderThemeHandler;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

public class TileRendererLayer extends TileLayer<RendererJob> {
	private final DatabaseRenderer databaseRenderer;
	private final GraphicFactory graphicFactory;
	private final MapDataStore mapDataStore;
	private MapWorker mapWorker;
	private RenderTheme renderTheme;
	private float textScale;
	private final TileBasedLabelStore tileBasedLabelStore;
	private XmlRenderTheme xmlRenderTheme;

	/**
	 * Creates a TileRendererLayer.
	 * @param tileCache cache where tiles are stored
	 * @param mapDataStore the mapsforge map file
	 * @param mapViewPosition the mapViewPosition to know which tiles to render
	 * @param isTransparent true if the tile should have an alpha/transparency
	 * @param renderLabels true if labels should be rendered onto tiles
	 * @param graphicFactory the graphicFactory to carry out platform specific operations
	 */
	public TileRendererLayer(TileCache tileCache, MapDataStore mapDataStore, MapViewPosition mapViewPosition, boolean isTransparent,
	                         boolean renderLabels, GraphicFactory graphicFactory) {
		super(tileCache, mapViewPosition, graphicFactory.createMatrix(), isTransparent);

		this.graphicFactory = graphicFactory;
		this.mapDataStore = mapDataStore;
		if (renderLabels) {
			this.tileBasedLabelStore = null;
			this.databaseRenderer = new DatabaseRenderer(this.mapDataStore, graphicFactory, tileCache);
		} else {
			this.tileBasedLabelStore = new TileBasedLabelStore(tileCache.getCapacityFirstLevel());
			this.databaseRenderer = new DatabaseRenderer(this.mapDataStore, graphicFactory, tileBasedLabelStore);
		}
		this.textScale = 1;
	}

	/**
	 * If the labels are not rendered onto the tile directly, they are stored in a LabelStore for
	 * rendering on a separate Layer.
	 * @return the LabelStore used for storing labels, null if labels are rendered onto tiles directly.
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
		new DestroyThread(this.mapWorker, this.mapDataStore, this.databaseRenderer).start();
		if (this.renderTheme != null) {
			this.renderTheme.destroy();
		}
		super.onDestroy();
	}

	@Override
	public synchronized void setDisplayModel(DisplayModel displayModel) {
		super.setDisplayModel(displayModel);
		if (displayModel != null) {
			compileRenderTheme();
			this.mapWorker = new MapWorker(this.tileCache, this.jobQueue, this.databaseRenderer, this);
			this.mapWorker.start();
		} else {
			// if we do not have a displayModel any more we can stop rendering.
			if (this.mapWorker != null) {
				this.mapWorker.interrupt();
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
		if (xmlRenderTheme == null || this.displayModel == null) {
			this.renderTheme = null;
			return;
		}
		try {
			this.renderTheme = RenderThemeHandler.getRenderTheme(this.graphicFactory, displayModel, this.xmlRenderTheme);
		} catch (XmlPullParserException e) {
			throw new IllegalArgumentException("Parse error for XML rendertheme", e);
		} catch (IOException e) {
			throw new IllegalArgumentException("File error for XML rendertheme", e);
		}
	}

	@Override
	protected RendererJob createJob(Tile tile) {
		return new RendererJob(tile, this.mapDataStore, this.renderTheme, this.displayModel, this.textScale,
				this.isTransparent, false);
	}

	@Override
	protected void onAdd() {
		this.mapWorker.proceed();
		super.onAdd();
	}

	@Override
	protected void onRemove() {
		this.mapWorker.pause();
		super.onRemove();
	}

	@Override
	protected void retrieveLabelsOnly(RendererJob job) {
		if (this.hasJobQueue && this.tileBasedLabelStore != null && this.tileBasedLabelStore.requiresTile(job.tile)) {
			job.setRetrieveLabelsOnly();
			this.jobQueue.add(job);
		}
	}

}
