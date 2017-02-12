/*
 * Copyright 2017 usrusr
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

import org.mapsforge.core.graphics.Bitmap;
import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.util.IOUtils;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Currently just a really simple slope-to-lightness.
 */
public class SimpleShadingAlgortithm implements ShadingAlgorithm {

    private static final Logger LOGGER = Logger.getLogger(SimpleShadingAlgortithm.class.getName());

    @Override
    public Bitmap convertTile(RawHillTileSource source, GraphicFactory graphicFactory) {
        long size = source.getSize();
        long elements = size / 2;
        int rowLen = (int) Math.ceil(Math.sqrt(elements));
        if (rowLen * rowLen * 2 != size) {
            return null;
        }
        BufferedInputStream in = null;
        try {
            in = source.openInputStream();
            return convert(in, size, graphicFactory);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
            return null;
        } finally {
            IOUtils.closeQuietly(in);
        }
    }

    private static Bitmap convert(InputStream in, long streamLen, GraphicFactory graphicFactory) throws IOException {
        byte[] bytes;
        int axisLength;
        int rowLen = (int) Math.ceil(Math.sqrt(streamLen / 2));

        axisLength = rowLen - 1;
        short[] ringbuffer = new short[rowLen];
        bytes = new byte[axisLength * axisLength];

        DataInputStream din = new DataInputStream(in);

        int outidx = 0;
        int rbcur = 0;
        {
            short last = 0;
            for (int col = 0; col < rowLen; col++) {
                last = readNext(din, last);
                ringbuffer[rbcur++] = last;
            }
        }
        for (int line = 1; line <= axisLength; line++) {
            if (rbcur >= rowLen) {
                rbcur = 0;
            }

            short nw = ringbuffer[rbcur];
            short sw = readNext(din, nw);
            ringbuffer[rbcur++] = sw;

            for (int col = 1; col <= axisLength; col++) {
                short ne = ringbuffer[rbcur];
                short se = readNext(din, ne);
                ringbuffer[rbcur++] = se;

                int noso = -((se - ne) + (sw - nw));

                int eawe = -((ne - nw) + (se - sw));

                int intVal = Math.min(255, Math.max(0, noso + eawe + 128));

                int shade = intVal & 0xFF;

                bytes[outidx++] = (byte) shade;

                nw = ne;
                sw = se;
            }
        }
        return graphicFactory.createMonoBitmap(axisLength, axisLength, bytes);
    }

    private static short readNext(DataInputStream din, short fallback) throws IOException {
        short read = din.readShort();
        if (read == Short.MIN_VALUE)
            return fallback;
        return read;
    }
}
