/*
 * Copyright (C) 2009 Huan Erdao
 * Copyright (C) 2014 Martin Vennekamp
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *	  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mapsforge.applications.android.samples.markerclusterer;

import org.mapsforge.core.model.LatLong;
import org.mapsforge.map.layer.Layers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Cluster class.
 * contains single marker object(ClusterMarker). mostly wraps methods in ClusterMarker.
 */
public class Cluster<T extends GeoItem> {
    /**	GeoClusterer object	 */
    private ClusterManager<T> mClusterManager;
    public ClusterManager<T> getClusterManager(){
    	return mClusterManager;
    }
    /**	center of cluster */
    private LatLong mCenter;
    /**	list of GeoItem within cluster */
    private List<T> mItems = Collections.synchronizedList(new ArrayList<T>());
    /** ClusterMarker object */
    private ClusterMarker<T> mClusterMarker;

	public String getTitle() {
		if ( getItems().size() == 1 ) {
			return getItems().get(0).getTitle();
		}
		return String.valueOf(getItems().size());
	}
    /**
     * @param clusterer ClusterManager object.
     */
    public Cluster(ClusterManager<T> clusterer, T item) {
    	mClusterManager = clusterer;
    	mClusterMarker = new ClusterMarker<T>(this, clusterer.getIgnoreOnTap() );
        addItem(item);
    }

    /**
     * add item to cluster object
     * @param item GeoItem object to be added.
     */
    public synchronized void addItem(T item){
    	synchronized (mItems) {
            mItems.add(item);
		}
//        mClusterMarker.setMarkerBitmap();
        if(mCenter == null){
            mCenter = item.getLatLong();
        } else {
            // computing the centroid
            double lat = 0, lon = 0;
            int n = 0;
        	synchronized (mItems) {
	            for (T object : mItems) {
	            	if ( object  == null ) {
	            		throw new NullPointerException(" object == null !");
	            	}
	            	if ( object.getLatLong()  == null ) {
	            		throw new NullPointerException(" object.getLatLong() == null !");
	            	}
	                lat += object.getLatLong().latitude;
	                lon += object.getLatLong().longitude;
	                n++;
	            }
        	}
            mCenter = new LatLong(lat / n, lon / n);
        }
    }

    /**
     * get center of the cluster.
     * @return center of the cluster in LatLong.
     */
    public LatLong getLocation(){
        return mCenter;
    }

    /**
     * get list of GeoItem.
     * @return list of GeoItem within cluster.
     */
    public synchronized List<T> getItems(){
    	synchronized (mItems) {
    		return mItems;
    	}
    }

    /**
     * clears cluster object and removes the cluster from the layers collection.
     */
    public void clear() {
        if(mClusterMarker != null) {
            Layers mapOverlays = mClusterManager.getMapView().getLayerManager().getLayers();
            if(mapOverlays.contains(mClusterMarker)){
                mapOverlays.remove(mClusterMarker);
            }
            mClusterManager = null;
            mClusterMarker = null;
        }
    	synchronized (mItems) {
    		mItems.clear();
    	}
    }

    /**
     * add the ClusterMarker to the Layers if is within Viewport, otherwise remove.
     */
    public void redraw(){
        Layers mapOverlays = mClusterManager.getMapView().getLayerManager().getLayers();
        if (mClusterMarker != null && !mClusterManager.getCurBounds().contains(mCenter)
                && mapOverlays.contains(mClusterMarker)) {
            mapOverlays.remove(mClusterMarker);
            return;
        }
        if (mClusterMarker != null && !mapOverlays.contains(mClusterMarker) && !mClusterManager.isClustering ) {
            mapOverlays.add(1, mClusterMarker);
        }
    }
}