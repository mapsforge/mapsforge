/*
 * Copyright 2014-2016 devemux86
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

public class OnlineTileSource extends AbstractTileSource {
    private boolean alpha = false;
    private String baseUrl = "/";
    private String extension = "png";
    private String name;
    private int parallelRequestsLimit = 8;
    private String protocol = "http";
    private int tileSize = 256;
    private byte zoomLevelMax = 18;
    private byte zoomLevelMin = 0;

    public OnlineTileSource(String[] hostNames, int port) {
        super(hostNames, port);
    }

    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj)) {
            return false;
        }
        if (!(obj instanceof OnlineTileSource)) {
            return false;
        }
        OnlineTileSource other = (OnlineTileSource) obj;
        if (!this.baseUrl.equals(other.baseUrl)) {
            return false;
        }
        return true;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getExtension() {
        return extension;
    }

    public String getName() {
        return name;
    }

    @Override
    public int getParallelRequestsLimit() {
        return parallelRequestsLimit;
    }

    public String getProtocol() {
        return protocol;
    }

    public int getTileSize() {
        return tileSize;
    }

    @Override
    public URL getTileUrl(Tile tile) throws MalformedURLException {
        StringBuilder stringBuilder = new StringBuilder(32);

        stringBuilder.append(baseUrl);
        stringBuilder.append(tile.zoomLevel);
        stringBuilder.append('/');
        stringBuilder.append(tile.tileX);
        stringBuilder.append('/');
        stringBuilder.append(tile.tileY);
        stringBuilder.append('.').append(extension);

        return new URL(this.protocol, getHostName(), this.port, stringBuilder.toString());
    }

    @Override
    public byte getZoomLevelMax() {
        return zoomLevelMax;
    }

    @Override
    public byte getZoomLevelMin() {
        return zoomLevelMin;
    }

    @Override
    public boolean hasAlpha() {
        return alpha;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + this.baseUrl.hashCode();
        return result;
    }

    public OnlineTileSource setAlpha(boolean alpha) {
        this.alpha = alpha;
        return this;
    }

    public OnlineTileSource setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
        return this;
    }

    public OnlineTileSource setExtension(String extension) {
        this.extension = extension;
        return this;
    }

    public OnlineTileSource setName(String name) {
        this.name = name;
        return this;
    }

    public OnlineTileSource setParallelRequestsLimit(int parallelRequestsLimit) {
        this.parallelRequestsLimit = parallelRequestsLimit;
        return this;
    }

    public OnlineTileSource setProtocol(String protocol) {
        this.protocol = protocol;
        return this;
    }

    public OnlineTileSource setTileSize(int tileSize) {
        this.tileSize = tileSize;
        return this;
    }

    public OnlineTileSource setZoomLevelMax(byte zoomLevelMax) {
        this.zoomLevelMax = zoomLevelMax;
        return this;
    }

    public OnlineTileSource setZoomLevelMin(byte zoomLevelMin) {
        this.zoomLevelMin = zoomLevelMin;
        return this;
    }
}
