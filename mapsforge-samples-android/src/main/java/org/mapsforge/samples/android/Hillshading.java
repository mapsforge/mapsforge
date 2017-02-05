/*
 * Copyright 2016 devemux86
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

import android.app.AlertDialog;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.layer.hills.HillsRenderConfig;
import org.mapsforge.map.layer.hills.SimpleShadingAlgortithm;

import java.io.File;

/**
 * Standard map view with hill shading.
 */
public class Hillshading extends DefaultTheme {

    @Override
    protected HillsRenderConfig getHillsRenderConfig() {
        File dem = new File(getMapFileDirectory(), "dem");

        if( ! (dem.exists() && dem.isDirectory() && dem.canRead() && dem.listFiles().length>0)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Hillshading demo needs SRTM hgt files");
            builder.setMessage("Currently looking in "+dem+"\noverride in "+this.getClass().getCanonicalName());
            builder.create().show();
        }
        return new HillsRenderConfig(
                dem,
                AndroidGraphicFactory.INSTANCE,
                new SimpleShadingAlgortithm()
        );
    }
}
