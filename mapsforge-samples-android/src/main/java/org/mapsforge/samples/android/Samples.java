/*
 * Copyright 2013-2015 Ludwig M Brinckmann
 * Copyright 2014-2022 devemux86
 * Copyright 2017 usrusr
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
package org.mapsforge.samples.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;

/**
 * Start screen for the sample activities.
 */
public class Samples extends Activity {
    /**
     * Serves as a substitute for command line arguments for easy launches during development.
     */
    public static Uri launchUrl;

    private Button createButton(Class<?> clazz) {
        return this.createButton(clazz, null, null);
    }

    private Button createButton(final Class<?> clazz, String text, View.OnClickListener customListener) {
        Button button = new Button(this);
        button.setAllCaps(false);
        if (text == null) {
            button.setText(clazz.getSimpleName());
        } else {
            button.setText(text);
        }
        if (customListener == null) {
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    startActivity(new Intent(Samples.this, clazz));
                }
            });
        } else {
            button.setOnClickListener(customListener);
        }
        return button;
    }

    private TextView createLabel(String text) {
        TextView textView = new TextView(this);
        textView.setGravity(Gravity.CENTER);
        if (text == null) {
            textView.setText("----------");
        } else {
            textView.setText(text);
        }
        return textView;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_samples);
        LinearLayout linearLayout = findViewById(R.id.samples);
        linearLayout.addView(createButton(MapsforgeMapViewer.class));

        linearLayout.addView(createLabel(null));
        linearLayout.addView(createButton(LocationOverlayMapViewer.class));
        linearLayout.addView(createButton(HillshadingMapViewer.class));
        linearLayout.addView(createButton(PoiSearchViewer.class, null, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startupDialog("poi", R.string.startup_message_poi, PoiSearchViewer.class);
            }
        }));
        linearLayout.addView(createButton(StyleMenuMapViewer.class));
        linearLayout.addView(createButton(LabelLayerUsingLabelCacheMapViewer.class));
        linearLayout.addView(createButton(LabelLayerUsingMapDataStoreMapViewer.class));
        linearLayout.addView(createButton(RotationMapViewer.class));
        linearLayout.addView(createButton(MBTilesBitmapActivity.class));

        linearLayout.addView(createLabel("Overlays"));
        linearLayout.addView(createButton(OverlayMapViewer.class));
        linearLayout.addView(createButton(BubbleOverlay.class));
        linearLayout.addView(createButton(ViewOverlayViewer.class));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.options_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_preferences) {
            Intent intent = new Intent(this, Settings.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
            startActivity(intent);
            return true;
        } else if (itemId == R.id.menu_svgclear) {
            AndroidGraphicFactory.clearResourceFileCache();
        }
        return false;
    }

    @Override
    public void onResume() {
        super.onResume();
        final SharedPreferences preferences = getSharedPreferences("installation", Activity.MODE_PRIVATE);
        final String accepted = "accepted";
        if (!preferences.getBoolean(accepted, false)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Warning");
            builder.setCancelable(true);
            builder.setPositiveButton(R.string.startup_dontshowagain,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            preferences.edit().putBoolean(accepted, true).apply();
                        }
                    });
            builder.setMessage(R.string.startup_message);
            builder.show();
        }

        launchUrl = getIntent().getData();
        String activity = (launchUrl == null) ? null : launchUrl.getQueryParameter("activity");
        if (activity != null) {
            String fqn = Samples.class.getPackage().getName() + "." + activity;
            try {
                Class<?> clazz = getClass().getClassLoader().loadClass(fqn);
                if (Activity.class.isAssignableFrom(clazz)) {
                    startActivity(new Intent(Samples.this, clazz));
                }
            } catch (ClassNotFoundException e) {
                Log.e(SamplesApplication.TAG, e.toString(), e);
            }
        }
    }

    /**
     * Warning startup dialog.
     */
    private void startupDialog(String prefs, int message, final Class clazz) {
        final SharedPreferences preferences = getSharedPreferences(prefs, Activity.MODE_PRIVATE);
        final String accepted = "accepted";
        if (!preferences.getBoolean(accepted, false)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(Samples.this);
            builder.setTitle("Warning");
            builder.setMessage(message);
            builder.setPositiveButton(R.string.startup_dontshowagain, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    preferences.edit().putBoolean(accepted, true).apply();
                    startActivity(new Intent(Samples.this, clazz));
                }
            });
            builder.show();
        } else {
            startActivity(new Intent(Samples.this, clazz));
        }
    }
}
