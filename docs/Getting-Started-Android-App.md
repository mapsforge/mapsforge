# A very basic Android app example

# Introduction

The best way to get started building an Android app with mapsforge is by studying the Samples app, which gives a complete overview of the capabilities of the mapsforge library. 

Here, however, we go through a very basic example of an app that simply displays a map rendered from a mapfile in the built-in render style.  


# App Initialization

Before you make any calls on the mapsforge library, you need to initialize the AndroidGraphicFactory. Behind the scenes, this initialization process gathers a bit of information on your device, such as the screen resolution, that allows mapsforge to automatically adapt the rendering for the device.

    AndroidGraphicFactory.createInstance(this.getApplication());

If you forget this step, your app will crash. You can place this code, like in the Samples app, in the Android Application class. This ensures it is created before any specific activity. But it can also be created in the onCreate() method in your activity.

## Create a MapView

A MapView is an Android View (or ViewGroup) that displays a mapsforge map. You can have multiple MapViews in your app or even a single Activity. Have a look at the BasicMapViewerXml on how to create a MapView using the Android XML Layout definitions. Here we create a MapView on the fly and make the content view of the activity the MapView. This means that no other elements make up the content of this activity.

    this.mapView = new MapView(this);
    setContentView(this.mapView);

We then make some simple adjustments, such as showing a scale bar and setting up the zoom buttons:

    this.mapView.setClickable(true);
    this.mapView.getMapScaleBar().setVisible(true);
    this.mapView.setBuiltInZoomControls(true);
    this.mapView.getMapZoomControls().setZoomLevelMin((byte) 10);
    this.mapView.getMapZoomControls().setZoomLevelMax((byte) 20);

## Create a TileCache

To avoid redrawing all the tiles all the time, we need to set up a tile cache. A utility method helps with this:

    this.tileCache = AndroidUtil.createTileCache(this, "mapcache",mapView.getModel().displayModel.getTileSize(),1f,this.mapView.getModel().frameBufferModel.getOverdrawFactor());

## Creating a Map Layer

Now we need to set up the process of displaying a map. A map can have several layers, stacked on top of each other. A layer can be a map or some visual elements, such as markers. Here we only show a map based on a mapsforge map file. For this we need a TileRendererLayer. A tileRendererLayer needs a tileCache to hold the generated map tiles, a mapfile from which the tiles are generated and rendertheme that defines the appearance of the map:

    this.tileRendererLayer = new TileRendererLayer(tileCache,
    				this.mapView.getModel().mapViewPosition, false, true, AndroidGraphicFactory.INSTANCE);
    tileRendererLayer.setMapFile(getMapFile());
    tileRendererLayer.setXmlRenderTheme(InternalRenderTheme.OSMARENDER);

On its own a tileRendererLayer does not know where to display the map, so we need to associate it with our mapView:

    this.mapView.getLayerManager().getLayers().add(tileRendererLayer);

## Specifying the Position

The map also needs to know which area to display and at what zoom level. This is set via a MapViewPosition:

    this.mapView.getModel().mapViewPosition.setCenter(new LatLong(52.517037, 13.38886));
    this.mapView.getModel().mapViewPosition.setZoomLevel((byte) 12);

Refer to the Samples app on how to read the initial position out of a mapfile or how to store the current position when a user leaves your app.

## Cleanup

Whenever your activity changes, some cleanup operations have to be performed lest your app runs out of memory. 

    @Override
    protected void onStop() {
    	super.onStop();
    	this.mapView.getLayerManager().getLayers().remove(this.tileRendererLayer);
    	this.tileRendererLayer.onDestroy();
    }
    
    @Override
    protected void onDestroy() {
    	super.onDestroy();
    	this.tileCache.destroy();
    	this.mapView.getModel().mapViewPosition.destroy();
    	this.mapView.destroy();
    	AndroidResourceBitmap.clearResourceBitmaps();
    }

## All in one

Here comes the whole as a single file:

    package org.mapsforge.applications.android.simplemapviewer;
    
    import java.io.File;
    
    import org.mapsforge.core.model.LatLong;
    import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
    import org.mapsforge.map.android.graphics.AndroidResourceBitmap;
    import org.mapsforge.map.android.util.AndroidUtil;
    import org.mapsforge.map.android.view.MapView;
    import org.mapsforge.map.layer.cache.TileCache;
    import org.mapsforge.map.layer.renderer.TileRendererLayer;
    import org.mapsforge.map.model.common.PreferencesFacade;
    import org.mapsforge.map.rendertheme.InternalRenderTheme;
    
    import android.app.Activity;
    import android.os.Bundle;
    import android.os.Environment;
    
    
    public class MainActivity extends Activity {
    	
    	// name of the map file in the external storage
    	private static final String MAPFILE = "germany.map";
    	
    	private MapView mapView;	
    	private TileCache tileCache;
    	private TileRendererLayer tileRendererLayer;
    
    
    	@Override
    	protected void onCreate(Bundle savedInstanceState) {
    		super.onCreate(savedInstanceState);
    		
    		AndroidGraphicFactory.createInstance(this.getApplication());
    
    		this.mapView = new MapView(this);
    		setContentView(this.mapView);
    
    		this.mapView.setClickable(true);
    		this.mapView.getMapScaleBar().setVisible(true);
    		this.mapView.setBuiltInZoomControls(true);
    		this.mapView.getMapZoomControls().setZoomLevelMin((byte) 10);
    		this.mapView.getMapZoomControls().setZoomLevelMax((byte) 20);
    
    		// create a tile cache of suitable size
    		this.tileCache = AndroidUtil.createTileCache(this, "mapcache",
    				mapView.getModel().displayModel.getTileSize(), 1f, 
    				this.mapView.getModel().frameBufferModel.getOverdrawFactor());
    	}
    
    	@Override
    	protected void onStart() {
    		super.onStart();
    		
    		this.mapView.getModel().mapViewPosition.setCenter(new LatLong(52.517037, 13.38886));
    		this.mapView.getModel().mapViewPosition.setZoomLevel((byte) 12);
    
    		// tile renderer layer using internal render theme
    		this.tileRendererLayer = new TileRendererLayer(tileCache,
    				this.mapView.getModel().mapViewPosition, false, true, AndroidGraphicFactory.INSTANCE);
    		tileRendererLayer.setMapFile(getMapFile());
    		tileRendererLayer.setXmlRenderTheme(InternalRenderTheme.OSMARENDER);
    		
    		// only once a layer is associated with a mapView the rendering starts
    		this.mapView.getLayerManager().getLayers().add(tileRendererLayer);
    
    	}
    
    	@Override
    	protected void onStop() {
    		super.onStop();
    		this.mapView.getLayerManager().getLayers().remove(this.tileRendererLayer);
    		this.tileRendererLayer.onDestroy();
    	}
    
    	@Override
    	protected void onDestroy() {
    		super.onDestroy();
    		this.tileCache.destroy();
    		this.mapView.getModel().mapViewPosition.destroy();
    		this.mapView.destroy();
    		AndroidResourceBitmap.clearResourceBitmaps();
    	}
    	
    	private File getMapFile() {
    		File file = new File(Environment.getExternalStorageDirectory(), MAPFILE);
    		return file;
    	}
    
    }
    
