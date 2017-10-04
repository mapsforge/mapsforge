package org.mapsforge.map.layer.hills;

import org.junit.Assert;
import org.junit.Test;

import org.mapsforge.core.graphics.Canvas;
import org.mapsforge.core.graphics.HillshadingBitmap;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.map.awt.graphics.AwtGraphicFactory;
import org.mapsforge.map.awt.graphics.AwtHillshadingBitmap;

import java.awt.image.BufferedImage;
import java.awt.image.Raster;

public class HgtCacheTest {
    @Test public void testMerge(){
        AwtGraphicFactory factory = new AwtGraphicFactory();
        byte[] even = new byte[]{
                0,2,4,6,0,
                2,2,4,6,6,
                8,8,10,12,12,
                14,14,16,18,18,
                0,14,16,18,0
        };


        byte[] odd = new byte[]{
                0,1,3,5,0,
                1,1,3,5,5,
                7,7,9,11,11,
                13,13,15,17,17,
                0,13,15,17,0
        };
        AwtHillshadingBitmap evens = factory.createMonoBitmap(3, 3, even, 1, new BoundingBox(0, 1, 0, 1));
        AwtHillshadingBitmap odds = factory.createMonoBitmap(3, 3, odd, 1, new BoundingBox(0, 1, 0, 1));
        Canvas canvas = factory.createCanvas();

        HgtCache.mergeSameSized(evens, odds, HillshadingBitmap.Border.EAST, 1, canvas);

        Assert.assertEquals("\n" +
                "     0:        0   2   4   6   0\n" +
                "     1:        2   2   4   6   1\n" +
                "     2:        8   8  10  12   7\n" +
                "     3:       14  14  16  18  13\n" +
                "     4:        0  14  16  18   0", logbytes(evens));
//        Assert.assertEquals("\n" +
//                "     0:        0   1   3   5   0\n" +
//                "     1:        6   1   3   5   5\n" +
//                "     2:       12   7   9  11  11\n" +
//                "     3:       18  13  15  17  17\n" +
//                "     4:        0  13  15  17   0", logbytes(odds));



        HgtCache.mergeSameSized(evens, odds, HillshadingBitmap.Border.NORTH, 1, canvas);

        Assert.assertEquals("\n" +
                "     0:        0  13  15  17   0\n" +
                "     1:        2   2   4   6   1\n" +
                "     2:        8   8  10  12   7\n" +
                "     3:       14  14  16  18  13\n" +
                "     4:        0  14  16  18   0", logbytes(evens));
//        Assert.assertEquals("\n" +
//                "     0:        0   1   3   5   0\n" +
//                "     1:        6   1   3   5   5\n" +
//                "     2:       12   7   9  11  11\n" +
//                "     3:       18  13  15  17  17\n" +
//                "     4:        0   2   4   6   0", logbytes(odds));




        HgtCache.mergeSameSized(evens, odds, HillshadingBitmap.Border.WEST, 1, canvas);

        Assert.assertEquals("\n" +
                "     0:        0  13  15  17   0\n" +
                "     1:        5   2   4   6   1\n" +
                "     2:       11   8  10  12   7\n" +
                "     3:       17  14  16  18  13\n" +
                "     4:        0  14  16  18   0", logbytes(evens));
//        Assert.assertEquals("\n" +
//                "     0:        0   1   3   5   0\n" +
//                "     1:        6   1   3   5   2\n" +
//                "     2:       12   7   9  11   8\n" +
//                "     3:       18  13  15  17  14\n" +
//                "     4:        0   2   4   6   0", logbytes(odds));





        HgtCache.mergeSameSized(evens, odds, HillshadingBitmap.Border.SOUTH, 1, canvas);

        Assert.assertEquals("\n" +
                "     0:        0  13  15  17   0\n" +
                "     1:        5   2   4   6   1\n" +
                "     2:       11   8  10  12   7\n" +
                "     3:       17  14  16  18  13\n" +
                "     4:        0   1   3   5   0", logbytes(evens));
//        Assert.assertEquals("\n" +
//                "     0:        0  14  16  18   0\n" +
//                "     1:        6   1   3   5   2\n" +
//                "     2:       12   7   9  11   8\n" +
//                "     3:       18  13  15  17  14\n" +
//                "     4:        0   2   4   6   0", logbytes(odds));
    }



    private String logbytes(AwtHillshadingBitmap fresh) {
//        try {
//            Class<?> superclass = fresh.getClass().getSuperclass();
//            Field bufferedImage = superclass.getDeclaredField("bufferedImage");
//            bufferedImage.setAccessible(true);
//            BufferedImage bi = (BufferedImage) bufferedImage.get(fresh);
            BufferedImage bi = AwtGraphicFactory.getBitmap(fresh);
            Raster data = bi.getData();
            StringBuilder sb = new StringBuilder();
            for(int y=0;y<data.getHeight();y+=(y==4 && y<data.getHeight()/2 ? data.getHeight()-8:1)) {
                if(y==4 && y<data.getHeight()/2 ) {
                    sb.append("\n");
                    continue;
                }
                sb.append("\n").append(String.format(" %5d", y)).append(":     ");
                for(int x=0;x<data.getWidth();x+=(x==4 & x<data.getWidth()/2 ?data.getWidth()-8:1)) {
                    if(x==4 & x<data.getWidth()/2) {
                        sb.append("   ");
                        continue;
                    }
                    int sample = data.getSample(x, y, 0);
                    sb.append(String.format(" %3d", sample));
                }

            }


//        } catch (NoSuchFieldException e) {
//            e.printStackTrace();
//        } catch (IllegalAccessException e) {
//            e.printStackTrace();
//        }

        return sb.toString();
    }
}