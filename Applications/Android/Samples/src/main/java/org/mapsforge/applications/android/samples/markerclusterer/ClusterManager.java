/*
 * Copyright 2009 Huan Erdao
 * Copyright 2014 Martin Vennekamp
 * Copyright 2015 mapsforge.org
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.mapsforge.applications.android.samples.markerclusterer;

import android.os.AsyncTask;
import android.widget.Toast;

import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Point;
import org.mapsforge.map.model.DisplayModel;
import org.mapsforge.map.model.MapViewPosition;
import org.mapsforge.map.model.common.Observer;
import org.mapsforge.map.util.MapViewProjection;
import org.mapsforge.map.view.MapView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Class for Clustering geotagged content. this clustering came from
 * "markerclusterer" which is available as opensource at
 * https://code.google.com/p/android-maps-extensions/, resp
 * https://github.com/googlemaps/android-maps-utils this is android ported
 * version with modification to fit to the application refer also to other
 * implementations:
 * https://code.google.com/p/osmbonuspack/source/browse/#svn%2Ftrunk
 * %2FOSMBonusPack%2Fsrc%2Forg%2Fosmdroid%2Fbonuspack%2Fclustering
 * http://developer.nokia.com/community/wiki/
 * Map_Marker_Clustering_Strategies_for_the_Maps_API_for_Java_ME
 * <p/>
 * based on http://ge.tt/7Zq63CH/v/1
 * <p/>
 * Should be added as Observer on Mapsforge frameBufferModel.
 */
public class ClusterManager<T extends GeoItem> implements Observer, SelectionHandler<T> {
    /**
     * A 'Toast' to display information, intended to show information on {@link ClusterMarker}
     * with more than one {@link GeoItem} (while Marker with a single GeoItem should have their
     * own OnClick functions)
     */
    protected static Toast toast;
    /**
     * grid size for Clustering(dip).
     */
    protected final float GRIDSIZE = 28 * DisplayModel.getDeviceScaleFactor();
    /**
     * MapView object.
     */
    protected final MapView mapView;

    /**
     * The maximum (highest) zoom level for clustering the items.,
     */
    protected final byte maxClusteringZoom;
    /**
     * Lock for the re-Clustering of the items.
     */
    public boolean isClustering = false;
    /**
     * MarkerBitmap object for marker icons, uses Static access.
     */
    protected List<MarkerBitmap> markerIconBmps = Collections
            .synchronizedList(new ArrayList<MarkerBitmap>());
    /**
     * The current BoundingBox of the viewport.
     */
    protected BoundingBox currBoundingBox;
    /**
     * GeoItem ArrayList object that are out of viewport and need to be
     * clustered on panning.
     */
    protected List<T> leftItems = Collections
            .synchronizedList(new ArrayList<T>());
    /**
     * Clustered object list.
     */
    protected List<Cluster<T>> clusters = Collections
            .synchronizedList(new ArrayList<Cluster<T>>());
    /**
     * Selected cluster
     */
    protected T selectedItem = null;
    /**
     * saves the actual ZoolLevel of the MapView
     */
    private double oldZoolLevel;
    /**
     * saves the actual Center as LatLong of the MapViewPosition
     */
    private LatLong oldCenterLatLong;
    private AsyncTask<Boolean, Void, Void> clusterTask;
    private boolean ignoreOnTapCallBack;

    /**
     * @param mapView           The Mapview in which the markers are shoen
     * @param markerBitmaps     a list of well formed {@link MarkerBitmap}
     * @param maxClusteringZoom The maximum zoom level, beyond this level no clustering is performed.
     */
    public ClusterManager(MapView mapView,
                          List<MarkerBitmap> markerBitmaps, byte maxClusteringZoom, boolean ignoreOnTapCallBack) {
        this.mapView = mapView;
        // set to impossible values to trigger clustering at first onChange
        oldZoolLevel = -1;
        oldCenterLatLong = new LatLong(-180.0, -180.0);
//		// Check correct order of the makerbitmaps within the list
//		for (int i = 1; i < markerBitmaps.size(); i++) {
//			if (markerBitmaps.get(i - 1).getItemMax() > markerBitmaps.get(i)
//					.getItemMax()) {
//				throw new IllegalArgumentException(
//						"markerBitmaps must be in "
//								+ "order from smallest to largest 'maxSize' values, but "
//								+ (i - 1)
//								+ ".element's maxSize is larger than the following value!");
//			}
//		}
        synchronized (this.markerIconBmps) {
            this.markerIconBmps = markerBitmaps;
        }
        // ClusterManager.gap = Caption.DEFAULT_GAP * mapView.getModel().displayModel.getScaleFactor();
        this.maxClusteringZoom = maxClusteringZoom;
        this.ignoreOnTapCallBack = ignoreOnTapCallBack;
    }

