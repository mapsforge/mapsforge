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
package org.mapsforge.map.layer.labels;

import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.mapelements.MapElementContainer;
import org.mapsforge.core.model.Tile;
import org.mapsforge.map.datastore.MapDataStore;
import org.mapsforge.map.datastore.MapReadResult;
import org.mapsforge.map.datastore.PointOfInterest;
import org.mapsforge.map.datastore.Way;
import org.mapsforge.map.layer.renderer.CanvasRasterer;
import org.mapsforge.map.layer.renderer.PolylineContainer;
import org.mapsforge.map.layer.renderer.RendererJob;
import org.mapsforge.map.layer.renderer.StandardRenderer;
import org.mapsforge.map.model.DisplayModel;
import org.mapsforge.map.rendertheme.RenderContext;
import org.mapsforge.map.rendertheme.rule.RenderThemeFuture;

import java.util.ArrayList;
import java.util.List;

/**
 * A LabelStore that reads the labels out of a MapDataStore
 */

public class MapDataStoreLabelStore implements LabelStore {

    final float textScale;
    final RenderThemeFuture renderThemeFuture;
    final StandardRenderer standardRenderer;
    final DisplayModel displayModel;

    public MapDataStoreLabelStore(MapDataStore mapDataStore, RenderThemeFuture renderThemeFuture, float textScale, DisplayModel displayModel, GraphicFactory graphicFactory) {

        this.textScale = textScale;
        this.renderThemeFuture = renderThemeFuture;
        // TODO what about way symbols, we have the problem that ways without names but symbols will not be included.
        this.standardRenderer = new StandardRenderer(mapDataStore, graphicFactory, true);
        this.displayModel = displayModel;
    }

    @Override
    public void clear() {
    }

    @Override
    public int getVersion() {
        // the mapDataStore cannot change, so version will always be the same.
        return 0;
    }

    @Override
    public synchronized List<MapElementContainer> getVisibleItems(Tile upperLeft, Tile lowerRight) {

        try {
            RendererJob rendererJob = new RendererJob(upperLeft, this.standardRenderer.mapDataStore, this.renderThemeFuture, this.displayModel, this.textScale, true, true);
            RenderContext renderContext = new RenderContext(rendererJob, new CanvasRasterer(standardRenderer.graphicFactory));

            MapReadResult mapReadResult = standardRenderer.mapDataStore.readLabels(upperLeft, lowerRight);

            if (mapReadResult == null) {
                return new ArrayList<>();
            }

            for (PointOfInterest pointOfInterest : mapReadResult.pointOfInterests) {
                renderContext.setDrawingLayers(pointOfInterest.layer);
                renderContext.rendererJob.renderThemeFuture.get().matchNode(standardRenderer, renderContext, pointOfInterest);
            }
            for (Way way : mapReadResult.ways) {
                PolylineContainer polylineContainer = new PolylineContainer(way, upperLeft, lowerRight);
                renderContext.setDrawingLayers(polylineContainer.getLayer());

                if (polylineContainer.isClosedWay()) {
                    renderContext.renderTheme.matchClosedWay(standardRenderer, renderContext, polylineContainer);
                } else {
                    renderContext.renderTheme.matchLinearWay(standardRenderer, renderContext, polylineContainer);
                }
            }

            return renderContext.labels;
        } catch (Exception e) {
            return new ArrayList<>();
        }

    }

}
