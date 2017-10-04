//package org.mapsforge.map.layer.hills;
//
//import org.mapsforge.core.graphics.Bitmap;
//import org.mapsforge.core.graphics.GraphicFactory;
//import org.mapsforge.core.model.LatLong;
//import org.mapsforge.core.util.IOUtils;
//import org.mapsforge.core.util.LatLongUtils;
//
//import java.io.BufferedInputStream;
//import java.io.DataInputStream;
//import java.io.IOException;
//import java.io.InputStream;
//import java.util.logging.Level;
//import java.util.logging.Logger;
//
///**
// * currently just a really stupid slope-to-lightness
// *
// * Created by usrusr on 22.01.2017.
// */
//public class TrigShadingAlgortithm implements ShadingAlgorithm {
//    private static final Logger LOGGER = Logger.getLogger(TrigShadingAlgortithm.class.getName());
//    @Override public Bitmap convertTile(RawHillTileSource source, GraphicFactory graphicFactory){
//        long size = source.getSize();
//        long elements = size / 2;
//        int rowLen = (int) Math.ceil(Math.sqrt(elements));
//        if(rowLen*rowLen*2!=size) {
//            return null;
//        }
//        BufferedInputStream in=null;
//        try{
//
//            return convert(source, size, graphicFactory);
//        } catch (IOException e) {
//            LOGGER.log(Level.FINE, e.getMessage(), e);
//            return null;
//        }finally{
//            IOUtils.closeQuietly(in);
//        }
//    }
//
//
//    private static Bitmap convert(RawHillTileSource source, long streamLen, GraphicFactory graphicFactory) throws IOException {
//        InputStream in = source.openInputStream();
//        byte[] bytes;
//        int axisLength;
//        int rowLen = (int) Math.ceil(Math.sqrt(streamLen/2));
//
//
//        axisLength = rowLen - 1;
//        short[] ringbuffer = new short[rowLen];
//        bytes = new byte[axisLength * axisLength];
//
//        DataInputStream din = new DataInputStream(in);
//
//        double widthLongitude = source.eastLng() - source.westLng();
//        while(widthLongitude<0) widthLongitude = Math.abs(widthLongitude-360d);
//        double heightLatitude = source.northLat() - source.southLat();
//
//        double pixelWidthNorth = LatLongUtils.sphericalDistance(
//                new LatLong(source.northLat(), 0)
//                , new LatLong(source.northLat(), widthLongitude)
//        );
//        double pixelWidthSouth = LatLongUtils.sphericalDistance(
//                new LatLong(source.southLat(), 0)
//                , new LatLong(source.southLat(), widthLongitude)
//        );
//        double pixelHeightNorth = LatLongUtils.sphericalDistance(
//                new LatLong(source.southLat(), 0)
//                , new LatLong(source.southLat(), widthLongitude)
//        );
//        double pixelHeightSouth = LatLongUtils.sphericalDistance(
//                new LatLong(source.southLat(), 0)
//                , new LatLong(source.southLat(), widthLongitude)
//        );
//
//        int outidx = 0;
//        int rbcur = 0;
//        {
//            short last = 0;
//            for (int col = 0; col < rowLen * 1; col++) {
//                last = readNext(din, last);
//                ringbuffer[rbcur++] = last;
//
//            }
//        }
//        for (int line = 1; line <= axisLength; line++) {
//            if (rbcur >= rowLen) {
//                rbcur = 0;
//            }
//
//            short nw = ringbuffer[rbcur];
//            short sw = readNext(din, nw);
//            ringbuffer[rbcur++] = sw;
//
//
//            for (int col = 1; col <= axisLength; col++) {
//
//                short ne = ringbuffer[rbcur];
//                short se = readNext(din, ne);
//                ringbuffer[rbcur++] = se;
//
//                int noso = -((se - ne) + (sw - nw));
//
//                int eawe = -((ne - nw) + (se - sw));
//
//                float avg = ((float)ne+(float)se+(float)nw+(float)sw)/4f;
//// todo geometry
//
//                int intVal = Math.min(255, Math.max(0, noso+eawe+128));
//
//                int shade = intVal & 0xFF;
//
//                bytes[outidx++] = (byte) shade;
//
//                nw = ne;
//                sw = se;
//            }
//        }
//        return graphicFactory.createMonoBitmap(axisLength, axisLength, bytes, , , , );
//    }
//
//    private static short readNext(DataInputStream din, short fallback) throws IOException {
//        short read = din.readShort();
//        if (read == Short.MIN_VALUE) return fallback;
//        return read;
//    }
//}
