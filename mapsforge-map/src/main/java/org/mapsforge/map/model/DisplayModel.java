/*
 * Copyright 2014 Ludwig M Brinckmann
 * Copyright 2015-2020 devemux86
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.mapsforge.map.model;

import org.mapsforge.core.graphics.Filter;
import org.mapsforge.map.model.common.Observable;
import org.mapsforge.map.rendertheme.ThemeCallback;

/**
 * Encapsulates the display characteristics for a MapView, such as tile size and background color. The size of map tiles
 * is used to adapt to devices with differing pixel densities and users with different preferences: The larger the tile,
 * the larger everything is rendered, the effect is one of effectively stretching everything. The default device
 * dependent scale factor is determined at the GraphicFactory level, while the DisplayModel allows further adaptation to
 * cater for user needs or application development (maybe a small map and large map, or to prevent upscaling for
 * downloaded tiles that do not scale well).
 */
public class DisplayModel extends Observable {
    private static final int DEFAULT_BACKGROUND_COLOR = 0xffeeeeee; // format AARRGGBB
    private static final int DEFAULT_TILE_SIZE = 256;
    private static final float DEFAULT_MAX_TEXT_WIDTH_FACTOR = 0.7f;
    private static final int DEFAULT_MAX_TEXT_WIDTH = (int) (DEFAULT_TILE_SIZE * DEFAULT_MAX_TEXT_WIDTH_FACTOR);

    /**
     * The symbol scale.
     */
    public static float symbolScale = 1;

    private static float defaultUserScaleFactor = 1f;
    private static float deviceScaleFactor = 1f;

    /**
     * Get the default scale factor for all newly created DisplayModels.
     *
     * @return the default scale factor to be applied to all new DisplayModels.
     */
    public static synchronized float getDefaultUserScaleFactor() {
        return defaultUserScaleFactor;
    }

    /**
     * Returns the device scale factor.
     *
     * @return the device scale factor.
     */
    public static synchronized float getDeviceScaleFactor() {
        return deviceScaleFactor;
    }

    /**
     * Set the default scale factor for all newly created DisplayModels, so can be used to apply user settings from a
     * device.
     *
     * @param scaleFactor the default scale factor to be applied to all new DisplayModels.
     */
    public static synchronized void setDefaultUserScaleFactor(float scaleFactor) {
        defaultUserScaleFactor = scaleFactor;
    }

    /**
     * Set the device scale factor.
     *
     * @param scaleFactor the device scale factor.
     */
    public static synchronized void setDeviceScaleFactor(float scaleFactor) {
        deviceScaleFactor = scaleFactor;
    }

    private int backgroundColor = DEFAULT_BACKGROUND_COLOR;
    private Filter filter = Filter.NONE;
    private int fixedTileSize;
    private int maxTextWidth = DEFAULT_MAX_TEXT_WIDTH;
    private float maxTextWidthFactor = DEFAULT_MAX_TEXT_WIDTH_FACTOR;
    private ThemeCallback themeCallback;
    private int tileSize = DEFAULT_TILE_SIZE;
    private int tileSizeMultiple = 64;
    private float userScaleFactor = defaultUserScaleFactor;

