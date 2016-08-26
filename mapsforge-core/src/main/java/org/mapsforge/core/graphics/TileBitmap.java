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
package org.mapsforge.core.graphics;

public interface TileBitmap extends Bitmap {

    /**
     * Returns the timestamp of the tile in milliseconds since January 1, 1970 GMT or 0 if this timestamp is unknown.
     * <p/>
     * The timestamp indicates when the tile was created and can be used together with a TTL in order to determine
     * whether to treat it as expired.
     */
    public long getTimestamp();

    /**
     * Whether the TileBitmap has expired.
     * <p/>
     * When a tile has expired, the requester should try to replace it with a fresh copy as soon as possible. The
     * expired tile may still be displayed to the user until the fresh copy is available. This may be desirable if
     * obtaining a fresh copy is time-consuming or a fresh copy is currently unavailable (e.g. because no network
     * connection is available for a {@link org.mapsforge.map.layer.download.tilesource.TileSource}).
     *
     * @return {@code true} if expired, {@code false} otherwise.
     */
    public boolean isExpired();

    /**
     * Sets the timestamp when this tile will be expired in milliseconds since January 1, 1970 GMT or 0 if this
     * timestamp is unknown.
     * <p/>
     * The timestamp indicates when the tile should be treated it as expired, i.e. {@link #isExpired()} will return
     * {@code true}. For a downloaded tile, pass the value returned by
     * {@link java.net.HttpURLConnection#getExpiration()}, if set by the server. In all other cases you can pass current
     * time plus a fixed TTL in order to have the tile expire after the specified time.
     */
    public void setExpiration(long expiration);

    /**
     * Sets the timestamp of the tile in milliseconds since January 1, 1970 GMT.
     * <p/>
     * The timestamp indicates when the information to create the tile was last retrieved from the source. It can be
     * used together with a TTL in order to determine whether to treat it as expired.
     * <p/>
     * The timestamp of a locally rendered tile should be set to the timestamp of the map database used to render it, as
     * returned by {@link org.mapsforge.map.reader.header.MapFileInfo#mapDate}. For a tile read from a disk cache, it
     * should be the file's timestamp. In all other cases (including downloaded tiles), the timestamp should be set to
     * wall clock time (as returned by {@link java.lang.System#currentTimeMillis()}) when the tile is created.
     * <p/>
     * Classes that implement this interface should call {@link java.lang.System#currentTimeMillis()} upon creating an
     * instance, store the result and return it unless {@code setTimestamp()} has been called for that instance.
     */
    public void setTimestamp(long timestamp);

}
