/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
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
package org.mapsforge.map.android;

import org.mapsforge.map.model.common.PreferencesFacade;

import android.content.SharedPreferences;

public class AndroidPreferences implements PreferencesFacade {
	private SharedPreferences.Editor editor;
	private final SharedPreferences sharedPreferences;

	public AndroidPreferences(SharedPreferences sharedPreferences) {
		this.sharedPreferences = sharedPreferences;
	}

	@Override
	public synchronized void clear() {
		createEditor();
		this.editor.clear();
	}

	@Override
	public synchronized boolean getBoolean(String key, boolean defaultValue) {
		return this.sharedPreferences.getBoolean(key, defaultValue);
	}

	@Override
	public synchronized byte getByte(String key, byte defaultValue) {
		int intValue = this.sharedPreferences.getInt(key, defaultValue);
		if (intValue < Byte.MIN_VALUE || intValue > Byte.MAX_VALUE) {
			throw new IllegalStateException("byte value out of range: " + intValue);
		}
		return (byte) intValue;
	}

	@Override
	public synchronized double getDouble(String key, double defaultValue) {
		long longValue = this.sharedPreferences.getLong(key, Double.doubleToLongBits(defaultValue));
		return Double.longBitsToDouble(longValue);
	}

	@Override
	public synchronized float getFloat(String key, float defaultValue) {
		return this.sharedPreferences.getFloat(key, defaultValue);
	}

	@Override
	public synchronized int getInt(String key, int defaultValue) {
		return this.sharedPreferences.getInt(key, defaultValue);
	}

	@Override
	public synchronized long getLong(String key, long defaultValue) {
		return this.sharedPreferences.getLong(key, defaultValue);
	}

	@Override
	public synchronized String getString(String key, String defaultValue) {
		return this.sharedPreferences.getString(key, defaultValue);
	}

	@Override
	public synchronized void putBoolean(String key, boolean value) {
		createEditor();
		this.editor.putBoolean(key, value);
	}

	@Override
	public synchronized void putByte(String key, byte value) {
		createEditor();
		this.editor.putInt(key, value);
	}

	@Override
	public synchronized void putDouble(String key, double value) {
		createEditor();
		this.editor.putLong(key, Double.doubleToLongBits(value));
	}

	@Override
	public synchronized void putFloat(String key, float value) {
		createEditor();
		this.editor.putFloat(key, value);
	}

	@Override
	public synchronized void putInt(String key, int value) {
		createEditor();
		this.editor.putInt(key, value);
	}

	@Override
	public synchronized void putLong(String key, long value) {
		this.editor.putLong(key, value);
	}

	@Override
	public synchronized void putString(String key, String value) {
		createEditor();
		this.editor.putString(key, value);
	}

	@Override
	public synchronized void save() {
		if (this.editor != null) {
			this.editor.apply();
			this.editor = null;
		}
	}

	private void createEditor() {
		if (this.editor == null) {
			this.editor = this.sharedPreferences.edit();
		}
	}
}
