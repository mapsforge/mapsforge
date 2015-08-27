// MapActivity
// NW new class to encapsulate some of the common functionality such as
// creating a TileCache

package org.mapsforge.map.android;

import android.app.Activity;
import android.os.Bundle;

import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.android.view.MapView;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.android.util.AndroidUtil;

public class MapActivity extends Activity {

	private TileCache tileCache;

	public void onCreate (Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		AndroidGraphicFactory.createInstance(this.getApplication());
	}

	public void createDefaultTileCache(MapView mv, String name) {
		tileCache = AndroidUtil.createTileCache(this, name,
			mv.getModel().displayModel.getTileSize(), 1f,
			mv.getModel().frameBufferModel.getOverdrawFactor());
	}

	public TileCache getTileCache() {
		return tileCache; 
	}
}
