/*
 * Copyright 2015 Ludwig M Brinckmann
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
package org.mapsforge.map.android.util;

import java.io.File;
import java.io.IOException;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.support.v4.content.ContextCompat;

public final class AndroidSupportUtil {



	/*
	 * Returns if it is required to ask for runtime permission for accessing the coarse location
	 * @param context the activity asking
	 * @return true if runtime permission must be asked for
	 */
	public static boolean runtimePermissionRequiredForAccessCoarseLocation(Context context) {
		return runtimePermissionRequired(context, Manifest.permission.ACCESS_COARSE_LOCATION);
	}

	/*
	 * Returns if it is required to ask for runtime permission for accessing the fine location
	 * @param context the activity asking
	 * @return true if runtime permission must be asked for
	 */
	public static boolean runtimePermissionRequiredForAccessFineLocationContext(Context context) {
		return runtimePermissionRequired(context, Manifest.permission.ACCESS_FINE_LOCATION);
	}

	/**
	 * Returns if it is required to ask for runtime permission for accessing a directory.
	 * @param context the activity asking
	 * @param directory the directory accessed
	 * @return true if runtime permission must be asked for
	 */
	public static boolean runtimePermissionRequiredForReadExternalStorage(Context context, File directory) {
		if (runtimePermissionRequired(context, Manifest.permission.READ_EXTERNAL_STORAGE)) {
			try {
				String canonicalPath = directory.getCanonicalPath();
				// not sure if this covers all possibilities: file is freely accessible if it is in the application external cache or external files
				// dir or somewhere else (e.g. internal storage) but not in the general external storage.
				return canonicalPath.startsWith(Environment.getExternalStorageDirectory().getCanonicalPath()) &&
						!canonicalPath.startsWith(context.getExternalCacheDir().getCanonicalPath()) &&
						!canonicalPath.startsWith(context.getExternalFilesDir(null).getCanonicalPath());
			} catch (IOException e) {
				return true; // ?? it probably means the file cannot be found
			}
		}
		return false;
	}

	public static boolean runtimePermissionRequired(Context context, String permission) {
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
				&& ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED;
	}

	private AndroidSupportUtil() {
		// no-op, for privacy
	}

}
