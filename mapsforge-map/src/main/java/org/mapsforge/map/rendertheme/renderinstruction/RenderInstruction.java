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
package org.mapsforge.map.rendertheme.renderinstruction;

import java.util.List;

import org.mapsforge.core.model.Tag;
import org.mapsforge.map.rendertheme.RenderCallback;

/**
 * A RenderInstruction is a basic graphical primitive to draw a map.
 */
public abstract class RenderInstruction {

	private String category;

	public RenderInstruction(String category) {
		this.category = category;
	}

	public abstract void destroy();

	public String getCategory() {
		return this.category;
	}

	/**
	 * @param renderCallback
	 *            a reference to the receiver of all render callbacks.
	 * @param tags
	 *            the tags of the node.
	 */
	public abstract void renderNode(RenderCallback renderCallback, List<Tag> tags);

	/**
	 * @param renderCallback
	 *            a reference to the receiver of all render callbacks.
	 * @param tags
	 *            the tags of the way.
	 */
	public abstract void renderWay(RenderCallback renderCallback, List<Tag> tags);

	/**
	 * Scales the stroke width of this RenderInstruction by the given factor.
	 * 
	 * @param scaleFactor
	 *            the factor by which the stroke width should be scaled.
	 */
	public abstract void scaleStrokeWidth(float scaleFactor);

	/**
	 * Scales the text size of this RenderInstruction by the given factor.
	 * 
	 * @param scaleFactor
	 *            the factor by which the text size should be scaled.
	 */
	public abstract void scaleTextSize(float scaleFactor);
}
