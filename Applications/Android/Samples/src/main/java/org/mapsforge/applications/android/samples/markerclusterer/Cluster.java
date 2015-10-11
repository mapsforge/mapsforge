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
    /**
     * GeoClusterer object
     */
    private ClusterManager<T> clusterManager;
    /**
     * Center of cluster
     */
    private LatLong center;
    /**
     * List of GeoItem within cluster
     */
    private List<T> items = Collections.synchronizedList(new ArrayList<T>());
    /**
     * ClusterMarker object
     */
    private ClusterMarker<T> clusterMarker;

    /**
     * @param clusterManager ClusterManager object.
     */
    public Cluster(ClusterManager<T> clusterManager, T item) {
        this.clusterManager = clusterManager;
        this.clusterMarker = new ClusterMarker<T>(this, clusterManager.getIgnoreOnTap());
        addItem(item);
    }

    public ClusterManager<T> getClusterManager() {
        return clusterManager;
    }

    public String getTitle() {
        if (getItems().size() == 1) {
            return getItems().get(0).getTitle();
        }
        return String.valueOf(getItems().size());
    }

    /**
     * add item to cluster object
     *
     * @param item GeoItem object to be added.
     */
    public synchronized void addItem(T item) {
        synchronized (items) {
            items.add(item);
        }
//        clusterMarker.setMarkerBitmap();
        if (center == null) {
            center = item.getLatLong();
        } else {
            // computing the centroid
            double lat = 0, lon = 0;
            int n = 0;
            synchronized (items) {
                for (T object : items) {
                    if (object == null) {
                        throw new NullPointerException("object == null");
                    }
                    if (object.getLatLong() == null) {
                        throw new NullPointerException("object.getLatLong() == null");
                    }
                    lat += object.getLatLong().latitude;
                    lon += object.getLatLong().longitude;
                    n++;
                }
            }
            center = new LatLong(lat / n, lon / n);
        }
    }

    /**
     * get center of the cluster.
     *
     * @return center of the cluster in LatLong.
     */
    public LatLong getLocation() {
        return center;
    }

    /**
     * get list of GeoItem.
     *
     * @return list of GeoItem within cluster.
     */
    public synchronized List<T> getItems() {
        synchronized (items) {
            return items;
        }
    }

    /**
     * clears cluster object and removes the cluster from the layers collection.
     */
    public void clear() {
        if (clusterMarker != null) {
            Layers mapOverlays = clusterManager.getMapView().getLayerManager().getLayers();
            if (mapOverlays.contains(clusterMarker)) {
                mapOverlays.remove(clusterMarker);
            }
            clusterManager = null;
            clusterMarker = null;
        }
        synchronized (items) {
            items.clear();
        }
    }

    /**
     * add the ClusterMarker to the Layers if is within Viewport, otherwise remove.
     */
    public void redraw() {
        Layers mapOverlays = clusterManager.getMapView().getLayerManager().getLayers();
        if (clusterMarker != null && !clusterManager.getCurBounds().contains(center)
                && mapOverlays.contains(clusterMarker)) {
            mapOverlays.remove(clusterMarker);
            return;
        }
        if (clusterMarker != null
                && mapOverlays.size() > 0
                && !mapOverlays.contains(clusterMarker)
                && !clusterManager.isClustering) {
            mapOverlays.add(1, clusterMarker);
        }
    }
}
