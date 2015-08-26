// JSONReader
// reads some GeoJSON into a PoiWayBundle 

package org.mapsforge.map.reader;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;
import org.mapsforge.core.model.Tag;
import org.mapsforge.core.model.Tile;
import org.mapsforge.core.model.LatLong;
import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

public class GeoJSONReader {
    
    public GeoJSONReader() {
    }

    public PoiWayBundle read(InputStream is, DownloadCache cache,
                                Tile tile) throws IOException, JSONException {
        PoiWayBundle bundle = new PoiWayBundle
            (new ArrayList<PointOfInterest>(),
            new ArrayList<Way>());
        String jsonString = readFromStream(is);
        System.out.println("GeoJSONReader.read(): json="+jsonString);
        if(cache!=null) {
            System.out.println("Writing to cache");
            cache.write(tile, jsonString);
        }

        JSONObject data = new JSONObject(jsonString);
        JSONArray features = data.getJSONArray("features");
        System.out.println("Read " + features.length()+" features from JSON.");
        for    (int i=0; i<features.length(); i++) {
            JSONObject currentFeature = features.getJSONObject(i);
            String type = currentFeature.getString("type");
            if(type.equals("Feature")) {
                JSONObject    geometry = currentFeature.getJSONObject("geometry"),
                    properties = currentFeature.getJSONObject("properties");
                ArrayList<Tag> tags = new ArrayList<Tag>();    
                Iterator it = properties.keys();
                while(it.hasNext()) {
                    String k = (String)it.next();
                    tags.add(new Tag(k,properties.getString(k)));                
                }

                String gType = geometry.getString("type");
                if(gType.equals("Point")) {
                    JSONArray coords = geometry.getJSONArray("coordinates");
                    LatLong ll = new LatLong
                        ( coords.getDouble(1), coords.getDouble(0) );
                    PointOfInterest poi = new PointOfInterest
                        ((byte)2, tags, ll);
                    bundle.pois.add(poi);
                } else if (gType.equals("LineString")) {
                    JSONArray coords = geometry.getJSONArray("coordinates");
                    LatLong[][] points = new LatLong[1][];
                    points[0] = new LatLong[coords.length()];
                    for(int j=0; j<coords.length(); j++) {
                        JSONArray curCoords = coords.getJSONArray(j);
                        points[0][j] = 
                            new LatLong(curCoords.getDouble(1),
                                        curCoords.getDouble(0));
                    }    
                    Way way = new Way((byte)1, tags, points, null);
                    bundle.ways.add(way);
                }    
            }
        }
        return bundle;
    }

    private String readFromStream (InputStream is) throws IOException {
        byte[] bytes = new byte[1024];
        StringBuffer text = new StringBuffer();

        int bytesRead;

        while((bytesRead = is.read(bytes,0,1024))>=0) {
            text.append(new String(bytes,0,bytesRead));
        }
        return text.toString();
    }
}
