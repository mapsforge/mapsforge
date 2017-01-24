package org.mapsforge.map.layer.hills;

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by usrusr on 22.01.2017.
 */
public class ShadingTile {

    public final byte[] bytes;
    public final int axisLength;

    public ShadingTile(InputStream in, int size) throws IOException {
        int rowLen = (int) Math.ceil(Math.sqrt(size/2));
        axisLength = rowLen-1;
        short[] ringbuffer = new short[rowLen];
        bytes = new byte[axisLength * axisLength];

        DataInputStream din = new DataInputStream(in);

        int outidx = 0;
        int rbcur = 0;
        {short last = 0;
        for(int col = 0;col<rowLen*1;col++){
            last = readNext(din, last);
            ringbuffer[rbcur++]= last;

        }}
        for(int line = 1; line<= axisLength; line++){
            if(rbcur>=rowLen) {
                rbcur=0;
            }

            short nw = ringbuffer[rbcur];
            short sw = readNext(din, nw);
            ringbuffer[rbcur++]= sw;




            for(int col = 1; col<= axisLength; col++){

                short ne = ringbuffer[rbcur];
                short se = readNext(din, ne);
                ringbuffer[rbcur++]= se;

                int noso = (se - ne) + (sw - nw);

                int eawe = (ne-nw) + (se-sw);

                int nosoClamped = Math.min(256, Math.max(0, noso+128));
                int eaweClamped = Math.min(256, Math.max(0, eawe+128));

                int shade = Math.min(Byte.MAX_VALUE, Math.max(Byte.MIN_VALUE, ((nosoClamped+eaweClamped)/2)-127));

//                shade = (byte) ((((int)cc))/50);

                if(shade!=(byte)shade){
                    System.out.println("extreme shade: "+shade);
                }

                bytes[outidx++]=(byte)shade;

                nw=ne;
                sw=se;
            }
        }



    }

    private short readNext(DataInputStream din, short fallback) throws IOException {
        short read = din.readShort();
        if(read==Short.MIN_VALUE) return fallback;
        return read;
    }

    public static ShadingTile create(HillsRenderConfig cfg, float x, float y){
        File demFolder = cfg.getDemFolder();
        return null;
    }

}
