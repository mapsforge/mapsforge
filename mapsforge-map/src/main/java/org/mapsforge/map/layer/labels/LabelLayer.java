/*
 * Copyright 2014 Ludwig M Brinckmann
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
import org.mapsforge.core.graphics.Matrix;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.Point;
import org.mapsforge.core.model.Tile;
import org.mapsforge.map.layer.Layer;
import org.mapsforge.map.util.LayerUtil;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public class LabelLayer extends Layer {

	private final LabelStore labelStore;
	private final Matrix matrix;
	private List<MapElementContainer> elementsToDraw;
	private Set<Tile> lastTileSet;
	private int lastLabelStoreVersion;


	public LabelLayer(GraphicFactory graphicFactory, LabelStore labelStore) {
		this.labelStore = labelStore;
		this.matrix = graphicFactory.createMatrix();
	}

	@Override
	public void draw(BoundingBox boundingBox, byte zoomLevel, Canvas canvas, Point topLeftPoint) {

		Set<Tile> currentTileSet = LayerUtil.getTiles(boundingBox, zoomLevel, displayModel.getTileSize());
		if (!currentTileSet.equals(lastTileSet) || lastLabelStoreVersion != labelStore.getVersion()) {
			// only need to get new data set if either set of tiles changed or the label store
			lastTileSet = currentTileSet;
			lastLabelStoreVersion = labelStore.getVersion();
			List<MapElementContainer> visibleItems = this.labelStore.getVisibleItems(currentTileSet);
			elementsToDraw = LayerUtil.collisionFreeOrdered(visibleItems);

			// TODO this is code duplicated from CanvasRasterer::drawMapElements, should be factored out
			// what LayerUtil.collisionFreeOrdered gave us is a list where highest priority comes first,
			// so we need to reverse that in order to
			// draw elements in order of priority: lower priority first, so more important
			// elements will be drawn on top (in case of display=true) items.
			Collections.sort(elementsToDraw);
		}

		for (MapElementContainer item : elementsToDraw) {
			item.draw(canvas, topLeftPoint, this.matrix);
		}
	}

}


