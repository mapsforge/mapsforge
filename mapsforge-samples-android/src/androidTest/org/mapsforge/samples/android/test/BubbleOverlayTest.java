package org.mapsforge.samples.android.test;

import android.test.ActivityInstrumentationTestCase2;

import com.robotium.solo.Solo;

import org.mapsforge.map.android.view.MapView;
import org.mapsforge.map.model.MapViewPosition;
import org.mapsforge.samples.android.BubbleOverlay;
import org.mapsforge.samples.android.R;
import org.mapsforge.samples.android.dummy.DummyContent;

public class BubbleOverlayTest extends ActivityInstrumentationTestCase2<BubbleOverlay> {

    Solo solo;
    final static int iterations = TestUtils.iterations;

    public BubbleOverlayTest() {
        super(BubbleOverlay.class);
    }

    @Override
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
