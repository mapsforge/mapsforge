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

import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.util.logging.Logger;

/**
 * Reads from a {@link RandomAccessFile} into a buffer and decodes the data.
 */
public class ReadBuffer {
	/**
	 * Maximum buffer size which is supported by this implementation.
	 */
	static final int MAXIMUM_BUFFER_SIZE = 2500000;
	private static final String CHARSET_UTF8 = "UTF-8";
	private static final Logger LOGGER = Logger.getLogger(ReadBuffer.class.getName());

	private byte[] bufferData;
	private int bufferPosition;
	private final RandomAccessFile inputFile;

	ReadBuffer(RandomAccessFile inputFile) {
		this.inputFile = inputFile;
	}

	/**
	 * Returns one signed byte from the read buffer.
	 * 
	 * @return the byte value.
	 */
	public byte readByte() {
		return this.bufferData[this.bufferPosition++];
	}

	/**
	 * Reads the given amount of bytes from the file into the read buffer and resets the internal buffer position. If
	 * the capacity of the read buffer is too small, a larger one is created automatically.
	 * 
	 * @param length
	 *            the amount of bytes to read from the file.
	 * @return true if the whole data was read successfully, false otherwise.
	 * @throws IOException
	 *             if an error occurs while reading the file.
	 */
	public boolean readFromFile(int length) throws IOException {
		// ensure that the read buffer is large enough
		if (this.bufferData == null || this.bufferData.length < length) {
			// ensure that the read buffer is not too large
			if (length > MAXIMUM_BUFFER_SIZE) {
				LOGGER.warning("invalid read length: " + length);
				return false;
			}
			this.bufferData = new byte[length];
		}

		// reset the buffer position and read the data into the buffer
		this.bufferPosition = 0;
		return this.inputFile.read(this.bufferData, 0, length) == length;
	}

	/**
	 * Converts four bytes from the read buffer to a signed int.
	 * <p>
	 * The byte order is big-endian.
	 * 
	 * @return the int value.
	 */
	public int readInt() {
		this.bufferPosition += 4;
		return Deserializer.getInt(this.bufferData, this.bufferPosition - 4);
	}

	/**
	 * Converts eight bytes from the read buffer to a signed long.
	 * <p>
	 * The byte order is big-endian.
	 * 
	 * @return the long value.
	 */
	public long readLong() {
		this.bufferPosition += 8;
		return Deserializer.getLong(this.bufferData, this.bufferPosition - 8);
	}

	/**
	 * Converts two bytes from the read buffer to a signed int.
	 * <p>
	 * The byte order is big-endian.
	 * 
	 * @return the int value.
	 */
	public int readShort() {
		this.bufferPosition += 2;
		return Deserializer.getShort(this.bufferData, this.bufferPosition - 2);
	}

	/**
	 * Converts a variable amount of bytes from the read buffer to a signed int.
	 * <p>
	 * The first bit is for continuation info, the other six (last byte) or seven (all other bytes) bits are for data.
	 * The second bit in the last byte indicates the sign of the number.
	 * 
	 * @return the int value.
	 */
	public int readSignedInt() {
		int variableByteDecode = 0;
		byte variableByteShift = 0;

		// check if the continuation bit is set
		while ((this.bufferData[this.bufferPosition] & 0x80) != 0) {
			variableByteDecode |= (this.bufferData[this.bufferPosition++] & 0x7f) << variableByteShift;
			variableByteShift += 7;
		}

		// read the six data bits from the last byte
		if ((this.bufferData[this.bufferPosition] & 0x40) != 0) {
			// negative
			return -(variableByteDecode | ((this.bufferData[this.bufferPosition++] & 0x3f) << variableByteShift));
		}
		// positive
		return variableByteDecode | ((this.bufferData[this.bufferPosition++] & 0x3f) << variableByteShift);
	}

	/**
	 * Converts a variable amount of bytes from the read buffer to an unsigned int.
	 * <p>
	 * The first bit is for continuation info, the other seven bits are for data.
	 * 
	 * @return the int value.
	 */
	public int readUnsignedInt() {
		int variableByteDecode = 0;
		byte variableByteShift = 0;

		// check if the continuation bit is set
		while ((this.bufferData[this.bufferPosition] & 0x80) != 0) {
			variableByteDecode |= (this.bufferData[this.bufferPosition++] & 0x7f) << variableByteShift;
			variableByteShift += 7;
		}

		// read the seven data bits from the last byte
		return variableByteDecode | (this.bufferData[this.bufferPosition++] << variableByteShift);
	}

	/**
	 * Decodes a variable amount of bytes from the read buffer to a string.
	 * 
	 * @return the UTF-8 decoded string (may be null).
	 */
	public String readUTF8EncodedString() {
		return readUTF8EncodedString(readUnsignedInt());
	}

	/**
	 * Decodes the given amount of bytes from the read buffer to a string.
	 * 
	 * @param stringLength
	 *            the length of the string in bytes.
	 * @return the UTF-8 decoded string (may be null).
	 */
	public String readUTF8EncodedString(int stringLength) {
		if (stringLength > 0 && this.bufferPosition + stringLength <= this.bufferData.length) {
			this.bufferPosition += stringLength;
			try {
				return new String(this.bufferData, this.bufferPosition - stringLength, stringLength, CHARSET_UTF8);
			} catch (UnsupportedEncodingException e) {
				throw new IllegalStateException(e);
			}
		}
		LOGGER.warning("invalid string length: " + stringLength);
		return null;
	}

	/**
	 * @return the current buffer position.
	 */
	int getBufferPosition() {
		return this.bufferPosition;
	}

	/**
	 * @return the current size of the read buffer.
	 */
	int getBufferSize() {
		return this.bufferData.length;
	}

	/**
	 * Sets the buffer position to the given offset.
	 * 
	 * @param bufferPosition
	 *            the buffer position.
	 */
	void setBufferPosition(int bufferPosition) {
		this.bufferPosition = bufferPosition;
	}

	/**
	 * Skips the given number of bytes in the read buffer.
	 * 
	 * @param bytes
	 *            the number of bytes to skip.
	 */
	void skipBytes(int bytes) {
		this.bufferPosition += bytes;
	}
}
