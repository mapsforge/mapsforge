/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
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

import java.io.File;

import org.mapsforge.applications.android.samples.dummy.DummyContent;
import org.mapsforge.map.android.util.AndroidUtil;
import org.mapsforge.map.android.view.MapView;
import org.mapsforge.map.layer.LayerManager;
import org.mapsforge.map.layer.Layers;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.model.MapViewPosition;
import org.mapsforge.map.rendertheme.InternalRenderTheme;

import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * A fragment representing a single Item detail screen. This fragment is either
 * contained in a {@link ItemListActivity} in two-pane mode (on tablets) or a
 * {@link ItemDetailActivity} on handsets.
 */
public class ItemDetailFragment extends Fragment {
	/**
	 * The fragment argument representing the item ID that this fragment
	 * represents.
	 */
	public static final String ARG_ITEM_ID = "item_id";

	/**
	 * The dummy content this fragment is presenting.
	 */
	private DummyContent.DummyItem dummyItem;
	private MapView mapView;
	private TileCache tileCache;

	/**
	 * Mandatory empty constructor for the fragment manager to instantiate the
	 * fragment (e.g. upon screen orientation changes).
	 */
	public ItemDetailFragment() {
		super();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (getArguments().containsKey(ARG_ITEM_ID)) {
			// Load the dummy content specified by the fragment
			// arguments. In a real-world scenario, use a Loader
			// to load content from a content provider.
			this.dummyItem = DummyContent.ITEM_MAP.get(getArguments()
					.getString(ARG_ITEM_ID));
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_item_detail,
				container, false);

		if (this.dummyItem != null) {
			this.mapView = (MapView) rootView.findViewById(R.id.mapView);
			this.mapView.setClickable(true);
			this.mapView.getFpsCounter().setVisible(true);
			this.mapView.getMapScaleBar().setVisible(true);

			LayerManager layerManager = this.mapView.getLayerManager();
			Layers layers = layerManager.getLayers();

			MapViewPosition mapViewPosition = this.mapView.getModel().mapViewPosition;
			mapViewPosition.setZoomLevel((byte) 16);
			this.tileCache = AndroidUtil.createTileCache(this.getActivity(),
					"fragments",
					this.mapView.getModel().displayModel.getTileSize(), 1.0f,
					1.5);

			mapViewPosition.setCenter(this.dummyItem.location);
			layers.add(Utils.createTileRendererLayer(this.tileCache,
					mapViewPosition, getMapFile(),
					InternalRenderTheme.OSMARENDER, false));
		}

		return rootView;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (this.mapView != null) {
			this.mapView.destroy();
		}
		if (this.tileCache != null) {
			this.tileCache.destroy();
		}
		org.mapsforge.map.android.graphics.AndroidResourceBitmap
				.clearResourceBitmaps();

	}

	protected File getMapFile() {
		return new File(Environment.getExternalStorageDirectory(),
				this.getMapFileName());
	}

	protected String getMapFileName() {
		return "germany.map";
	}
}