    /**
     * You might like to set the Toast from external, in order to make sure that only a single Toast
     * is showing up.
     *
     * @param toast A 'Toast' to display information, intended to show information on {@link ClusterMarker}
     */
    public static void setToast(Toast toast) {
        ClusterManager.toast = toast;
    }

    public MapView getMapView() {
        return mapView;
    }

    public synchronized List<T> getAllItems() {
        // Log.w(TAG,"List<T> getAllItems() {...");
        List<T> rtnList = Collections.synchronizedList(new ArrayList<T>());
        synchronized (leftItems) {
            rtnList.addAll(leftItems);
        }
        synchronized (clusters) {
            for (Cluster<T> mCluster : clusters) {
                rtnList.addAll(mCluster.getItems());
            }
        }
        return rtnList;
    }

    public T getSelectedItem() {
        return selectedItem;
    }

    // public static float gap;

    public void setSelectedItem(SelectionHandler<T> sender, T selectedItem) {
//		Log.d(TAG,"setSelectedItem(Selecting... (super) is called" );
        this.selectedItem = selectedItem;
    }

    /**
     * add item and do isClustering. NOTE: this method will not redraw screen.
     * after adding all items, you must call redraw() method.
     *
     * @param item GeoItem to be clustered.
     */
    public synchronized void addItem(final T item) {
        // Log.w(TAG,"public synchronized void addItem(T item) {... (START)");
        // if mapView is not inflated or if not in viewport, add to leftItems
        if (mapView.getWidth() == 0 || !isItemInViewport(item)) {
//			Log.i(TAG,"public synchronized void addItem(T item) {... (1)");
            synchronized (leftItems) {
                // Log.w(TAG,"public synchronized void addItem(T item) {... (2)");
                if (clusterTask != null && clusterTask.isCancelled()) return;
                leftItems.add(item);
            }
        } else if (maxClusteringZoom >= mapView.getModel().mapViewPosition
                .getZoomLevel()) {
            // else add to a cluster;
            MapViewProjection proj = new MapViewProjection(mapView);
            Point pos = proj.toPixels(item.getLatLong());
            // check existing cluster
//			Log.i(TAG,"spublic synchronized void addItem(T item) {...... (3)");
            synchronized (clusters) {
                // Log.w(TAG,"spublic synchronized void addItem(T item) {...... (4)");
                for (Cluster<T> mCluster : clusters) {
//					Log.i(TAG,"public synchronized void addItem(T item) {... (5)");
                    if (clusterTask != null && clusterTask.isCancelled()) return;
                    if (mCluster.getItems().size() == 0)
                        throw new IllegalArgumentException("cluster.getItems().size() == 0");
                    // find a cluster which contains the marker.
                    // use 1st element to fix the location, hinder the cluster from
                    // running around and isClustering.
                    LatLong gpCenter = mCluster.getItems().get(0)
                            .getLatLong();
                    if (gpCenter == null)
                        throw new IllegalArgumentException();
                    Point ptCenter = proj.toPixels(gpCenter);
                    // find a cluster which contains the marker.
                    if (pos.distance(ptCenter) <= GRIDSIZE
                    /*
                     * pos.x >= ptCenter.x - GRIDSIZE && pos.x <= ptCenter.x +
					 * GRIDSIZE && pos.y >= ptCenter.y - GRIDSIZE && pos.y <=
					 * ptCenter.y + GRIDSIZE
					 */) {
                        mCluster.addItem(item);
                        return;
                    }
                } // END: for (Cluster<T> cluster : clusters) {
                // No cluster contain the marker, create a new cluster.
                clusters.add(createCluster(item));
            } // END: synchronized (clusters) {
        } else {
            // No clustering allowed, create a new cluster with single item.
            synchronized (clusters) {
//				Log.i(TAG,"public synchronized void addItem(T item) {... (6)");
                clusters.add(createCluster(item));
            }
        }
//		Log.i(TAG,"public synchronized void addItem(T item) {... (END)");
    }

