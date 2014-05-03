package org.mapsforge.applications.android.samples.test;
import org.mapsforge.applications.android.samples.R;
import org.mapsforge.applications.android.samples.BubbleOverlay;
import org.mapsforge.applications.android.samples.dummy.DummyContent;
import org.mapsforge.map.android.view.MapView;
import org.mapsforge.map.model.MapViewPosition;

import android.test.ActivityInstrumentationTestCase2;

import com.jayway.android.robotium.solo.Solo;

public class BubbleOverlayTest extends ActivityInstrumentationTestCase2<BubbleOverlay> {

	Solo solo;
	final int iterations = TestUtils.iterations;

	public BubbleOverlayTest() {
		super(BubbleOverlay.class);
	}

	public void setUp() throws Exception {
		this.solo = new Solo(getInstrumentation(), getActivity());
	}

	public void testBubbles() throws Exception {
		MapView mapView = (MapView) solo.getView(R.id.mapView);
		MapViewPosition mapViewPosition = mapView.getModel().mapViewPosition;

		for (int i = 0; i < iterations; i++) {
			for (DummyContent.DummyItem item : DummyContent.ITEMS) {
				mapViewPosition.setCenter(item.location);
				solo.sleep(1000);
			}
		}
		solo.assertMemoryNotLow();
	}

	@Override
	public void tearDown() throws Exception {
		this.solo.finishOpenedActivities();
	}
}
