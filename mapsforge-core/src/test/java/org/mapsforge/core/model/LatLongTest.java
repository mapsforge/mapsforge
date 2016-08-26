/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright 2010, 2011, 2012 Patrick Jungermann
 * Copyright 2010, 2011, 2012 Eike Send
 * Copyright 2015 devemux86
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
package org.mapsforge.core.model;

import org.junit.Test;
import org.mapsforge.core.util.LatLongUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests related to {@link LatLong}.
 */
public class LatLongTest {
    // Here is a fun fact:
    // The original meter was defined as 1/10.000.000 the distance between the north pole
    // and the equator on the Paris meridian. The prototype which created turned out to be
    // very good. Only hundreds of years later, after the meter definition had changed, it
    // was found out that the distance measured in this meter was actually more than
    // 10.000.000 meters.
    private static final double DISTANCE_POLE_TO_EQUATOR = 10001966.0; // 10.001,966 km

    // This is the length of earth's equatorial circumference in meters.
    private static final double EARTH_EQUATOR_CIRCUMFERENCE = LatLongUtils.EQUATORIAL_RADIUS * 2 * Math.PI;

    @Test
    public void constructor_byDoubleValues_createInstance() {
        LatLong alex = new LatLong(52.52235, 13.4125);

        assertEquals(52.52235, alex.getLatitude(), 0d);
        assertEquals(13.4125, alex.getLongitude(), 0d);
    }

    @Test
    public void constructor_byIntValues_createInstance() {
        LatLong alex = LatLong.fromMicroDegrees(52522350, 13412500);

        assertEquals(52522350, alex.getLatitudeE6());
        assertEquals(13412500, alex.getLongitudeE6());
    }

    @Test
    public void constructor_byWellKnownText_createInstance() {
        LatLong alex = new LatLong("POINT(13.4125 52.52235)");

        assertEquals(52.52235, alex.getLatitude(), 0d);
        assertEquals(13.4125, alex.getLongitude(), 0d);
    }

    @Test
    public void equals_differentCoordinateValues_returnFalse() {
        LatLong alex = LatLong.fromMicroDegrees(52522350, 13412500);
        LatLong bundestag = LatLong.fromMicroDegrees(52518590, 13375400);

        assertFalse(bundestag.equals(alex));
    }

    @Test
    public void equals_differentObjectType_returnFalse() {
        LatLong latLong = LatLong.fromMicroDegrees(52518590, 13375400);

        assertFalse(latLong.equals(new Object()));
    }

    @Test
    public void equals_sameCoordinateValues_returnTrue() {
        LatLong bundestag = LatLong.fromMicroDegrees(52518590, 13375400);
        LatLong bundestag2 = new LatLong(52.518590, 13.3754);

        assertTrue(bundestag.equals(bundestag2));
    }

    @Test
    public void equals_sameInstance_returnTrue() {
        LatLong bundestag = LatLong.fromMicroDegrees(52518590, 13375400);

        assertTrue(bundestag.equals(bundestag));
    }

    @Test
    public void sphericalDistance_nearOfSriLankaToIslaGenovesa_returnHalfOfEarthEquatorCircumference() {
        // These coordinates are 1/4 Earth circumference from zero on the equator
        LatLong nearSriLanka = new LatLong(0d, 90d);
        // These coordinates are 1/4 Earth circumference from zero on the equator
        LatLong islaGenovesa = new LatLong(0d, -90d);

        // These points are as far apart as they could be, half way around the earth
        double spherical = LatLongUtils.sphericalDistance(nearSriLanka, islaGenovesa);
        assertEquals(EARTH_EQUATOR_CIRCUMFERENCE / 2, spherical, 0d);
    }

    @Test
    public void sphericalDistance_originToIslaGenovesa_returnQuarterOfEarthEquatorCircumference() {
        // This is the origin of the WGS-84 reference system
        LatLong zeroZero = new LatLong(0d, 0d);
        // These coordinates are 1/4 Earth circumference from zero on the equator
        LatLong islaGenovesa = new LatLong(0d, -90d);

        double spherical = LatLongUtils.sphericalDistance(zeroZero, islaGenovesa);
        assertEquals(EARTH_EQUATOR_CIRCUMFERENCE / 4, spherical, 0d);
    }

