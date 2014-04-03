/*
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

import org.mapsforge.core.graphics.Bitmap;
import org.mapsforge.core.graphics.Cap;
import org.mapsforge.core.graphics.CaptionPosition;
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.model.Tag;
import org.mapsforge.map.rendertheme.RenderCallback;

import java.util.List;
import java.util.logging.Logger;

/**
 * Represents a text label on the map.
 */
public class CaptionedSymbol extends RenderInstruction {

	private final Bitmap bitmap;
	private final CaptionPosition captionPosition;
	private final float dy;
	private final float gap;
	private final Paint fill;
	private final float fontSize;
	private final boolean renderCaptionless;
	private final Paint stroke;
	private final TextKey textKey;

	CaptionedSymbol(CaptionedSymbolBuilder captionedSymbolBuilder) {
		super(captionedSymbolBuilder.getCategory());
		this.bitmap = captionedSymbolBuilder.bitmap;
		this.captionPosition = captionedSymbolBuilder.captionPosition;
		this.gap = captionedSymbolBuilder.gap;
		this.dy = captionedSymbolBuilder.dy;
		this.fill = captionedSymbolBuilder.fill;
		this.fontSize = captionedSymbolBuilder.fontSize;
		this.renderCaptionless = captionedSymbolBuilder.renderCaptionless;
		this.stroke = captionedSymbolBuilder.stroke;
		this.textKey = captionedSymbolBuilder.textKey;
	}

	@Override
	public void destroy() {
		// no-op
	}

	@Override
	public void renderNode(RenderCallback renderCallback, List<Tag> tags) {
		String caption = this.textKey.getValue(tags);
		if (!renderCaptionless && caption == null) {
			return;
		}

		float horizontalOffset = 0f;
		float verticalOffset = this.dy;

		float textHeight = 0f;
		if (caption != null) {
			if (this.stroke != null) {
				textHeight = this.stroke.getTextHeight(caption);
			} else {
				textHeight = this.fill.getTextHeight(caption);
			}
		}

		if (CaptionPosition.RIGHT == this.captionPosition || CaptionPosition.LEFT == this.captionPosition) {
			float textWidth = 0f;
			if (caption != null) {
				if (this.stroke != null) {
					textWidth = this.stroke.getTextWidth(caption) / 2f;
				} else {
					textWidth = this.fill.getTextWidth(caption) / 2f;
				}
			}
			horizontalOffset = this.bitmap.getWidth() / 2f + this.gap + textWidth;
			if (CaptionPosition.LEFT == this.captionPosition) {
				horizontalOffset *= -1f;
			}
			verticalOffset = textHeight / 2f;
		} else if (CaptionPosition.ABOVE == this.captionPosition) {
			verticalOffset -= this.bitmap.getHeight() / 2f + this.gap;
		} else if (CaptionPosition.BELOW == this.captionPosition) {
			verticalOffset += this.bitmap.getHeight() / 2f + this.gap + textHeight;
		}

		renderCallback.renderPointOfInterestCaption(caption, horizontalOffset, verticalOffset, this.fill, this.stroke);
		renderCallback.renderPointOfInterestSymbol(this.bitmap);
	}

	@Override
	public void renderWay(RenderCallback renderCallback, List<Tag> tags) {
		// do nothing
	}

	@Override
	public void scaleStrokeWidth(float scaleFactor) {
		// do nothing
	}

	@Override
	public void scaleTextSize(float scaleFactor) {
		this.fill.setTextSize(this.fontSize * scaleFactor);
		this.stroke.setTextSize(this.fontSize * scaleFactor);
	}

}
