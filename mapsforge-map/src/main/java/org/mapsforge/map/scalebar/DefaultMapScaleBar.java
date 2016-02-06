/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright 2014 Ludwig M Brinckmann
 * Copyright 2014, 2015 devemux86
 * Copyright 2014 Erik Duisters
 * Copyright 2014 Christian Pesch
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
package org.mapsforge.map.scalebar;

import org.mapsforge.core.graphics.Canvas;
import org.mapsforge.core.graphics.Cap;
import org.mapsforge.core.graphics.Color;
import org.mapsforge.core.graphics.FontFamily;
import org.mapsforge.core.graphics.FontStyle;
import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.graphics.Style;
import org.mapsforge.map.model.DisplayModel;
import org.mapsforge.map.model.MapViewDimension;
import org.mapsforge.map.model.MapViewPosition;

/**
 * Displays the default mapsforge MapScaleBar
 */
public class DefaultMapScaleBar extends MapScaleBar {
    private static final int BITMAP_HEIGHT = 40;
    private static final int BITMAP_WIDTH = 120;
    private static final int SCALE_BAR_MARGIN = 10;
    private static final float STROKE_EXTERNAL = 4;
    private static final float STROKE_INTERNAL = 2;
    private static final int TEXT_MARGIN = 1;

    public static enum ScaleBarMode {BOTH, SINGLE}

    private ScaleBarMode scaleBarMode;
    private DistanceUnitAdapter secondaryDistanceUnitAdapter;

    private final Paint paintScaleBar;
    private final Paint paintScaleBarStroke;
    private final Paint paintScaleText;
    private final Paint paintScaleTextStroke;

    public DefaultMapScaleBar(MapViewPosition mapViewPosition, MapViewDimension mapViewDimension,
                              GraphicFactory graphicFactory, DisplayModel displayModel) {
        super(mapViewPosition, mapViewDimension, displayModel, graphicFactory, BITMAP_WIDTH, BITMAP_HEIGHT);

        this.scaleBarMode = ScaleBarMode.BOTH;
        this.secondaryDistanceUnitAdapter = ImperialUnitAdapter.INSTANCE;

        this.paintScaleBar = createScaleBarPaint(Color.BLACK, STROKE_INTERNAL, Style.FILL);
        this.paintScaleBarStroke = createScaleBarPaint(Color.WHITE, STROKE_EXTERNAL, Style.STROKE);
        this.paintScaleText = createTextPaint(Color.BLACK, 0, Style.FILL);
        this.paintScaleTextStroke = createTextPaint(Color.WHITE, 2, Style.STROKE);
    }

    /**
     * @return the secondary {@link DistanceUnitAdapter} in use by this MapScaleBar
     */
    public DistanceUnitAdapter getSecondaryDistanceUnitAdapter() {
        return this.secondaryDistanceUnitAdapter;
    }

    /**
     * Set the secondary {@link DistanceUnitAdapter} for the MapScaleBar
     *
     * @param distanceUnitAdapter The secondary {@link DistanceUnitAdapter} to be used by this {@link MapScaleBar}
     */
    public void setSecondaryDistanceUnitAdapter(DistanceUnitAdapter distanceUnitAdapter) {
        if (distanceUnitAdapter == null) {
            throw new IllegalArgumentException("adapter must not be null");
        }
        this.secondaryDistanceUnitAdapter = distanceUnitAdapter;
        this.redrawNeeded = true;
    }

    public ScaleBarMode getScaleBarMode() {
        return this.scaleBarMode;
    }

    public void setScaleBarMode(ScaleBarMode scaleBarMode) {
        this.scaleBarMode = scaleBarMode;
        this.redrawNeeded = true;
    }

    private Paint createScaleBarPaint(Color color, float strokeWidth, Style style) {
        Paint paint = this.graphicFactory.createPaint();
        paint.setColor(color);
        paint.setStrokeWidth(strokeWidth * this.displayModel.getScaleFactor());
        paint.setStyle(style);
        paint.setStrokeCap(Cap.SQUARE);
        return paint;
    }

