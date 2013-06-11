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
package org.mapsforge.map.scalebar;

public final class Imperial implements Adapter {
	public static final Imperial INSTANCE = new Imperial();
	private static final double METER_FOOT_RATIO = 0.3048;
	private static final int ONE_MILE = 5280;
	private static final int[] SCALE_BAR_VALUES = { 26400000, 10560000, 5280000, 2640000, 1056000, 528000, 264000,
			105600, 52800, 26400, 10560, 5280, 2000, 1000, 500, 200, 100, 50, 20, 10, 5, 2, 1 };

	private Imperial() {
		// do nothing
	}

	@Override
	public double getMeterRatio() {
		return METER_FOOT_RATIO;
	}

	@Override
	public int[] getScaleBarValues() {
		return SCALE_BAR_VALUES;
	}

	@Override
	public String getScaleText(int mapScaleValue) {
		if (mapScaleValue < ONE_MILE) {
			return mapScaleValue + " ft";
		}
		return (mapScaleValue / ONE_MILE) + " mi";
	}
}
