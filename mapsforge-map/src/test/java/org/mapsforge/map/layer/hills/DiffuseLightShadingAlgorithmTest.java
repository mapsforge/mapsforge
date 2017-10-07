package org.mapsforge.map.layer.hills;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class DiffuseLightShadingAlgorithmTest {

    DiffuseLightShadingAlgorithm algo = new DiffuseLightShadingAlgorithm();
    @Test public void examples(){
        assertThat("neutral", example(0,0), is(0));
        assertThat("very much away from light", example(1000,-10000), is(-128));
        assertThat("exactly pointing at light", example(1/algo.getLightHeight(),1/algo.getLightHeight()), is(127));
    }

    private int example(double e, double n) {
        int res = algo.calculate(n, e);
        return res;
    }

    @Test public void heightAngleToRelativeHeight(){
//        assertThat("nan", DiffuseShadingAlgorithm.heightAngleToRelativeHeight(90), is(Double.NaN));
        assertThat("flat", DiffuseLightShadingAlgorithm.heightAngleToRelativeHeight(0), is(0d));
//        assertThat("half", DiffuseShadingAlgorithm.heightAngleToRelativeHeight(45), is(Math.sqrt(2d)));

    }

}