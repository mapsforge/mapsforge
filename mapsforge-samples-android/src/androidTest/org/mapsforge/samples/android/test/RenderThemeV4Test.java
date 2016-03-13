package org.mapsforge.samples.android.test;

import android.test.ActivityInstrumentationTestCase2;

import com.robotium.solo.Solo;

import org.mapsforge.samples.android.RenderTheme4;

public class RenderThemeV4Test extends ActivityInstrumentationTestCase2<RenderTheme4> {

    Solo solo;
    final static int iterations = TestUtils.iterations;

    public RenderThemeV4Test() {
        super(RenderTheme4.class);
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

    @Override
    public void tearDown() throws Exception {
        this.solo.finishOpenedActivities();
    }
}
