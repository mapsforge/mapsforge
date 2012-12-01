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
package org.mapsforge.map.writer;

import java.security.InvalidParameterException;

/**
 * This class converts numbers to byte arrays.
 */
public final class Serializer {
	/**
	 * Converts a signed int to a byte array.
	 * <p>
	 * The byte order is big-endian.
	 * 
	 * @param value
	 *            the int value.
	 * @return an array with four bytes.
	 */
	public static byte[] getBytes(int value) {
		return new byte[] { (byte) (value >> 24), (byte) (value >> 16), (byte) (value >> 8), (byte) value };
	}

	/**
	 * Converts a signed long to a byte array.
	 * <p>
	 * The byte order is big-endian.
	 * 
	 * @param value
	 *            the long value.
	 * @return an array with eight bytes.
	 */
	public static byte[] getBytes(long value) {
		return new byte[] { (byte) (value >> 56), (byte) (value >> 48), (byte) (value >> 40), (byte) (value >> 32),
				(byte) (value >> 24), (byte) (value >> 16), (byte) (value >> 8), (byte) value };
	}

	/**
	 * Converts a signed short to a byte array.
	 * <p>
	 * The byte order is big-endian.
	 * 
	 * @param value
	 *            the short value.
	 * @return an array with two bytes.
	 */
	public static byte[] getBytes(short value) {
		return new byte[] { (byte) (value >> 8), (byte) value };
	}

	/**
	 * Converts the lowest five bytes of an unsigned long to a byte array.
	 * <p>
	 * The byte order is big-endian.
	 * 
	 * @param value
	 *            the long value, must not be negative.
	 * @return an array with five bytes.
	 */
	public static byte[] getFiveBytes(long value) {
		if (value < 0) {
			throw new IllegalArgumentException("negative value not allowed: " + value);
		} else if (value > 1099511627775L) {
			throw new IllegalArgumentException("value out of range: " + value);
		}
		return new byte[] { (byte) (value >> 32), (byte) (value >> 24), (byte) (value >> 16), (byte) (value >> 8),
				(byte) value };
	}

	/**
	 * Converts a signed int to a variable length byte array.
	 * <p>
	 * The first bit is for continuation info, the other six (last byte) or seven (all other bytes) bits for data. The
	 * second bit in the last byte indicates the sign of the number.
	 * 
	 * @param value
	 *            the int value.
	 * @return an array with 1-5 bytes.
	 */
	public static byte[] getVariableByteSigned(int value) {
		long absValue = Math.abs((long) value);
		if (absValue < 64) { // 2^6
			// encode the number in a single byte
			if (value < 0) {
				return new byte[] { (byte) (absValue | 0x40) };
			}
			return new byte[] { (byte) absValue };
		} else if (absValue < 8192) { // 2^13
			// encode the number in two bytes
			if (value < 0) {
				return new byte[] { (byte) (absValue | 0x80), (byte) ((absValue >> 7) | 0x40) };
			}
			return new byte[] { (byte) (absValue | 0x80), (byte) (absValue >> 7) };
		} else if (absValue < 1048576) { // 2^20
			// encode the number in three bytes
			if (value < 0) {
				return new byte[] { (byte) (absValue | 0x80), (byte) ((absValue >> 7) | 0x80),
						(byte) ((absValue >> 14) | 0x40) };
			}
			return new byte[] { (byte) (absValue | 0x80), (byte) ((absValue >> 7) | 0x80), (byte) (absValue >> 14) };
		} else if (absValue < 134217728) { // 2^27
			// encode the number in four bytes
			if (value < 0) {
				return new byte[] { (byte) (absValue | 0x80), (byte) ((absValue >> 7) | 0x80),
						(byte) ((absValue >> 14) | 0x80), (byte) ((absValue >> 21) | 0x40) };
			}
			return new byte[] { (byte) (absValue | 0x80), (byte) ((absValue >> 7) | 0x80),
					(byte) ((absValue >> 14) | 0x80), (byte) (absValue >> 21) };
		} else {
			// encode the number in five bytes
			if (value < 0) {
				return new byte[] { (byte) (absValue | 0x80), (byte) ((absValue >> 7) | 0x80),
						(byte) ((absValue >> 14) | 0x80), (byte) ((absValue >> 21) | 0x80),
						(byte) ((absValue >> 28) | 0x40) };
			}
			return new byte[] { (byte) (absValue | 0x80), (byte) ((absValue >> 7) | 0x80),
					(byte) ((absValue >> 14) | 0x80), (byte) ((absValue >> 21) | 0x80), (byte) (absValue >> 28) };
		}
	}

	/**
	 * Converts an unsigned int to a variable length byte array.
	 * <p>
	 * The first bit is for continuation info, the other seven bits for data.
	 * 
	 * @param value
	 *            the int value, must not be negative.
	 * @return an array with 1-5 bytes.
	 */
	public static byte[] getVariableByteUnsigned(int value) {
		if (value < 0) {
			throw new InvalidParameterException("negative value not allowed: " + value);
		} else if (value < 128) { // 2^7
			// encode the number in a single byte
			return new byte[] { (byte) value };
		} else if (value < 16384) { // 2^14
			// encode the number in two bytes
			return new byte[] { (byte) (value | 0x80), (byte) (value >> 7) };
		} else if (value < 2097152) { // 2^21
			// encode the number in three bytes
			return new byte[] { (byte) (value | 0x80), (byte) ((value >> 7) | 0x80), (byte) (value >> 14) };
		} else if (value < 268435456) { // 2^28
			// encode the number in four bytes
			return new byte[] { (byte) (value | 0x80), (byte) ((value >> 7) | 0x80), (byte) ((value >> 14) | 0x80),
					(byte) (value >> 21) };
		} else {
			// encode the number in five bytes
			return new byte[] { (byte) (value | 0x80), (byte) ((value >> 7) | 0x80), (byte) ((value >> 14) | 0x80),
					(byte) ((value >> 21) | 0x80), (byte) (value >> 28) };
		}
	}

	private Serializer() {
		throw new IllegalStateException();
	}
}