    @Test
    public void sphericalDistance_originToNearOfSriLanka_returnQuarterOfEarthEquatorCircumference() {
        // This is the origin of the WGS-84 reference system
        LatLong zeroZero = new LatLong(0d, 0d);
        // These coordinates are 1/4 Earth circumference from zero on the equator
        LatLong nearSriLanka = new LatLong(0d, 90d);

        double spherical = LatLongUtils.sphericalDistance(zeroZero, nearSriLanka);
        assertEquals(EARTH_EQUATOR_CIRCUMFERENCE / 4, spherical, 0d);
    }

    @Test
    public void sphericalDistance_originToNorthPole_returnQuarterOfEarthEquatorCircumference() {
        // This is the origin of the WGS-84 reference system
        LatLong zeroZero = new LatLong(0d, 0d);
        // Calculating the distance between the north pole and the equator
        LatLong northPole = new LatLong(90d, 0d);

        double spherical = LatLongUtils.sphericalDistance(zeroZero, northPole);
        assertEquals(EARTH_EQUATOR_CIRCUMFERENCE / 4, spherical, 0d);
    }

    @Test
    public void sphericalDistanceAndVincentyDistance_originToNearOfSriLanka_bothShouldBeNearlyTheSame() {
        // This is the origin of the WGS-84 reference system
        LatLong zeroZero = new LatLong(0d, 0d);
        // These coordinates are 1/4 Earth circumference from zero on the equator
        LatLong nearSriLanka = new LatLong(0d, 90d);

        // On the equator the result of the different distance calculation methods should be
        // about the same
        double spherical = LatLongUtils.sphericalDistance(zeroZero, nearSriLanka);
        double vincenty = LatLongUtils.vincentyDistance(zeroZero, nearSriLanka);
        assertEquals(spherical, vincenty, 1E-4);
    }

    @Test
    public void vincentyDistance_originToNearOfSriLanka_returnQuarterOfEarthEquatorCircumference() {
        // This is the origin of the WGS-84 reference system
        LatLong zeroZero = new LatLong(0d, 0d);
        // These coordinates are 1/4 Earth circumference from zero on the equator
        LatLong nearSriLanka = new LatLong(0d, 90d);

        double vincenty = LatLongUtils.vincentyDistance(zeroZero, nearSriLanka);
        assertEquals(EARTH_EQUATOR_CIRCUMFERENCE / 4, vincenty, 1E-4);
    }

    @Test
    public void vincentyDistance_originToNorthPole_returnDistanceFromPoleToEquator() {
        // This is the origin of the WGS-84 reference system
        LatLong zeroZero = new LatLong(0d, 0d);
        // Calculating the distance between the north pole and the equator
        LatLong northPole = new LatLong(90d, 0d);

        double vincenty = LatLongUtils.vincentyDistance(zeroZero, northPole);
        assertEquals(DISTANCE_POLE_TO_EQUATOR, vincenty, 1);
    }

    @Test
    public void vincentyDistance_southPoleToNorthPole_returnTwiceOfDistanceFromPoleToEquator() {
        // Calculating the distance between the north pole and the equator
        LatLong northPole = new LatLong(90d, 0d);
        // Check if the distance from pole to pole works as well in the vincentyDistance
        LatLong southPole = new LatLong(-90d, 0d);

        double vincenty = LatLongUtils.vincentyDistance(southPole, northPole);
        assertEquals(2 * DISTANCE_POLE_TO_EQUATOR, vincenty, 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void validateLatitude_higherThanMaxValue_throwException() {
        LatLongUtils.validateLatitude(LatLongUtils.LATITUDE_MAX + 1);
    }

    @Test
    public void validateLatitude_legalValue_returnThatValue() {
        assertEquals(10d, LatLongUtils.validateLatitude(10d), 0d);
    }

    @Test(expected = IllegalArgumentException.class)
    public void validateLatitude_lowerThanMinValue_throwException() {
        LatLongUtils.validateLatitude(LatLongUtils.LATITUDE_MIN - 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void validateLongitude_higherThanMaxValue_throwException() {
        LatLongUtils.validateLatitude(LatLongUtils.LONGITUDE_MAX + 1);
    }

    @Test
    public void validateLongitude_legalValue_returnThatValue() {
        assertEquals(10d, LatLongUtils.validateLongitude(10d), 0d);
    }

    @Test(expected = IllegalArgumentException.class)
    public void validateLongitude_lowerThanMinValue_throwException() {
        LatLongUtils.validateLatitude(LatLongUtils.LONGITUDE_MIN - 1);
    }
}
