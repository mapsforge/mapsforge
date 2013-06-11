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
package org.mapsforge.map.model;

import java.util.HashMap;
import java.util.Map;

import org.mapsforge.map.model.common.PreferencesFacade;

class DummyPreferencesFacade implements PreferencesFacade {
	private final Map<String, Object> map = new HashMap<String, Object>();

	@Override
	public void clear() {
		this.map.clear();
	}

	@Override
	public boolean getBoolean(String key, boolean defaultValue) {
		return ((Boolean) this.map.get(key)).booleanValue();
	}

	@Override
	public byte getByte(String key, byte defaultValue) {
		return ((Byte) this.map.get(key)).byteValue();
	}

	@Override
	public double getDouble(String key, double defaultValue) {
		return ((Double) this.map.get(key)).doubleValue();
	}

	@Override
	public float getFloat(String key, float defaultValue) {
		return ((Float) this.map.get(key)).floatValue();
	}

	@Override
	public int getInt(String key, int defaultValue) {
		return ((Integer) this.map.get(key)).intValue();
	}

	@Override
	public long getLong(String key, long defaultValue) {
		return ((Long) this.map.get(key)).longValue();
	}

	@Override
	public String getString(String key, String defaultValue) {
		return (String) this.map.get(key);
	}

	@Override
	public void putBoolean(String key, boolean value) {
		this.map.put(key, Boolean.valueOf(value));
	}

	@Override
	public void putByte(String key, byte value) {
		this.map.put(key, Byte.valueOf(value));
	}

	@Override
	public void putDouble(String key, double value) {
		this.map.put(key, Double.valueOf(value));
	}

	@Override
	public void putFloat(String key, float value) {
		this.map.put(key, Float.valueOf(value));
	}

	@Override
	public void putInt(String key, int value) {
		this.map.put(key, Integer.valueOf(value));
	}

	@Override
	public void putLong(String key, long value) {
		this.map.put(key, Long.valueOf(value));
	}

	@Override
	public void putString(String key, String value) {
		this.map.put(key, value);
	}

	@Override
	public void save() {
		// do nothing
	}
}
