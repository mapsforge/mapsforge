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
package org.mapsforge.map.awt.graphics;

import org.junit.Assert;
import org.junit.Test;
import org.mapsforge.core.graphics.Color;
import org.mapsforge.core.graphics.Display;
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.graphics.Position;
import org.mapsforge.core.model.Point;
import org.mapsforge.core.model.Rectangle;
import org.mapsforge.core.model.Rotation;

public class AwtPointTextContainerTest {

    @Test
    public void testLabelClashBounds() {
        final double Delta = 0.0001;

        // Some paint
        final Paint paint = new AwtPaint();
        // Need to set some fields for clashRect to be generated
        paint.setColor(Color.BLACK);
        paint.setBitmapShader(new AwtBitmap(1, 1));
        paint.setTextSize(15);

        // Some rotation
        final Rotation rotation = new Rotation(37, 115, 93);

        // Some container ("label") positioned ABOVE
        final AwtPointTextContainer container = new AwtPointTextContainer(new Point(211, 143), 9, 17, Display.ORDER, 5, "some label", paint, null, null, Position.ABOVE, 150);

        // Some origin
        final Point origin = new Point(17, 19);

        // Imitate drawing the label
        final Point labelPosition = AwtPointTextContainer.getRotatedRelativePosition(container, origin.x, origin.y, rotation);
        // Coordinates are chosen for the ABOVE position
        final Rectangle labelRect = new Rectangle(labelPosition.x, labelPosition.y, labelPosition.x + container.boundary.getWidth(), labelPosition.y + container.boundary.getHeight());

        // Create the clash rectangle
        final Rectangle clashRect = container.getClashRect(rotation);

        assert clashRect != null;

        // Imitate drawing the clash rectangle as a label
        final Rectangle clashRectAsLabel = AwtPointTextContainer.getClashRectangleTransformed(container, origin.x, origin.y, rotation);

        Assert.assertEquals("Label and clash rect centers do not match!", labelRect.getCenterX(), clashRectAsLabel.getCenterX(), Delta);
        Assert.assertEquals("Label and clash rect widths not equal!", labelRect.getWidth(), clashRectAsLabel.getWidth(), Delta);
        Assert.assertEquals("Label and clash rect heights not equal!", labelRect.getHeight(), clashRectAsLabel.getHeight(), Delta);
    }

    @Test
    public void testLabelsClash() {
        final double Delta = 0.0001;

        // Some paint
        final Paint paint = new AwtPaint();
        // Need to set some fields for clashRect to be generated
        paint.setColor(Color.BLACK);
        paint.setBitmapShader(new AwtBitmap(1, 1));
        paint.setTextSize(15);

        // Some rotation
        final Rotation rotation = new Rotation(37, 115, 93);

        // Single-line
        {
            // Some containers ("labels") positioned ABOVE
            final AwtPointTextContainer container1 = new AwtPointTextContainer(new Point(211, 143), 9, 17, Display.ORDER, 5, "12345 67890", paint, null, null, Position.ABOVE, 150);
            final AwtPointTextContainer container2 = new AwtPointTextContainer(new Point(207, 140), 9, 17, Display.ORDER, 5, "12345 67890", paint, null, null, Position.ABOVE, 150);

            final int lines1 = container1.boxWidth / container1.maxTextWidth + 1;
            final int lines2 = container2.boxWidth / container2.maxTextWidth + 1;

            assert lines1 <= 1;
            assert lines2 <= 1;

            // Create the clash rectangles
            Rectangle clashRect1 = container1.getClashRect(rotation);
            Rectangle clashRect2 = container2.getClashRect(rotation);

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
            // Some *narrow* containers ("labels") positioned ABOVE
            final AwtPointTextContainer container1 = new AwtPointTextContainer(new Point(211, 143), 9, 17, Display.ORDER, 5, "12345 67890", paint, null, null, Position.ABOVE, 20);
            final AwtPointTextContainer container2 = new AwtPointTextContainer(new Point(207, 140), 9, 17, Display.ORDER, 5, "12345 67890", paint, null, null, Position.ABOVE, 20);

            final int lines1 = container1.boxWidth / container1.maxTextWidth + 1;
            final int lines2 = container2.boxWidth / container2.maxTextWidth + 1;

            assert lines1 > 1;
            assert lines2 > 1;

            // Create the clash rectangles
            Rectangle clashRect1 = container1.getClashRect(rotation);
            Rectangle clashRect2 = container2.getClashRect(rotation);

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