    /**
     * Create Cluster Object. override this method, if you want to use custom
     * GeoCluster class.
     *
     * @param item GeoItem to be set to cluster.
     */
    public Cluster<T> createCluster(T item) {
        return new Cluster<T>(this, item);
    }

    /**
     * redraws clusters
     */
    public synchronized void redraw() {
        synchronized (clusters) {
            if (!isClustering)
                for (Cluster<T> mCluster : clusters) {
                    mCluster.redraw();
                }
        }
    }

    /**
     * check if the item is within current viewport.
     *
     * @return true if item is within viewport.
     */
    protected boolean isItemInViewport(final GeoItem item) {
        return getCurBounds().contains(item.getLatLong());
    }

    /**
     * get the current BoundingBox of the viewport
     *
     * @return current BoundingBox of the viewport
     */
    protected synchronized BoundingBox getCurBounds() {
        if (currBoundingBox == null) {
            MapViewProjection projection = new MapViewProjection(mapView);
            if (mapView == null) {
                throw new NullPointerException("mapView == null");
            }
            if (mapView.getWidth() <= 0 || mapView.getHeight() <= 0) {
                throw new IllegalArgumentException(" mapView.getWidth() <= 0 " +
                        "|| mapView.getHeight() <= 0 "
                        + mapView.getWidth() + " || " + mapView.getHeight());
            }
            /** North-West geo point of the bound */
            LatLong nw_ = projection.fromPixels(0, 0);
//			Log.e(TAG, " nw_.latitude => " + nw_.latitude + " nw_.longitude => " + nw_.longitude );
            /** South-East geo point of the bound */
            LatLong se_ = projection.fromPixels(mapView.getWidth(),
                    mapView.getHeight());
//			Log.e(TAG, " se_.latitude => " + se_.latitude + " se_.longitude => " + se_.longitude );
            if (se_.latitude > nw_.latitude) {
                currBoundingBox = new BoundingBox(nw_.latitude, se_.longitude, se_.latitude,
                        nw_.longitude);
            } else {
                currBoundingBox = new BoundingBox(se_.latitude, nw_.longitude, nw_.latitude,
                        se_.longitude);
            }
        }
        return currBoundingBox;
    }

    /**
     * add items that were not clustered in last isClustering.
     */
    private void addLeftItems() {
        // Log.w(TAG,"addLeftItems() {.... (0)");

        if (leftItems.size() == 0) {
            return;
        }
        // Log.w(TAG,"addLeftItems() {.... (1)");
        ArrayList<T> currentLeftItems = new ArrayList<T>();
        currentLeftItems.addAll(leftItems);
        // Log.w(TAG,"addLeftItems() {.... (2)");

        synchronized (leftItems) {
            // Log.w(TAG,"addLeftItems() {.... (3.1)");
            leftItems.clear();
            // Log.w(TAG,"addLeftItems() {.... (3.1)");
        }
        // Log.w(TAG,"addLeftItems() {.... (4)");
        for (T currentLeftItem : currentLeftItems) {
            // Log.w(TAG,"addLeftItems() {.... (5.1)");
            addItem(currentLeftItem);
            // Log.w(TAG,"addLeftItems() {.... (5.2)");
        }
    }

    // *********************************************************************************************************************
    // Methods to implement 'Observer'
    // *********************************************************************************************************************

