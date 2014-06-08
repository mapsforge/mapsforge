/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
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
package org.mapsforge.map.layer.renderer;

import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Point;
import org.mapsforge.core.model.Tag;
import org.mapsforge.core.model.Tile;
import org.mapsforge.core.util.MercatorProjection;
import org.mapsforge.map.reader.Way;

import java.util.ArrayList;
import java.util.List;

/**
 * A PolylineContainer encapsulates the way data retrieved from a map file.
 *
 * The class uses deferred evaluation for computing the absolute and relative
 * pixel coordinates of the way as many ways will not actually be rendered on a
 * map. In order to save memory, after evaluation, the internally stored way is
 * released.
 */

public class PolylineContainer implements ShapeContainer {

	private Point center;
	private List<List<Point>> coordinatesAbsolute;
	private List<List<Point>> coordinatesRelativeToTile;
	private final List<Tag> tags;
	private final byte layer;
	private final Tile tile;
	private final boolean isClosedWay;
	private Way way;

	PolylineContainer(Way way, Tile tile) {
		this.tags = way.tags;
		this.tile = tile;
		layer = way.layer;
		this.way = way;
		this.isClosedWay = isClosedWay(way.latLongs[0]);
	}

	PolylineContainer(List<Point> coordinates, Tile tile, List tags) {
		this.coordinatesAbsolute = new ArrayList<List<Point>>(1);
		this.coordinatesAbsolute.add(coordinates);
		this.tags = tags;
		this.tile = tile;
		this.layer = 0;
		isClosedWay = coordinates.get(0).equals(coordinates.get(coordinates.size()-1));
	}

	public Point getCenterAbsolute() {
		if (null == center) {
			this.center = GeometryUtils.calculateCenterOfBoundingBox(getCoordinatesAbsolute().get(0));
		}
		return this.center;
	}

	public List<List<Point>> getCoordinatesAbsolute() {
		// deferred evaluation as some PolyLineContainers will never be drawn. However,
		// to save memory, after computing the absolute coordinates, the way is released.
		if (coordinatesAbsolute == null) {
			coordinatesAbsolute = new ArrayList<List<Point>>(way.latLongs.length);
			for (int i = 0; i < way.latLongs.length; ++i) {
				if (way.latLongs[i] == null) {
					// TODO what is happening here?
					return null;
				}
				List inner = new ArrayList<Point>(way.latLongs[i].length);
				coordinatesAbsolute.add(inner);
				for (int j = 0; j < way.latLongs[i].length; ++j) {
					inner.add(MercatorProjection.getPixelAbsolute(way.latLongs[i][j], tile.zoomLevel, tile.tileSize));
				}
			}
			this.way = null;
		}
		return coordinatesAbsolute;
	}

	public List<List<Point>> getCoordinatesRelativeToTile() {
		if (coordinatesRelativeToTile == null) {
			Point tileOrigin = tile.getOrigin();
			coordinatesRelativeToTile = new ArrayList<List<Point>>(getCoordinatesAbsolute());
			for (List<Point> innerList : getCoordinatesAbsolute()) {
				List<Point> inner = new ArrayList<Point>(innerList.size());
				coordinatesRelativeToTile.add(inner);
				for (Point p : innerList) {
					inner.add(p.offset(-tileOrigin.x, -tileOrigin.y));
				}
			}
		}
		return coordinatesRelativeToTile;
	}

	public byte getLayer() {
		return layer;
	}

	@Override
	public ShapeType getShapeType() {
		return ShapeType.POLYLINE;
	}

	public List<Tag> getTags() {
		return tags;
	}

	public boolean isClosedWay() {
		return isClosedWay;
	}

	public Tile getTile() {
		return tile;
	}

	private boolean isClosedWay(LatLong[] latLongs) {
		return latLongs[0].distance(latLongs[latLongs.length -1]) < 0.000000001;
	}

}
