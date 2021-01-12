/*
 * Copyright 2014 Christian Pesch
 * Copyright 2014-2021 devemux86
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

public class NauticalImperialUnitAdapter implements DistanceUnitAdapter {
    public static final NauticalImperialUnitAdapter INSTANCE = new NauticalImperialUnitAdapter();
    public static final double METER_FOOT_RATIO = 0.3048;
    public static final int ONE_MILE = 6076;
    public static final int[] SCALE_BAR_VALUES = {30380000, 12152000, 6076000, 3038000, 1215200, 607600, 303800,
            121520, 60760, 30380, 12152, 6076, 3038, 1000, 500, 200, 100, 50, 20, 10, 5, 2, 1};

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
        if (mapScaleValue < ONE_MILE / 2) {
            return mapScaleValue + " ft";
        }
        if (mapScaleValue == ONE_MILE / 2) {
            return "0.5 nmi";
        }
        return (mapScaleValue / ONE_MILE) + " nmi";
    }
}
