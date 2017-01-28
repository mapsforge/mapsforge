package org.mapsforge.map.layer.hills;

import org.mapsforge.core.graphics.*;
import org.mapsforge.core.graphics.Color;
import org.mapsforge.map.awt.graphics.AwtGraphicFactory;

import java.awt.Canvas;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.*;

/**
 * Created by usrusr on 24.01.2017.
 */
public class HgtReaderTest {
    public static void main(String... argv) throws IOException, InterruptedException {
        for(int east = 11;east<12;east++){
            for(int north = 49;north<50;north++){
                file(east, north);
            }
        }
        Thread.currentThread().sleep(600000L);
    }
    static void file(final int east, final int north) throws IOException, InterruptedException {
        final int scale = 1;
        String northName = ""+north;
        while(northName.length()<2) northName="0"+northName;
        while(northName.length()<2) northName="0"+northName;
        String eastName = ""+east;
        while(eastName.length()<2) eastName="0"+eastName;
        while(eastName.length()<3) eastName="0"+eastName;
        final String fileName = "N" + northName + "E" + eastName + ".hgt";
        final File f = new File("C:\\lunaworkspace\\deskmap\\data\\dem\\M32\\" , fileName);
//        Assume.assumeTrue(f.exists());


        final GraphicFactory graphicsFactory = AwtGraphicFactory.INSTANCE;


        ShadingAlgorithm.RawHillTileSource tileSource = new ShadingAlgorithm.RawHillTileSource() {
            @Override
            public long getSize() {
                return f.length();
            }

            @Override
            public BufferedInputStream openInputStream() throws FileNotFoundException, IOException {
                return new BufferedInputStream(new FileInputStream(f));
            }

            @Override
            public ShadingAlgorithm.RawHillTileSource getNeighborNorth() {
                return null;
            }

            @Override
            public ShadingAlgorithm.RawHillTileSource getNeighborSouth() {
                return null;
            }

            @Override
            public ShadingAlgorithm.RawHillTileSource getNeighborEast() {
                return null;
            }

            @Override
            public ShadingAlgorithm.RawHillTileSource getNeighborWest() {
                return null;
            }
        };
        final Bitmap reader = new SimpleShadingAlgortithm().convertTile(tileSource, graphicsFactory);

        new Frame(){
            {

//                BufferedImage image = new BufferedImage(1200, 1200, BufferedImage.TYPE_BYTE_GRAY);
////                Component bitmap;
////                this.add(bitmap);
//
//                image.setData(
//                        new Raster(
//                                new SampleModel(DataBuffer.TYPE_BYTE, 1200, 1200, 1),
//                                new DataBufferByte(reader.bytes, reader.axisLength*reader.axisLength)
//                ));

                //this.setSize(reader.axisLength,reader.axisLength);
                this.setLocation(-1600,0);
                setTitle(fileName);

//                Canvas comp = new Canvas(){
//
//                    int min = Integer.MAX_VALUE;
//                    int max = Integer.MIN_VALUE;
//                    {
//                        for(int i = 0; i<reader.bytes.length; i++){
//                            int readVal = reader.bytes[i];
//                            min = Math.min(min, readVal -Byte.MIN_VALUE);
//                            max = Math.max(max, readVal -Byte.MIN_VALUE);
//                        }
//                    }
//                    @Override
//                    public void paint(Graphics g) {
//                        int i=0;
//                        for(int x = 0; x<reader.axisLength; x++){
//
//                            for(int y = 0; y<reader.axisLength; y++){
//                                byte readVal = reader.bytes[i++];
////                                g.setColor(Color.getHSBColor(0,0,256f/ readVal));
////                                g.setColor(Color.getHSBColor(0,0,(int)readVal+127/256f));
////                                g.setColor(Color.HSBtoRGB(0,0, readVal/256f));
//                                int v = (int)readVal-Byte.MIN_VALUE;
//
//                                float delta = (max - min);
//                                float s = (((v-min) - 0f) / delta);
//
//                                g.setColor(new Color(s, s, s));
//                                int y1 = x / scale;
//                                int x1 = y / scale;
////                                int x1 = (reader.axisLength-y-1) / 2;
//
//
//                                g.drawLine(x1, y1, x1, y1);
//                            }
////                            i--;
//                        }
//
////                        super.paint(g);
//                    }
//                };
//                comp.setSize(reader.axisLength /scale,reader.axisLength /scale);
//                comp.setPreferredSize(comp.getSize());
//                Insets insets = getInsets();
//                setSize(comp.getWidth()+insets.left+insets.right, comp.getHeight()+insets.top+insets.bottom);
//                this.add(comp);

                final BufferedImage bitmap = AwtGraphicFactory.getBitmap(reader);
                Canvas comp = new Canvas(){
                    public void paint(Graphics g) {
                        GraphicContext gc = AwtGraphicFactory.createGraphicContext(g);
                        org.mapsforge.core.graphics.Paint paint = graphicsFactory.createPaint();
                        paint.setTextSize(50);
                        paint.setColor(Color.GREEN);
                        gc.drawCircle(600,600, 400, paint);
                        paint.setColor(Color.RED);
                        gc.drawText("lorem\nipsum ", 300,300, paint);

                        Matrix matrix = graphicsFactory.createMatrix();
                        matrix.scale(1, 1);
                        matrix.translate(0,0);

                        gc.shadeBitmap(reader, matrix);
//                        gc.drawBitmap(reader, matrix);

                    }
                };

                add(comp);
                comp.setSize(1210, 1210);
                this.pack();
//                TextArea text = new TextArea("N:"+north+" E:"+east);
//                text.setForeground(new Color(255,30,30));
//                this.add(text);

                addWindowListener(new WindowAdapter() {
                    @Override
                    public void windowClosing(WindowEvent e) {
                        System.exit(0);
                    }
                });

            }
        }.show();


    }

}