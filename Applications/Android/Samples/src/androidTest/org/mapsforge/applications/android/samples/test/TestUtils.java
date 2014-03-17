package org.mapsforge.applications.android.samples.test;

import org.mapsforge.applications.android.samples.R;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.map.android.view.MapView;
import org.mapsforge.map.model.MapViewPosition;

import com.jayway.android.robotium.solo.Solo;

public class TestUtils {

	public static final int delay = 400;
    public static final int iterations = 1;

	public static void testClickWithRotation(Solo solo, int iterations) throws Exception {
		for (int i = 0; i < iterations; i++) {
			solo.setActivityOrientation(Solo.LANDSCAPE);
			solo.clickOnScreen(22, 90);
			solo.clickOnScreen(22, 190);
			solo.clickOnScreen(232, 90);
			solo.clickOnScreen(232, 90);
			solo.setActivityOrientation(Solo.PORTRAIT);
			solo.clickOnScreen(22, 90);
			solo.clickOnScreen(22, 190);
			solo.clickOnScreen(232, 90);
			solo.clickOnScreen(232, 90);
			solo.setActivityOrientation(Solo.LANDSCAPE);
			solo.clickOnScreen(22, 90);
			solo.clickOnScreen(22, 190);
			solo.clickOnScreen(232, 90);
			solo.clickOnScreen(232, 90);
			solo.setActivityOrientation(Solo.PORTRAIT);
			solo.assertMemoryNotLow();
		}
	}

	public static void testClickWithoutRotation(Solo solo, int iterations) throws Exception {
		for (int i = 0; i < iterations; i++) {
			solo.clickOnScreen(0, 90);
			solo.clickOnScreen(0, 90);
			solo.clickOnScreen(232, 90);
			solo.clickOnScreen(232, 90);
			solo.clickOnScreen(22, 90);
			solo.clickOnScreen(22, 190);
			solo.clickOnScreen(232, 90);
			solo.clickOnScreen(232, 90);
			solo.clickOnScreen(22, 90);
			solo.clickOnScreen(22, 190);
			solo.clickOnScreen(232, 90);
			solo.clickOnScreen(232, 90);
			solo.assertMemoryNotLow();
		}
	}

	public static void testScrollWithRotation(Solo solo, int iterations) throws Exception {
		for (int i = 0; i < iterations; i++) {
			solo.setActivityOrientation(Solo.LANDSCAPE);
			solo.drag(22, 44, 170, 220, 12);
			solo.sleep(delay);
			solo.drag(27, 49, 170, 220, 12);
			solo.sleep(delay);
			solo.drag(20, 40, 170, 220, 12);
			solo.sleep(delay);
			solo.setActivityOrientation(Solo.PORTRAIT);
			solo.sleep(delay);
			solo.drag(22, 44, 170, 220, 12);
			solo.sleep(delay);
			solo.drag(27, 49, 170, 220, 12);
			solo.sleep(delay);
			solo.drag(20, 40, 170, 220, 12);
			solo.sleep(delay);
			solo.setActivityOrientation(Solo.LANDSCAPE);
			solo.sleep(delay);
			solo.drag(22, 44, 170, 220, 12);
			solo.sleep(delay);
			solo.drag(27, 49, 170, 220, 12);
			solo.sleep(delay);
			solo.drag(20, 40, 170, 220, 12);
			solo.sleep(delay);
			solo.setActivityOrientation(Solo.PORTRAIT);
			solo.sleep(delay);
			solo.assertMemoryNotLow();
		}
	}

	public static void testScrollWithoutRotation(Solo solo, int iterations) throws Exception {
		for (int i = 0; i < iterations; i++) {
			solo.drag(22, 44, 170, 220, 12);
			solo.sleep(delay);
			solo.drag(27, 49, 170, 220, 12);
			solo.sleep(delay);
			solo.drag(20, 40, 170, 220, 12);
			solo.sleep(delay);
			solo.drag(22, 44, 170, 220, 2);
			solo.sleep(delay);
			solo.drag(27, 49, 170, 220, 2);
			solo.sleep(delay);
			solo.drag(20, 40, 170, 220, 2);
			solo.sleep(delay);
			solo.drag(22, 14, 170, 220, 2);
			solo.sleep(delay);
			solo.drag(27, 449, 170, 220, 2);
			solo.sleep(delay);
			solo.drag(210, 430, 170, 220, 2);
			solo.sleep(delay);
			solo.drag(212, 44, 170, 220, 2);
			solo.sleep(delay);
			solo.drag(27, 49, 170, 220, 2);
			solo.sleep(delay);
			solo.sleep(delay);
			solo.drag(20, 40, 170, 220, 2);
			solo.sleep(delay);
			solo.drag(22, 14, 170, 220, 2);
			solo.sleep(delay);
			solo.drag(27, 449, 170, 220, 2);
			solo.sleep(delay);
			solo.drag(210, 430, 170, 220, 2);
			solo.sleep(delay);
			solo.drag(212, 44, 170, 220, 2);
			solo.sleep(delay);
			solo.drag(27, 49, 170, 220, 2);
			solo.sleep(delay);
			solo.drag(20, 40, 170, 220, 2);
			solo.sleep(delay);
			solo.drag(22, 14, 170, 220, 2);
			solo.sleep(delay);
			solo.drag(27, 449, 170, 220, 2);
			solo.sleep(delay);
			solo.drag(27, 49, 170, 220, 2);
			solo.sleep(delay);
			solo.drag(20, 40, 170, 220, 2);
			solo.sleep(delay);
			solo.drag(22, 14, 170, 220, 2);
			solo.sleep(delay);
			solo.drag(27, 449, 170, 220, 2);
			solo.sleep(delay);
			solo.drag(210, 430, 170, 220, 2);
			solo.sleep(delay);
			solo.drag(212, 44, 170, 220, 2);
			solo.sleep(delay);
			solo.drag(210, 430, 170, 220, 2);
			solo.sleep(delay);
			solo.drag(212, 44, 170, 220, 2);
			solo.sleep(delay);
			solo.drag(237, 49, 170, 220, 22);
			solo.sleep(delay);
			solo.drag(10, 40, 170, 220, 2);
			solo.sleep(delay);
			solo.drag(22, 14, 170, 220, 17);
			solo.sleep(delay);
			solo.drag(20, 40, 170, 220, 2);
			solo.sleep(delay);
			solo.drag(22, 14, 170, 220, 2);
			solo.sleep(delay);
			solo.drag(27, 449, 170, 220, 2);
			solo.sleep(delay);
			solo.drag(210, 430, 170, 220, 2);
			solo.sleep(delay);
			solo.drag(212, 44, 170, 220, 2);
			solo.sleep(delay);
			solo.drag(237, 49, 170, 220, 22);
			solo.sleep(delay);
			solo.drag(10, 40, 170, 220, 2);
			solo.sleep(delay);
			solo.drag(22, 14, 170, 220, 17);
			solo.sleep(delay);
			solo.drag(27, 49, 170, 220, 22);
			solo.sleep(delay);
			solo.drag(20, 120, 170, 220, 2);
			solo.sleep(delay);
			solo.drag(22, 24, 170, 220, 12);
			solo.sleep(delay);
			solo.drag(27, 49, 370, 220, 92);
			solo.sleep(delay);
			solo.drag(20, 40, 170, 220, 22);
			solo.sleep(delay);
			solo.assertMemoryNotLow();
		}
	}

