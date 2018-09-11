/*
 * Copyright 2010, 2011, 2012 mapsforge.org
 * Copyright 2017-2018 devemux86
 * Copyright 2017 Gustl22
 *
 * This file is part of the OpenScienceMap project (http://www.opensciencemap.org).
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
package com.ikroshlab.ovtencoder;

import org.oscim.core.Tag;
import org.oscim.core.TagSet;
import org.oscim.utils.Parameters;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Reads from a {@link RandomAccessFile} into a buffer and decodes the data.
 */
public class ReadBuffer {
    private static final String CHARSET_UTF8 = "UTF-8";
    private static final Logger LOG = Logger.getLogger(ReadBuffer.class.getName());

    private byte[] mBufferData;
    private int mBufferPosition;
    private final RandomAccessFile mInputFile;

    private final List<Integer> mTagIds = new ArrayList<>();

    ReadBuffer(RandomAccessFile inputFile) {
        mInputFile = inputFile;
    }

    /**
     * Returns one signed byte from the read buffer.
     *
     * @return the byte value.
     */
    public byte readByte() {
        return mBufferData[mBufferPosition++];
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
     * Reads the given amount of bytes from the file into the read buffer and
     * resets the internal buffer position. If
     * the capacity of the read buffer is too small, a larger one is created
     * automatically.
     *
     * @param length the amount of bytes to read from the file.
     * @return true if the whole data was read successfully, false otherwise.
     * @throws IOException if an error occurs while reading the file.
     */
    public boolean readFromFile(int length) throws IOException {
        // ensure that the read buffer is large enough
        if (mBufferData == null || mBufferData.length < length) {
            // ensure that the read buffer is not too large
            if (length > Parameters.MAXIMUM_BUFFER_SIZE) {
                LOG.warning("invalid read length: " + length);
                return false;
            }
            mBufferData = new byte[length];
        }

        mBufferPosition = 0;

        // reset the buffer position and read the data into the buffer
        // bufferPosition = 0;
        return mInputFile.read(mBufferData, 0, length) == length;
    }

    /**
     * Converts four bytes from the read buffer to a signed int.
     * <p/>
     * The byte order is big-endian.
     *
     * @return the int value.
     */
    public int readInt() {
        int pos = mBufferPosition;
        byte[] data = mBufferData;
        mBufferPosition += 4;

        return data[pos] << 24
                | (data[pos + 1] & 0xff) << 16
                | (data[pos + 2] & 0xff) << 8
                | (data[pos + 3] & 0xff);
    }

    /**
     * Converts eight bytes from the read buffer to a signed long.
     * <p/>
     * The byte order is big-endian.
     *
     * @return the long value.
     */
    public long readLong() {
        int pos = mBufferPosition;
        byte[] data = mBufferData;
        mBufferPosition += 8;

        return (data[pos] & 0xffL) << 56
                | (data[pos + 1] & 0xffL) << 48
                | (data[pos + 2] & 0xffL) << 40
                | (data[pos + 3] & 0xffL) << 32
                | (data[pos + 4] & 0xffL) << 24
                | (data[pos + 5] & 0xffL) << 16
                | (data[pos + 6] & 0xffL) << 8
                | (data[pos + 7] & 0xffL);

    }

    /**
     * Converts two bytes from the read buffer to a signed int.
     * <p/>
     * The byte order is big-endian.
     *
     * @return the int value.
     */
    public int readShort() {
        mBufferPosition += 2;
        return mBufferData[mBufferPosition - 2] << 8 | (mBufferData[mBufferPosition - 1] & 0xff);
    }

    /**
     * Converts a variable amount of bytes from the read buffer to a signed int.
     * <p/>
     * The first bit is for continuation info, the other six (last byte) or
     * seven (all other bytes) bits are for data. The second bit in the last
     * byte indicates the sign of the number.
     *
     * @return the value.
     */
    public int readSignedInt() {
        int pos = mBufferPosition;
        byte[] data = mBufferData;
        int flag;

        if ((data[pos] & 0x80) == 0) {
            mBufferPosition += 1;
            flag = ((data[pos] & 0x40) >> 6);

            return ((data[pos] & 0x3f) ^ -flag) + flag;
        }

        if ((data[pos + 1] & 0x80) == 0) {
            mBufferPosition += 2;
            flag = ((data[pos + 1] & 0x40) >> 6);

            return (((data[pos] & 0x7f)
                    | (data[pos + 1] & 0x3f) << 7) ^ -flag) + flag;

        }

        if ((data[pos + 2] & 0x80) == 0) {
            mBufferPosition += 3;
            flag = ((data[pos + 2] & 0x40) >> 6);

            return (((data[pos] & 0x7f)
                    | (data[pos + 1] & 0x7f) << 7
                    | (data[pos + 2] & 0x3f) << 14) ^ -flag) + flag;

        }

        if ((data[pos + 3] & 0x80) == 0) {
            mBufferPosition += 4;
            flag = ((data[pos + 3] & 0x40) >> 6);

            return (((data[pos] & 0x7f)
                    | ((data[pos + 1] & 0x7f) << 7)
                    | ((data[pos + 2] & 0x7f) << 14)
                    | ((data[pos + 3] & 0x3f) << 21)) ^ -flag) + flag;
        }

        mBufferPosition += 5;
        flag = ((data[pos + 4] & 0x40) >> 6);

        return ((((data[pos] & 0x7f)
                | (data[pos + 1] & 0x7f) << 7
                | (data[pos + 2] & 0x7f) << 14
                | (data[pos + 3] & 0x7f) << 21
                | (data[pos + 4] & 0x3f) << 28)) ^ -flag) + flag;

    }

    /**
     * Converts a variable amount of bytes from the read buffer to a signed int
     * array.
     * <p/>
     * The first bit is for continuation info, the other six (last byte) or
     * seven (all other bytes) bits are for data. The second bit in the last
     * byte indicates the sign of the number.
     *
     * @param values result values
     * @param length number of values to read
     */
    public void readSignedInt(int[] values, int length) {
        int pos = mBufferPosition;
        byte[] data = mBufferData;
        int flag;

        for (int i = 0; i < length; i++) {

            if ((data[pos] & 0x80) == 0) {

                flag = ((data[pos] & 0x40) >> 6);

                values[i] = ((data[pos] & 0x3f) ^ -flag) + flag;
                pos += 1;

            } else if ((data[pos + 1] & 0x80) == 0) {

                flag = ((data[pos + 1] & 0x40) >> 6);

                values[i] = (((data[pos] & 0x7f)
                        | ((data[pos + 1] & 0x3f) << 7)) ^ -flag) + flag;
                pos += 2;

            } else if ((data[pos + 2] & 0x80) == 0) {

                flag = ((data[pos + 2] & 0x40) >> 6);

                values[i] = (((data[pos] & 0x7f)
                        | ((data[pos + 1] & 0x7f) << 7)
                        | ((data[pos + 2] & 0x3f) << 14)) ^ -flag) + flag;
                pos += 3;

            } else if ((data[pos + 3] & 0x80) == 0) {

                flag = ((data[pos + 3] & 0x40) >> 6);

                values[i] = (((data[pos] & 0x7f)
                        | ((data[pos + 1] & 0x7f) << 7)
                        | ((data[pos + 2] & 0x7f) << 14)
                        | ((data[pos + 3] & 0x3f) << 21)) ^ -flag) + flag;

                pos += 4;
            } else {
                flag = ((data[pos + 4] & 0x40) >> 6);

                values[i] = ((((data[pos] & 0x7f)
                        | ((data[pos + 1] & 0x7f) << 7)
                        | ((data[pos + 2] & 0x7f) << 14)
                        | ((data[pos + 3] & 0x7f) << 21)
                        | ((data[pos + 4] & 0x3f) << 28))) ^ -flag) + flag;

                pos += 5;
            }
        }

        mBufferPosition = pos;
    }

    /**
     * Converts a variable amount of bytes from the read buffer to an unsigned
     * int.
     * <p/>
     * The first bit is for continuation info, the other seven bits are for
     * data.
     *
     * @return the int value.
     */
    public int readUnsignedInt() {
        int pos = mBufferPosition;
        byte[] data = mBufferData;

        if ((data[pos] & 0x80) == 0) {
            mBufferPosition += 1;
            return (data[pos] & 0x7f);
        }

        if ((data[pos + 1] & 0x80) == 0) {
            mBufferPosition += 2;
            return (data[pos] & 0x7f)
                    | (data[pos + 1] & 0x7f) << 7;
        }

        if ((data[pos + 2] & 0x80) == 0) {
            mBufferPosition += 3;
            return (data[pos] & 0x7f)
                    | ((data[pos + 1] & 0x7f) << 7)
                    | ((data[pos + 2] & 0x7f) << 14);
        }

        if ((data[pos + 3] & 0x80) == 0) {
            mBufferPosition += 4;
            return (data[pos] & 0x7f)
                    | ((data[pos + 1] & 0x7f) << 7)
                    | ((data[pos + 2] & 0x7f) << 14)
                    | ((data[pos + 3] & 0x7f) << 21);
        }

        mBufferPosition += 5;
        return (data[pos] & 0x7f)
                | ((data[pos + 1] & 0x7f) << 7)
                | ((data[pos + 2] & 0x7f) << 14)
                | ((data[pos + 3] & 0x7f) << 21)
                | ((data[pos + 4] & 0x7f) << 28);
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
     * @return ...
     */
    public int getPositionAndSkip() {
        int pos = mBufferPosition;
        int length = readUnsignedInt();
        skipBytes(length);
        return pos;
    }

    /**
     * Decodes the given amount of bytes from the read buffer to a string.
     *
     * @param stringLength the length of the string in bytes.
     * @return the UTF-8 decoded string (may be null).
     */
    public String readUTF8EncodedString(int stringLength) {
        if (stringLength > 0 && mBufferPosition + stringLength <= mBufferData.length) {
            mBufferPosition += stringLength;
            try {
                return new String(mBufferData, mBufferPosition - stringLength, stringLength,
                        CHARSET_UTF8);
            } catch (UnsupportedEncodingException e) {
                throw new IllegalStateException(e);
            }
        }
        LOG.warning("invalid string length: " + stringLength);
        return null;
    }

    /**
     * Decodes a variable amount of bytes from the read buffer to a string.
     *
     * @param position buffer offset position of string
     * @return the UTF-8 decoded string (may be null).
     */
    public String readUTF8EncodedStringAt(int position) {
        int curPosition = mBufferPosition;
        mBufferPosition = position;
        String result = readUTF8EncodedString(readUnsignedInt());
        mBufferPosition = curPosition;
        return result;
    }

    /**
     * @return the current buffer position.
     */
    int getBufferPosition() {
        return mBufferPosition;
    }

    /**
     * @return the current size of the read buffer.
     */
    int getBufferSize() {
        return mBufferData.length;
    }

    /**
     * Sets the buffer position to the given offset.
     *
     * @param bufferPosition the buffer position.
     */
    void setBufferPosition(int bufferPosition) {
        mBufferPosition = bufferPosition;
    }

    /**
     * Skips the given number of bytes in the read buffer.
     *
     * @param bytes the number of bytes to skip.
     */
    void skipBytes(int bytes) {
        mBufferPosition += bytes;
    }

    boolean readTags(TagSet tags, Tag[] tagsArray, byte numberOfTags) {
        tags.clear();
        mTagIds.clear();

        int maxTag = tagsArray.length;

        for (byte i = 0; i < numberOfTags; i++) {
            int tagId = readUnsignedInt();
            if (tagId < 0 || tagId >= maxTag) {
                LOG.warning("invalid tag ID: " + tagId);
                break;
            }
            mTagIds.add(tagId);
        }

        for (int tagId : mTagIds) {
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

        return true;
    }

    private static final int WAY_NUMBER_OF_TAGS_BITMASK = 0x0f;
    int lastTagPosition;

    int skipWays(int queryTileBitmask, int elements) {
        int pos = mBufferPosition;
        byte[] data = mBufferData;
        int cnt = elements;
        int skip;

        lastTagPosition = -1;

        while (cnt > 0) {
            // read way size (unsigned int)
            if ((data[pos] & 0x80) == 0) {
                skip = (data[pos] & 0x7f);
                pos += 1;
            } else if ((data[pos + 1] & 0x80) == 0) {
                skip = (data[pos] & 0x7f)
                        | (data[pos + 1] & 0x7f) << 7;
                pos += 2;
            } else if ((data[pos + 2] & 0x80) == 0) {
                skip = (data[pos] & 0x7f)
                        | ((data[pos + 1] & 0x7f) << 7)
                        | ((data[pos + 2] & 0x7f) << 14);
                pos += 3;
            } else if ((data[pos + 3] & 0x80) == 0) {
                skip = (data[pos] & 0x7f)
                        | ((data[pos + 1] & 0x7f) << 7)
                        | ((data[pos + 2] & 0x7f) << 14)
                        | ((data[pos + 3] & 0x7f) << 21);
                pos += 4;
            } else {
                skip = (data[pos] & 0x7f)
                        | ((data[pos + 1] & 0x7f) << 7)
                        | ((data[pos + 2] & 0x7f) << 14)
                        | ((data[pos + 3] & 0x7f) << 21)
                        | ((data[pos + 4] & 0x7f) << 28);
                pos += 5;
            }
            // invalid way size
            if (skip < 0) {
                mBufferPosition = pos;
                return -1;
            }

            // check if way matches queryTileBitmask
            if ((((data[pos] << 8) | (data[pos + 1] & 0xff)) & queryTileBitmask) == 0) {

                // remember last tags position
                if ((data[pos + 2] & WAY_NUMBER_OF_TAGS_BITMASK) != 0)
                    lastTagPosition = pos + 2;

                pos += skip;
                cnt--;
            } else {
                pos += 2;
                break;
            }
        }
        mBufferPosition = pos;
        return cnt;
    }
}
