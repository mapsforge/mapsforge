/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright 2015 devemux86
 * Copyright 2025 Sublimis
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
package org.mapsforge.core.model;

import org.mapsforge.core.util.Utils;

import java.io.Serializable;

/**
 * A tag represents an immutable key-value pair.
 */
public class Tag implements Comparable<Tag>, Serializable {
    private static final long serialVersionUID = 1L;

    public static final char KEY_VALUE_SEPARATOR = '=';

    /**
     * The key of this tag.
     */
    public final String key;

    /**
     * Key of this tag in short "int" version.
     */
    public final int keyCode;

    /**
     * The value of this tag.
     */
    public final String value;

    /**
     * Value of this tag in short "int" version.
     */
    public final int valueCode;

    /**
     * @param tag the textual representation of the tag.
     */
    public Tag(String tag) {
        this(tag, tag.indexOf(KEY_VALUE_SEPARATOR));
    }

    /**
     * @param key   the key of the tag.
     * @param value the value of the tag.
     */
    public Tag(String key, String value) {
        this.key = key;
        this.keyCode = (key == null) ? 0 : Utils.hashTagParameter(key);
        this.value = value;
        this.valueCode = (value == null) ? 0 : Utils.hashTagParameter(value);
    }

    private Tag(String tag, int splitPosition) {
        this(tag.substring(0, splitPosition), tag.substring(splitPosition + 1));
    }

    /**
     * Compares this tag to the specified tag.
     * The tag comparison is based on a comparison of key and value in that order.
     *
     * @param other The tag to compare to.
     * @return 0 if equal, &lt; 0 if considered "smaller", and &gt; 0 if considered "bigger".
     */
    @Override
    public int compareTo(Tag other) {
        int retVal = 0;

        if (this.key != null && other.key != null) {
            retVal = this.key.compareTo(other.key);
            if (retVal != 0) {
                return retVal;
            }
        } else if (this.key != null) {
            return -1;
        } else if (other.key != null) {
            return 1;
        }

        if (this.value != null && other.value != null) {
            retVal = this.value.compareTo(other.value);
            if (retVal != 0) {
                return retVal;
            }
        } else if (this.value != null) {
            return -1;
        } else if (other.value != null) {
            return 1;
        }

        return retVal;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (!(obj instanceof Tag)) {
            return false;
        }
        Tag other = (Tag) obj;
        if (this.key == null) {
            if (other.key != null) {
                return false;
            }
        } else if (!this.key.equals(other.key)) {
            return false;
        } else if (this.value == null) {
            if (other.value != null) {
                return false;
            }
        } else if (!this.value.equals(other.value)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + keyCode;
        result = prime * result + valueCode;
        return result;
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("key=");
        stringBuilder.append(this.key);
        stringBuilder.append(", value=");
        stringBuilder.append(this.value);
        return stringBuilder.toString();
    }
}
