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
package org.mapsforge.map.model.common;

public interface PreferencesFacade {
	void clear();

	boolean getBoolean(String key, boolean defaultValue);

	byte getByte(String key, byte defaultValue);

	double getDouble(String key, double defaultValue);

	float getFloat(String key, float defaultValue);

	int getInt(String key, int defaultValue);

	long getLong(String key, long defaultValue);

	String getString(String key, String defaultValue);

	void putBoolean(String key, boolean value);

	void putByte(String key, byte value);

	void putDouble(String key, double value);

	void putFloat(String key, float value);

	void putInt(String key, int value);

	void putLong(String key, long value);

	void putString(String key, String value);

	void save();
}
