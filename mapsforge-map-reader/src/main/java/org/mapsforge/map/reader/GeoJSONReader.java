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

    static class FeatureTests {
        public static boolean isWaterFeature(String k, String v) {
            return k.equals("natural") && v.equals("water") ||
                    k.equals("waterway");
        }
    
        public static boolean isLandscapeFeature(String k, String v) {
            return k.equals("natural") && v.equals("wood") ||
                    k.equals("landuse") && v.equals("forest") ||
                    k.equals("natural") && v.equals("heath");
        }
    }    

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
        byte layer;
        for    (int i=0; i<features.length(); i++) {
            JSONObject currentFeature = features.getJSONObject(i);
            String type = currentFeature.getString("type");
            layer=(byte)4; // default for roads, paths etc
            if(type.equals("Feature")) {
                JSONObject geometry = currentFeature.getJSONObject("geometry"),
                    properties = currentFeature.getJSONObject("properties");
                ArrayList<Tag> tags = new ArrayList<Tag>();    
                Iterator it = properties.keys();
                while(it.hasNext()) {
                    String k = (String)it.next(), v=properties.getString(k);
                    if(k.equals("contour")) {
                        layer=(byte)2; // contours under roads/paths
                    } else if (FeatureTests.isLandscapeFeature(k,v)) {
                        layer = (byte)1; // woods etc below contours
                    } else if (FeatureTests.isWaterFeature(k,v)) {
                        layer = (byte)3; // lakes above contours, below roads
                    }
                    tags.add(new Tag(k,v));                
                }

                String gType = geometry.getString("type");
                JSONArray coords = geometry.getJSONArray("coordinates");
                if(gType.equals("Point")) {
                    LatLong ll = new LatLong
                        ( coords.getDouble(1), coords.getDouble(0) );
                    PointOfInterest poi = new PointOfInterest
                        ((byte)5, tags, ll); // pois above all else
                    bundle.pois.add(poi);
                } else if (gType.equals("LineString")) {
                    LatLong[][] points = readWayFeature(coords);
                    Way way = new Way(layer, tags, points, null);
                    bundle.ways.add(way);
                } else if (gType.equals("MultiLineString")) {
                    LatLong[][] points = readMultiWayFeature(coords);
                    Way way = new Way(layer, tags, points, null);
                    bundle.ways.add(way);
                } else if (gType.equals("Polygon")) {
                    // polygons are 3-deep in geojson but only actually 
                    // contain one line so we simplify them
                    LatLong[][] points = readWayFeature(coords.getJSONArray(0));
                    Way way = new Way(layer, tags, points, null);
                    bundle.ways.add(way);
                } else if (gType.equals("MultiPolygon")) {
                    LatLong[][] points=readMultiWayFeature
                        (coords.getJSONArray(0));
                    Way way = new Way(layer, tags, points, null);
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


    private LatLong[][] readWayFeature(JSONArray coords) {
        LatLong[][] points = new LatLong[1][];
        points[0] = new LatLong[coords.length()];
        readLineString(points[0], coords);
        return points;
    }

    private void readLineString(LatLong[] points, JSONArray coords) {
        for(int j=0; j<coords.length(); j++) {
            JSONArray curPoint = coords.getJSONArray(j);
            points[j] = new LatLong(curPoint.getDouble(1),
                                        curPoint.getDouble(0));
        }    
    }

    private LatLong[][] readMultiWayFeature (JSONArray coords) {
        LatLong[][] points = new LatLong[coords.length()][];    
        for(int i=0; i<coords.length(); i++) {
            points[i]=new LatLong[coords.getJSONArray(i).length()];
            readLineString(points[i], coords.getJSONArray(i));
        }
        return points;
    }
}
