/*
 * Copyright 2013-2014 Ludwig M Brinckmann
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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;

/**
 * A simple start screen for the sample activities.
 */
public class Samples extends Activity {
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.options_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Intent intent;
		switch (item.getItemId()) {
		case R.id.menu_preferences:
			intent = new Intent(this, Settings.class);
			intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
			startActivity(intent);
			return true;
		}
		return false;
	}

	@Override
	public void onResume() {
		super.onResume();
		final SharedPreferences preferences = getSharedPreferences(
				"installation", Activity.MODE_PRIVATE);
		final String accepted = "accepted";
		if (!preferences.getBoolean(accepted, false)) {
			final AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle("Warning");
			builder.setCancelable(true);
			builder.setPositiveButton(R.string.startup_dontshowagain,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							preferences.edit().putBoolean(accepted, true)
									.commit();
						}
					});
			builder.setMessage(R.string.startup_message);
			builder.create().show();
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_samples);
		LinearLayout linearLayout = (LinearLayout) findViewById(R.id.samples);
		linearLayout.addView(createButton(BasicMapViewer.class));
		linearLayout.addView(createButton(BasicMapViewerXml.class));
		linearLayout.addView(createButton(DiagnosticsMapViewer.class));
		linearLayout.addView(createButton(RenderThemeMapViewer.class));

		linearLayout.addView(createButton(AssetsRenderThemeMapViewer.class,
				"Rendertheme using Android Assets"));
		linearLayout.addView(createButton(SVGAssetsRenderThemeMapViewer.class,
				"Rendertheme using SVG files"));
		linearLayout.addView(createButton(RenderThemeChanger.class,
				"Automatically changing render themes"));
		linearLayout.addView(createButton(ChangingBitmaps.class,
				"Automatically changing bitmaps"));
		linearLayout.addView(createButton(DownloadLayerViewer.class,
				"Downloading Mapnik"));
		linearLayout.addView(createButton(OverlayMapViewer.class));
		linearLayout.addView(createButton(LongPressAction.class,
				"Long Press Action"));
		linearLayout
				.addView(createButton(MoveAnimation.class, "Move Animation"));
		linearLayout
				.addView(createButton(ZoomToBounds.class, "Zoom to Bounds"));
		linearLayout.addView(createButton(OverlayWithoutBaseMapViewer.class,
				"Just Overlays, No Map"));
		linearLayout.addView(createButton(LocationOverlayMapViewer.class));
		linearLayout.addView(createButton(DualMapViewer.class, "Dual MapDB"));
		linearLayout.addView(createButton(
				DualMapViewerWithDifferentDisplayModels.class,
				"Dual Viewer with different DisplayModels"));
		linearLayout.addView(createButton(DualMapnikMapViewer.class,
				"Tied MapViews MapDB/Mapnik"));
		linearLayout.addView(createButton(DualOverviewMapViewer.class,
				"Overview Mapview"));
		linearLayout
				.addView(createButton(BubbleOverlay.class, "Bubble Overlay"));
		linearLayout.addView(createButton(ItemListActivity.class,
				"Fragment List/View"));
		linearLayout.addView(createButton(StackedLayersMapViewer.class,
				"Stacked rendered tiles"));
	}

	private Button createButton(final Class<?> clazz) {
		return this.createButton(clazz, null);
	}

	private Button createButton(final Class<?> clazz, String text) {
		Button button = new Button(this);
		if (text == null) {
			button.setText(clazz.getSimpleName());
		} else {
			button.setText(text);
		}
		button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				startActivity(new Intent(Samples.this, clazz));
			}
		});
		return button;
	}
}
