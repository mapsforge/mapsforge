/*
 * Copyright (C) 2009 Huan Erdao
 * Copyright (C) 2014 Martin Vennekamp
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

import android.util.Log;

import org.mapsforge.core.graphics.Bitmap;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Point;
import org.mapsforge.core.model.Rectangle;
import org.mapsforge.core.util.MercatorProjection;
import org.mapsforge.map.layer.Layer;

/**
 * Layer extended class to display Clustered Marker.
 * @param <T>
 */
public class ClusterMarker<T extends GeoItem> extends Layer {
	public final static String TAG = ClusterMarker.class.getSimpleName();
    /**
     * cluster object
     */
    protected final Cluster<T> mCluster;
    /**
     * icon marker type
     */
    protected int mMarkerType = 0;
//    /**
//     * the rectangle spanning around the bitmap of this ClusterMarer on the screen
//     */
//    protected Rectangle mBitmapRectangle; 

    /**
     * Whether this marker should react on Tap (implement a working onTap 
     * Listener)
     */
    protected boolean ignoreOnTap;
    
    private Bitmap bubble;
    /**
     * @param cluster a cluster to be rendered for this marker
     */
    public ClusterMarker(Cluster<T> cluster, boolean ignoreOnTap ) {
        this.mCluster = cluster;
        this.ignoreOnTap = ignoreOnTap;
    }

    /**
     * change icon bitmaps according to the state and content size.
     */
    private void setMarkerBitmap() {
        for (mMarkerType = 0; mMarkerType < mCluster.getClusterManager().mMarkerIconBmps.size(); mMarkerType++) {
            // Check if the number of items in this cluster is below or equal the limit of the MarkerBitMap
            if (mCluster.getItems().size() <= mCluster.getClusterManager()
            		.mMarkerIconBmps.get(mMarkerType).getItemMax()) {
               return;
            }
        }
        // set the mMarkerType to maximum value ==> reduce mMarkerType by one.
        mMarkerType--;
    }

    @Override
    public synchronized void draw(BoundingBox boundingBox, byte zoomLevel
            , org.mapsforge.core.graphics.Canvas canvas, Point topLeftPoint) {
    	Boolean isSelected = isSelected();
        if (mCluster.getClusterManager() == null ||
        		mCluster.getClusterManager().isClustering ||
                this.getLatLong() == null) {
        	return;
        }
        setMarkerBitmap();
        long mapSize = MercatorProjection.getMapSize(zoomLevel, this.displayModel.getTileSize());
        double pixelX = MercatorProjection.longitudeToPixelX(this.getLatLong().longitude, mapSize);
        double pixelY = MercatorProjection.latitudeToPixelY(this.getLatLong().latitude, mapSize);
//        if (mCluster.getClusterManager().mMarkerIconBmps.get(mMarkerType).getBitmap(isSelected) == null) {
//        	return;
//        }
        double halfBitmapWidth;
        double halfBitmapHeight;
        try{
	        halfBitmapWidth = mCluster.getClusterManager().mMarkerIconBmps
	        		.get(mMarkerType).getBitmap(isSelected).getWidth() / 2f;
	        halfBitmapHeight = mCluster.getClusterManager().mMarkerIconBmps.get(mMarkerType).getBitmap(isSelected).getHeight() / 2f;
        }
        catch (NullPointerException e) {
        	Log.e(TAG,e.getMessage() + e.getStackTrace());
        	return;
        }
        int left = (int)(pixelX - topLeftPoint.x - halfBitmapWidth 
        		+ mCluster.getClusterManager().mMarkerIconBmps.get(mMarkerType).getIconOffset().x);
        int top = (int)(pixelY - topLeftPoint.y - halfBitmapHeight 
        		+ mCluster.getClusterManager().mMarkerIconBmps.get(mMarkerType).getIconOffset().y);
        int right = (left
        		+ mCluster.getClusterManager().mMarkerIconBmps.get(mMarkerType).getBitmap(isSelected)
        		.getWidth());
        int bottom = (top 
        		+ mCluster.getClusterManager().mMarkerIconBmps.get(mMarkerType).getBitmap(isSelected)
        		.getHeight());
        Rectangle mBitmapRectangle = new Rectangle(left, top, right, bottom);
        Rectangle canvasRectangle = new Rectangle(0, 0, canvas.getWidth(), canvas.getHeight());
        if (!canvasRectangle.intersects(mBitmapRectangle)) {
            return;
        }
        // Draw bitmap
        canvas.drawBitmap(mCluster.getClusterManager().mMarkerIconBmps
        		.get(mMarkerType).getBitmap(isSelected), (int)left, (int)top);
        
        // Draw Text
        if (mMarkerType == 0 ) {
        	// Draw bitmap
        	bubble = MarkerBitmap.getBitmapFromTitle(mCluster.getTitle(),
                    mCluster.getClusterManager().mMarkerIconBmps
                            .get(mMarkerType).getPaint());
            canvas.drawBitmap(bubble, 
            		(int)(left + halfBitmapWidth - bubble.getWidth() / 2),
            		(int)(top - bubble.getHeight() ));
        }
        else {
            int x = (int)(left + halfBitmapWidth);
            int y = (int)(top + halfBitmapHeight 
            		+ mCluster.getClusterManager().mMarkerIconBmps
            			.get(mMarkerType).getPaint().getTextHeight(mCluster.getTitle()) / 2);
            canvas.drawText(mCluster.getTitle(), x, y, 
            		mCluster.getClusterManager().mMarkerIconBmps
            			.get(mMarkerType).getPaint());
        }
        
    }

