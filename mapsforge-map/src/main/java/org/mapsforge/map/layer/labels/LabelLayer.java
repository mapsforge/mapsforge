/*
 * Copyright 2014-2016 Ludwig M Brinckmann
 * Copyright 2016 devemux86
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
import org.mapsforge.core.graphics.Matrix;
import org.mapsforge.core.mapelements.MapElementContainer;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.Point;
import org.mapsforge.core.model.Tile;
import org.mapsforge.map.layer.Layer;
import org.mapsforge.map.util.LayerUtil;

import java.util.Collections;
import java.util.List;

public class LabelLayer extends Layer {
    protected final LabelStore labelStore;
    protected final Matrix matrix;
    protected List<MapElementContainer> elementsToDraw;
    protected Tile upperLeft;
    protected Tile lowerRight;
    protected int lastLabelStoreVersion;

    public LabelLayer(GraphicFactory graphicFactory, LabelStore labelStore) {
        this.labelStore = labelStore;
        this.matrix = graphicFactory.createMatrix();
        this.lastLabelStoreVersion = -1;
    }

    @Override
    public void draw(BoundingBox boundingBox, byte zoomLevel, Canvas canvas, Point topLeftPoint) {
        Tile newUpperLeft = LayerUtil.getUpperLeft(boundingBox, zoomLevel, displayModel.getTileSize());
        Tile newLowerRight = LayerUtil.getLowerRight(boundingBox, zoomLevel, displayModel.getTileSize());
        if (!newUpperLeft.equals(this.upperLeft) || !newLowerRight.equals(this.lowerRight)
                || lastLabelStoreVersion != labelStore.getVersion()) {
            // only need to get new data set if either set of tiles changed or the label store
            this.upperLeft = newUpperLeft;
            this.lowerRight = newLowerRight;
            lastLabelStoreVersion = labelStore.getVersion();
            List<MapElementContainer> visibleItems = this.labelStore.getVisibleItems(upperLeft, lowerRight);

            elementsToDraw = LayerUtil.collisionFreeOrdered(visibleItems);

            // TODO this is code duplicated from CanvasRasterer::drawMapElements, should be factored out
            // what LayerUtil.collisionFreeOrdered gave us is a list where highest priority comes first,
            // so we need to reverse that in order to
            // draw elements in order of priority: lower priority first, so more important
            // elements will be drawn on top (in case of display=true) items.
            Collections.sort(elementsToDraw);
        }

        draw(canvas, topLeftPoint);
    }

    protected void draw(Canvas canvas, Point topLeftPoint) {
        for (MapElementContainer item : elementsToDraw) {
            item.draw(canvas, topLeftPoint, this.matrix, this.displayModel.getFilter());
        }
    }
}