    @Override
    public synchronized void onChange() {
        currBoundingBox = null;
        if (isClustering)
            return;
        // Log.e(getClass().getSimpleName(), "Position: "
        // + mapView.getModel().mapViewPosition.getCenter().toString()
        // + "ZoomLavel: " +
        // mapView.getModel().mapViewPosition.getZoomLevel());
        if (oldZoolLevel != mapView.getModel().mapViewPosition.getZoomLevel()) {
            // react on zoom changes
            // Log.d(TAG, "zooming...");
            oldZoolLevel = mapView.getModel().mapViewPosition.getZoomLevel();
            resetViewport(false);
        } else {
            // react on position changes
            MapViewPosition mapViewPosition = mapView.getModel().mapViewPosition;
            MapViewProjection projection = new MapViewProjection(mapView);

            Point posOld = projection.toPixels(oldCenterLatLong);
            Point posNew = projection.toPixels(mapViewPosition.getCenter());
            if (posOld != null && posOld.distance(posNew) > GRIDSIZE / 2) {
                // Log.d(TAG, "moving...");
                oldCenterLatLong = mapViewPosition.getCenter();
                resetViewport(true);
            }
        }
    }

    /**
     * reset current viewport, re-cluster the items when zoom has changed, else
     * add not clustered items .
     */
    private synchronized void resetViewport(boolean isMoving) {
        isClustering = true;
        clusterTask = new ClusterTask();
        clusterTask.execute(new Boolean[]{isMoving});
    }

    public void cancelClusterTask() {
        if (clusterTask != null) {
            clusterTask.cancel(true);
        }
    }

    public synchronized void destroyGeoClusterer() {
        synchronized (clusters) {
            for (Cluster<T> cluster : clusters) {
                cluster.getClusterManager().cancelClusterTask();
                cluster.clear();
            }
            clusters.clear();
        }
        for (MarkerBitmap markerBitmap : markerIconBmps) {
            if (markerBitmap.getBitmap(true) != null)
                markerBitmap.getBitmap(true).decrementRefCount();
            if (markerBitmap.getBitmap(false) != null)
                markerBitmap.getBitmap(false).decrementRefCount();
        }
        /** Clustered object list. */
        // Log.w(TAG,"synchronized (leftItems) {... in destroyGeoClusterer() {....");
        synchronized (leftItems) {
            leftItems.clear();
        }
        MarkerBitmap.clearCaptionBitmap();
    }

    public boolean getIgnoreOnTap() {
        return this.ignoreOnTapCallBack;
    }

    private class ClusterTask extends AsyncTask<Boolean, Void, Void> {

        @Override
        protected Void doInBackground(Boolean... params) {
            // If the map is moved without zoom-change: Add unclustered items.
            if (params[0]) {
                addLeftItems();
            }
            // If the cluster zoom level changed then destroy the cluster and
            // collect its markers.
            else {
                // Log.w(TAG,"protected Void doInBackground(Boolean... params) { (1)");
                synchronized (clusters) {
                    // Log.w(TAG,"protected Void doInBackground(Boolean... params) { (2)");
                    for (Cluster<T> mCluster : clusters) {
                        // Log.w(TAG,"protected Void doInBackground(Boolean... params) { (3)");
                        synchronized (leftItems) {
                            // Log.w(TAG,"protected Void doInBackground(Boolean... params) { (4)");
                            leftItems.addAll(mCluster.getItems());
                            // Log.w(TAG,"protected Void doInBackground(Boolean... params) { (5)");
                        }
                        // Log.w(TAG,"protected Void doInBackground(Boolean... params) { (6)");
                        mCluster.clear();
                        // Log.w(TAG,"protected Void doInBackground(Boolean... params) { (7)");
                    }
                }
                // Log.w(TAG,"protected Void doInBackground(Boolean... params) { (8)");
                synchronized (clusters) {
                    clusters.clear();
                }
                // Log.w(TAG,"protected Void doInBackground(Boolean... params) { (9)");
                if (!isCancelled()) {
                    synchronized (clusters) {
                        if (clusters.size() != 0) {
                            throw new IllegalArgumentException();
                        }
                    }
                    // Log.w(TAG,"protected Void doInBackground(Boolean... params) { (10)");
                    // Log.w(TAG,"protected Void doInBackground(Boolean... params) { (11)");
                    addLeftItems();
                }
            }
            // Log.w(TAG,"protected Void doInBackground(Boolean... params) { (ENDE)");
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            isClustering = false;
            redraw();
        }
    }
}
