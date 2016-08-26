# A very basic Android app example

# Introduction

The best way to get started building an Android app with mapsforge is by studying the Samples app, which gives a complete overview of the capabilities of the mapsforge library. 

Here, however, we go through a very basic example of an app that simply displays a map rendered from a map file in the built-in render style.

# Map file

To run this samples app, you need a map with filename [germany.map](http://download.mapsforge.org/maps/europe/germany.map) installed on storage.

Preferably the [berlin.map](http://download.mapsforge.org/maps/europe/germany/berlin.map) renamed as `germany.map` will suffice because of smaller size.

# Hardware acceleration

Mapsforge currently requires disabling hardware acceleration for the map view. This can be controlled in various levels, better described in Android [documentation](http://developer.android.com/guide/topics/graphics/hardware-accel.html#controlling).

# Android manifest
You'll need to have the appropriate permissions in manifest for tile cache to work properly:

```xml
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
```

# App Initialization

Before you make any calls on the mapsforge library, you need to initialize the AndroidGraphicFactory. Behind the scenes, this initialization process gathers a bit of information on your device, such as the screen resolution, that allows mapsforge to automatically adapt the rendering for the device.

```java
AndroidGraphicFactory.createInstance(this.getApplication());
```

If you forget this step, your app will crash. You can place this code, like in the Samples app, in the Android Application class. This ensures it is created before any specific activity. But it can also be created in the onCreate() method in your activity.

## Create a MapView

A MapView is an Android View (or ViewGroup) that displays a mapsforge map. You can have multiple MapViews in your app or even a single Activity. Have a look at the mapviewer.xml on how to create a MapView using the Android XML Layout definitions. Here we create a MapView on the fly and make the content view of the activity the MapView. This means that no other elements make up the content of this activity.

```java
this.mapView = new MapView(this);
setContentView(this.mapView);
```

We then make some simple adjustments, such as showing a scale bar, zoom controls and setting zoom limits:

```java
this.mapView.setClickable(true);
this.mapView.getMapScaleBar().setVisible(true);
this.mapView.setBuiltInZoomControls(true);
this.mapView.setZoomLevelMin((byte) 10);
this.mapView.setZoomLevelMax((byte) 20);
```

## Create a TileCache

To avoid redrawing all the tiles all the time, we need to set up a tile cache. A utility method helps with this:

```java
this.tileCache = AndroidUtil.createTileCache(this, "mapcache", mapView.getModel().displayModel.getTileSize(), 1f, this.mapView.getModel().frameBufferModel.getOverdrawFactor());
```

## Creating a Map Layer

Now we need to set up the process of displaying a map. A map can have several layers, stacked on top of each other. A layer can be a map or some visual elements, such as markers. Here we only show a map based on a mapsforge map file. For this we need a TileRendererLayer. A tileRendererLayer needs a tileCache to hold the generated map tiles, a map file from which the tiles are generated and rendertheme that defines the appearance of the map:

```java
MapDataStore mapDataStore = new MapFile(new File(Environment.getExternalStorageDirectory(), MAP_FILE));
this.tileRendererLayer = new TileRendererLayer(tileCache, mapDataStore, this.mapView.getModel().mapViewPosition, AndroidGraphicFactory.INSTANCE);
tileRendererLayer.setXmlRenderTheme(InternalRenderTheme.OSMARENDER);
```

On its own a tileRendererLayer does not know where to display the map, so we need to associate it with our mapView:

```java
this.mapView.getLayerManager().getLayers().add(tileRendererLayer);
```

## Specifying the Position

The map also needs to know which area to display and at what zoom level:

```java
this.mapView.setCenter(new LatLong(52.517037, 13.38886));
this.mapView.setZoomLevel((byte) 12);
```

Note: Above map position is specific to Berlin region.

Refer to the Samples app on how to read the initial position out of a map file or how to store the current position when a user leaves your app.

## Cleanup

Whenever your activity changes, some cleanup operations have to be performed lest your app runs out of memory. 

```java
@Override
protected void onDestroy() {
    this.mapView.destroyAll();
    AndroidGraphicFactory.clearResourceMemoryCache();
    super.onDestroy();
}
```

## All in one

Here comes the whole as a single file:

```java
import java.io.File;

import org.mapsforge.core.model.LatLong;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.android.util.AndroidUtil;
import org.mapsforge.map.android.view.MapView;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.layer.renderer.TileRendererLayer;
import org.mapsforge.map.datastore.MapDataStore;
import org.mapsforge.map.reader.MapFile;
import org.mapsforge.map.rendertheme.InternalRenderTheme;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;

public class MainActivity extends Activity {
    // name of the map file in the external storage
    private static final String MAP_FILE = "germany.map";

    private MapView mapView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AndroidGraphicFactory.createInstance(this.getApplication());

        this.mapView = new MapView(this);
        setContentView(this.mapView);

        this.mapView.setClickable(true);
        this.mapView.getMapScaleBar().setVisible(true);
        this.mapView.setBuiltInZoomControls(true);
        this.mapView.setZoomLevelMin((byte) 10);
        this.mapView.setZoomLevelMax((byte) 20);

        // create a tile cache of suitable size
        TileCache tileCache = AndroidUtil.createTileCache(this, "mapcache",
                mapView.getModel().displayModel.getTileSize(), 1f,
                this.mapView.getModel().frameBufferModel.getOverdrawFactor());

        // tile renderer layer using internal render theme
        MapDataStore mapDataStore = new MapFile(new File(Environment.getExternalStorageDirectory(), MAP_FILE));
        TileRendererLayer tileRendererLayer = new TileRendererLayer(tileCache, mapDataStore,
                this.mapView.getModel().mapViewPosition, AndroidGraphicFactory.INSTANCE);
        tileRendererLayer.setXmlRenderTheme(InternalRenderTheme.OSMARENDER);

        // only once a layer is associated with a mapView the rendering starts
        this.mapView.getLayerManager().getLayers().add(tileRendererLayer);

        this.mapView.setCenter(new LatLong(52.517037, 13.38886));
        this.mapView.setZoomLevel((byte) 12);
    }

    @Override
    protected void onDestroy() {
        this.mapView.destroyAll();
        AndroidGraphicFactory.clearResourceMemoryCache();
        super.onDestroy();
    }
}
```
