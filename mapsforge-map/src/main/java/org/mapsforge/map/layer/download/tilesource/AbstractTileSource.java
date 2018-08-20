/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright 2014 Ludwig M Brinckmann
 * Copyright 2014-2018 devemux86
 * Copyright 2018 iPSAlex
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

import java.util.Arrays;
import java.util.Random;

/**
 * The abstract base class for tiles downloaded from a web server.
 * <p/>
 * This class defines a default TTL for cached tiles, accessible through the {@link #getDefaultTimeToLive()}  method. The value
 * here will be used as the initial TTL by the {@link org.mapsforge.map.layer.download.TileDownloadLayer} using this
 * tile source, but applications can change the TTL at any time (refer to
 * {@link org.mapsforge.map.layer.download.TileDownloadLayer} for details). The default value is set to one day, or
 * 86,400,000 milliseconds. Subclasses should set {@code #defaultTTL} in their constructor to a value that is
 * appropriate for their tile source.
 */
public abstract class AbstractTileSource implements TileSource {
    private static final int TIMEOUT_CONNECT = 5000;
    private static final int TIMEOUT_READ = 10000;

    protected String apiKey;
    protected String authorization;
    /**
     * The default time-to-live (TTL) for cached tiles (one day, or 86,400,000 milliseconds).
     */
    protected long defaultTimeToLive = 86400000;
    protected boolean followRedirects = true;
    protected final String[] hostNames;
    protected String keyName = "key";
    protected final int port;
    protected final Random random = new Random();
    protected String referer;
    protected int timeoutConnect = TIMEOUT_CONNECT;
    protected int timeoutRead = TIMEOUT_READ;
    protected String userAgent;

    protected AbstractTileSource(String[] hostNames, int port) {
        if (hostNames == null || hostNames.length == 0) {
            throw new IllegalArgumentException("no host names specified");
        }
        if (port < 0 || port > 65535) {
            throw new IllegalArgumentException("invalid port number: " + port);
        }
        for (String hostname : hostNames) {
            if (hostname.isEmpty()) {
                throw new IllegalArgumentException("empty host name in host name list");
            }
        }

        this.hostNames = hostNames;
        this.port = port;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (!(obj instanceof AbstractTileSource)) {
            return false;
        }
        AbstractTileSource other = (AbstractTileSource) obj;
        if (!Arrays.equals(this.hostNames, other.hostNames)) {
            return false;
        } else if (this.port != other.port) {
            return false;
        }
        return true;
    }

    public String getApiKey() {
        return apiKey;
    }

    @Override
    public String getAuthorization() {
        return authorization;
    }

    /**
     * Returns the default time-to-live (TTL) for cached tiles.
     */
    @Override
    public long getDefaultTimeToLive() {
        return this.defaultTimeToLive;
    }

    protected String getHostName() {
        return this.hostNames[random.nextInt(this.hostNames.length)];
    }

    public String getKeyName() {
        return keyName;
    }

    @Override
    public String getReferer() {
        return referer;
    }

    @Override
    public int getTimeoutConnect() {
        return timeoutConnect;
    }

    @Override
    public int getTimeoutRead() {
        return timeoutRead;
    }

    @Override
    public String getUserAgent() {
        return userAgent;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.hashCode(this.hostNames);
        result = prime * result + this.port;
        return result;
    }

    @Override
    public boolean isFollowRedirects() {
        return followRedirects;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public void setAuthorization(String authorization) {
        this.authorization = authorization;
    }

    public void setFollowRedirects(boolean followRedirects) {
        this.followRedirects = followRedirects;
    }

    public void setKeyName(String keyName) {
        this.keyName = keyName;
    }

    public void setReferer(String referer) {
        this.referer = referer;
    }

    public void setTimeoutConnect(int timeoutConnect) {
        this.timeoutConnect = timeoutConnect;
    }

    public void setTimeoutRead(int timeoutRead) {
        this.timeoutRead = timeoutRead;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }
}