    private Paint createTextPaint(Color color, float strokeWidth, Style style) {
        Paint paint = this.graphicFactory.createPaint();
        paint.setColor(color);
        paint.setStrokeWidth(strokeWidth * this.displayModel.getScaleFactor());
        paint.setStyle(style);
        paint.setTypeface(FontFamily.DEFAULT, FontStyle.BOLD);
        paint.setTextSize(12 * this.displayModel.getScaleFactor());

        return paint;
    }

    @Override
    protected void redraw(Canvas canvas) {
        canvas.fillColor(Color.TRANSPARENT);

        float scale = this.displayModel.getScaleFactor();
        ScaleBarLengthAndValue lengthAndValue = this.calculateScaleBarLengthAndValue();
        ScaleBarLengthAndValue lengthAndValue2;

        if (this.scaleBarMode == ScaleBarMode.BOTH) {
            lengthAndValue2 = this.calculateScaleBarLengthAndValue(this.secondaryDistanceUnitAdapter);
        } else {
            lengthAndValue2 = new ScaleBarLengthAndValue(0, 0);
        }

        drawScaleBar(canvas, lengthAndValue.scaleBarLength, lengthAndValue2.scaleBarLength, this.paintScaleBarStroke, scale);
        drawScaleBar(canvas, lengthAndValue.scaleBarLength, lengthAndValue2.scaleBarLength, this.paintScaleBar, scale);

        String scaleText1 = this.distanceUnitAdapter.getScaleText(lengthAndValue.scaleBarValue);
        String scaleText2 = this.scaleBarMode == ScaleBarMode.BOTH ? this.secondaryDistanceUnitAdapter.getScaleText(lengthAndValue2.scaleBarValue) : "";

        drawScaleText(canvas, scaleText1, scaleText2, this.paintScaleTextStroke, scale);
        drawScaleText(canvas, scaleText1, scaleText2, this.paintScaleText, scale);
    }

