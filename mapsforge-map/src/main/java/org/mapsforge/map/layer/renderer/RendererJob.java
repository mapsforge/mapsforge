/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright 2014 Ludwig M Brinckmann
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
package org.mapsforge.map.layer.renderer;

import org.mapsforge.core.model.Tile;
import org.mapsforge.map.datastore.MapDataStore;
import org.mapsforge.map.layer.queue.Job;
import org.mapsforge.map.model.DisplayModel;
import org.mapsforge.map.rendertheme.rule.RenderThemeFuture;

public class RendererJob extends Job {
    public final DisplayModel displayModel;
    public boolean labelsOnly;
    public final MapDataStore mapDataStore;
    public final RenderThemeFuture renderThemeFuture;
    public final float textScale;
    private final int hashCodeValue;

    public RendererJob(Tile tile, MapDataStore mapFile, RenderThemeFuture renderThemeFuture, DisplayModel displayModel,
                       float textScale, boolean isTransparent, boolean labelsOnly) {
        super(tile, isTransparent);

        if (mapFile == null) {
            throw new IllegalArgumentException("mapFile must not be null");
        } else if (textScale <= 0 || Float.isNaN(textScale)) {
            throw new IllegalArgumentException("invalid textScale: " + textScale);
        }

        this.labelsOnly = labelsOnly;
        this.displayModel = displayModel;
        this.mapDataStore = mapFile;
        this.renderThemeFuture = renderThemeFuture;
        this.textScale = textScale;

        this.hashCodeValue = calculateHashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (!super.equals(obj)) {
            return false;
        } else if (!(obj instanceof RendererJob)) {
            return false;
        }
        RendererJob other = (RendererJob) obj;
        if (!this.mapDataStore.equals(other.mapDataStore)) {
            return false;
        } else if (Float.floatToIntBits(this.textScale) != Float.floatToIntBits(other.textScale)) {
            return false;
        } else if (this.renderThemeFuture == null && other.renderThemeFuture != null) {
            return false;
        } else if (this.renderThemeFuture != null && !this.renderThemeFuture.equals(other.renderThemeFuture)) {
            return false;
        } else if (this.labelsOnly != other.labelsOnly) {
            return false;
        } else if (!this.displayModel.equals(other.displayModel)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return this.hashCodeValue;
    }

    /**
     * Just a way of generating a hash key for a tile if only the RendererJob is known.
     *
     * @param tile the tile that changes
     * @return a RendererJob based on the current one, only tile changes
     */
    public RendererJob otherTile(Tile tile) {
        return new RendererJob(tile, this.mapDataStore, this.renderThemeFuture, this.displayModel, this.textScale, this.hasAlpha, this.labelsOnly);
    }

    /**
     * Indicates that for this job only the labels should be generated.
     */
    public void setRetrieveLabelsOnly() {
        this.labelsOnly = true;
    }

    private int calculateHashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + this.mapDataStore.hashCode();
        result = prime * result + Float.floatToIntBits(this.textScale);
        if (renderThemeFuture != null) {
            result = prime * result + this.renderThemeFuture.hashCode();
        }
        return result;
    }
}
