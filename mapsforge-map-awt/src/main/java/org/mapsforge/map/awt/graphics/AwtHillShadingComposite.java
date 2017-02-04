package org.mapsforge.map.awt.graphics;

import java.awt.*;
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.util.Arrays;

/**
 * Created by usrusr on 31.01.2017.
 */
class AwtHillShadingComposite implements Composite {
    public AwtHillShadingComposite(int magnitude){
        int clamped = Math.max(0, Math.min(magnitude, 255));

        float dstMul = (256f-clamped)/256f;
        float srcMul = clamped/256f;

        StringBuilder sb = new StringBuilder();
        sb.append("\nfloat input:");

        double first = 0;

        for(int i=0;i<256; i++){
            scaledDst[i] = (short) ((i*dstMul));
            scaledSrc[i] = (short) (((float)i*srcMul));


            float norm = (i- 128f) / 128f;

            if(i%16==0) sb.append("\n   ");
            sb.append("[" + String.format("%3d",i) + "->" + String.format("%3.3f",norm) + "]");

            double polynomized = Math.sqrt(norm);
            polynomized = Math.sqrt(polynomized);

            polynomized = norm*norm*norm;// * Math.signum(norm);

            polynomized=(polynomized*128f)*srcMul+128f;

            double out = polynomized;

            if(i==0){
                first = out;
            }

            scaledSrc[i]= (short) (out-first);
        }

        sb.append("\nscaling down existing source:");

        for (int i = 0; i < scaledSrc.length; i++) {
            if(i%16==0) sb.append("\n   ");
            short v = scaledSrc[i];
            sb.append("[" + String.format("%3d",i) + "->" + String.format("%3d",v) + "]");
        }
        sb.append("\nscaling down heightval:");
        for (int i = 0; i < scaledDst.length; i++) {
            if(i%16==0) sb.append("\n   ");
            short v = scaledDst[i];
            sb.append("[" + String.format("%3d",i) + "->" + String.format("%3d",v) + "]");
        }

        System.out.println("" + sb );
    }
    short[] scaledDst = new short[256];
    short[] scaledSrc = new short[256];

    @Override
    public CompositeContext createContext(final ColorModel srcColorModel, final ColorModel dstColorModel, final RenderingHints hints) {
        int srcNumComponents = srcColorModel.getNumComponents();
        if (srcNumComponents != 1)
            throw new IllegalArgumentException(this.getClass() + " only supports single-component input");

        return new CompositeContext() {
            @Override
            public void dispose() {

            }

            int[] srcBytes = new int[4];
            int[] inBytes = new int[4];
            int[] outBytes = new int[4];

            float[] inFloats = new float[4];
            float[] hsvFloats = new float[4];
            float[] outFloats = new float[4];
            @Override
            public void compose(Raster src, Raster dstIn, WritableRaster dstOut) {
                int width = src.getWidth();
                int height = src.getHeight();

                int[] histogram = new int[256];
                int[] scaledHistogram = new int[256];

                for (int x = 0; x < width; ++x) {
                    for (int y = 0; y < height; ++y) {
                        src.getPixel(x, y, srcBytes);
                        dstIn.getPixel(x, y, inBytes);

//                        outBytes[0] = inBytes[0];

                        int shadeVal = srcBytes[0];



                        int add = scaledSrc[shadeVal];
//                        System.out.println(x+","+y+":"+shadeVal +" => +"+add);

                        histogram[shadeVal]++;

                        scaledHistogram[add]++;
//
//                        outBytes[0] = scaledDst[inBytes[0]] + add;
//
//                        outBytes[1] = scaledDst[inBytes[1]] + add;
//                        outBytes[2] = scaledDst[inBytes[2]] + add;
//                        outBytes[3] = scaledDst[inBytes[3]] + add;
//                        outBytes[3] = 128;


                        int pixel = x + y;
//                        int alpha = dstColorModel.getAlpha(pixel);


                        dstIn.getPixel(x, y, inFloats);

                        toHsv(inFloats, hsvFloats);


                        fromHsv(hsvFloats, outFloats);

                        dstOut.setPixel(x, y, outFloats);
                    }
                }

                StringBuilder sb = new StringBuilder();
                sb.append("\nshade in histogram:");

                for (int i = 0; i < 256; i++) {
                    if(i%16==0) sb.append("\n   ");

                    sb.append("[" + String.format("%6d",histogram[i])  + "]");
                }

                sb.append("\nshade scaled histogram:");

                for (int i = 0; i < 256; i++) {
                    if(i%16==0) sb.append("\n   ");

                    sb.append("[" + String.format("%6d",scaledHistogram[i])  + "]");
                }
                System.out.println("" + sb );


            }

        };
    }


    static void toHsv(float[] inFloats, float[] outFloats) {
//        outFloats[0]= inFloats[0];
//        outFloats[1]= inFloats[1];
//        outFloats[2]= inFloats[2];
        outFloats[3]= inFloats[3];


        float r = inFloats[0];
        float g = inFloats[1];
        float b = inFloats[2];

//        r/=255f;
//        g/=255f;
//        b/=255f;


        float sixth = 42.5f; // 255f / 6f;
        float h,s,v, min, max;
        if(r==g&&g==b){
            h=0;
            max = r;
            min = r;
        }else if (r >= g && r >= b) {
            max = r;
            min = Math.min(b, g);
            h = sixth * (0 + (g - b) / (max - min));
        } else if (g >= b) {
            max = g;
            min = Math.min(r, b);
            h = sixth * (2 + (b - r) / (max - min));
        } else {
            max = b;
            min = Math.min(r, g);
            h = sixth * (4 + (r - g) / (max - min));
        }
        if(h<0) {
            h = h + 255f;
        }
        if(max==0f) {
            s = 0;
        }else{
            s = 255f*(max-min)/max;
        }
        v = max;


//        h*=255f;
//        s*=255f;
//        v*=255f;

        outFloats[0]= h;
        outFloats[1]= s;
        outFloats[2]= v;
    }
    static void fromHsv(float[] inFloats, float[] outFloats) {
        float h = inFloats[0];
        float s = inFloats[1];
        float v = inFloats[2];

//        h/=255f;
//        s/=255f;
//        v/=255f;

        float f = h / 42.5f;
        int block = (int) f;
        f = (f-block)/6f;

        float sfract = s/255f;

        float p = v * (1 - sfract);
        float q = v * (1 - sfract * f);
        float t = v * (1 - sfract * (1 - f));

        float r,g,b;

        if(block==0||block==6){
            r=v;
            g=t;
            b=p;
        }else if(block==1){
            r=q;
            g=v;
            b=p;
        }else if(block==2){
            r=p;
            g=v;
            b=t;
        }else if(block==3){
            r=p;
            g=q;
            b=v;
        }else if(block==4){
            r=t;
            g=p;
            b=v;
        }else if(block==5){
            r=v;
            g=p;
            b=q;
        }else{
            throw new IllegalStateException("nonsensical hsv values in "+ Arrays.toString(inFloats));
        }

//        r*=255f;
//        g*=255f;
//        b*=255f;

        outFloats[0]=r;
        outFloats[1]=g;
        outFloats[2]=b;
        outFloats[3]=inFloats[3];

    }

}
