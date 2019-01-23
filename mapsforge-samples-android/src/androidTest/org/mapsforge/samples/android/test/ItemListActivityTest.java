/*
package org.mapsforge.samples.android.test;

import android.test.ActivityInstrumentationTestCase2;
import android.widget.ListView;

import com.robotium.solo.Solo;

import junit.framework.Assert;

import org.mapsforge.samples.android.ItemDetailActivity;
import org.mapsforge.samples.android.ItemListActivity;

public class ItemListActivityTest extends ActivityInstrumentationTestCase2<ItemListActivity> {

    Solo solo;
    final static int iterations = TestUtils.iterations;

    public ItemListActivityTest() {
        super(ItemListActivity.class);
    }

    @Override
    public void setUp() throws Exception {
        this.solo = new Solo(getInstrumentation(), getActivity());
    }

    public void testClickOnPoiList() {
        boolean usesFragments = TestUtils.usesFragments(this.solo);
        this.solo.sleep(300);
        ListView lv = this.solo.getCurrentViews(ListView.class).get(0);
        // for testing three pois in list
        Assert.assertTrue(lv.getCount() == 3);

        this.solo.clickOnText("Branden");
        if (usesFragments) {
            this.solo.assertCurrentActivity("ItemListActivity", getActivity().getClass());
        } else {
            this.solo.goBack();
        }
        // test we are back at list of pois
        this.solo.assertCurrentActivity("ItemListActivity", getActivity().getClass());
        lv = this.solo.getCurrentViews(ListView.class).get(0);

        Assert.assertTrue(lv.getCount() == 3);
    }

    public void testClickOnPoiListWithRotation() {
        boolean usesFragments = TestUtils.usesFragments(this.solo);

        this.solo.setActivityOrientation(Solo.LANDSCAPE);
        this.solo.sleep(300);
        this.solo.waitForView(ListView.class);
        ListView lv = this.solo.getCurrentViews(ListView.class).get(0);
        // for testing three pois in list
        Assert.assertTrue(lv.getCount() == 3);
        this.solo.setActivityOrientation(Solo.PORTRAIT);
        this.solo.sleep(100);
        this.solo.clickOnText("Branden");
        if (usesFragments) {
            this.solo.assertCurrentActivity("ItemListActivity", getActivity().getClass());
        } else {
            this.solo.assertCurrentActivity("ItemDetailActivity", ItemDetailActivity.class);
        }
        this.solo.setActivityOrientation(Solo.LANDSCAPE);
        if (!usesFragments) {
            this.solo.goBack();
            this.solo.sleep(200);
        }
        this.solo.setActivityOrientation(Solo.PORTRAIT);
        // test we are back at list of pois
        this.solo.assertCurrentActivity("ItemListActivity", getActivity().getClass());
        this.solo.waitForView(ListView.class);
        lv = this.solo.getCurrentViews(ListView.class).get(0);
        Assert.assertTrue(lv.getCount() == 3);
        this.solo.setActivityOrientation(Solo.LANDSCAPE);
        this.solo.setActivityOrientation(Solo.PORTRAIT);
    }

    @Override
    public void tearDown() throws Exception {
        this.solo.finishOpenedActivities();
    }
}
*/
