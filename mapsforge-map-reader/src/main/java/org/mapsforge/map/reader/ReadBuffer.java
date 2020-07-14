/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright 2015-2020 devemux86
 * Copyright 2016 bvgastel
 * Copyright 2017 linuskr
 * Copyright 2017 Gustl22
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

import org.mapsforge.core.model.Tag;
import org.mapsforge.core.util.Parameters;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Reads from a {@link RandomAccessFile} into a buffer and decodes the data.
 */
public class ReadBuffer {

    private static final String CHARSET_UTF8 = "UTF-8";
    private static final Logger LOGGER = Logger.getLogger(ReadBuffer.class.getName());

    private byte[] bufferData;
    private int bufferPosition;
    private ByteBuffer bufferWrapper;
    private final FileChannel inputChannel;

    private final List<Integer> tagIds = new ArrayList<>();

    ReadBuffer(FileChannel inputChannel) {
        this.inputChannel = inputChannel;
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
     * Converts four bytes from the read buffer to a float.
     * <p/>
     * The byte order is big-endian.
     *
     * @return the float value.
     */
    public float readFloat() {
        return Float.intBitsToFloat(readInt());
    }

    /**
     * Reads the given amount of bytes from the file into the read buffer and resets the internal buffer position. If
     * the capacity of the read buffer is too small, a larger one is created automatically.
     *
     * @param length the amount of bytes to read from the file.
     * @return true if the whole data was read successfully, false otherwise.
     * @throws IOException if an error occurs while reading the file.
     */
    public boolean readFromFile(int length) throws IOException {
        // ensure that the read buffer is large enough
        if (this.bufferData == null || this.bufferData.length < length) {
            // ensure that the read buffer is not too large
            if (length > Parameters.MAXIMUM_BUFFER_SIZE) {
                LOGGER.warning("invalid read length: " + length);
                return false;
            }
            try {
                this.bufferData = new byte[length];
            } catch (Throwable t) {
                LOGGER.log(Level.SEVERE, t.getMessage(), t);
                return false;
            }
            this.bufferWrapper = ByteBuffer.wrap(this.bufferData, 0, length);
        }

        // reset the buffer position and read the data into the buffer
        this.bufferPosition = 0;
        this.bufferWrapper.clear();

        return this.inputChannel.read(this.bufferWrapper) == length;
    }

    /**
     * Reads the given amount of bytes from the file into the read buffer and resets the internal buffer position. If
     * the capacity of the read buffer is too small, a larger one is created automatically.
     *
     * @param offset the offset position, measured in bytes from the beginning of the file, at which to set the file pointer.
     * @param length the amount of bytes to read from the file.
     * @return true if the whole data was read successfully, false otherwise.
     * @throws IOException if an error occurs while reading the file.
     */
    public boolean readFromFile(long offset, int length) throws IOException {
        // ensure that the read buffer is large enough
        if (this.bufferData == null || this.bufferData.length < length) {
            // ensure that the read buffer is not too large
            if (length > Parameters.MAXIMUM_BUFFER_SIZE) {
                LOGGER.warning("invalid read length: " + length);
                return false;
            }
            try {
                this.bufferData = new byte[length];
            } catch (Throwable t) {
                LOGGER.log(Level.SEVERE, t.getMessage(), t);
                return false;
            }
            this.bufferWrapper = ByteBuffer.wrap(this.bufferData, 0, length);
        }

        // reset the buffer position and read the data into the buffer
        this.bufferPosition = 0;
        this.bufferWrapper.clear();

        synchronized (this.inputChannel) {
            this.inputChannel.position(offset);
            return this.inputChannel.read(this.bufferWrapper) == length;
        }
    }

    /**
     * Converts four bytes from the read buffer to a signed int.
     * <p/>
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
     * <p/>
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
     * <p/>
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
     * <p/>
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

    List<Tag> readTags(Tag[] tagsArray, byte numberOfTags) {
        List<Tag> tags = new ArrayList<>();
        tagIds.clear();

        int maxTag = tagsArray.length;

        for (byte tagIndex = numberOfTags; tagIndex != 0; --tagIndex) {
            int tagId = readUnsignedInt();
            if (tagId < 0 || tagId >= maxTag) {
                LOGGER.warning("invalid tag ID: " + tagId);
                return null;
            }
            tagIds.add(tagId);
        }

        for (int tagId : tagIds) {
            Tag tag = tagsArray[tagId];
            // Decode variable values of tags
            if (tag.value.length() == 2 && tag.value.charAt(0) == '%') {
                String value = tag.value;
                if (value.charAt(1) == 'b') {
                    value = String.valueOf(readByte());
                } else if (value.charAt(1) == 'i') {
                    if (tag.key.contains(":colour")) {
                        value = "#" + Integer.toHexString(readInt());
                    } else {
                        value = String.valueOf(readInt());
                    }
                } else if (value.charAt(1) == 'f') {
                    value = String.valueOf(readFloat());
                } else if (value.charAt(1) == 'h') {
                    value = String.valueOf(readShort());
                } else if (value.charAt(1) == 's') {
                    value = readUTF8EncodedString();
                }
                tag = new Tag(tag.key, value);
            }
            tags.add(tag);
        }

        return tags;
    }

    /**
     * Converts a variable amount of bytes from the read buffer to an unsigned int.
     * <p/>
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
     * @param stringLength the length of the string in bytes.
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
     * @param bufferPosition the buffer position.
     */
    void setBufferPosition(int bufferPosition) {
        this.bufferPosition = bufferPosition;
    }

    /**
     * Skips the given number of bytes in the read buffer.
     *
     * @param bytes the number of bytes to skip.
     */
    void skipBytes(int bytes) {
        this.bufferPosition += bytes;
    }
}
