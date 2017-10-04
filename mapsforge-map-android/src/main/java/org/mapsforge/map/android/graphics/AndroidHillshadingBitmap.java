package org.mapsforge.map.android.graphics;

import org.mapsforge.core.graphics.HillshadingBitmap;
import org.mapsforge.core.model.BoundingBox;

/**
 * Created by usrusr on 28.02.2017.
 */
public class AndroidHillshadingBitmap extends AndroidBitmap implements HillshadingBitmap{
    private final int padding;
    private final BoundingBox areaRect;

    public AndroidHillshadingBitmap(int width, int height, int padding, BoundingBox areaRect) {
        super(width, height, AndroidGraphicFactory.MONO_ALPHA_BITMAP);

        this.padding = padding;
        this.areaRect = areaRect;
    }

    @Override
    public int getPadding() {
        return padding;
    }

    @Override
    public BoundingBox getAreaRect() {
        return areaRect;
    }


}
