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
package org.mapsforge.map.reader;

/**
 * An utility class to convert byte arrays to numbers.
 */
final class Deserializer {
	/**
	 * Converts five bytes of a byte array to an unsigned long.
	 * <p>
	 * The byte order is big-endian.
	 * 
	 * @param buffer
	 *            the byte array.
	 * @param offset
	 *            the offset in the array.
	 * @return the long value.
	 */
	static long getFiveBytesLong(byte[] buffer, int offset) {
		return (buffer[offset] & 0xffL) << 32 | (buffer[offset + 1] & 0xffL) << 24 | (buffer[offset + 2] & 0xffL) << 16
				| (buffer[offset + 3] & 0xffL) << 8 | (buffer[offset + 4] & 0xffL);
	}

	/**
	 * Converts four bytes of a byte array to a signed int.
	 * <p>
	 * The byte order is big-endian.
	 * 
	 * @param buffer
	 *            the byte array.
	 * @param offset
	 *            the offset in the array.
	 * @return the int value.
	 */
	static int getInt(byte[] buffer, int offset) {
		return buffer[offset] << 24 | (buffer[offset + 1] & 0xff) << 16 | (buffer[offset + 2] & 0xff) << 8
				| (buffer[offset + 3] & 0xff);
	}

	/**
	 * Converts eight bytes of a byte array to a signed long.
	 * <p>
	 * The byte order is big-endian.
	 * 
	 * @param buffer
	 *            the byte array.
	 * @param offset
	 *            the offset in the array.
	 * @return the long value.
	 */
	static long getLong(byte[] buffer, int offset) {
		return (buffer[offset] & 0xffL) << 56 | (buffer[offset + 1] & 0xffL) << 48 | (buffer[offset + 2] & 0xffL) << 40
				| (buffer[offset + 3] & 0xffL) << 32 | (buffer[offset + 4] & 0xffL) << 24
				| (buffer[offset + 5] & 0xffL) << 16 | (buffer[offset + 6] & 0xffL) << 8 | (buffer[offset + 7] & 0xffL);
	}

	/**
	 * Converts two bytes of a byte array to a signed int.
	 * <p>
	 * The byte order is big-endian.
	 * 
	 * @param buffer
	 *            the byte array.
	 * @param offset
	 *            the offset in the array.
	 * @return the int value.
	 */
	static int getShort(byte[] buffer, int offset) {
		return buffer[offset] << 8 | (buffer[offset + 1] & 0xff);
	}

	private Deserializer() {
		throw new IllegalStateException();
	}
}
