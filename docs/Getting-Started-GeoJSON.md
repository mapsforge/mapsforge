# Using a GeoJSON data source

This example will show how to use the in-development GeoJSONDataSource.
It should be pointed out that this is an extremely early stage of development
right now, and the rendering does not look at all good. Nonetheless the
example below compiles and runs and shows a (rather messy) map.

    package freemap.mapsforgebasic;
    
    import android.os.Bundle;
    import android.os.Environment;
  
    import org.mapsforge.map.android.MapActivity;  
    import org.mapsforge.map.android.util.AndroidUtil;
    import org.mapsforge.map.android.view.MapView;
    import org.mapsforge.map.layer.renderer.TileRendererLayer;
    import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
    import org.mapsforge.map.reader.GeoJSONDataSource;
    import org.mapsforge.map.reader.DownloadCache;
    import org.mapsforge.core.model.LatLong;
    
    import java.io.File;
    
    public class MainActivity extends MapActivity {
    
        MapView mv;
        TileCache tileCache;
        TileRendererLayer layer;
        GeoJSONDataSource ds;
    
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
    
            mv = new MapView(this);
    
            setContentView(mv);
            mv.setBuiltInZoomControls(true);
            mv.setClickable(true);
    
            createDefaultTileCache(mv, "mapcache");
    
            DownloadCache downloadCache = AndroidUtil.getDownloadCache (this, "gjtest");
    
            ds = new GeoJSONDataSource("http://www.free-map.org.uk/fm/ws/tsvr.php",
                    "way=highway,natural&poi=natural,place,amenity&ext=20&contour=1&outProj=4326");
    
    
            ds.setDownloadCache(downloadCache);
    
        }
    
        protected void onStart() {
            super.onStart();
            try {
                String dir = Environment.getExternalStorageDirectory().getAbsolutePath() + "/opentrail/";
                mv.setCenter(new LatLong(51.05, -0.72));
                mv.setZoomLevel((byte) 14);
    
    
                layer = new TileRendererLayer(getTileCache(), ds,
                        mv.getModel().mapViewPosition, false, true,
                        AndroidGraphicFactory.INSTANCE);
    
                ExternalRenderTheme theme = new ExternalRenderTheme
                        (new File(dir + "freemap_v4.xml"));
                layer.setXmlRenderTheme(theme);
                mv.addLayer(layer);
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    
        protected void onDestroy() {
            super.onDestroy();
            mv.destroyAll();
        }
    }

Note the following on this example:
* We create a GeoJSONDataSource object. This takes two parameters:
	* The URL of the webservice which generates the GeoJSON;
	* Any additional query string parameters needed by the webservice.
  Note that the webservice must accept three query string parameters "x", "y"
  and "z" representing the tile, and must return the GeoJSON in epsg:4326 
  standard lat/lon. The GeoJSON must include OSM key/value pairs in the
  "properties" attribute of each feature.
* Note how we use a DownloadCache object. This caches the GeoJSON on the device
  avoiding having to download it next time. AndroidUtil.getDownloadCache()
  returns a DownloadCache object, which we pass to the GeoJSONDataSource.
* The freemap_v4.xml may be downloaded from

  http://www.free-map.org.uk/data/android
 
  It is England and Wales right-of-way oriented as it colours paths based on
  the "designation" tag. Otherwise just use the default OSMARENDER theme.

The example also shows the following Android Mapsforge API tweaks for this
fork of the repository:
* We use MapActivity for our activity. This takes care of some commonly-used
  tasks for us, e.g. cache management, simplifying the code:
    * its onCreate() calls AndroidGraphicFactory.createInstance();
    * it has a convenience createDefaultTileCache() method which creates a
    tile cache with default settings given a MapView. We can then access the 
    cache with getTileCache() (e.g. when we create our TileRendererLayer);
* MapView has convenience methods setCenter(), setZoomLevel() and addLayer()
  added, simplifying the API.
