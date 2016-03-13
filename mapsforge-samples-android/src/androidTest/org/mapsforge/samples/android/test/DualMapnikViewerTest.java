package org.mapsforge.samples.android.test;

import android.test.ActivityInstrumentationTestCase2;

import com.robotium.solo.Solo;

import org.mapsforge.samples.android.DualMapnikMapViewer;

public class DualMapnikViewerTest extends ActivityInstrumentationTestCase2<DualMapnikMapViewer> {

    Solo solo;
    final static int iterations = TestUtils.iterations;

    public DualMapnikViewerTest() {
        super(DualMapnikMapViewer.class);
    }

    @Override
    public void setUp() throws Exception {
        this.solo = new Solo(getInstrumentation(), getActivity());
    }

    public void testClickWithRotation() throws Exception {
        TestUtils.testClickWithRotation(this.solo, this.iterations);
    }

    public void testClickWithoutRotation() throws Exception {
        TestUtils.testClickWithoutRotation(this.solo, this.iterations);
    }

    public void testScrollWithRotation() throws Exception {
        TestUtils.testScrollWithRotation(this.solo, this.iterations);
    }

    public void testScrollWithoutRotation() throws Exception {
        TestUtils.testScrollWithoutRotation(this.solo, this.iterations);
    }

    public void testZoom() throws Exception {
        TestUtils.testZoom(this.solo, this.iterations);
    }

    public void testZoomChanges() throws Exception {
        TestUtils.testZoomChanges(this.solo, this.iterations);
    }

    public void testSetPositionAndZoom() throws Exception {
        TestUtils.testPositionAndZoom(this.solo, this.iterations);
    }

    @Override
    public void tearDown() throws Exception {
        this.solo.finishOpenedActivities();
    }
}