    private void drawScaleBar(Canvas canvas, int scaleBarLength1, int scaleBarLength2, Paint paint, float scale) {
        int maxScaleBarLength = Math.max(scaleBarLength1, scaleBarLength2);

        switch (scaleBarPosition) {
            case BOTTOM_CENTER:
                if (scaleBarLength2 == 0) {
                    canvas.drawLine(Math.round((canvas.getWidth() - maxScaleBarLength) * 0.5f), Math.round(canvas.getHeight() - SCALE_BAR_MARGIN * scale),
                            Math.round((canvas.getWidth() + maxScaleBarLength) * 0.5f), Math.round(canvas.getHeight() - SCALE_BAR_MARGIN * scale), paint);
                    canvas.drawLine(Math.round((canvas.getWidth() - maxScaleBarLength) * 0.5f), Math.round(canvas.getHeight() * 0.5f),
                            Math.round((canvas.getWidth() - maxScaleBarLength) * 0.5f), Math.round(canvas.getHeight() - SCALE_BAR_MARGIN * scale), paint);
                    canvas.drawLine(Math.round((canvas.getWidth() + maxScaleBarLength) * 0.5f), Math.round(canvas.getHeight() * 0.5f),
                            Math.round((canvas.getWidth() + maxScaleBarLength) * 0.5f), Math.round(canvas.getHeight() - SCALE_BAR_MARGIN * scale), paint);
                } else {
                    canvas.drawLine(Math.round(STROKE_EXTERNAL * scale * 0.5f), Math.round(canvas.getHeight() * 0.5f),
                            Math.round(STROKE_EXTERNAL * scale * 0.5f + maxScaleBarLength), Math.round(canvas.getHeight() * 0.5f), paint);
                    canvas.drawLine(Math.round(STROKE_EXTERNAL * scale * 0.5f), Math.round(SCALE_BAR_MARGIN * scale),
                            Math.round(STROKE_EXTERNAL * scale * 0.5f), Math.round(canvas.getHeight() - SCALE_BAR_MARGIN * scale), paint);
                    canvas.drawLine(Math.round(STROKE_EXTERNAL * scale * 0.5f + scaleBarLength1), Math.round(SCALE_BAR_MARGIN * scale),
                            Math.round(STROKE_EXTERNAL * scale * 0.5f + scaleBarLength1), Math.round(canvas.getHeight() * 0.5f), paint);
                    canvas.drawLine(Math.round(STROKE_EXTERNAL * scale * 0.5f + scaleBarLength2), Math.round(canvas.getHeight() * 0.5f),
                            Math.round(STROKE_EXTERNAL * scale * 0.5f + scaleBarLength2), Math.round(canvas.getHeight() - SCALE_BAR_MARGIN * scale), paint);
                }
                break;
            case BOTTOM_LEFT:
                if (scaleBarLength2 == 0) {
                    canvas.drawLine(Math.round(STROKE_EXTERNAL * scale * 0.5f), Math.round(canvas.getHeight() - SCALE_BAR_MARGIN * scale),
                            Math.round(STROKE_EXTERNAL * scale * 0.5f + maxScaleBarLength), Math.round(canvas.getHeight() - SCALE_BAR_MARGIN * scale), paint);
                    canvas.drawLine(Math.round(STROKE_EXTERNAL * scale * 0.5f), Math.round(canvas.getHeight() * 0.5f),
                            Math.round(STROKE_EXTERNAL * scale * 0.5f), Math.round(canvas.getHeight() - SCALE_BAR_MARGIN * scale), paint);
                    canvas.drawLine(Math.round(STROKE_EXTERNAL * scale * 0.5f + maxScaleBarLength), Math.round(canvas.getHeight() * 0.5f),
                            Math.round(STROKE_EXTERNAL * scale * 0.5f + maxScaleBarLength), Math.round(canvas.getHeight() - SCALE_BAR_MARGIN * scale), paint);
                } else {
                    canvas.drawLine(Math.round(STROKE_EXTERNAL * scale * 0.5f), Math.round(canvas.getHeight() * 0.5f),
                            Math.round(STROKE_EXTERNAL * scale * 0.5f + maxScaleBarLength), Math.round(canvas.getHeight() * 0.5f), paint);
                    canvas.drawLine(Math.round(STROKE_EXTERNAL * scale * 0.5f), Math.round(SCALE_BAR_MARGIN * scale),
                            Math.round(STROKE_EXTERNAL * scale * 0.5f), Math.round(canvas.getHeight() - SCALE_BAR_MARGIN * scale), paint);
                    canvas.drawLine(Math.round(STROKE_EXTERNAL * scale * 0.5f + scaleBarLength1), Math.round(SCALE_BAR_MARGIN * scale),
                            Math.round(STROKE_EXTERNAL * scale * 0.5f + scaleBarLength1), Math.round(canvas.getHeight() * 0.5f), paint);
                    canvas.drawLine(Math.round(STROKE_EXTERNAL * scale * 0.5f + scaleBarLength2), Math.round(canvas.getHeight() * 0.5f),
                            Math.round(STROKE_EXTERNAL * scale * 0.5f + scaleBarLength2), Math.round(canvas.getHeight() - SCALE_BAR_MARGIN * scale), paint);
                }
                break;
            case BOTTOM_RIGHT:
                if (scaleBarLength2 == 0) {
                    canvas.drawLine(Math.round(canvas.getWidth() - STROKE_EXTERNAL * scale * 0.5f - maxScaleBarLength), Math.round(canvas.getHeight() - SCALE_BAR_MARGIN * scale),
                            Math.round(canvas.getWidth() - STROKE_EXTERNAL * scale * 0.5f), Math.round(canvas.getHeight() - SCALE_BAR_MARGIN * scale), paint);
                    canvas.drawLine(Math.round(canvas.getWidth() - STROKE_EXTERNAL * scale * 0.5f), Math.round(canvas.getHeight() * 0.5f),
                            Math.round(canvas.getWidth() - STROKE_EXTERNAL * scale * 0.5f), Math.round(canvas.getHeight() - SCALE_BAR_MARGIN * scale), paint);
                    canvas.drawLine(Math.round(canvas.getWidth() - STROKE_EXTERNAL * scale * 0.5f - maxScaleBarLength), Math.round(canvas.getHeight() * 0.5f),
                            Math.round(canvas.getWidth() - STROKE_EXTERNAL * scale * 0.5f - maxScaleBarLength), Math.round(canvas.getHeight() - SCALE_BAR_MARGIN * scale), paint);
                } else {
                    canvas.drawLine(Math.round(canvas.getWidth() - STROKE_EXTERNAL * scale * 0.5f), Math.round(canvas.getHeight() * 0.5f),
                            Math.round(canvas.getWidth() - STROKE_EXTERNAL * scale * 0.5f - maxScaleBarLength), Math.round(canvas.getHeight() * 0.5f), paint);
                    canvas.drawLine(Math.round(canvas.getWidth() - STROKE_EXTERNAL * scale * 0.5f), Math.round(SCALE_BAR_MARGIN * scale),
                            Math.round(canvas.getWidth() - STROKE_EXTERNAL * scale * 0.5f), Math.round(canvas.getHeight() - SCALE_BAR_MARGIN * scale), paint);
                    canvas.drawLine(Math.round(canvas.getWidth() - STROKE_EXTERNAL * scale * 0.5f - scaleBarLength1), Math.round(SCALE_BAR_MARGIN * scale),
                            Math.round(canvas.getWidth() - STROKE_EXTERNAL * scale * 0.5f - scaleBarLength1), Math.round(canvas.getHeight() * 0.5f), paint);
                    canvas.drawLine(Math.round(canvas.getWidth() - STROKE_EXTERNAL * scale * 0.5f - scaleBarLength2), Math.round(canvas.getHeight() * 0.5f),
                            Math.round(canvas.getWidth() - STROKE_EXTERNAL * scale * 0.5f - scaleBarLength2), Math.round(canvas.getHeight() - SCALE_BAR_MARGIN * scale), paint);
                }
                break;
            case TOP_CENTER:
                if (scaleBarLength2 == 0) {
                    canvas.drawLine(Math.round((canvas.getWidth() - maxScaleBarLength) * 0.5f), Math.round(SCALE_BAR_MARGIN * scale),
                            Math.round((canvas.getWidth() + maxScaleBarLength) * 0.5f), Math.round(SCALE_BAR_MARGIN * scale), paint);
                    canvas.drawLine(Math.round((canvas.getWidth() - maxScaleBarLength) * 0.5f), Math.round(SCALE_BAR_MARGIN * scale),
                            Math.round((canvas.getWidth() - maxScaleBarLength) * 0.5f), Math.round(canvas.getHeight() * 0.5f), paint);
                    canvas.drawLine(Math.round((canvas.getWidth() + maxScaleBarLength) * 0.5f), Math.round(SCALE_BAR_MARGIN * scale),
                            Math.round((canvas.getWidth() + maxScaleBarLength) * 0.5f), Math.round(canvas.getHeight() * 0.5f), paint);
                } else {
                    canvas.drawLine(Math.round(STROKE_EXTERNAL * scale * 0.5f), Math.round(canvas.getHeight() * 0.5f),
                            Math.round(STROKE_EXTERNAL * scale * 0.5f + maxScaleBarLength), Math.round(canvas.getHeight() * 0.5f), paint);
                    canvas.drawLine(Math.round(STROKE_EXTERNAL * scale * 0.5f), Math.round(SCALE_BAR_MARGIN * scale),
                            Math.round(STROKE_EXTERNAL * scale * 0.5f), Math.round(canvas.getHeight() - SCALE_BAR_MARGIN * scale), paint);
                    canvas.drawLine(Math.round(STROKE_EXTERNAL * scale * 0.5f + scaleBarLength1), Math.round(SCALE_BAR_MARGIN * scale),
                            Math.round(STROKE_EXTERNAL * scale * 0.5f + scaleBarLength1), Math.round(canvas.getHeight() * 0.5f), paint);
                    canvas.drawLine(Math.round(STROKE_EXTERNAL * scale * 0.5f + scaleBarLength2), Math.round(canvas.getHeight() * 0.5f),
                            Math.round(STROKE_EXTERNAL * scale * 0.5f + scaleBarLength2), Math.round(canvas.getHeight() - SCALE_BAR_MARGIN * scale), paint);
                }
                break;
            case TOP_LEFT:
                if (scaleBarLength2 == 0) {
                    canvas.drawLine(Math.round(STROKE_EXTERNAL * scale * 0.5f), Math.round(SCALE_BAR_MARGIN * scale),
                            Math.round(STROKE_EXTERNAL * scale * 0.5f + maxScaleBarLength), Math.round(SCALE_BAR_MARGIN * scale), paint);
                    canvas.drawLine(Math.round(STROKE_EXTERNAL * scale * 0.5f), Math.round(SCALE_BAR_MARGIN * scale),
                            Math.round(STROKE_EXTERNAL * scale * 0.5f), Math.round(canvas.getHeight() * 0.5f), paint);
                    canvas.drawLine(Math.round(STROKE_EXTERNAL * scale * 0.5f + maxScaleBarLength), Math.round(SCALE_BAR_MARGIN * scale),
                            Math.round(STROKE_EXTERNAL * scale * 0.5f + maxScaleBarLength), Math.round(canvas.getHeight() * 0.5f), paint);
                } else {
                    canvas.drawLine(Math.round(STROKE_EXTERNAL * scale * 0.5f), Math.round(canvas.getHeight() * 0.5f),
                            Math.round(STROKE_EXTERNAL * scale * 0.5f + maxScaleBarLength), Math.round(canvas.getHeight() * 0.5f), paint);
                    canvas.drawLine(Math.round(STROKE_EXTERNAL * scale * 0.5f), Math.round(SCALE_BAR_MARGIN * scale),
                            Math.round(STROKE_EXTERNAL * scale * 0.5f), Math.round(canvas.getHeight() - SCALE_BAR_MARGIN * scale), paint);
                    canvas.drawLine(Math.round(STROKE_EXTERNAL * scale * 0.5f + scaleBarLength1), Math.round(SCALE_BAR_MARGIN * scale),
                            Math.round(STROKE_EXTERNAL * scale * 0.5f + scaleBarLength1), Math.round(canvas.getHeight() * 0.5f), paint);
                    canvas.drawLine(Math.round(STROKE_EXTERNAL * scale * 0.5f + scaleBarLength2), Math.round(canvas.getHeight() * 0.5f),
                            Math.round(STROKE_EXTERNAL * scale * 0.5f + scaleBarLength2), Math.round(canvas.getHeight() - SCALE_BAR_MARGIN * scale), paint);
                }
                break;
            case TOP_RIGHT:
                if (scaleBarLength2 == 0) {
                    canvas.drawLine(Math.round(canvas.getWidth() - STROKE_EXTERNAL * scale * 0.5f - maxScaleBarLength), Math.round(SCALE_BAR_MARGIN * scale),
                            Math.round(canvas.getWidth() - STROKE_EXTERNAL * scale * 0.5f), Math.round(SCALE_BAR_MARGIN * scale), paint);
                    canvas.drawLine(Math.round(canvas.getWidth() - STROKE_EXTERNAL * scale * 0.5f), Math.round(SCALE_BAR_MARGIN * scale),
                            Math.round(canvas.getWidth() - STROKE_EXTERNAL * scale * 0.5f), Math.round(canvas.getHeight() * 0.5f), paint);
                    canvas.drawLine(Math.round(canvas.getWidth() - STROKE_EXTERNAL * scale * 0.5f - maxScaleBarLength), Math.round(SCALE_BAR_MARGIN * scale),
                            Math.round(canvas.getWidth() - STROKE_EXTERNAL * scale * 0.5f - maxScaleBarLength), Math.round(canvas.getHeight() * 0.5f), paint);
                } else {
                    canvas.drawLine(Math.round(canvas.getWidth() - STROKE_EXTERNAL * scale * 0.5f), Math.round(canvas.getHeight() * 0.5f),
                            Math.round(canvas.getWidth() - STROKE_EXTERNAL * scale * 0.5f - maxScaleBarLength), Math.round(canvas.getHeight() * 0.5f), paint);
                    canvas.drawLine(Math.round(canvas.getWidth() - STROKE_EXTERNAL * scale * 0.5f), Math.round(SCALE_BAR_MARGIN * scale),
                            Math.round(canvas.getWidth() - STROKE_EXTERNAL * scale * 0.5f), Math.round(canvas.getHeight() - SCALE_BAR_MARGIN * scale), paint);
                    canvas.drawLine(Math.round(canvas.getWidth() - STROKE_EXTERNAL * scale * 0.5f - scaleBarLength1), Math.round(SCALE_BAR_MARGIN * scale),
                            Math.round(canvas.getWidth() - STROKE_EXTERNAL * scale * 0.5f - scaleBarLength1), Math.round(canvas.getHeight() * 0.5f), paint);
                    canvas.drawLine(Math.round(canvas.getWidth() - STROKE_EXTERNAL * scale * 0.5f - scaleBarLength2), Math.round(canvas.getHeight() * 0.5f),
                            Math.round(canvas.getWidth() - STROKE_EXTERNAL * scale * 0.5f - scaleBarLength2), Math.round(canvas.getHeight() - SCALE_BAR_MARGIN * scale), paint);
                }
                break;
        }
    }

