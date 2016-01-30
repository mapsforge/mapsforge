/*
 * Copyright 2015-2016 Ludwig M Brinckmann
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

import android.Manifest;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;

/**
 * An extension to the MapViewerTemplate that supports the runtime permissions introduced in Android 6.
 */
public abstract class MapViewerTemplateRuntimePermissions extends MapViewerTemplate implements ActivityCompat.OnRequestPermissionsResultCallback {

	protected static final byte PERMISSIONS_REQUEST_READ_STORAGE = 122;

	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
		if (PERMISSIONS_REQUEST_READ_STORAGE == requestCode) {
			if (grantResults.length == 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
				showDialogWhenPermissionDenied();
				return;
			}
			createLayers();
			createControls();
		}
		super.onRequestPermissionsResult(requestCode,permissions,grantResults);
	}

	/**
	 * Hook to check for Android Runtime Permissions.
	 */
	@Override
	protected void checkPermissionsAndCreateLayersAndControls() {
		if (AndroidSupportUtil.runtimePermissionRequiredForReadExternalStorage(this, getMapFileDirectory())) {
			ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSIONS_REQUEST_READ_STORAGE);
		} else {
			createLayers();
			createControls();
		}
	}

	/**
	 * Sample dialog shown when permission to read storage denied.
	*/
	protected void showDialogWhenPermissionDenied() {
		final AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Warning");
		builder.setMessage("Without granting access to storage you will not see a map");
		builder.show();
	}
}
