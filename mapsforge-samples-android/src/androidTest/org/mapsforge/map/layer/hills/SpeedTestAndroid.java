package org.mapsforge.map.layer.hills;

import android.test.AndroidTestCase;
import junit.framework.Assert;

import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.graphics.HillshadingBitmap;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;

import java.io.File;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;


public class SpeedTestAndroid extends AndroidTestCase {
    protected File path = new File("/storage/0000-0000/Android/data/com.orux.oruxmapsDonate/files/dem/");
    protected GraphicFactory graphics = AndroidGraphicFactory.INSTANCE;
    protected ShadingAlgorithm algo = new SimpleShadingAlgorithm();


    long shadeNanos;
    int shadeCount;

    long noNanos;
    int noCount;

    long bmpNanos;
    int bmpCount;


    public void testLogTiming() throws ExecutionException, InterruptedException {
        String speedcheck = speedcheck();
        System.out.println("result: "+ speedcheck);
        Assert.assertEquals("", speedcheck);
    }
    public String speedcheck() throws ExecutionException, InterruptedException {
        shadeNanos=0;
        shadeCount=0;
        ShadingAlgorithm algorithm = new ShadingAlgorithm() {
            @Override
            public int getAxisLenght(HgtCache.HgtFileInfo source) {
                return algo.getAxisLenght(source);
            }

            @Override
            public RawShadingResult transformToByteBuffer(HgtCache.HgtFileInfo hgtFileInfo, int padding) {
                Future<HillshadingBitmap> future = hgtFileInfo.getBitmapFuture(99999, 99999);

                if(future==null) return null;


                long shadeBefore = System.nanoTime();
                RawShadingResult transform = algo.transformToByteBuffer(hgtFileInfo, padding);
                long shadeDuration = System.nanoTime() - shadeBefore;
                shadeNanos+=shadeDuration;
                shadeCount++;

                return transform;

            }
        };

        final MemoryCachingHgtReaderTileSource rootSource = new MemoryCachingHgtReaderTileSource(path, algo, graphics);



        rootSource.setEnableInterpolationOverlap(false);
//        rootSource.applyConfiguration();

        HillsRenderConfig config = new HillsRenderConfig(rootSource);


        long crawlNanos = System.nanoTime();
        rootSource.applyConfiguration(false);
        crawlNanos = System.nanoTime() - crawlNanos;

        int minlat = -90;
        int maxlat = 90;

        int minlng = -20;
        int maxlng = 20;

        StringBuilder sb = new StringBuilder();

//        minlat=50;
//        maxlat=51;

//        minlng = 8;
//        maxlng = 9;

        for(int run=0;run<2;run++) {
            shadeCount=0;
            shadeNanos=0;
            noCount=0;
            noNanos=0;
            bmpCount=0;
            bmpNanos=0;
            for (int lat = minlat; lat < maxlat; lat++) {
                for (int lng = minlng; lng < maxlng; lng++) {
                    //System.out.println(getClass()+String.format(" %d %d ",lat,lng));
                    long nanos = System.nanoTime();
                    HillshadingBitmap hillshadingBitmap = rootSource.getHillshadingBitmap(lat, lng, 99999, 99999);
                    nanos = System.nanoTime() - nanos;
                    if (hillshadingBitmap == null) {
                        noNanos += nanos;
                        noCount++;
                        System.out.println(getClass() + String.format(" %d %d done skip in %,d ns", lat, lng, nanos));
                    } else {
                        bmpNanos += nanos;
                        bmpCount++;
                        System.out.println(getClass() + String.format(" %d %d done bmp in %,d ns", lat, lng, nanos));
                    }
                }
            }
            String format = String.format(
                    "\n\nrun %d\n%d algo averaging %,d ns " +
                            "\n%d skip averaging %,d ns " +
                            "\n%d bmp averaging %,d ns " +
                            "afer %,d ns crawl",
                    run,
                    shadeCount, (shadeNanos / Math.max(1,shadeCount)),
                    noCount, (noNanos / Math.max(1,noCount)),
                    bmpCount, (bmpNanos / Math.max(1,bmpCount)),
                    crawlNanos);
            sb.append(format);
        }
        sb.append("\n");

        return sb.toString();
    }

}