    /**
     * get center location of the marker.
     *
     * @return GeoPoint object of current marker center.
     */
    public LatLong getLatLong() {
        return mCluster.getLocation();
    }

    /**
     * @return Gets the LatLong Position of the Layer Object
     */
    @Override
    public LatLong getPosition() {
        return  getLatLong();
    }
    @Override
    public synchronized boolean onTap(LatLong geoPoint, Point viewPosition,
                         Point tapPoint) {
    	if ( ignoreOnTap ) return false;
    	// Log.w(TAG, "onTap is called...");
        if (mCluster.getItems().size() == 1 && contains(viewPosition, tapPoint)) {
            Log.w(TAG, "The Marker was touched with onTap: "
                    + this.getPosition().toString());
            mCluster.getClusterManager().setSelectedItem(null, mCluster.getItems().get(0));
            requestRedraw();
            return true;
        }
        else if ( contains(viewPosition, tapPoint) ) {
        	StringBuilder mText = new StringBuilder(mCluster.getItems().size() + " items:");
        	for ( int i = 0; i < mCluster.getItems().size(); i++ ) {
        		mText.append("\n- ");
        		mText.append( mCluster.getItems().get(i).getTitle());
        		if ( i == 7 ) {
        			mText.append("\n...");
        			break;
        		}
        	}
        	ClusterManager.toast.setText(mText);
        	ClusterManager.toast.show();        	
        }
        return false;
    }

    public synchronized boolean contains(Point viewPosition, Point tapPoint) {
        return getBitmapRectangle(viewPosition).contains(tapPoint);
    }
    
    private Rectangle getBitmapRectangle(Point center){
    	Boolean isSelected = isSelected();
    	return new Rectangle(
            center.x
                    - (float) mCluster.getClusterManager().mMarkerIconBmps.get(mMarkerType)
                    	.getBitmap(isSelected).getWidth()
                    + mCluster.getClusterManager().mMarkerIconBmps.get(mMarkerType).getIconOffset().x,
            center.y
                    - (float) mCluster.getClusterManager().mMarkerIconBmps.get(mMarkerType)
                    	.getBitmap(isSelected).getHeight()
                    + mCluster.getClusterManager().mMarkerIconBmps.get(mMarkerType).getIconOffset().y,
            center.x
                    + (float) mCluster.getClusterManager().mMarkerIconBmps.get(mMarkerType)
                    	.getBitmap(isSelected).getWidth()
                    + mCluster.getClusterManager().mMarkerIconBmps.get(mMarkerType).getIconOffset().x,
            center.y
                    + (float) mCluster.getClusterManager().mMarkerIconBmps.get(mMarkerType)
                    .getBitmap(isSelected).getHeight()
                    + mCluster.getClusterManager().mMarkerIconBmps.get(mMarkerType).getIconOffset().y);
}
    public Boolean isSelected(){
    	return (mCluster.getItems().size() == 1 && 
    			mCluster.getItems().get(0) == mCluster.getClusterManager().getSelectedItem());
    }
}














