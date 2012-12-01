/*
 * Copyright 2010, 2011, 2012 mapsforge.org
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
package org.mapsforge.applications.android.advancedmapviewer;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

import org.mapsforge.android.maps.PausableThread;

import android.graphics.Bitmap.CompressFormat;
import android.os.Environment;

class ScreenshotCapturer extends PausableThread {
	private static final String SCREENSHOT_FILE_NAME = "Map screenshot";
	private static final int SCREENSHOT_QUALITY = 90;
	private static final String THREAD_NAME = "ScreenshotCapturer";

	private final AdvancedMapViewer advancedMapViewer;
	private CompressFormat compressFormat;

	ScreenshotCapturer(AdvancedMapViewer advancedMapViewer) {
		this.advancedMapViewer = advancedMapViewer;
	}

	private File assembleFilePath(File directory) {
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append(SCREENSHOT_FILE_NAME);
		stringBuilder.append('.');
		stringBuilder.append(this.compressFormat.name().toLowerCase(Locale.ENGLISH));
		return new File(directory, stringBuilder.toString());
	}

	@Override
	protected synchronized void doWork() {
		try {
			File directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
			if (!directory.exists() && !directory.mkdirs()) {
				this.advancedMapViewer.showToastOnUiThread("Could not create screenshot directory");
				return;
			}

			File outputFile = assembleFilePath(directory);
			if (this.advancedMapViewer.mapView.takeScreenshot(this.compressFormat, SCREENSHOT_QUALITY, outputFile)) {
				this.advancedMapViewer.showToastOnUiThread(outputFile.getAbsolutePath());
			} else {
				this.advancedMapViewer.showToastOnUiThread("Screenshot could not be saved");
			}
		} catch (IOException e) {
			this.advancedMapViewer.showToastOnUiThread(e.getMessage());
		}

		this.compressFormat = null;
	}

	@Override
	protected String getThreadName() {
		return THREAD_NAME;
	}

	@Override
	protected ThreadPriority getThreadPriority() {
		return ThreadPriority.BELOW_NORMAL;
	}

	@Override
	protected synchronized boolean hasWork() {
		return this.compressFormat != null;
	}

	synchronized void captureScreenshot(CompressFormat screenhotFormat) {
		this.compressFormat = screenhotFormat;
		notify();
	}
}
