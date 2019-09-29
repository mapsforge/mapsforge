/*
 * Copyright 2015-2019 devemux86
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
package org.mapsforge.map.android.rotation;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

public class RotateView extends ViewGroup {

    private float heading = 0;
    private final Matrix matrix = new Matrix();
    private final float[] points = new float[2];
    private int saveCount = -1;
    private final SmoothCanvas smoothCanvas = new SmoothCanvas();

    public RotateView(Context context) {
        this(context, null);
    }

    public RotateView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        if (heading == 0) {
            super.dispatchDraw(canvas);
            return;
        }

        saveCount = canvas.save();
        canvas.rotate(-heading, getWidth() * 0.5f, getHeight() * 0.5f);
        smoothCanvas.delegate = canvas;
        super.dispatchDraw(smoothCanvas);
        if (saveCount != -1) {
            canvas.restoreToCount(saveCount);
            saveCount = -1;
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (heading == 0) {
            return super.dispatchTouchEvent(event);
        }

        MotionEvent rotatedEvent = rotateEvent(event, heading, getWidth() * 0.5f, getHeight() * 0.5f);
        try {
            return super.dispatchTouchEvent(rotatedEvent);
        } finally {
            if (rotatedEvent != event)
                rotatedEvent.recycle();
        }
    }

    public float getHeading() {
        return heading;
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int width = getWidth();
        int height = getHeight();
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            View view = getChildAt(i);
            int childWidth = view.getMeasuredWidth();
            int childHeight = view.getMeasuredHeight();
            int childLeft = (width - childWidth) / 2;
            int childTop = (height - childHeight) / 2;
            view.layout(childLeft, childTop, childLeft + childWidth, childTop + childHeight);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int w = getDefaultSize(getSuggestedMinimumWidth(), widthMeasureSpec);
        int h = getDefaultSize(getSuggestedMinimumHeight(), heightMeasureSpec);
        int sizeSpec = MeasureSpec.makeMeasureSpec((int) Math.hypot(w, h), MeasureSpec.EXACTLY);
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            getChildAt(i).measure(sizeSpec, sizeSpec);
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    private MotionEvent rotateEvent(final MotionEvent event, float degrees, float px, float py) {
        if (degrees == 0)
            return event;

        matrix.setRotate(degrees, px, py);

        final MotionEvent rotatedEvent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            rotatedEvent = MotionEvent.obtain(event);
            rotatedEvent.transform(matrix);
        } else {
            rotatedEvent = MotionEvent.obtainNoHistory(event);
            points[0] = event.getX();
            points[1] = event.getY();
            matrix.mapPoints(points);
            rotatedEvent.setLocation(points[0], points[1]);
        }
        return rotatedEvent;
    }

    public void setHeading(float heading) {
        this.heading = heading;
    }
}
