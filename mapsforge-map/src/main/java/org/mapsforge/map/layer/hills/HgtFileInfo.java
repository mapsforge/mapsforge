/*
 * Copyright 2017-2022 usrusr
 * Copyright 2019 devemux86
 * Copyright 2024 Sublimis
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
package org.mapsforge.map.layer.hills;

import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.map.layer.hills.HgtCache.HgtFileLoadFuture;

import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class HgtFileInfo extends BoundingBox implements ShadingAlgorithm.RawHillTileSource {
    protected final DemFile file;
    protected final long fileSize;
    protected final Map<Long, SoftReference<HgtFileLoadFuture>> map = new HashMap<>();

    protected HgtFileInfo(DemFile file, double minLatitude, double minLongitude, double maxLatitude, double maxLongitude, long fileSize) {
        super(minLatitude, minLongitude, maxLatitude, maxLongitude);
        this.file = file;
        this.fileSize = fileSize;
    }

    protected HgtFileLoadFuture getBitmapFuture(HgtCache hgtCache, ShadingAlgorithm shadingAlgorithm, int padding, int zoomLevel, double pxPerLat, double pxPerLon, int color) {
        final long cacheTag = shadingAlgorithm.getCacheTag(HgtFileInfo.this, padding, zoomLevel, pxPerLat, pxPerLon);

        synchronized (this.map) {
            final SoftReference<HgtFileLoadFuture> reference = this.map.get(cacheTag);
            HgtFileLoadFuture candidate = reference == null ? null : reference.get();

            if (candidate == null || candidate.getCacheTag() != cacheTag) {
                candidate = hgtCache.createHgtFileLoadFuture(HgtFileInfo.this, padding, zoomLevel, pxPerLat, pxPerLon, color);
                this.map.put(cacheTag, new SoftReference<>(candidate));
            }

            return candidate;
        }
    }

    @Override
    public long getSize() {
        return fileSize;
    }

    @Override
    public DemFile getFile() {
        return file;
    }

    @Override
    public double northLat() {
        return maxLatitude;
    }

    @Override
    public double southLat() {
        return minLatitude;
    }

    @Override
    public double westLng() {
        return minLongitude;
    }

    @Override
    public double eastLng() {
        return maxLongitude;
    }

    @Override
    public String toString() {
        final HgtFileLoadFuture future = null;
        return "[lt:" + minLatitude + "-" + maxLatitude + " ln:" + minLongitude + "-" + maxLongitude + (future == null ? "" : future.isDone() ? "done" : "wip") + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }

        HgtFileInfo that = (HgtFileInfo) o;
        return Objects.equals(getFile().getName(), that
                .getFile()
                .getName());
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + Objects.hashCode(getFile().getName());
        return result;
    }
}
