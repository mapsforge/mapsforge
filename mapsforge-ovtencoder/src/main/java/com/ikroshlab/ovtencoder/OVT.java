/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ikroshlab.ovtencoder;

import com.ikroshlab.ovtencoder.header.MapFileInfo;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.oscim.core.BoundingBox;
import org.oscim.core.Point;
import org.oscim.core.Tile;
import org.oscim.layers.tile.MapTile;
import org.oscim.tiling.ITileDataSource;
import org.oscim.tiling.TileSource.OpenResult;




/**
 *
 * @author ikroshlab.com
 */
public class OVT {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        logDebug("Starting application...");
        
        
        String fname = "Mapsforge_file.map";   // change it !!!
        
        
        
        MapFileTileSource src = new MapFileTileSource(1, 17);
        boolean ok = src.setMapFile(fname);
        
        if (!ok) {
            logDebug("Bad file !");
            return;
        }
        
        logDebug("file has been set. Getting datasource...");
        OpenResult res = src.open();
        if (!res.isSuccess()) {
            logDebug("file cannot be open !");
            return;
        }
        
        ITileDataSource mdb = src.getDataSource();
        logDebug("data source OK: " + (mdb != null));
        
        // extract tile numbers from bounding box
        MapFileInfo fileInfo = src.fileHeader.getMapFileInfo();
        BoundingBox bb       = fileInfo.boundingBox;       
        //int[] zooms          = fileInfo.zoomLevel;
        //Arrays.sort(zooms);
        //byte pZoomMin = (byte)zooms[0];       
        //byte pZoomMax = (byte)zooms[zooms.length-1];       
        
        long start = System.currentTimeMillis();
        for (byte zoomLevel=0; zoomLevel<=16; zoomLevel++) {
            Collection<Tile> tiles = getTilesCoverage(bb, zoomLevel);  
            
            for (Tile t : tiles) {
                MapTile mtile = new MapTile(t.tileX, t.tileY, (int)t.zoomLevel);
                mdb.query(mtile, null);
            }
        }                                                     
        
        long end = System.currentTimeMillis();
        logDebug("closing source... time: " + (end-start)/1000);
        src.close();
        mdb.dispose();
        
    }
    
    
    
    
    ////////////////////////////////////////////////////////////////////////////
    
    public static List<Tile> getTilesCoverage(BoundingBox pBB, byte pZoomMin, byte pZoomMax) {
        ArrayList<Tile> result = new ArrayList<>();

        for(byte zoomLevel = pZoomMin; zoomLevel <= pZoomMax; ++zoomLevel) {
            Collection<Tile> resultForZoom = getTilesCoverage(pBB, zoomLevel);
            result.addAll(resultForZoom);
        }
        return result;
    }


    public static Collection<Tile> getTilesCoverage(BoundingBox pBB, byte pZoomLevel) {

        HashSet<Tile> result = new HashSet<>();
        int mapTileUpperBound   = 1 << pZoomLevel;

        Point lowerRight = getMapTileFromCoordinates(pBB.getMinLatitude(), pBB.getMaxLongitude(), pZoomLevel);
        Point upperLeft  = getMapTileFromCoordinates(pBB.getMaxLatitude(), pBB.getMinLongitude(), pZoomLevel);

        int width  = (int) (lowerRight.x - upperLeft.x + 1);
        int height = (int) (lowerRight.y - upperLeft.y + 1);

        if(width  <= 0)  width  += mapTileUpperBound;
        if(height <= 0)  height += mapTileUpperBound;

        for(int i = 0; i < width; ++i) {
            for(int j = 0; j < height; ++j) {
                int x = MyMath.mod((int) (upperLeft.x + i), mapTileUpperBound);
                int y = MyMath.mod((int) (upperLeft.y + j), mapTileUpperBound);
                result.add(new Tile(x, y, pZoomLevel));
            }
        }

        return result;
    }
    
    
    
    
    public static Point getMapTileFromCoordinates(double aLat, double aLon, int zoom) {
        int y = (int)Math.floor((1.0D - Math.log(Math.tan(aLat * 3.141592653589793D / 180.0D) + 1.0D / Math.cos(aLat * 3.141592653589793D / 180.0D)) / 3.141592653589793D) / 2.0D * (double)(1 << zoom));
        int x = (int)Math.floor((aLon + 180.0D) / 360.0D * (double)(1 << zoom));
        return new Point(x, y);
    }
    
    
    
    
    
    
    
    
    
    
    
    // handling Logging:
    static void logDebug(String msg) { System.out.println(" ### MAIN ###  " + msg); }
}
