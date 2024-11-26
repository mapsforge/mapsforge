/*
 * Copyright 2014 Ludwig M Brinckmann
 * Copyright 2014-2016 devemux86
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
package org.mapsforge.map.android.graphics;

import android.graphics.RectF;
import android.graphics.text.LineBreaker;
import android.os.Build;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;

import org.mapsforge.core.graphics.Canvas;
import org.mapsforge.core.graphics.Display;
import org.mapsforge.core.graphics.Matrix;
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.graphics.Position;
import org.mapsforge.core.mapelements.PointTextContainer;
import org.mapsforge.core.mapelements.SymbolContainer;
import org.mapsforge.core.model.Point;
import org.mapsforge.core.model.Rectangle;
import org.mapsforge.core.model.Rotation;

public class AndroidPointTextContainer extends PointTextContainer {

    protected final Rectangle boundary;
    private StaticLayout backLayout;
    private StaticLayout frontLayout;
    public final int textHeight;
    public final int textWidth;
    public final boolean isMultiline;
    public final Rectangle textBounds;
    public final int fontPadding;

    protected final android.graphics.Paint debugClashBoundsPaint;

    AndroidPointTextContainer(Point xy, double horizontalOffset, double verticalOffset,
                              Display display, int priority, String text, Paint paintFront, Paint paintBack,
                              SymbolContainer symbolContainer, Position position, int maxTextWidth) {
        super(xy, horizontalOffset, verticalOffset, display, priority, text,
                paintFront, paintBack, symbolContainer, position, maxTextWidth);

        final Paint measurePaint;
        if (paintBack != null) {
            measurePaint = paintBack;
        } else {
            measurePaint = paintFront;
        }

        this.textBounds = measurePaint.getTextBounds(text);
        this.fontPadding = AndroidPaint.getFontPadding((int) Math.round(this.textBounds.getHeight()));

        final int myTextWidth = (int) Math.round(this.textBounds.getWidth()) + 2 * this.fontPadding;

        final float boxWidth, boxHeight;
        if (myTextWidth > this.maxTextWidth) {
            // if the text is too wide its layout is done by the Android StaticLayout class,
            // which automatically inserts line breaks. There is not a whole lot of useful
            // documentation of this class.
            // For below and above placements the text is center-aligned, for left on the right
            // and for right on the left.
            // One disadvantage is that it will always keep the text within the maxWidth,
            // even if that means breaking text mid-word.

            this.isMultiline = true;

            TextPaint frontTextPaint = new TextPaint(AndroidGraphicFactory.getPaint(this.paintFront));
            TextPaint backTextPaint = null;
            if (this.paintBack != null) {
                backTextPaint = new TextPaint(AndroidGraphicFactory.getPaint(this.paintBack));
            }

            // strange Android behaviour: if alignment is set to center, then
            // text is rendered with right alignment if using StaticLayout
            frontTextPaint.setTextAlign(android.graphics.Paint.Align.LEFT);
            if (backTextPaint != null) {
                backTextPaint.setTextAlign(android.graphics.Paint.Align.LEFT);
            }

            this.frontLayout = createTextLayout(frontTextPaint);
            if (backTextPaint != null) {
                this.backLayout = createTextLayout(backTextPaint);
            } else {
                this.backLayout = null;
            }

            this.textWidth = AndroidPaint.getTextWidth(backLayout != null ? backLayout : frontLayout);
            this.textHeight = (backLayout != null ? backLayout : frontLayout).getHeight();

            boxWidth = this.textWidth + 2 * this.fontPadding;
            boxHeight = this.textHeight + 2 * this.fontPadding;
        } else {
            this.isMultiline = false;

            this.textWidth = myTextWidth - 2 * this.fontPadding;
            this.textHeight = (int) Math.round(this.textBounds.getHeight());

            boxWidth = myTextWidth;
            boxHeight = this.textHeight + 2 * this.fontPadding;
        }

        switch (this.position) {
            case CENTER:
            default:
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

        if (DEBUG_CLASH_BOUNDS) {
            debugClashBoundsPaint = AndroidGraphicFactory.getPaint(AndroidGraphicFactory.INSTANCE.createPaint(measurePaint));
            debugClashBoundsPaint.setColor(android.graphics.Color.BLACK);
        } else {
            debugClashBoundsPaint = null;
        }
    }

    @SuppressWarnings("deprecation")
    private StaticLayout createTextLayout(TextPaint paint) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // API29+, A10+
            return StaticLayout.Builder
                    .obtain(this.text, 0, this.text.length(), paint, this.maxTextWidth)
                    .setBreakStrategy(LineBreaker.BREAK_STRATEGY_HIGH_QUALITY)
                    .setAlignment(Layout.Alignment.ALIGN_CENTER)
                    .setIncludePad(true)
                    .build();
        } else {
            return new StaticLayout(this.text, paint, this.maxTextWidth, Layout.Alignment.ALIGN_CENTER, 1, 0, true);
        }
    }

    @Override
    protected Rectangle getBoundary() {
        return boundary;
    }

    @Override
    public void draw(Canvas canvas, Point origin, Matrix matrix, Rotation rotation) {
        if (!this.isVisible) {
            return;
        }

        android.graphics.Canvas androidCanvas = AndroidGraphicFactory.getCanvas(canvas);

        final Point rotRelPosition = getRotatedRelativePosition(origin.x, origin.y, rotation);

        double x = rotRelPosition.x;
        double y = rotRelPosition.y;

        if (isMultiline) {
            // in this case we draw the precomputed staticLayout onto the canvas by translating
            // the canvas.

            androidCanvas.save();

            if (!Rotation.noRotation(rotation)) {
                androidCanvas.rotate(-rotation.degrees, rotation.px, rotation.py);
            }

            if (DEBUG_CLASH_BOUNDS) {
                drawClashBounds(origin.x, origin.y, rotation, androidCanvas);
            }

            x -= (this.maxTextWidth - boundary.getWidth()) / 2f;

            y += fontPadding;

            androidCanvas.translate((float) x, (float) y);

            if (this.backLayout != null) {
                this.backLayout.draw(androidCanvas);
            }
            this.frontLayout.draw(androidCanvas);

            androidCanvas.restore();
        } else {
            if (!Rotation.noRotation(rotation)) {
                androidCanvas.save();
                androidCanvas.rotate(-rotation.degrees, rotation.px, rotation.py);
            }

            if (DEBUG_CLASH_BOUNDS) {
                drawClashBounds(origin.x, origin.y, rotation, androidCanvas);
            }

            x += fontPadding - textBounds.left;

            y += boundary.getHeight();
            y += -fontPadding - textBounds.bottom;

            if (this.paintBack != null) {
                androidCanvas.drawText(this.text, (float) x, (float) y, AndroidGraphicFactory.getPaint(this.paintBack));
            }

            androidCanvas.drawText(this.text, (float) x, (float) y, AndroidGraphicFactory.getPaint(this.paintFront));

            if (!Rotation.noRotation(rotation)) {
                androidCanvas.restore();
            }
        }
    }

    protected void drawClashBounds(double originX, double originY, Rotation rotation, android.graphics.Canvas androidCanvas) {
        final Rectangle transformed = getClashRectangleTransformed(this, originX, originY, rotation);

        if (transformed != null) {
            androidCanvas.drawRect(new RectF((float) transformed.left, (float) transformed.top, (float) transformed.right, (float) transformed.bottom), debugClashBoundsPaint);
        }
    }
}
