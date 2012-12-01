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
package org.mapsforge.android;

import android.os.Build;
import android.os.Looper;

/**
 * A utility class with Android-specific helper methods.
 */
public final class AndroidUtils {
	/**
	 * Build names to detect the emulator from the Android SDK.
	 */
	private static final String[] EMULATOR_NAMES = { "google_sdk", "sdk" };

	/**
	 * @return true if the application is running on the Android emulator, false otherwise.
	 */
	public static boolean applicationRunsOnAndroidEmulator() {
		for (int i = 0, n = EMULATOR_NAMES.length; i < n; ++i) {
			if (Build.PRODUCT.equals(EMULATOR_NAMES[i])) {
				return true;
			}
		}

		return false;
	}

	/**
	 * @return true if the current thread is the UI thread, false otherwise.
	 */
	public static boolean currentThreadIsUiThread() {
		return Looper.getMainLooper().getThread() == Thread.currentThread();
	}

	private AndroidUtils() {
		throw new IllegalStateException();
	}
}
