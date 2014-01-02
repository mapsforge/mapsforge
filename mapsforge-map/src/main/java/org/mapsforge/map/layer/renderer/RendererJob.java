/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright © 2014 Ludwig M Brinckmann
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

import java.io.File;

import org.mapsforge.core.model.Tile;
import org.mapsforge.map.layer.queue.Job;
import org.mapsforge.map.model.DisplayModel;
import org.mapsforge.map.rendertheme.XmlRenderTheme;

public class RendererJob extends Job {
	public final DisplayModel displayModel;
	public final File mapFile;
	public final float textScale;
	public final XmlRenderTheme xmlRenderTheme;
	private final int hashCodeValue;

	public RendererJob(Tile tile, File mapFile, XmlRenderTheme xmlRenderTheme, DisplayModel displayModel, float textScale) {
		super(tile, displayModel.getTileSize());

		if (mapFile == null) {
			throw new IllegalArgumentException("mapFile must not be null");
		} else if (xmlRenderTheme == null) {
			throw new IllegalArgumentException("xmlRenderTheme must not be null");
		} else if (textScale <= 0 || Float.isNaN(textScale)) {
			throw new IllegalArgumentException("invalid textScale: " + textScale);
		}

		this.displayModel = displayModel;
		this.mapFile = mapFile;
		this.xmlRenderTheme = xmlRenderTheme;
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
		if (!this.mapFile.equals(other.mapFile)) {
			return false;
		} else if (Float.floatToIntBits(this.textScale) != Float.floatToIntBits(other.textScale)) {
			return false;
		} else if (!this.xmlRenderTheme.equals(other.xmlRenderTheme)) {
			return false;
		}
		return true;
	}

	@Override
	public int hashCode() {
		return this.hashCodeValue;
	}

	private int calculateHashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + this.mapFile.hashCode();
		result = prime * result + Float.floatToIntBits(this.textScale);
		result = prime * result + this.xmlRenderTheme.hashCode();
		return result;
	}
}
