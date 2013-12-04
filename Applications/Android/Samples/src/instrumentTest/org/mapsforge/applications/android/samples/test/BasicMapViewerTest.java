package org.mapsforge.applications.android.samples.test;

import org.mapsforge.applications.android.samples.BasicMapViewer;

import android.test.ActivityInstrumentationTestCase2;

import com.jayway.android.robotium.solo.Solo;

public class BasicMapViewerTest extends ActivityInstrumentationTestCase2<BasicMapViewer> {

	Solo solo;
	final int iterations = TestUtils.iterations;

	public BasicMapViewerTest() {
		super(BasicMapViewer.class);
	}

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
