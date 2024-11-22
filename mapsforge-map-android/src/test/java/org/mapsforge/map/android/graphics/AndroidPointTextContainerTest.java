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
package org.mapsforge.map.android.graphics;

import android.graphics.Bitmap;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mapsforge.core.graphics.Color;
import org.mapsforge.core.graphics.Display;
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.graphics.Position;
import org.mapsforge.core.model.Point;
import org.mapsforge.core.model.Rectangle;
import org.mapsforge.core.model.Rotation;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class AndroidPointTextContainerTest {

    @Test
    public void testLabelClashBounds() {
        final double Delta = 0.0001;

        // Some paint
        final Paint paint = new AndroidPaint();
        // Need to set some fields for clashRect to be generated
        paint.setColor(Color.BLACK);
        paint.setBitmapShader(new AndroidBitmap(1, 1, Bitmap.Config.ARGB_8888));

        // Some rotation
        final Rotation rotation = new Rotation(37, 115, 93);

        // Some container ("label") positioned ABOVE
        final AndroidPointTextContainer container = new AndroidPointTextContainer(new Point(211, 143), 9, 17, Display.ALWAYS, 5, "some label", paint, null, null, Position.ABOVE, 150);

        // Some origin
        final Point origin = new Point(17, 19);

        // Imitate drawing the label
        final Point labelPosition = AndroidPointTextContainer.getRotRelPosition(container, origin.x, origin.y, rotation);
        // Coordinates are chosen for the ABOVE position
        final Rectangle labelRect = new Rectangle(labelPosition.x, labelPosition.y, labelPosition.x + container.boundary.getWidth(), labelPosition.y + container.boundary.getHeight());

        // Create the clash rectangle
        final Rectangle clashRect = AndroidPointTextContainer.getClashRect(container, rotation);

        assert clashRect != null;

        // Imitate drawing the clash rectangle as a label
        final Rectangle clashRectAsLabel = AndroidPointTextContainer.getClashRectTransformed(container, origin.x, origin.y, rotation);

        Assert.assertEquals("Label and clash rect centers do not match!", labelRect.getCenterX(), clashRectAsLabel.getCenterX(), Delta);
        Assert.assertEquals("Label and clash rect widths not equal!", labelRect.getWidth(), clashRectAsLabel.getWidth(), Delta);
        Assert.assertEquals("Label and clash rect heights not equal!", labelRect.getHeight(), clashRectAsLabel.getHeight(), Delta);
    }

    @Test
    public void testLabelsClash() {
        final double Delta = 0.0001;

        // Some paint
        final Paint paint = new AndroidPaint();
        // Need to set some fields for clashRect to be generated
        paint.setColor(Color.BLACK);
        paint.setBitmapShader(new AndroidBitmap(1, 1, Bitmap.Config.ARGB_8888));
        paint.setTextSize(15);

        // Some rotation
        final Rotation rotation = new Rotation(37, 115, 93);

        // Single-line
        {
            // Some containers ("labels") positioned ABOVE
            final AndroidPointTextContainer container1 = new AndroidPointTextContainer(new Point(211, 143), 9, 17, Display.ALWAYS, 5, "12345 67890", paint, null, null, Position.ABOVE, 150);
            final AndroidPointTextContainer container2 = new AndroidPointTextContainer(new Point(207, 140), 9, 17, Display.ALWAYS, 5, "12345 67890", paint, null, null, Position.ABOVE, 150);

            assert !container1.isMultiline;
            assert !container2.isMultiline;

            // Create the clash rectangles
            Rectangle clashRect1 = AndroidPointTextContainer.getClashRect(container1, rotation);
            Rectangle clashRect2 = AndroidPointTextContainer.getClashRect(container2, rotation);

            assert clashRect1 != null;
            assert clashRect2 != null;

            // Only during testing it happens that clash rectangles have zero height
            if (clashRect1.getHeight() < Delta || clashRect2.getHeight() < Delta) {
                clashRect1 = clashRect1.enlarge(0, 0, 0, 50);
                clashRect2 = clashRect2.enlarge(0, 0, 0, 50);
            }

            Assert.assertTrue("Labels do not clash, but they should (single line).", clashRect1.intersects(clashRect2));
        }

        // Multi-line
        {
            // Some containers ("labels") positioned ABOVE
            final AndroidPointTextContainer container1 = new AndroidPointTextContainer(new Point(211, 143), 9, 17, Display.ALWAYS, 5, "12345 67890", paint, null, null, Position.ABOVE, 20);
            final AndroidPointTextContainer container2 = new AndroidPointTextContainer(new Point(207, 140), 9, 17, Display.ALWAYS, 5, "12345 67890", paint, null, null, Position.ABOVE, 20);

            assert container1.isMultiline;
            assert container2.isMultiline;

            // Create the clash rectangles
            Rectangle clashRect1 = AndroidPointTextContainer.getClashRect(container1, rotation);
            Rectangle clashRect2 = AndroidPointTextContainer.getClashRect(container2, rotation);

            assert clashRect1 != null;
            assert clashRect2 != null;

            // Only during testing it happens that clash rectangles have zero height
            if (clashRect1.getHeight() < Delta || clashRect2.getHeight() < Delta) {
                clashRect1 = clashRect1.enlarge(0, 0, 0, 50);
                clashRect2 = clashRect2.enlarge(0, 0, 0, 50);
            }

            Assert.assertTrue("Labels do not clash, but they should (multi line).", clashRect1.intersects(clashRect2));
        }
    }
}
