package org.mapsforge.samples.android;

import android.os.Environment;

import java.io.File;

/**
 * Created by k3b on 27.08.2016.
 */
public class Global {
    /**
     * Provides the directory of the map file, by default the Android external storage
     * directory (e.g. sdcard).
     *
     * @return
     */
    public static File getMapFileDirectory() {
        // return new File("/mnt/extsd/osmdroid/"); 
		return Environment.getExternalStorageDirectory();
    }

    public static String getMapFileName() {
        return "germany.map";
    }

}
