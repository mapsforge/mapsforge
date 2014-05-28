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
import org.mapsforge.map.layer.Layer;

import java.util.List;

public class LabelLayer extends Layer {

	private final TileBasedLabelStore tileBasedLabelStore;
	private final Matrix matrix;

	public LabelLayer(GraphicFactory graphicFactory, TileBasedLabelStore tileBasedLabelStore) {
		this.tileBasedLabelStore = tileBasedLabelStore;
		this.tileBasedLabelStore.setLayer(this);
		this.matrix = graphicFactory.createMatrix();
	}

	@Override
	public void draw(BoundingBox boundingBox, byte zoomLevel, Canvas canvas, Point topLeftPoint) {

		List<MapElementContainer> itemsToDraw = this.tileBasedLabelStore.getVisibleItems(boundingBox, this.displayModel, zoomLevel, topLeftPoint);
		if (itemsToDraw != null) {
			for (MapElementContainer item : itemsToDraw) {
				item.draw(canvas, topLeftPoint, this.matrix);
			}
		}
	}

	@Override
	public void onAdd() {
		this.tileBasedLabelStore.startLayoutEngine();
	}

	@Override
	public void onRemove() {
		this.tileBasedLabelStore.stopLayoutEngine();
	}

}


