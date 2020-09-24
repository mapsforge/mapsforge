/*
 * Copyright 2013-2014 Ludwig M Brinckmann
 * Copyright 2016-2020 devemux86
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
package org.mapsforge.samples.android;

import android.content.SharedPreferences;
import android.util.Log;
import org.mapsforge.map.android.rendertheme.AssetsRenderTheme;
import org.mapsforge.map.android.util.AndroidUtil;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.rendertheme.XmlRenderTheme;
import org.mapsforge.map.rendertheme.XmlRenderThemeMenuCallback;
import org.mapsforge.map.rendertheme.XmlRenderThemeStyleLayer;
import org.mapsforge.map.rendertheme.XmlRenderThemeStyleMenu;

import java.util.Set;

/**
 * Load render theme from Android assets folder and show a configuration menu based on stylemenu.
 */
public class StyleMenuMapViewer extends SamplesBaseActivity implements XmlRenderThemeMenuCallback {

    @Override
    protected XmlRenderTheme getRenderTheme() {
        return new AssetsRenderTheme(getAssets(), getRenderThemePrefix(), getRenderThemeFile(), this);
    }

    protected String getRenderThemePrefix() {
        return "";
    }

    @Override
    public Set<String> getCategories(XmlRenderThemeStyleMenu menuStyle) {
        this.renderThemeStyleMenu = menuStyle;
        String id = this.sharedPreferences.getString(this.renderThemeStyleMenu.getId(),
                this.renderThemeStyleMenu.getDefaultValue());

        XmlRenderThemeStyleLayer baseLayer = this.renderThemeStyleMenu.getLayer(id);
        if (baseLayer == null) {
            Log.w(SamplesApplication.TAG, "Invalid style " + id);
            return null;
        }
        Set<String> result = baseLayer.getCategories();

        // add the categories from overlays that are enabled
        for (XmlRenderThemeStyleLayer overlay : baseLayer.getOverlays()) {
            if (this.sharedPreferences.getBoolean(overlay.getId(), overlay.isEnabled())) {
                result.addAll(overlay.getCategories());
            }
        }

        return result;
    }

    protected String getRenderThemeFile() {
        return "mapsforge/stylemenu.xml";
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences preferences, String key) {
        super.onSharedPreferenceChanged(preferences, key);
        // difficult to know which render theme options have changed since we
        // do not know all the keys, so we just purge the caches and redraw
        // the map whenever there is a change in the settings.
        // TODO: for real applications it is essential to know when the style menu has
        // changed. It would be good to find a way of identifying when a key is
        // for the style menu.
        Log.i(SamplesApplication.TAG, "Purging tile caches");

        for (TileCache tileCache : tileCaches) {
            tileCache.purge();
        }
        AndroidUtil.restartActivity(this);
    }
}
