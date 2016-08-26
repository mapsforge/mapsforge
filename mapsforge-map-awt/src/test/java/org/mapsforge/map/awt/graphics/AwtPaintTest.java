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
package org.mapsforge.map.awt.graphics;

import org.junit.Assert;
import org.junit.Test;
import org.mapsforge.core.graphics.Bitmap;
import org.mapsforge.core.graphics.Color;
import org.mapsforge.core.graphics.Paint;

public class AwtPaintTest {
    @Test
    public void isTransparentTest() {
        Paint paint = new AwtPaint();
        Assert.assertFalse(paint.isTransparent());

        paint.setColor(Color.TRANSPARENT);
        Assert.assertTrue(paint.isTransparent());

        Bitmap bitmap = org.mapsforge.map.awt.graphics.AwtGraphicFactory.INSTANCE.createBitmap(1, 1);
        paint.setBitmapShader(bitmap);
        Assert.assertFalse(paint.isTransparent());
    }
}
