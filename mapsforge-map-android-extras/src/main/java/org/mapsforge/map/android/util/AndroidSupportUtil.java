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
import java.util.logging.Logger;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.support.v4.content.ContextCompat;

public final class AndroidSupportUtil {

	private static final Logger LOGGER = Logger.getLogger(AndroidSupportUtil.class.getName());

	private AndroidSupportUtil() {
		// no-op, for privacy
	}

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
	public static boolean runtimePermissionRequiredForAccessFineLocation(Context context) {
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
				LOGGER.warning("Directory access exception " + directory.toString() + e.getMessage());
				return true; // ?? it probably means the file cannot be found
			}
		}
		return false;
	}

	public static boolean runtimePermissionRequired(Context context, String permission) {
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
				&& ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED;
	}

	/**
	 * Check that all given permissions have been granted by verifying that each entry in the
	 * given array is of the value {@link PackageManager#PERMISSION_GRANTED}.
	 *
	 * Copyright 2015 The Android Open Source Project
	 *
	 * Licensed under the Apache License, Version 2.0 (the "License");
	 * you may not use this file except in compliance with the License.
	 * You may obtain a copy of the License at
	 *
	 *     http://www.apache.org/licenses/LICENSE-2.0
	 *
	 * Unless required by applicable law or agreed to in writing, software
	 * distributed under the License is distributed on an "AS IS" BASIS,
	 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
	 * See the License for the specific language governing permissions and
	 * limitations under the License.
	 *
	 * From
	 * https://github.com/googlesamples/android-RuntimePermissions/blob/master/Application/src/main/java/com/example/android/system/runtimepermissions/PermissionUtil.java
	 *
	 */

	public static boolean verifyPermissions(int[] grantResults) {
		// At least one result must be checked.
		if(grantResults.length < 1){
			return false;
		}

		// Verify that each required permission has been granted, otherwise return false.
		for (int result : grantResults) {
			if (result != PackageManager.PERMISSION_GRANTED) {
				return false;
			}
		}
		return true;
	}
}
