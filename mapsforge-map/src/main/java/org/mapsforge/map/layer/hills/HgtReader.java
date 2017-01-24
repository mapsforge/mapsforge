package org.mapsforge.map.layer.hills;

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by usrusr on 22.01.2017.
 */
public class HgtReader {

    public final byte[] readVals;
    public final int length;

    public HgtReader(InputStream in, int size) throws IOException {
        int rowLen = (int) Math.ceil(Math.sqrt(size/2));
        length = rowLen-1;
        short[] ringbuffer = new short[rowLen*2];
        readVals = new byte[length*length];

        DataInputStream din = new DataInputStream(in);

        int outidx = 0;
        int rbcur = 0;
        {short last = 0;
        for(int col = 0;col<rowLen*2;col++){
            last = readNext(din, last);
            ringbuffer[rbcur++]= last;

        }}
        int rblast = rowLen;
        for(int line = 2;line<length;line++){
            if(rbcur>=2*rowLen) {
                rbcur=0;
            }else{
                rblast = rbcur-rowLen;
            }

            short nw = ringbuffer[rbcur];
            short sw = readNext(din, nw);
            ringbuffer[rbcur++]= sw;
            short cw=ringbuffer[rblast++];

            short nc = ringbuffer[rbcur];
            short sc = readNext(din, nc);
            ringbuffer[rbcur++]= sc;
            short cc=ringbuffer[rblast++];

            int noso = (sw-nw) + 2*(sc-nc);


            for(int col = 1;col<=length;col++){

                short ne = ringbuffer[rbcur];
                short se = readNext(din, ne);
                ringbuffer[rbcur++]= se;
                short ce=ringbuffer[rblast++];

                int nosoEast = se - ne;
                noso += nosoEast;

                int eawe = (ne-nw) + 2*(ce-cw) + (se-sw);

                int nosoClamped = Math.min(256, Math.max(0, noso+128));
                int eaweClamped = Math.min(256, Math.max(0, eawe+128));

                int shade = Math.min(Byte.MAX_VALUE, Math.max(Byte.MIN_VALUE, ((nosoClamped+eaweClamped)/2)-127));

//                shade = (byte) ((((int)cc))/50);

                if(shade!=(byte)shade){
                    System.out.println("extreme shade: "+shade);
                }

                readVals[outidx++]=(byte)shade;

                noso -= ((sw-nw) + (sc-nc));
                noso += nosoEast;
                nw=nc;
                cw=cc;
                sw=sc;
                nc=ne;
                cc=ce;
                sc=se;
            }
            rbcur--;
            rblast--;
        }



    }

    private short readNext(DataInputStream din, short fallback) throws IOException {
        short read = din.readShort();
        if(read==Short.MIN_VALUE) return fallback;
        return read;
    }

    public static HgtReader create(HillsRenderConfig cfg, float x, float y){
        File demFolder = cfg.getDemFolder();
        return null;
    }

}
