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
package org.mapsforge.map.awt.graphics;

import org.mapsforge.core.graphics.Canvas;
import org.mapsforge.core.graphics.Display;
import org.mapsforge.core.graphics.Matrix;
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.mapelements.PointTextContainer;
import org.mapsforge.core.graphics.Position;
import org.mapsforge.core.mapelements.SymbolContainer;
import org.mapsforge.core.model.Point;
import org.mapsforge.core.model.Rectangle;

import java.awt.font.FontRenderContext;
import java.awt.font.LineBreakMeasurer;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;

public class AwtPointTextContainer extends PointTextContainer {

	AwtPointTextContainer(Point xy, Display display, int priority, String text, Paint paintFront, Paint paintBack,
	                      SymbolContainer symbolContainer, Position position, int maxTextWidth) {
		super(xy, display, priority, text, paintFront, paintBack, symbolContainer, position, maxTextWidth);

		this.boundary = computeBoundary();
	}

	@Override
	public void draw(Canvas canvas, Point origin, Matrix matrix) {

		if (this.paintFront.isTransparent() && (this.paintBack == null || this.paintBack.isTransparent())) {
			return;
		}

		AwtCanvas awtCanvas = (AwtCanvas) canvas;

		Point pointAdjusted = this.xy.offset(-origin.x, -origin.y);

		int textWidth = this.paintFront.getTextWidth(this.text);
		if (textWidth > maxTextWidth) {
			AttributedString attrString = new AttributedString(this.text);
			org.mapsforge.map.awt.graphics.AwtPaint awtPaintFront = org.mapsforge.map.awt.graphics.AwtGraphicFactory.getPaint(this.paintFront);
			attrString.addAttribute(TextAttribute.FOREGROUND, awtPaintFront.color);
			attrString.addAttribute(TextAttribute.FONT, awtPaintFront.font);
			AttributedCharacterIterator paragraph = attrString.getIterator();
			int paragraphStart = paragraph.getBeginIndex();
			int paragraphEnd = paragraph.getEndIndex();
			FontRenderContext frc = awtCanvas.getGraphicObject().getFontRenderContext();
			LineBreakMeasurer lineMeasurer = new LineBreakMeasurer(paragraph, frc);

			float layoutHeight = 0;
			lineMeasurer.setPosition(paragraphStart);
			while (lineMeasurer.getPosition() < paragraphEnd) {
				TextLayout layout = lineMeasurer.nextLayout(maxTextWidth);
				layoutHeight += layout.getAscent() + layout.getDescent() + layout.getLeading();
			}

			float drawPosY = (float) pointAdjusted.y;
			lineMeasurer.setPosition(paragraphStart);
			while (lineMeasurer.getPosition() < paragraphEnd) {
				TextLayout layout = lineMeasurer.nextLayout(maxTextWidth);
				float posX = (float) pointAdjusted.x;
				float posY = drawPosY;
				if (Position.CENTER == this.position) {
					posX += -layout.getAdvance() * 0.5f;
					posY += layout.getAscent() - layoutHeight * 0.5f;
				} else if (Position.BELOW == this.position) {
					posX += -layout.getAdvance() * 0.5f;
					posY += (layout.getAscent() + layout.getDescent() + layout.getLeading()) * 0.5f;
				} else if (Position.BELOW_LEFT == this.position) {
					posX += -layout.getAdvance();
					posY += (layout.getAscent() + layout.getDescent() + layout.getLeading()) * 0.5f;
				} else if (Position.BELOW_RIGHT == this.position) {
					posY += (layout.getAscent() + layout.getDescent() + layout.getLeading()) * 0.5f;
				} else if (Position.ABOVE == this.position) {
					posX += -layout.getAdvance() * 0.5f;
					posY += layout.getAscent() + layout.getDescent() + layout.getLeading() - layoutHeight;
				} else if (Position.ABOVE_LEFT == this.position) {
					posX += -layout.getAdvance();
					posY += layout.getAscent() + layout.getDescent() + layout.getLeading() - layoutHeight;
				} else if (Position.ABOVE_RIGHT == this.position) {
					posY += layout.getAscent() + layout.getDescent() + layout.getLeading() - layoutHeight;
				} else if (Position.LEFT == this.position) {
					posX += -layout.getAdvance();
					posY += layout.getAscent() - layoutHeight * 0.5f;
				} else if (Position.RIGHT == this.position) {
					posY += layout.getAscent() - layoutHeight * 0.5f;
				} else {
					throw new IllegalArgumentException("No position for drawing PointTextContainer");
				}
				if (this.paintBack != null) {
					awtCanvas.setColorAndStroke(org.mapsforge.map.awt.graphics.AwtGraphicFactory.getPaint(this.paintBack));
					AffineTransform affineTransform = new AffineTransform();
					affineTransform.translate(posX, posY);
					awtCanvas.getGraphicObject().draw(layout.getOutline(affineTransform));
				}
				layout.draw(awtCanvas.getGraphicObject(), posX, posY);
				drawPosY += layout.getAscent() + layout.getDescent() + layout.getLeading();
			}
		} else {
			if (this.paintBack != null) {
				canvas.drawText(this.text, (int) (pointAdjusted.x + boundary.left), (int) (pointAdjusted.y + boundary.top + this.textHeight), this.paintBack);
			}
			canvas.drawText(this.text, (int) (pointAdjusted.x + boundary.left), (int) (pointAdjusted.y + boundary.top + this.textHeight), this.paintFront);
		}


	}


	private Rectangle computeBoundary() {

		int lines = this.textWidth / maxTextWidth + 1;
		double boxWidth = this.textWidth;
		double boxHeight = this.textHeight;

		if (lines > 1) {
			// a crude approximation of the size of the text box
			boxWidth = maxTextWidth;
			boxHeight = this.textHeight * lines;
		}

		switch (this.position) {
			case CENTER:
				return new Rectangle(-boxWidth / 2f, -boxHeight / 2f, boxWidth / 2f, boxHeight / 2f);
			case BELOW:
				return new Rectangle(-boxWidth / 2f, 0, boxWidth / 2f, boxHeight);
			case BELOW_LEFT:
				return new Rectangle(-boxWidth, 0, 0, boxHeight);
			case BELOW_RIGHT:
				return new Rectangle(0, 0, boxWidth, boxHeight);
			case ABOVE:
				return new Rectangle(-boxWidth / 2f, -boxHeight, boxWidth / 2f, 0);
			case ABOVE_LEFT:
				return new Rectangle(-boxWidth, -boxHeight, 0, 0);
			case ABOVE_RIGHT:
				return new Rectangle(0, -boxHeight, boxWidth, 0);
			case LEFT:
				return new Rectangle(-boxWidth, -boxHeight / 2f, 0, boxHeight / 2f);
			case RIGHT:
				return new Rectangle(0, -boxHeight / 2f, boxWidth, boxHeight / 2f);
			default:
				break;
		}
		return null;
	}
}
