/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright 2014 Ludwig M Brinckmann
 * Copyright 2014-2019 devemux86
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
package org.mapsforge.map.layer.download.tilesource;

import org.mapsforge.core.model.Tile;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * A tile source which fetches standard Mapnik tiles from OpenStreetMap.
 * <p/>
 * Layers using this tile source will enforce a time-to-live (TTL) of 8,279,000 milliseconds for cached tiles (unless
 * the application explicitly sets a different TTL for that layer). The default TTL corresponds to the lifetime which
 * the OSM server sets on a newly rendered tile.
 * <p/>
 * Refer to {@link org.mapsforge.map.layer.download.TileDownloadLayer} for details on the TTL mechanism.
 * <p/>
 * Requires a valid HTTP User-Agent identifying application: https://operations.osmfoundation.org/policies/tiles/
 */
public class OpenStreetMapMapnik extends AbstractTileSource {
    public static final OpenStreetMapMapnik INSTANCE = new OpenStreetMapMapnik(new String[]{"tile.openstreetmap.org"}, 443);
    private static final int PARALLEL_REQUESTS_LIMIT = 8;
    private static final String PROTOCOL = "https";
    private static final int ZOOM_LEVEL_MAX = 18;
    private static final int ZOOM_LEVEL_MIN = 0;

    public OpenStreetMapMapnik(String[] hostNames, int port) {
        super(hostNames, port);
        /* Default TTL: 8279 seconds (the TTL currently set by the OSM server). */
        defaultTimeToLive = 8279000;
    }

    @Override
    public int getParallelRequestsLimit() {
        return PARALLEL_REQUESTS_LIMIT;
    }

    @Override
    public URL getTileUrl(Tile tile) throws MalformedURLException {

        return new URL(PROTOCOL, getHostName(), this.port, "/" + tile.zoomLevel + '/' + tile.tileX + '/' + tile.tileY + ".png");
    }

    @Override
    public byte getZoomLevelMax() {
        return ZOOM_LEVEL_MAX;
    }

    @Override
    public byte getZoomLevelMin() {
        return ZOOM_LEVEL_MIN;
    }

    @Override
    public boolean hasAlpha() {
        return false;
    }

}
