package org.mapsforge.map.reader;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.InputStream;


import org.mapsforge.core.model.Tile;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.BoundingBox;

// NW now implements MapDataStore rather than extending MapDataSource
public class GeoJSONDataSource implements MapDataStore
{
    byte startZoomLevel;
    Tile quickfix;
    DownloadCache cache;

    // queryString = any compulsory query string data needed by the server
    String server, queryString;

    public GeoJSONDataSource(String server)
    {
        this(server,null);
    }

    public GeoJSONDataSource(String server, String queryString)
    {
        quickfix = new Tile (127, 85, (byte)8, 256);
        startZoomLevel = (byte)14;
        this.server = server;
        this.queryString = queryString;
    }

    public void setDownloadCache(DownloadCache cache)
    {
        this.cache = cache;
    }

    // Read a tile
    // Query the Freemap server for the appropriate GeoJSON tile by x,y,z
    // request all highways and all POIs for now
    public MapReadResult readMapData(Tile tile) 
    {
      InputStream in;
      HttpURLConnection conn=null;
      try
      {
        if(cache!=null && cache.inCache(tile))
        {
            System.out.println("In cache: " + 
                tile.tileX+","+tile.tileY+","+tile.zoomLevel);
            in  = cache.getInputStream(tile);
            // Pass in null as the used cache so that the reader doesn't
            // try to cache the tile as it's already there!
            return doReadJSON(in, tile, null);
        }
        else
        {    
            URL url = new URL (server + "?x="+
                    tile.tileX+"&y="+tile.tileY+"&z="+tile.zoomLevel+
					(queryString==null ? "":"&"+queryString));
            System.out.println("Tile details: " + 
            tile.tileX+","+tile.tileY+","+tile.zoomLevel);
            conn = (HttpURLConnection)url.openConnection();
            in = conn.getInputStream();
            if(conn.getResponseCode()==200)
                return doReadJSON(in, tile, cache);    
        }
       }
       catch(Exception e)
       {
         e.printStackTrace();
       }
        return null;
    }

    private MapReadResult doReadJSON
            (InputStream in, Tile tile, DownloadCache usedCache) 
                    throws IOException
    {
            System.out.println("doReadJSON(): InputStream=" + in + 
            " cache="+usedCache);
            GeoJSONReader reader = new GeoJSONReader();

            PoiWayBundle bundle=reader.read(in, usedCache, tile);
            if(bundle!=null)
            {
                MapReadResultBuilder builder = new MapReadResultBuilder();
                builder.add(bundle);
                startZoomLevel = tile.zoomLevel;
                return builder.build();
            }
            return null;
    }

    // NW changed to fit interface
    // TODO this is a quickfix for the moment
    public LatLong startPosition()
    {
        return new LatLong (51.05, -0.72); 
    }

    // NW changed to fit interface
    public  Byte startZoomLevel()
    {
        return startZoomLevel;
    }

    /* NW not specified in interface therefore not necessary?
    public boolean hasOpenFile()
    {
        return true;
    }
    */

    // NW changed name to fit interface
    public BoundingBox boundingBox()
    {
        // quick fix
        return quickfix.getBoundingBox();
    }

    // NW in interface. We can't really "close" this as the web connection
    // is closed after we have read the geojson
    public void close()
    {
        // do nothing
    }

    // taken from version in MapFile, modified to use quickfix
    public boolean supportsTile(Tile tile) 
    {
        return tile.getBoundingBox().intersects(quickfix.getBoundingBox());
    }

    public long getDataTimestamp(Tile tile)
    {
        // always get new tile for now
        return System.currentTimeMillis();
    }
}

