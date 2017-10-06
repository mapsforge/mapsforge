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

import org.mapsforge.core.util.IOUtils;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Simple, but expressive slope visualisation (e.g. no pretentions of physical accuracy, separate north and west lightsources instead of one northwest, so a round dome would not look round, saturation works different depending on slope direction)
 *
 * <p>variations can be created by overriding {@link #exaggerate(double)}</p>
 */
public class SimpleShadingAlgorithm implements ShadingAlgorithm {

    private static final Logger LOGGER = Logger.getLogger(SimpleShadingAlgorithm.class.getName());
    public final double linearity;
    public final double scale;

    private byte[] lookup;
    private int lookupOffset;

    public SimpleShadingAlgorithm(){
        this(0.1d, 0.666d);
    }

    /**
     * customization constructor for controlling some parameters of the shading formula
     * @param linearity 1 or higher for linear grade, 0 or lower for a triple-applied
     *                  sine of grade that gives high emphasis on changes in slope in
     *                  near-flat areas, but reduces details within steep slopes
     *                  (default 0.1)
     * @param scale scales the input slopes, with lower values slopes will saturate later, but nuances closer to flat will suffer
     *              (default: 0.666d)
     */
    public SimpleShadingAlgorithm(double linearity, double scale) {
        this.linearity = Math.min(1d, Math.max(0d, linearity));
        this.scale = Math.max(0d, scale);
    }
    /**
     * should calculate values from -128 to +127 using whatever range required (within reason)
     * @param in a grade, ascent per projected distance (along coordinate axis)
     */
    protected double exaggerate(double in) {
        double x = in * scale;
        x = Math.max(-128d, Math.min(128d, x));
        double ret = (Math.sin(0.5d*Math.PI*Math.sin(0.5d*Math.PI*Math.sin(0.5d*Math.PI*x/128d)))*128*(1d-linearity)+x*linearity);
        return ret;
    }

    @Override
    public int getAxisLenght(HgtCache.HgtFileInfo source){
        long size = source.getSize();
        long elements = size / 2;
        int rowLen = (int) Math.ceil(Math.sqrt(elements));
        if (rowLen * rowLen * 2 != size) {
            return 0;
        }
        return rowLen - 1;
    }

    @Override
    public RawShadingResult transformToByteBuffer(HgtCache.HgtFileInfo source, int padding) {
        int axisLength = getAxisLenght(source);
        int rowLen = axisLength+1;
        BufferedInputStream in = null;
        try {
            in = source.openInputStream();


            byte[] bytes = convert(in, axisLength, rowLen, padding);
            return new RawShadingResult(bytes, axisLength, axisLength, padding);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
            return null;
        } finally {
            IOUtils.closeQuietly(in);
        }
    }

    private byte[] convert(InputStream in, int axisLength, int rowLen, int padding) throws IOException {
        byte[] bytes;

        short[] ringbuffer = new short[rowLen];
        bytes = new byte[(axisLength +2*padding) * (axisLength+2*padding)];

        DataInputStream din = new DataInputStream(in);

        byte[] lookup = this.lookup;
        if(lookup==null) {
            fillLookup();
            lookup = this.lookup;
        }

        int outidx = (axisLength +2*padding)*padding+padding;
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

                noso = (int)exaggerate(lookup, noso);
                eawe = (int)exaggerate(lookup, eawe);

                int zeroIsFlat = noso + eawe ;

                int intVal = Math.min(255, Math.max(0, zeroIsFlat + 127));

                int shade = intVal & 0xFF;

                bytes[outidx++] = (byte) shade;

                nw = ne;
                sw = se;
            }
            outidx+=2*padding;
        }
        return bytes;
    }


    private byte exaggerate(byte[] lookup, int x) {

        return lookup[Math.max(0, Math.min(lookup.length-1, x+lookupOffset))];
    }


    private void fillLookup(){
        int lowest = 0;
        while(lowest > -1024){
            double exaggerate = exaggerate(lowest);
            double exaggerated = Math.round(exaggerate);
            if(exaggerated<=-128 ||exaggerated >= 127) break;
            lowest--;
        }
        int highest = 0;
        while(highest < 1024){
            double exaggerated = Math.round(exaggerate(highest));
            if(exaggerated<=-128 ||exaggerated >= 127) break;
            highest++;
        }
        int size = 1 + highest - lowest;
        byte[] nextLookup = new byte[size];
        int in = lowest;
        for(int i=0;i<size;i++){
            byte exaggerated = (byte) Math.round(exaggerate(in));
            nextLookup[i]=exaggerated;
            in++;
        }
        lookup=nextLookup;
        lookupOffset=-lowest;
    }

    private static short readNext(DataInputStream din, short fallback) throws IOException {
        short read = din.readShort();
        if (read == Short.MIN_VALUE)
            return fallback;
        return read;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SimpleShadingAlgorithm that = (SimpleShadingAlgorithm) o;

        if (Double.compare(that.linearity, linearity) != 0) return false;
        return Double.compare(that.scale, scale) == 0;
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        temp = Double.doubleToLongBits(linearity);
        result = (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(scale);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }
}
