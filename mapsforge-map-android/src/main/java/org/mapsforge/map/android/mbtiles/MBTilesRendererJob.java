/*
 * Copyright 2025 cpesch
 * Copyright 2025 moving-bits
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
package org.mapsforge.map.android.mbtiles;

import org.mapsforge.core.model.Tile;
import org.mapsforge.map.layer.queue.Job;

public class MBTilesRendererJob extends Job {
    private final MBTilesRenderer renderer;
    private final int hashCodeValue;

    public MBTilesRendererJob(final Tile tile, final MBTilesRenderer renderer, final boolean isTransparent) {
        super(tile, isTransparent);
        this.renderer = renderer;
        this.hashCodeValue = calculateHashCode();
    }

    public MBTilesRenderer getDatabaseRenderer() {
        return renderer;
    }

    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        } else if (!super.equals(obj)) {
            return false;
        } else if (!(obj instanceof MBTilesRendererJob)) {
            return false;
        }
        final MBTilesRendererJob other = (MBTilesRendererJob) obj;
        return this.getDatabaseRenderer().equals(other.getDatabaseRenderer());
    }

    public int hashCode() {
        return this.hashCodeValue;
    }

    private int calculateHashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + this.getDatabaseRenderer().hashCode();
        return result;
    }
}
