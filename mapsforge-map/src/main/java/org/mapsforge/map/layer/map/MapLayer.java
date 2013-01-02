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
package org.mapsforge.map.layer.map;

import java.util.Random;

import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.GeoPoint;
import org.mapsforge.core.model.MapPosition;
import org.mapsforge.core.model.Tile;
import org.mapsforge.core.util.MercatorProjection;
import org.mapsforge.map.layer.Layer;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;

public class MapLayer extends Layer {
	@Override
	public void draw(BoundingBox boundingBox, MapPosition mapPosition, Canvas canvas) {
		byte zoomLevel = mapPosition.zoomLevel;

		long tileLeft = MercatorProjection.longitudeToTileX(boundingBox.minLongitude, zoomLevel);
		long tileTop = MercatorProjection.latitudeToTileY(boundingBox.maxLatitude, zoomLevel);
		long tileRight = MercatorProjection.longitudeToTileX(boundingBox.maxLongitude, zoomLevel);
		long tileBottom = MercatorProjection.latitudeToTileY(boundingBox.minLatitude, zoomLevel);

		GeoPoint geoPoint = mapPosition.geoPoint;
		double pixelX = MercatorProjection.longitudeToPixelX(geoPoint.longitude, zoomLevel) - canvas.getWidth() / 2;
		double pixelY = MercatorProjection.latitudeToPixelY(geoPoint.latitude, zoomLevel) - canvas.getHeight() / 2;

		for (long tileX = tileLeft; tileX <= tileRight; ++tileX) {
			for (long tileY = tileTop; tileY <= tileBottom; ++tileY) {
				double longitude = MercatorProjection.tileXToLongitude(tileX, zoomLevel);
				double latitude = MercatorProjection.tileYToLatitude(tileY, zoomLevel);

				double pixelX2 = MercatorProjection.longitudeToPixelX(longitude, zoomLevel);
				double pixelY2 = MercatorProjection.latitudeToPixelY(latitude, zoomLevel);

				float x = (float) (pixelX2 - pixelX);
				float y = (float) (pixelY2 - pixelY);

				Path path = new Path();
				path.moveTo(x, y);
				path.lineTo(x + Tile.TILE_SIZE, y);
				path.lineTo(x + Tile.TILE_SIZE, y + Tile.TILE_SIZE);
				path.lineTo(x, y + Tile.TILE_SIZE);
				path.lineTo(x, y);

				Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
				Random random = new Random(new Tile(tileX, tileY, zoomLevel).hashCode());
				int color = Color.argb(random.nextInt(), random.nextInt(), random.nextInt(), random.nextInt());
				paint.setColor(color);
				paint.setStyle(Style.FILL);
				canvas.drawPath(path, paint);
			}
		}
	}
}
