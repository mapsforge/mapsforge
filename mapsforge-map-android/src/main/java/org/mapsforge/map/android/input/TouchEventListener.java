/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
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
package org.mapsforge.map.android.input;

import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Point;

/**
 * Time-consuming operations should be performed in a separate thread.
 */
public interface TouchEventListener {
	void onActionUp(LatLong latLong, Point point, long eventTime, boolean moveThresholdReached);

	void onLongPress(LatLong latLong, Point xy);

	void onPointerDown(long eventTime);

	void onPointerUp(long eventTime);
}