    private void drawScaleText(Canvas canvas, String scaleText1, String scaleText2, Paint paint, float scale) {
        switch (scaleBarPosition) {
            case BOTTOM_CENTER:
                if (scaleText2.length() == 0) {
                    canvas.drawText(scaleText1, Math.round((canvas.getWidth() - this.paintScaleTextStroke.getTextWidth(scaleText1)) * 0.5f),
                            Math.round(canvas.getHeight() - SCALE_BAR_MARGIN * scale - STROKE_EXTERNAL * scale * 0.5f - TEXT_MARGIN * scale), paint);
                } else {
                    canvas.drawText(scaleText1, Math.round(STROKE_EXTERNAL * scale + TEXT_MARGIN * scale),
                            Math.round(canvas.getHeight() * 0.5f - STROKE_EXTERNAL * scale * 0.5f - TEXT_MARGIN * scale), paint);
                    canvas.drawText(scaleText2, Math.round(STROKE_EXTERNAL * scale + TEXT_MARGIN * scale),
                            Math.round(canvas.getHeight() * 0.5f + STROKE_EXTERNAL * scale * 0.5f + TEXT_MARGIN * scale + this.paintScaleTextStroke.getTextHeight(scaleText2)), paint);
                }
                break;
            case BOTTOM_LEFT:
                if (scaleText2.length() == 0) {
                    canvas.drawText(scaleText1, Math.round(STROKE_EXTERNAL * scale + TEXT_MARGIN * scale),
                            Math.round(canvas.getHeight() - SCALE_BAR_MARGIN * scale - STROKE_EXTERNAL * scale * 0.5f - TEXT_MARGIN * scale), paint);
                } else {
                    canvas.drawText(scaleText1, Math.round(STROKE_EXTERNAL * scale + TEXT_MARGIN * scale),
                            Math.round(canvas.getHeight() * 0.5f - STROKE_EXTERNAL * scale * 0.5f - TEXT_MARGIN * scale), paint);
                    canvas.drawText(scaleText2, Math.round(STROKE_EXTERNAL * scale + TEXT_MARGIN * scale),
                            Math.round(canvas.getHeight() * 0.5f + STROKE_EXTERNAL * scale * 0.5f + TEXT_MARGIN * scale + this.paintScaleTextStroke.getTextHeight(scaleText2)), paint);
                }
                break;
            case BOTTOM_RIGHT:
                if (scaleText2.length() == 0) {
                    canvas.drawText(scaleText1, Math.round(canvas.getWidth() - STROKE_EXTERNAL * scale - TEXT_MARGIN * scale - this.paintScaleTextStroke.getTextWidth(scaleText1)),
                            Math.round(canvas.getHeight() - SCALE_BAR_MARGIN * scale - STROKE_EXTERNAL * scale * 0.5f - TEXT_MARGIN * scale), paint);
                } else {
                    canvas.drawText(scaleText1, Math.round(canvas.getWidth() - STROKE_EXTERNAL * scale - TEXT_MARGIN * scale - this.paintScaleTextStroke.getTextWidth(scaleText1)),
                            Math.round(canvas.getHeight() * 0.5f - STROKE_EXTERNAL * scale * 0.5f - TEXT_MARGIN * scale), paint);
                    canvas.drawText(scaleText2, Math.round(canvas.getWidth() - STROKE_EXTERNAL * scale - TEXT_MARGIN * scale - this.paintScaleTextStroke.getTextWidth(scaleText2)),
                            Math.round(canvas.getHeight() * 0.5f + STROKE_EXTERNAL * scale * 0.5f + TEXT_MARGIN * scale + this.paintScaleTextStroke.getTextHeight(scaleText2)), paint);
                }
                break;
            case TOP_CENTER:
                if (scaleText2.length() == 0) {
                    canvas.drawText(scaleText1, Math.round((canvas.getWidth() - this.paintScaleTextStroke.getTextWidth(scaleText1)) * 0.5f),
                            Math.round(SCALE_BAR_MARGIN * scale + STROKE_EXTERNAL * scale * 0.5f + TEXT_MARGIN * scale + this.paintScaleTextStroke.getTextHeight(scaleText1)), paint);
                } else {
                    canvas.drawText(scaleText1, Math.round(STROKE_EXTERNAL * scale + TEXT_MARGIN * scale),
                            Math.round(canvas.getHeight() * 0.5f - STROKE_EXTERNAL * scale * 0.5f - TEXT_MARGIN * scale), paint);
                    canvas.drawText(scaleText2, Math.round(STROKE_EXTERNAL * scale + TEXT_MARGIN * scale),
                            Math.round(canvas.getHeight() * 0.5f + STROKE_EXTERNAL * scale * 0.5f + TEXT_MARGIN * scale + this.paintScaleTextStroke.getTextHeight(scaleText2)), paint);
                }
                break;
            case TOP_LEFT:
                if (scaleText2.length() == 0) {
                    canvas.drawText(scaleText1, Math.round(STROKE_EXTERNAL * scale + TEXT_MARGIN * scale),
                            Math.round(SCALE_BAR_MARGIN * scale + STROKE_EXTERNAL * scale * 0.5f + TEXT_MARGIN * scale + this.paintScaleTextStroke.getTextHeight(scaleText1)), paint);
                } else {
                    canvas.drawText(scaleText1, Math.round(STROKE_EXTERNAL * scale + TEXT_MARGIN * scale),
                            Math.round(canvas.getHeight() * 0.5f - STROKE_EXTERNAL * scale * 0.5f - TEXT_MARGIN * scale), paint);
                    canvas.drawText(scaleText2, Math.round(STROKE_EXTERNAL * scale + TEXT_MARGIN * scale),
                            Math.round(canvas.getHeight() * 0.5f + STROKE_EXTERNAL * scale * 0.5f + TEXT_MARGIN * scale + this.paintScaleTextStroke.getTextHeight(scaleText2)), paint);
                }
                break;
            case TOP_RIGHT:
                if (scaleText2.length() == 0) {
                    canvas.drawText(scaleText1, Math.round(canvas.getWidth() - STROKE_EXTERNAL * scale - TEXT_MARGIN * scale - this.paintScaleTextStroke.getTextWidth(scaleText1)),
                            Math.round(SCALE_BAR_MARGIN * scale + STROKE_EXTERNAL * scale * 0.5f + TEXT_MARGIN * scale + this.paintScaleTextStroke.getTextHeight(scaleText1)), paint);
                } else {
                    canvas.drawText(scaleText1, Math.round(canvas.getWidth() - STROKE_EXTERNAL * scale - TEXT_MARGIN * scale - this.paintScaleTextStroke.getTextWidth(scaleText1)),
                            Math.round(canvas.getHeight() * 0.5f - STROKE_EXTERNAL * scale * 0.5f - TEXT_MARGIN * scale), paint);
                    canvas.drawText(scaleText2, Math.round(canvas.getWidth() - STROKE_EXTERNAL * scale - TEXT_MARGIN * scale - this.paintScaleTextStroke.getTextWidth(scaleText2)),
                            Math.round(canvas.getHeight() * 0.5f + STROKE_EXTERNAL * scale * 0.5f + TEXT_MARGIN * scale + this.paintScaleTextStroke.getTextHeight(scaleText2)), paint);
                }
                break;
        }
    }
}
