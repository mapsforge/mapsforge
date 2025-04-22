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

import java.io.InputStream;
import java.util.logging.Logger;

import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.graphics.TileBitmap;
import org.mapsforge.core.model.Tile;

public class MBTilesRenderer {
    private static final Logger LOGGER = Logger.getLogger(MBTilesRenderer.class.getName());

    private final MBTilesFile file;
    private final GraphicFactory graphicFactory;
    private final long timestamp;

    MBTilesRenderer(final MBTilesFile file, final GraphicFactory graphicFactory) {
        this.file = file;
        this.graphicFactory = graphicFactory;
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * Called when a job needs to be executed.
     *
     * @param rendererJob the job that should be executed.
     */
    public TileBitmap executeJob(final MBTilesRendererJob rendererJob) {

        try {
            final InputStream inputStream = file.getTileAsBytes(rendererJob.tile.tileX, rendererJob.tile.tileY, rendererJob.tile.zoomLevel);

            final TileBitmap bitmap;
            if (inputStream == null) {
                bitmap = graphicFactory.createTileBitmap(rendererJob.tile.tileSize, rendererJob.hasAlpha);
            } else {
                bitmap = graphicFactory.createTileBitmap(inputStream, rendererJob.tile.tileSize, rendererJob.hasAlpha);
                bitmap.scaleTo(rendererJob.tile.tileSize, rendererJob.tile.tileSize);
            }
            bitmap.setTimestamp(rendererJob.getDatabaseRenderer().getDataTimestamp(rendererJob.tile));
            return bitmap;
        } catch (Exception e) {
            LOGGER.warning("Error while rendering job " + rendererJob + ": " + e.getMessage());
            return null;
        }
    }

    public long getDataTimestamp(final Tile tile) {
        return timestamp;
    }

    MBTilesFile getMBTilesFile() {
        return file;
    }
}
