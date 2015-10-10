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
package org.mapsforge.applications.android.samples.dummy;

import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mapsforge.core.model.LatLong;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ManyDummyContent extends DummyContent {

    private static final String TAG = ManyDummyContent.class.getSimpleName();
    private final static String searchPart1 = "http://nominatim.openstreetmap.org/search.php?q=Hotel&addressdetails=1&limit=99&viewbox=";
    private final static String searchPart2 = "&bounded=1&format=json";
    // A handler to broadcast the data change from the AsyncTask to the activity.
    private Handler mHandler;
    public ManyDummyContent(Handler handler) {
        super();
        mHandler = handler;
        createItems();
    }

    /**
     * A map of sample (dummy) items, by ID. Retrieved from http://nominatim.openstreetmap.org
     * inspired by e.g. https://code.google.com/p/osmbonuspack/wiki/Tutorial_2
     * --> https://code.google.com/p/osmbonuspack/source/browse/trunk/OSMBonusPack/src/org/osmdroid/bonuspack/location/NominatimPOIProvider.java
     */
    public static final Map<String, DummyItem> MANYITEM_MAP = new HashMap<String, DummyItem>();

    /**
     * An array of many sample (dummy) items.
     */
    public static final List<DummyItem> MANYITEMS = new ArrayList<DummyItem>();

    private static void addItem(DummyItem item) {
        if ( !MANYITEM_MAP.containsKey(item.id) ) {
            MANYITEMS.add(item);
            MANYITEM_MAP.put(item.id, item);
        }
    }
    public void createItems() {
        String[] boundingBoxes = {"13.2,52.6,13.4,52.5",
                "13.4,52.6,13.6,52.5",
                "13.2,52.5,13.4,52.4",
                "13.4,52.5,13.6,52.4"};
            // download the places in the view boxes (4 pieces of the city of berlin)
            // reason: the number of results is limited to 50.
            new DownloadXmlTask().execute(searchPart1 + boundingBoxes[0] + searchPart2,
                    searchPart1 + boundingBoxes[1] + searchPart2,
                    searchPart1 + boundingBoxes[2] + searchPart2,
                    searchPart1 + boundingBoxes[3] + searchPart2);
    }
    // Taken from standard example in ANDROID-DOC:
    // http://developer.android.com/training/basics/network-ops/xml.html
    // Implementation of AsyncTask used to download XML feed from nominatim.openstreetmap.org.
    private class DownloadXmlTask extends AsyncTask<String, Void, Void> {


        @Override
        protected Void doInBackground(String... urls) {
            /**
             * An array of sample (dummy) items.
             */
            List<DummyItem> someItems = new ArrayList<DummyItem>();
            try {
                for (String url : urls) {
                    someItems = loadXmlFromNetwork(url);
                    for (DummyItem content : someItems) {
                        addItem(content);
                    }
                }
            } catch (IOException e) {
                Log.e(getClass().getSimpleName(), e.toString());
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            mHandler.sendEmptyMessage(0);
        }
    }

    // This class represents a single entry (post) in the JSON feed.
    public static class Entry {
        public final Long mPlace_id;
        public final String mOsm_type;
        public final String mOsm_id;
        // public final String mPlace_rank;
        public final String mBoundingbox;
        public final String mLat;
        public final String mLon;
        public final String mDisplay_name;
        public final String mClass;
        public final String mType;
        public final String mImportance;
        private Entry(Long mPlace_id, String mOsm_type, String mOsm_id, /* String mPlace_rank, */
                      String mBoundingbox, String mLat, String mLon, String mDisplay_name,
                      String mClass, String mType, String mImportance) {
            this.mPlace_id = mPlace_id;
            this.mOsm_type = mOsm_type;
            this.mOsm_id = mOsm_id;
            // this.mPlace_rank = mPlace_rank;
            this.mBoundingbox = mBoundingbox;
            this.mLat = mLat;
            this.mLon = mLon;
            this.mDisplay_name = mDisplay_name;
            this.mClass = mClass;
            this.mType = mType;
            this.mImportance = mImportance;

        }
    }

    // Uploads XML from nominatim.openstreetmap.org, parses it, and  create DummyItems from it
    private ArrayList<DummyItem> loadXmlFromNetwork(String urlString) throws IOException {
        String jString  = null;
        // Instantiate the parser
        List<Entry> entries = null;
        ArrayList<DummyItem> rtnArray = new ArrayList<DummyItem>();
        BufferedReader streamReader = null;
        try {
            streamReader = new BufferedReader(downloadUrl(urlString));
            StringBuilder responseStrBuilder = new StringBuilder();

            String inputStr;
            while ((inputStr = streamReader.readLine()) != null)
                responseStrBuilder.append(inputStr);
            jString = responseStrBuilder.toString();
            if (jString == null) {
                Log.e(getClass().getSimpleName(), "Nominatim Webpage: request failed for "
                    + urlString);
                return new ArrayList<DummyItem>(0);
            }
                JSONArray jPlaceIds = new JSONArray(jString);
                int n = jPlaceIds.length();
                entries = new ArrayList<Entry>(n);
                for (int i=0; i<n; i++){
                    JSONObject jPlace = jPlaceIds.getJSONObject(i);
                    Entry poi = new  Entry(
                            jPlace.optLong("place_id"),
                            jPlace.getString("osm_type"),
                            jPlace.getString("osm_id"),
                            // jPlace.getString("place_rank"),
                            jPlace.getString("boundingbox"),
                            jPlace.getString("lat"),
                            jPlace.getString("lon"),
                            jPlace.getString("display_name"),
                            jPlace.getString("class"),
                            jPlace.getString("type"),
                            jPlace.getString("importance"));
                    entries.add(poi);
                }
            } catch (JSONException e) {
                e.printStackTrace();
                return null;
            }
            finally {
            if ( streamReader != null ) {
                streamReader.close();
            }
        }

        // StackOverflowXmlParser returns a List (called "entries") of Entry objects.
        // Each Entry object represents a single place in the XML searchresult.
        // This section processes the entries list to create a 'DummyItem' from each entry.
        for (Entry entry : entries) {
            rtnArray.add(
                    new DummyItem(entry.mOsm_id,
                            entry.mDisplay_name.split(",")[0],
                            new LatLong(Double.parseDouble(entry.mLat), Double.parseDouble(entry.mLon)),
                            entry.mDisplay_name ));
        }
        return rtnArray;
    }

    // Given a string representation of a URL, sets up a connection and gets
    // an input stream.
    private InputStreamReader downloadUrl(String urlString) throws IOException {
        URL url = null;
        Reader stream = null;
        BufferedReader streamReader = null;
        url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setReadTimeout(10000 /* milliseconds */);
        conn.setConnectTimeout(15000 /* milliseconds */);
        conn.setRequestMethod("GET");
        conn.setDoInput(true);
        // Starts the query
        conn.connect();

        return new InputStreamReader(conn.getInputStream(), "UTF-8");

    }
}

