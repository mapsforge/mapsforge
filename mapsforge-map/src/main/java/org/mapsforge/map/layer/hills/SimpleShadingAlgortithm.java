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
 * Created by usrusr on 22.01.2017.
 */
public class SimpleShadingAlgortithm implements ShadingAlgorithm {
    private static final Logger LOGGER = Logger.getLogger(SimpleShadingAlgortithm.class.getName());
    @Override public Bitmap convertTile(RawHillTileSource source, GraphicFactory graphicFactory){
        long size = source.getSize();
        long elements = size / 2;
        int rowLen = (int) Math.ceil(Math.sqrt(elements));
        if(rowLen*rowLen*2!=size) {
            return null;
        }
        BufferedInputStream in=null;
        try{
            in = source.openInputStream();
            return convert(in, size, graphicFactory);
        } catch (IOException e) {
            LOGGER.log(Level.FINE, e.getMessage(), e);
            return null;
        }finally{
            IOUtils.closeQuietly(in);
        }
    }


    private static Bitmap convert(InputStream in, long streamLen, GraphicFactory graphicFactory) throws IOException {
        byte[] bytes;
        int axisLength;
        int rowLen = (int) Math.ceil(Math.sqrt(streamLen/2));


        axisLength = rowLen - 1;
        short[] ringbuffer = new short[rowLen];
        bytes = new byte[axisLength * axisLength];

        DataInputStream din = new DataInputStream(in);

        int outidx = 0;
        int rbcur = 0;
        {
            short last = 0;
            for (int col = 0; col < rowLen * 1; col++) {
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

                int noso = (se - ne) + (sw - nw);

                int eawe = (ne - nw) + (se - sw);

                int nosoClamped = Math.min(256, Math.max(0, noso + 128));
                int eaweClamped = Math.min(256, Math.max(0, eawe + 128));

                int shade = Math.min(Byte.MAX_VALUE, Math.max(Byte.MIN_VALUE, ((nosoClamped + eaweClamped) / 2) - 127));

//                shade = (byte) ((((int)cc))/50);

                if (shade != (byte) shade) {
                    System.out.println("extreme shade: " + shade);
                }

                bytes[outidx++] = (byte) shade;

                nw = ne;
                sw = se;
            }
        }

//            ComponentColorModel componentColorModel = new ComponentColorModel(
//                    ColorSpace.getInstance(ColorSpace.CS_GRAY),
//                    false, false, Transparency.OPAQUE, DataBuffer.TYPE_BYTE
//            );
//            SampleModel sampleModel = componentColorModel.createCompatibleSampleModel(axisLength, axisLength);
//
//            new BufferedImage(componentColorModel,new WritableRaster(sampleModel, new Point(0,0))
//            )
        return graphicFactory.createMonoBitmap(axisLength, axisLength, bytes);
    }

    private static short readNext(DataInputStream din, short fallback) throws IOException {
        short read = din.readShort();
        if (read == Short.MIN_VALUE) return fallback;
        return read;
    }
}
