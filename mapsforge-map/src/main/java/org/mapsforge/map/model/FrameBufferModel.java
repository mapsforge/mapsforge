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
package org.mapsforge.map.model;

import org.mapsforge.core.model.Dimension;
import org.mapsforge.core.model.MapPosition;
import org.mapsforge.map.model.common.Observable;

public class FrameBufferModel extends Observable {
	private Dimension dimension;
	private MapPosition mapPosition;
	private double overdrawFactor = 1.2;

	/**
	 * @return the current dimension of the {@code FrameBuffer} (may be null).
	 */
	public synchronized Dimension getDimension() {
		return this.dimension;
	}

	/**
	 * @return the current {@code MapPosition} of the {@code FrameBuffer} (may be null).
	 */
	public synchronized MapPosition getMapPosition() {
		return this.mapPosition;
	}

	public synchronized double getOverdrawFactor() {
		return this.overdrawFactor;
	}

	public void setDimension(Dimension dimension) {
		synchronized (this) {
			this.dimension = dimension;
		}
		notifyObservers();
	}

	public void setMapPosition(MapPosition mapPosition) {
		synchronized (this) {
			this.mapPosition = mapPosition;
		}
		notifyObservers();
	}

	/**
	 * @throws IllegalArgumentException
	 *             if the {@code overdrawFactor} is less or equal zero.
	 */
	public void setOverdrawFactor(double overdrawFactor) {
		if (overdrawFactor <= 0) {
			throw new IllegalArgumentException("overdrawFactor must be > 0: " + overdrawFactor);
		}
		synchronized (this) {
			this.overdrawFactor = overdrawFactor;
		}
		notifyObservers();
	}
}
