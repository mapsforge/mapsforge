package org.mapsforge.applications.android.samples;

import java.io.File;
import java.util.List;

import org.mapsforge.applications.android.samples.dummy.DummyContent;
import org.mapsforge.map.android.view.MapView;
import org.mapsforge.map.layer.Layer;
import org.mapsforge.map.layer.LayerManager;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.model.MapViewPosition;

import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * A fragment representing a single Item detail screen. This fragment is either contained in a {@link ItemListActivity}
 * in two-pane mode (on tablets) or a {@link ItemDetailActivity} on handsets.
 */
public class ItemDetailFragment extends Fragment {
	/**
	 * The fragment argument representing the item ID that this fragment represents.
	 */
	public static final String ARG_ITEM_ID = "item_id";
	private TileCache tileCache;

	/**
	 * The dummy content this fragment is presenting.
	 */
	private DummyContent.DummyItem mItem;

	private MapView mapView;

	/**
	 * Mandatory empty constructor for the fragment manager to instantiate the fragment (e.g. upon screen orientation
	 * changes).
	 */
	public ItemDetailFragment() {
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (getArguments().containsKey(ARG_ITEM_ID)) {
			// Load the dummy content specified by the fragment
			// arguments. In a real-world scenario, use a Loader
			// to load content from a content provider.
			this.mItem = DummyContent.ITEM_MAP.get(getArguments().getString(ARG_ITEM_ID));
			this.tileCache = Utils.createTileCache(this.getActivity(), "fragments");
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_item_detail, container, false);

		if (this.mItem != null) {

			this.mapView = ((MapView) rootView.findViewById(R.id.mapView));
			this.mapView.setClickable(true);
			this.mapView.getFpsCounter().setVisible(true);
			this.mapView.getMapScaleBar().setVisible(true);

			LayerManager layerManager = this.mapView.getLayerManager();
			List<Layer> layers = layerManager.getLayers();

			MapViewPosition mapViewPosition = this.mapView.getModel().mapViewPosition;
			mapViewPosition.setZoomLevel((byte) 16);
			mapViewPosition.setCenter(this.mItem.location);
			layers.add(Utils.createTileRendererLayer(this.tileCache, mapViewPosition, layerManager, getMapFile()));

		}

		return rootView;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (this.mapView != null) {
			this.mapView.destroy();
		}
	}

	protected File getMapFile() {
		return new File(Environment.getExternalStorageDirectory(), this.getMapFileName());
	}

	protected String getMapFileName() {
		return "germany.map";
	}

}
