/*
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

package org.mapsforge.applications.android.samples;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.widget.Toast;

import org.mapsforge.applications.android.samples.dummy.ManyDummyContent;
import org.mapsforge.applications.android.samples.markerclusterer.ClusterManager;
import org.mapsforge.applications.android.samples.markerclusterer.GeoItem;
import org.mapsforge.applications.android.samples.markerclusterer.MarkerBitmap;
import org.mapsforge.core.graphics.Align;
import org.mapsforge.core.graphics.Bitmap;
import org.mapsforge.core.graphics.Color;
import org.mapsforge.core.graphics.FontFamily;
import org.mapsforge.core.graphics.FontStyle;
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.graphics.Style;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Point;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;

import java.util.ArrayList;

public class ClusterMapActivity extends RenderTheme4 {
    private static final String TAG = ClusterMapActivity.class.getSimpleName();
    private MenuItem displayItems;
    private MenuItem displayMoreItems;
    private MenuItem hideItems;
    protected ClusterManager clusterer = null;

    // sample geo items
    //############################# internal class ####################################
    protected class MyGeoItem implements GeoItem {
        public String title;
        public LatLong latLong;
        public MyGeoItem(int _id, double lat, double lng) {
            this.title = String.valueOf(_id);
            this.latLong = new LatLong(lat,lng);
        }
        public MyGeoItem(String title, LatLong latLong) {
            this.title = title;
            this.latLong = latLong;
        }
        public LatLong getLatLong(){
            return latLong;
        }
        public String getTitle(){
            return String.valueOf(this.title);
        }
    }
    private MyGeoItem[] geoItems_ = {
            new MyGeoItem("1st Item", new LatLong(52.504266, 13.392996)),
            new MyGeoItem("2nd Item", new LatLong(52.514266, 13.392996)),
            new MyGeoItem("3rd Item", new LatLong(52.524266, 13.392996)),
            new MyGeoItem("4th Item", new LatLong(52.534266, 13.392996)),
            new MyGeoItem("5th Item", new LatLong(52.514266, 13.352996)),
            new MyGeoItem("6th Item", new LatLong(52.514266, 13.362996)),
            new MyGeoItem("7th Item", new LatLong(52.514266, 13.372996)),
            new MyGeoItem("8th Item", new LatLong(52.514266, 13.382996)),
            new MyGeoItem("9th Item", new LatLong(52.514266, 13.383796)),
            new MyGeoItem("10th Item", new LatLong(52.514266, 13.383700))
    };
    //##################sample geo items class ####################################
    @Override
    public void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // create clusterer instance
        clusterer = new ClusterManager(
                mapView,
                getMarkerBitmap(this),
                getZoomLevelMax(),
                false);

        // Create a Toast, see e.g. http://www.mkyong.com/android/android-toast-example/
        Toast toast = Toast.makeText(this, "", Toast.LENGTH_LONG);
        ClusterManager.setToast(toast);
        // this uses the framebuffer position, the mapview position can be out of sync with
        // what the user sees on the screen if an animation is in progress
        this.mapView.getModel().frameBufferModel.addObserver(clusterer);
        // add geoitems for clustering
        for (int i = 0; i < geoItems_.length; i++) {
            clusterer.addItem(geoItems_[i]);
        }
    }


    @Override
    protected void onPause() {
        super.onPause();
        if (clusterer != null) {
            clusterer.destroyGeoClusterer();
            this.mapView.getModel().frameBufferModel.removeObserver(clusterer);
            clusterer = null;
        }
        displayItems.setEnabled(false);
        displayMoreItems.setEnabled(true);
        hideItems.setEnabled(true);
    }


    private void addMarker() {

        if ( ManyDummyContent.MANYITEMS.size() == 0 ) {
            Toast.makeText(getApplication(),"No items received...",Toast.LENGTH_LONG).show();
        }
        for (int i = 0; i < ManyDummyContent.MANYITEMS.size(); i++) {
            LatLong latLong = ManyDummyContent.MANYITEMS.get(i).location;
            String title = ManyDummyContent.MANYITEMS.get(i).content;
            clusterer.addItem(new MyGeoItem(title, latLong));
        }
        clusterer.redraw();
        setProgressBarIndeterminateVisibility(false);
    }

    /**
     * onCreateOptionsMenu handler
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        displayItems = menu.add(0, 1234, 0, "Display GeoItems");
        displayMoreItems = menu.add(0, 5678, 0, "Add more GeoItems");
        displayMoreItems.setEnabled(false);
        hideItems = menu.add(0, 9012, 0, "Remove GeoItems");
        hideItems.setEnabled(false);

        displayItems.setEnabled(false);
        displayMoreItems.setEnabled(true);
        hideItems.setEnabled(true);

        return true;
    }

    /**
     * onOptionsItemSelected handler
     * since clustering need MapView to be created and visible,
     * this sample do clustering here.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        switch (item.getItemId()) {
            case 1234:
                if (clusterer != null) {
                    break;
                }
                // create clusterer instance
                clusterer = new ClusterManager(
                        mapView,
                        getMarkerBitmap(this),
                        getZoomLevelMax(),
                        false);

                // Create a Toast, see e.g. http://www.mkyong.com/android/android-toast-example/
                Toast toast = Toast.makeText(this, "", Toast.LENGTH_LONG);
                ClusterManager.setToast(toast);
                // this uses the framebuffer position, the mapview position can be out of sync with
                // what the user sees on the screen if an animation is in progress
                this.mapView.getModel().frameBufferModel.addObserver(clusterer);
                // add geoitems for clustering
                for (int i = 0; i < geoItems_.length; i++) {
                    clusterer.addItem(geoItems_[i]);
                }
                // now redraw the cluster. it will create markers.
                clusterer.redraw();
                displayItems.setEnabled(false);
                displayMoreItems.setEnabled(true);
                hideItems.setEnabled(true);
                // now you can see items clustered on the map.
                // zoom in/out to see how icons change.
                break;
            case 5678:
                setProgressBarIndeterminateVisibility(true);
                Handler myHandler = new Handler() {
                    @Override
                    public void handleMessage(Message msg) {
                        switch (msg.what) {
                            case 0:
                                // calling to this function from other places
                                // The notice call method of doing things
                                addMarker();
                                break;
                            default:
                                break;
                        }
                    }
                };
                new ManyDummyContent(myHandler);
                item.setEnabled(false);
                break;
            case 9012:
                if (clusterer != null) {
                    clusterer.destroyGeoClusterer();
                    this.mapView.getModel().frameBufferModel.removeObserver(clusterer);
                    clusterer = null;
                }
                displayItems.setEnabled(true);
                displayMoreItems.setEnabled(false);
                hideItems.setEnabled(false);
                break;
        }
        return true;
    }

    private static ArrayList<MarkerBitmap> getMarkerBitmap(Context context) {
        ArrayList<MarkerBitmap> markerBitmaps = new ArrayList<MarkerBitmap>();
        // prepare for marker icons.
        Drawable balloon;
        // small icon for maximum single item
        balloon = context.getResources().getDrawable(R.drawable.marker_green);
        Bitmap bitmap_climbing_peak = AndroidGraphicFactory.convertToBitmap(balloon);
        bitmap_climbing_peak.incrementRefCount();
        balloon = context.getResources().getDrawable(R.drawable.marker_red);
        Bitmap marker_red_s = AndroidGraphicFactory.convertToBitmap(balloon);
        marker_red_s.incrementRefCount();
        Paint paint_1;
        paint_1 = AndroidGraphicFactory.INSTANCE.createPaint();
        paint_1.setStyle(Style.STROKE);
        paint_1.setTextAlign(Align.CENTER);
        FontFamily fontFamily = FontFamily.DEFAULT;
        FontStyle fontStyle = FontStyle.BOLD;
        paint_1.setTypeface(fontFamily, fontStyle);
        paint_1.setColor(Color.RED);
        markerBitmaps.add(new MarkerBitmap(context, bitmap_climbing_peak, marker_red_s,
                new Point(0, 0), 10f, 1, paint_1));
        // small icon. for 10 or less items.
        balloon = context.getResources().getDrawable(R.drawable.balloon_s_n);
        Bitmap bitmap_balloon_s_n = AndroidGraphicFactory
                .convertToBitmap(balloon);
        bitmap_balloon_s_n.incrementRefCount();
        balloon = context.getResources().getDrawable(R.drawable.balloon_s_s);
        Bitmap bitmap_balloon_s_s = AndroidGraphicFactory
                .convertToBitmap(balloon);
        bitmap_balloon_s_s.incrementRefCount();
        Paint paint_2;
        paint_2 = AndroidGraphicFactory.INSTANCE.createPaint();
        paint_2.setStyle(Style.STROKE);
        paint_2.setTextAlign(Align.CENTER);
        fontFamily = FontFamily.DEFAULT;
        fontStyle = FontStyle.BOLD;
        paint_2.setTypeface(fontFamily, fontStyle);
        paint_2.setColor(Color.WHITE);
        markerBitmaps.add(new MarkerBitmap(context, bitmap_balloon_s_n,
                bitmap_balloon_s_s, new Point(0, 0), 9f, 10,paint_2));
        // large icon. 100 will be ignored.
        balloon = context.getResources().getDrawable(R.drawable.balloon_m_n);
        Bitmap bitmap_balloon_m_n = AndroidGraphicFactory
                .convertToBitmap(balloon);
        bitmap_balloon_m_n.incrementRefCount();
        balloon = context.getResources().getDrawable(R.drawable.balloon_m_s);
        Bitmap bitmap_balloon_m_s = AndroidGraphicFactory
                .convertToBitmap(balloon);
        bitmap_balloon_m_s.incrementRefCount();
        Paint paint_3;
        paint_3 = AndroidGraphicFactory.INSTANCE.createPaint();
        paint_3.setStyle(Style.STROKE);
        paint_3.setTextAlign(Align.CENTER);
        fontFamily = FontFamily.DEFAULT;
        fontStyle = FontStyle.BOLD;
        paint_3.setTypeface(fontFamily, fontStyle);
        paint_3.setColor(Color.WHITE);
        markerBitmaps.add(new MarkerBitmap(context, bitmap_balloon_m_n,
                bitmap_balloon_m_s, new Point(0, 0), 11f, 100,paint_3));
        return markerBitmaps;
    }

}