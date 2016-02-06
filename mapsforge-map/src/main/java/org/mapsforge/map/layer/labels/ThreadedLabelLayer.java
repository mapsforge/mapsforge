/*
 * Copyright 2016 Ludwig M Brinckmann
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

import org.mapsforge.core.graphics.Canvas;
import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.mapelements.MapElementContainer;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.Point;
import org.mapsforge.core.model.Tile;
import org.mapsforge.map.util.LayerUtil;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * This ThreadedLabelLayer employs a single thread to request new data from the LabelStore. In case
 * the previously displayed data is still valid for at least some part of the map, the old data is
 * drawn first. When the thread delivers the data for the new area, the layer is redrawn.
 * The layer keeps track of the requested area and will not request the same area multiple times. Also
 * the queue length is limited to one, so outdated requests will not be started.
 */
public class ThreadedLabelLayer extends LabelLayer {

    ExecutorService executorService;
    Future<?> future;
    Tile requestedUpperLeft;
    Tile requestedLowerRight;

    public ThreadedLabelLayer(GraphicFactory graphicFactory, LabelStore labelStore) {
        super(graphicFactory, labelStore);
        this.executorService = Executors.newSingleThreadExecutor();
    }

    @Override
    public void draw(BoundingBox boundingBox, byte zoomLevel, Canvas canvas, Point topLeftPoint) {
        Tile newUpperLeft = LayerUtil.getUpperLeft(boundingBox, zoomLevel, this.displayModel.getTileSize());
        Tile newLowerRight = LayerUtil.getLowerRight(boundingBox, zoomLevel, this.displayModel.getTileSize());
        if (!newUpperLeft.equals(this.upperLeft) || !newLowerRight.equals(this.lowerRight)
                || this.lastLabelStoreVersion != this.labelStore.getVersion()) {
            getData(newUpperLeft, newLowerRight);
        }

        if (this.upperLeft != null && Tile.tileAreasOverlap(this.upperLeft, this.lowerRight, newUpperLeft, newLowerRight)) {
            // draw data if the area of data overlaps, even if the areas are not the same. This
            // is to make layer more responsive.
            draw(canvas, topLeftPoint);
        }
    }

    protected void getData(final Tile upperLeft, final Tile lowerRight) {
        if (upperLeft.equals(this.requestedUpperLeft) && lowerRight.equals(this.requestedLowerRight)) {
            // same data already requested
            return;
        }

        this.requestedUpperLeft = upperLeft;
        this.requestedLowerRight = lowerRight;

        if (this.future != null) {
            // we only want a single item in the queue, no point retrieving data that is not required
            // any more
            this.future.cancel(false);
        }

        this.future = executorService.submit(new Runnable() {
            public void run() {
                List<MapElementContainer> visibleItems = ThreadedLabelLayer.this.labelStore.getVisibleItems(upperLeft, lowerRight);

                ThreadedLabelLayer.this.elementsToDraw = LayerUtil.collisionFreeOrdered(visibleItems);

                // TODO this is code duplicated from CanvasRasterer::drawMapElements, should be factored out
                // what LayerUtil.collisionFreeOrdered gave us is a list where highest priority comes first,
                // so we need to reverse that in order to
                // draw elements in order of priority: lower priority first, so more important
                // elements will be drawn on top (in case of display=true) items.
                Collections.sort(ThreadedLabelLayer.this.elementsToDraw);
                ThreadedLabelLayer.this.upperLeft = upperLeft;
                ThreadedLabelLayer.this.lowerRight = lowerRight;
                ThreadedLabelLayer.this.lastLabelStoreVersion = labelStore.getVersion();
                ThreadedLabelLayer.this.requestRedraw();

            }
        });
    }

    @Override
    public void onDestroy() {
        this.executorService.shutdownNow();
    }
}