    public DisplayModel() {
        super();
        this.setTileSize();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!(obj instanceof DisplayModel))
            return false;
        DisplayModel other = (DisplayModel) obj;
        if (this.backgroundColor != other.backgroundColor)
            return false;
        if (this.filter != other.filter)
            return false;
        if (this.fixedTileSize != other.fixedTileSize)
            return false;
        if (this.maxTextWidth != other.maxTextWidth)
            return false;
        if (Float.floatToIntBits(this.maxTextWidthFactor) != Float.floatToIntBits(other.maxTextWidthFactor))
            return false;
        if (this.tileSize != other.tileSize)
            return false;
        if (this.tileSizeMultiple != other.tileSizeMultiple)
            return false;
        if (Float.floatToIntBits(this.userScaleFactor) != Float.floatToIntBits(other.userScaleFactor))
            return false;
        return true;
    }

    /**
     * Returns the background color.
     *
     * @return the background color.
     */
    public synchronized int getBackgroundColor() {
        return backgroundColor;
    }

    /**
     * Color filtering in map rendering.
     */
    public synchronized Filter getFilter() {
        return this.filter;
    }

    /**
     * Returns the maximum width of text beyond which the text is broken into lines.
     *
     * @return the maximum text width
     */
    public int getMaxTextWidth() {
        return maxTextWidth;
    }

    /**
     * Returns the overall scale factor.
     *
     * @return the combined device/user scale factor.
     */
    public synchronized float getScaleFactor() {
        return deviceScaleFactor * this.userScaleFactor;
    }

    /**
     * Theme callback.
     */
    public synchronized ThemeCallback getThemeCallback() {
        return this.themeCallback;
    }

    /**
     * Width and height of a map tile in pixel after system and user scaling is applied.
     */
    public synchronized int getTileSize() {
        return tileSize;
    }

    /**
     * Gets the tile size multiple.
     */
    public synchronized int getTileSizeMultiple() {
        return this.tileSizeMultiple;
    }

    /**
     * Returns the user scale factor.
     *
     * @return the user scale factor.
     */
    public synchronized float getUserScaleFactor() {
        return this.userScaleFactor;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + this.backgroundColor;
        result = prime * result + this.filter.hashCode();
        result = prime * result + this.fixedTileSize;
        result = prime * result + this.maxTextWidth;
        result = prime * result + Float.floatToIntBits(this.maxTextWidthFactor);
        result = prime * result + this.tileSize;
        result = prime * result + this.tileSizeMultiple;
        result = prime * result + Float.floatToIntBits(this.userScaleFactor);
        return result;
    }

    /**
     * Set the background color.
     *
     * @param color the color to use.
     */
    public synchronized void setBackgroundColor(int color) {
        this.backgroundColor = color;
    }

    /**
     * Color filtering in map rendering.
     */
    public synchronized void setFilter(Filter filter) {
        this.filter = filter;
    }

    /**
     * Forces the tile size to a fixed value
     *
     * @param tileSize the fixed tile size to use if != 0, if 0 the tile size will be calculated
     */
    public void setFixedTileSize(int tileSize) {
        this.fixedTileSize = tileSize;
        setTileSize();
    }

    /**
     * Sets the factor to compute the maxTextWidth
     *
     * @param maxTextWidthFactor to compute maxTextWidth
     */
    public void setMaxTextWidthFactor(float maxTextWidthFactor) {
        this.maxTextWidthFactor = maxTextWidthFactor;
        this.setMaxTextWidth();
    }

    /**
     * Theme callback.
     */
    public synchronized void setThemeCallback(ThemeCallback themeCallback) {
        this.themeCallback = themeCallback;
    }

    /**
     * Clamps the tile size to a multiple of the supplied value.
     * <p/>
     * The default value of tileSizeMultiple will be overwritten with this call.
     * The default value should be good enough for most applications and setting
     * this value should rarely be required.
     * Applications that allow external renderthemes might negatively impact
     * their layout as area fills may depend on the default value being used.
     *
     * @param multiple tile size multiple
     */
    public synchronized void setTileSizeMultiple(int multiple) {
        this.tileSizeMultiple = multiple;
        setTileSize();
    }

    /**
     * Set the user scale factor.
     *
     * @param scaleFactor the user scale factor to use.
     */
    public synchronized void setUserScaleFactor(float scaleFactor) {
        userScaleFactor = scaleFactor;
        setTileSize();
    }

    private void setMaxTextWidth() {
        this.maxTextWidth = (int) (this.tileSize * maxTextWidthFactor);
    }

    private void setTileSize() {
        if (this.fixedTileSize == 0) {
            float temp = DEFAULT_TILE_SIZE * deviceScaleFactor * userScaleFactor;
            // this will clamp to the nearest multiple of the tileSizeMultiple
            // and make sure we do not end up with 0
            this.tileSize = Math.max(tileSizeMultiple,
                    Math.round(temp / this.tileSizeMultiple) * this.tileSizeMultiple);
        } else {
            this.tileSize = this.fixedTileSize;
        }
        this.setMaxTextWidth();
    }
}
