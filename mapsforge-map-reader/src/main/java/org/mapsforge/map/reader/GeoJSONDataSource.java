package org.mapsforge.map.reader;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.InputStream;


import org.mapsforge.core.model.Tile;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.BoundingBox;

// NW now implements MapDataStore rather than extending MapDataSource
public class GeoJSONDataSource implements MapDataStore {

    byte startZoomLevel;
    DownloadCache cache;
	BoundingBox boundingBox; // bounding box of all data so far
	Byte lastRequestedZoomLevel;

    // queryString = any compulsory query string data needed by the server
    String server, queryString;

    public GeoJSONDataSource(String server) {
        this(server,null);
    }

    public GeoJSONDataSource(String server, String queryString) {
        startZoomLevel = (byte)14;
        this.server = server;
        this.queryString = queryString;
    }

    public void setDownloadCache(DownloadCache cache) {
        this.cache = cache;
    }

    // Read a tile
    // Query the Freemap server for the appropriate GeoJSON tile by x,y,z
    // request all highways and all POIs for now
    public MapReadResult readMapData(Tile tile) {
      InputStream in;
      HttpURLConnection conn=null;
      lastRequestedZoomLevel = tile.zoomLevel;
      try {
      	if(cache!=null && cache.inCache(tile)) {
            System.out.println("In cache: " + 
                tile.tileX+","+tile.tileY+","+tile.zoomLevel);
            in  = cache.getInputStream(tile);
            // Pass in null as the used cache so that the reader doesn't
            // try to cache the tile as it's already there!
            MapReadResult result = doReadJSON(in, tile, null);
			if(result!=null)
				extendBoundingBox(tile);
			return result;
        	} else {    
            	URL url = new URL (server + "?x="+
                    tile.tileX+"&y="+tile.tileY+"&z="+tile.zoomLevel+
					(queryString==null ? "":"&"+queryString));
            	System.out.println("Tile details: " + 
            		tile.tileX+","+tile.tileY+","+tile.zoomLevel);
            	conn = (HttpURLConnection)url.openConnection();
            	in = conn.getInputStream();
            	if(conn.getResponseCode()==200) {
					MapReadResult result = doReadJSON(in, tile, cache);
					if(result!=null)
						extendBoundingBox(tile);
					return result;
            	}
			}
       } catch(Exception e) {
       		e.printStackTrace();
       }
        return null;
    }

    private MapReadResult doReadJSON
            (InputStream in, Tile tile, DownloadCache usedCache) 
                    throws IOException {
            System.out.println("doReadJSON(): InputStream=" + in + 
            " cache="+usedCache);
            GeoJSONReader reader = new GeoJSONReader();

            PoiWayBundle bundle=reader.read(in, usedCache, tile);
            if(bundle!=null) {
                MapReadResultBuilder builder = new MapReadResultBuilder();
                builder.add(bundle);
                startZoomLevel = tile.zoomLevel;
                return builder.build();
            }
            return null;
    }

    // NW changed to fit interface
	// the MapFile implementation either gets a start point from the mapfile
	// header, or uses the centre point of the map, so we do the latter
    public LatLong startPosition() {
        return boundingBox==null? null: boundingBox.getCenterPoint();
    }

	// NW changed to fit interface
	// Only appears to be used in DatabaseRenderer.getStartZoomLevel(),
	// which returns a default zoom level if this returns null
	// get it to return the last zoom level requested (or null if no requests)
    public  Byte startZoomLevel() {
        return lastRequestedZoomLevel;
    }

    // NW changed name to fit interface
	// returns the current bounding box or an arbitrary zero-size one
    public BoundingBox boundingBox() {
        return boundingBox;
    }

    // NW in interface. We can't really "close" this as the web connection
    // is closed after we have read the geojson
    public void close() {
        // do nothing
    }

	// always return true as this is called before the data is requested
	// (so the bounding box might be null at this point)
    public boolean supportsTile(Tile tile) {
		return true;
    }

	// always get new tile for now
	// again for a GeoJSONDataSource this isn't really relevant as the
	// datasource time will never be more recent than the rendered tile
    public long getDataTimestamp(Tile tile) {
        return System.currentTimeMillis();
    }
	
	private void extendBoundingBox(Tile tile) {
		BoundingBox box = tile.getBoundingBox();
		boundingBox = (boundingBox==null) ? box: boundingBox.extend(box);
	}
}

