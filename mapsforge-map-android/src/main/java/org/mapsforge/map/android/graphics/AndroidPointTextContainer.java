/*
 * Copyright 2014 Ludwig M Brinckmann
 * Copyright 2014 devemux86
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
package org.mapsforge.map.android.graphics;

import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;

import org.mapsforge.core.graphics.Canvas;
import org.mapsforge.core.graphics.Matrix;
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.mapelements.PointTextContainer;
import org.mapsforge.core.graphics.Position;
import org.mapsforge.core.mapelements.SymbolContainer;
import org.mapsforge.core.model.Point;
import org.mapsforge.core.model.Rectangle;

public class AndroidPointTextContainer extends PointTextContainer {

	private StaticLayout frontLayout;
	private StaticLayout backLayout;
	private Rectangle debugBoundary;
	private float boxWidth;
	private float boxHeight;

	AndroidPointTextContainer(Point xy, int priority, String text, Paint paintFront, Paint paintBack,
	                          SymbolContainer symbolContainer, Position position, int maxTextWidth) {
		super(xy, priority, text, paintFront, paintBack, symbolContainer, position, maxTextWidth);

		if (this.textWidth > this.maxTextWidth) {

			// if the text is too wide its layout is done by the Android StaticLayout class,
			// which automagically inserts line breaks. There is not a whole lot of useful
			// documentation of this class.
			// For below and above placements the text is center-aligned, for left on the right
			// and for right on the left.
			// One disadvantage is that it will always keep the text within the maxWidth,
			// even if that means breaking text mid-word.
			// This code currently does not play that well with the LabelPlacement algorithm.
			// The best way to disable it is to make the maxWidth really wide.

			TextPaint frontTextPaint = new TextPaint(AndroidGraphicFactory.getPaint(this.paintFront));
			TextPaint backTextPaint = null;
			if (this.paintBack != null) {
				backTextPaint = new TextPaint(AndroidGraphicFactory.getPaint(this.paintBack));
			}

			Layout.Alignment alignment = Layout.Alignment.ALIGN_CENTER;
			if (Position.LEFT == this.position
					|| Position.BELOW_LEFT == this.position
					|| Position.ABOVE_LEFT == this.position) {
				alignment = Layout.Alignment.ALIGN_OPPOSITE;
			} else if (Position.RIGHT == this.position
					|| Position.BELOW_RIGHT == this.position
					|| Position.ABOVE_RIGHT == this.position) {
				alignment = Layout.Alignment.ALIGN_NORMAL;
			}

			// strange Android behaviour: if alignment is set to center, then
			// text is rendered with right alignment if using StaticLayout
			frontTextPaint.setTextAlign(android.graphics.Paint.Align.LEFT);
			backTextPaint.setTextAlign(android.graphics.Paint.Align.LEFT);

			frontLayout = new StaticLayout(this.text, frontTextPaint, this.maxTextWidth, alignment, 1, 0, false);
			backLayout = null;
			if (this.paintBack != null) {
				backLayout = new StaticLayout(this.text, backTextPaint, this.maxTextWidth, alignment, 1, 0, false);
			}

			this.boxWidth = frontLayout.getWidth();
			this.boxHeight = frontLayout.getHeight();

		} else {
			this.boxWidth = textWidth;
			this.boxHeight = textHeight;
		}

		switch (this.position) {
			case CENTER:
				boundary = new Rectangle(-boxWidth / 2f, -boxHeight / 2f, boxWidth / 2f, boxHeight / 2f);
				break;
			case BELOW:
				boundary = new Rectangle(-boxWidth / 2f, 0, boxWidth / 2f, boxHeight);
				break;
			case BELOW_LEFT:
				boundary = new Rectangle(-boxWidth, 0, 0, boxHeight);
				break;
			case BELOW_RIGHT:
				boundary = new Rectangle(0, 0, boxWidth, boxHeight);
				break;
			case ABOVE:
				boundary = new Rectangle(-boxWidth / 2f, -boxHeight, boxWidth / 2f, 0);
				break;
			case ABOVE_LEFT:
				boundary = new Rectangle(-boxWidth, -boxHeight, 0, 0);
				break;
			case ABOVE_RIGHT:
				boundary = new Rectangle(0, -boxHeight, boxWidth, 0);
				break;
			case LEFT:
				boundary = new Rectangle(-boxWidth, -boxHeight / 2f, 0, boxHeight / 2f);
				break;
			case RIGHT:
				boundary = new Rectangle(0, -boxHeight / 2f, boxWidth, boxHeight / 2f);
				break;
		}
		// debug
		switch (this.position) {
			case CENTER:
				debugBoundary = new Rectangle(-textWidth / 2f, -textHeight / 2f, textWidth / 2f, textHeight / 2f);
				break;
			case BELOW:
				debugBoundary = new Rectangle(-textWidth / 2f, 0, textWidth / 2f, textHeight);
				break;
			case BELOW_LEFT:
				debugBoundary = new Rectangle(-textWidth, 0, 0, textHeight);
				break;
			case BELOW_RIGHT:
				debugBoundary = new Rectangle(0, 0, textWidth, textHeight);
				break;
			case ABOVE:
				debugBoundary = new Rectangle(-textWidth / 2f, -textHeight, textWidth / 2f, 0);
				break;
			case ABOVE_LEFT:
				debugBoundary = new Rectangle(-textWidth, -textHeight, 0, 0);
				break;
			case ABOVE_RIGHT:
				debugBoundary = new Rectangle(0, -textHeight, textWidth, 0);
				break;
			case LEFT:
				debugBoundary = new Rectangle(-textWidth, -textHeight / 2f, 0, textHeight / 2f);
				break;
			case RIGHT:
				debugBoundary = new Rectangle(0, -textHeight / 2f, textWidth, textHeight / 2f);
				break;
		}

	}

	@Override
	public void draw(Canvas canvas, Point origin, Matrix matrix) {
		if (!this.isVisible) {
			return;
		}

		android.graphics.Canvas androidCanvas = AndroidGraphicFactory.getCanvas(canvas);

		if (this.textWidth > this.maxTextWidth) {
			// in this case we draw the precomputed staticLayout onto the canvas by translating
			// the canvas.
			androidCanvas.save();
			androidCanvas.translate((float) (this.xy.x - origin.x + boundary.left), (float) (this.xy.y - origin.y + boundary.top));

			if (this.backLayout != null) {
				this.backLayout.draw(androidCanvas);
			}
			this.frontLayout.draw(androidCanvas);
			//debugDrawBounds(androidCanvas, (float) -boundary.left, (float) -boundary.top);
			androidCanvas.restore();
		} else {
			// the origin of the text is the base line, so we need to make adjustments
			// so that the text will be within its box
			float textOffset = 0;
			switch (this.position) {
				case CENTER:
				case LEFT:
				case RIGHT:
					textOffset = textHeight / 2f;
					break;
				case BELOW:
				case BELOW_LEFT:
				case BELOW_RIGHT:
					textOffset = textHeight;
					break;
			}

			float adjustedX = (float) (this.xy.x - origin.x);
			float adjustedY = (float) (this.xy.y - origin.y) + textOffset;

			if (this.paintBack != null) {
				androidCanvas.drawText(this.text, adjustedX, adjustedY, AndroidGraphicFactory.getPaint(this.paintBack));
			}
			androidCanvas.drawText(this.text, adjustedX, adjustedY, AndroidGraphicFactory.getPaint(this.paintFront));
			//debugDrawBounds(androidCanvas, adjustedX, adjustedY - textOffset);
		}
	}


	private void debugDrawBounds(android.graphics.Canvas androidCanvas, float offsetX, float offsetY) {
		androidCanvas.drawLine((float) (offsetX + boundary.left), (float) (offsetY + boundary.top), (float) (offsetX + boundary.right), (float) (offsetY + boundary.top), AndroidGraphicFactory.getPaint(this.paintFront));
		androidCanvas.drawLine((float) (offsetX + boundary.right), (float) (offsetY + boundary.top), (float) (offsetX + boundary.right), (float) (offsetY + boundary.bottom), AndroidGraphicFactory.getPaint(this.paintFront));
		androidCanvas.drawLine((float) (offsetX + boundary.right), (float) (offsetY + boundary.bottom), (float) (offsetX + boundary.left), (float) (offsetY + boundary.bottom), AndroidGraphicFactory.getPaint(this.paintFront));
		androidCanvas.drawLine((float) (offsetX + boundary.left), (float) (offsetY + boundary.bottom), (float) (offsetX + boundary.left), (float) (offsetY + boundary.top), AndroidGraphicFactory.getPaint(this.paintFront));

		androidCanvas.drawLine((float) (offsetX + debugBoundary.left), (float) (offsetY +debugBoundary.top), (float) (offsetX +debugBoundary.right), (float) (offsetY +debugBoundary.top), AndroidGraphicFactory.getPaint(this.paintFront));
		androidCanvas.drawLine((float) (offsetX +debugBoundary.right), (float) (offsetY +debugBoundary.top), (float) (offsetX +debugBoundary.right), (float) (offsetY +debugBoundary.bottom), AndroidGraphicFactory.getPaint(this.paintFront));
		androidCanvas.drawLine((float) (offsetX +debugBoundary.right), (float) (offsetY +debugBoundary.bottom), (float) (offsetX +debugBoundary.left), (float) (offsetY +debugBoundary.bottom), AndroidGraphicFactory.getPaint(this.paintFront));
		androidCanvas.drawLine((float) (offsetX +debugBoundary.left), (float) (offsetY +debugBoundary.bottom), (float) (offsetX +debugBoundary.left), (float) (offsetY +debugBoundary.top), AndroidGraphicFactory.getPaint(this.paintFront));
	}

}