	public static void testZoomChanges(Solo solo, int iterations) throws Exception {
		MapView mapView = (MapView) solo.getView(R.id.mapView);
		MapViewPosition mapViewPosition = mapView.getModel().mapViewPosition;

		for (int i = 0; i < iterations; i++) {
			byte startZoomLevel = mapViewPosition.getZoomLevel();
			mapViewPosition.zoom((byte) -1);
			solo.sleep(delay);
			mapViewPosition.zoom((byte) -6);
			solo.sleep(delay);
			mapViewPosition.zoom((byte) 1);
			solo.sleep(delay);
			mapViewPosition.zoom((byte) 1);
			solo.sleep(delay);
			mapViewPosition.zoom((byte) -2);
			solo.sleep(delay);
			solo.setActivityOrientation(Solo.LANDSCAPE);
			mapViewPosition.zoom((byte) 2);
			solo.sleep(delay);
			mapViewPosition.zoom((byte) -3);
			solo.sleep(delay);
			mapViewPosition.zoom((byte) 3);
			solo.drag(210, 430, 170, 220, 2);
			solo.sleep(delay);
			solo.drag(212, 44, 170, 220, 2);
			solo.sleep(delay);
			solo.drag(237, 49, 170, 220, 22);
			solo.sleep(delay);
			solo.drag(10, 40, 170, 220, 2);
			solo.sleep(delay);
			solo.drag(22, 14, 170, 220, 17);
			solo.sleep(delay);
			solo.drag(27, 49, 170, 220, 22);
			solo.sleep(delay);
			solo.drag(20, 120, 170, 220, 2);
			solo.sleep(delay);
			mapViewPosition.zoom((byte) -1);
			solo.setActivityOrientation(Solo.PORTRAIT);
			solo.sleep(delay);
			mapViewPosition.zoom((byte) 6);
			solo.sleep(delay);
			mapViewPosition.zoom((byte) 1);
			solo.sleep(delay);
			mapViewPosition.zoom((byte) -1);
			solo.sleep(delay);

			assert (mapViewPosition.getZoomLevel() == startZoomLevel);
			solo.assertMemoryNotLow();
		}
	}

	public static void testZoom(Solo solo, int iterations) throws Exception {
		MapView mapView = (MapView) solo.getView(R.id.mapView);
		MapViewPosition mapViewPosition = mapView.getModel().mapViewPosition;

		for (int i = 0; i < iterations; i++) {
			mapViewPosition.setZoomLevel((byte) 8);
			solo.sleep(200);
			mapViewPosition.setZoomLevel((byte) 16);
			solo.sleep(200);
			mapViewPosition.setZoomLevel((byte) 8);
			solo.sleep(200);
			mapViewPosition.setZoomLevel((byte) 12);
			solo.sleep(200);
			solo.assertMemoryNotLow();
		}
	}

	public static void testPositionAndZoom(Solo solo, int iterations) throws Exception {
		MapView mapView = (MapView) solo.getView(R.id.mapView);
		MapViewPosition mapViewPosition = mapView.getModel().mapViewPosition;

		for (int i = 0; i < iterations; i++) {
			mapViewPosition.setZoomLevel((byte) 8);
			mapViewPosition.setCenter(new LatLong(52.5, 13.3));
			solo.sleep(200);
			mapViewPosition.setZoomLevel((byte) 16);
			solo.sleep(200);
			mapViewPosition.setCenter(new LatLong(52.6, 13.5));
			mapViewPosition.setZoomLevel((byte) 8);
			solo.sleep(200);
			mapViewPosition.setZoomLevel((byte) 12);
			mapViewPosition.setCenter(new LatLong(52.1, 13.3));
			solo.sleep(200);
			solo.assertMemoryNotLow();
		}
	}

	/**
	 * @param solo
	 * @return true if the current activity uses fragments in one screen
	 */
	static public boolean usesFragments(Solo solo) {
		if (solo.getCurrentActivity().findViewById(R.id.item_detail_container) != null) {
			return true;
		}
		return false;
	}

}
