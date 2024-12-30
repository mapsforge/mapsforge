/*
 * Copyright 2024 Sublimis
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
package org.mapsforge.map.layer.hills;

import junit.framework.TestCase;

import org.junit.Assert;

import java.io.File;

public class AdaptiveClasyHillShadingTest extends TestCase {

    final AdaptiveClasyHillShading algorithm = new AdaptiveClasyHillShading();
    final long hgtFileSize = (long) 2 * (1 + AdaptiveClasyHillShading.HGTFILE_WIDTH_BASE) * (1 + AdaptiveClasyHillShading.HGTFILE_WIDTH_BASE);
    final HgtFileInfo hgtFileInfo = new HgtFileInfo(new DemFileFS(new File("dummy")), 0, 0, 1, 1, hgtFileSize);

    public void testGetQualityFactor() {
        final int tileSizePerLat = AdaptiveClasyHillShading.HGTFILE_WIDTH_BASE;

        Assert.assertEquals(2, algorithm.getQualityFactor(hgtFileInfo, 12, 1.5 * tileSizePerLat, 1.5 * tileSizePerLat));
        Assert.assertEquals(1, algorithm.getQualityFactor(hgtFileInfo, 12, tileSizePerLat, tileSizePerLat));
        Assert.assertEquals(-2, algorithm.getQualityFactor(hgtFileInfo, 12, (double) tileSizePerLat / 2, (double) tileSizePerLat / 2));
        Assert.assertEquals(-4, algorithm.getQualityFactor(hgtFileInfo, 12, (double) tileSizePerLat / 4, (double) tileSizePerLat / 4));
        Assert.assertEquals(-100, algorithm.getQualityFactor(hgtFileInfo, 12, (double) tileSizePerLat / 100, (double) tileSizePerLat / 100));
    }

    public void testScaleByQualityFactor() {
        final int value = AdaptiveClasyHillShading.HGTFILE_WIDTH_BASE;

        Assert.assertEquals(value * 2, AdaptiveClasyHillShading.scaleByQualityFactor(value, algorithm.getQualityFactor(hgtFileInfo, 12, 1.5 * value, 1.5 * value)));
        Assert.assertEquals(value, AdaptiveClasyHillShading.scaleByQualityFactor(value, algorithm.getQualityFactor(hgtFileInfo, 12, value, value)));
        Assert.assertEquals(value / 2, AdaptiveClasyHillShading.scaleByQualityFactor(value, algorithm.getQualityFactor(hgtFileInfo, 12, (double) value / 2, (double) value / 2)));
        Assert.assertEquals(value / 4, AdaptiveClasyHillShading.scaleByQualityFactor(value, algorithm.getQualityFactor(hgtFileInfo, 12, (double) value / 4, (double) value / 4)));
        Assert.assertEquals(value / 100, AdaptiveClasyHillShading.scaleByQualityFactor(value, algorithm.getQualityFactor(hgtFileInfo, 12, (double) value / 100, (double) value / 100)));
    }

    public void testIsHighQuality() {
        final int tileSizePerLat = 2 * AdaptiveClasyHillShading.HGTFILE_WIDTH_BASE;

        Assert.assertTrue(algorithm.isHighQuality(hgtFileInfo, 12, tileSizePerLat, tileSizePerLat));
    }

    public void testStrideDivisibility() {
        final int inputLen = AdaptiveClasyHillShading.HGTFILE_WIDTH_BASE;

        for (int value = AdaptiveClasyHillShading.HGTFILE_WIDTH_BASE / 2; value > 1; value--) {
            final int stride = algorithm.getStrideFactor(inputLen, algorithm.getOutputAxisLen(hgtFileInfo, 12, value, value));
            Assert.assertEquals(inputLen, inputLen / stride * stride);
        }
    }

    public void testBicubicFormulae() {
        final byte[] output = new byte[4];
        final byte[] outputBaseline = new byte[4];
        final double dsf = 1;

        // Optimized method as used in the algorithm
        {
            final AThreadedHillShading.ComputingParams params = new AThreadedHillShading.ComputingParams.Builder().setOutput(output).setOutputWidth(2).build();

            algorithm.processUnitElement_4x4(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, dsf, 0, params);
        }

        // Unoptimized baseline method
        {
            final double center = algorithm.getBicubicPoint(0.5, 0.5, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16);
            final double nwswHalf = algorithm.getBicubicPoint(0, 0.5, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16);
            final double swseHalf = algorithm.getBicubicPoint(0.5, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16);
            final double neseHalf = algorithm.getBicubicPoint(1.0, 0.5, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16);
            final double nwneHalf = algorithm.getBicubicPoint(0.5, 1.0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16);

            final double dsfDouble = 2 * dsf;

            // NW corner
            outputBaseline[0] = algorithm.unitElementToShadePixel(1, nwswHalf, center, nwneHalf, dsfDouble);
            // SW corner
            outputBaseline[2] = algorithm.unitElementToShadePixel(nwswHalf, 2, swseHalf, center, dsfDouble);

            // SE corner
            outputBaseline[3] = algorithm.unitElementToShadePixel(center, swseHalf, 3, neseHalf, dsfDouble);
            // NE corner
            outputBaseline[1] = algorithm.unitElementToShadePixel(nwneHalf, center, neseHalf, 4, dsfDouble);
        }

        Assert.assertArrayEquals(output, outputBaseline);
    }
}
