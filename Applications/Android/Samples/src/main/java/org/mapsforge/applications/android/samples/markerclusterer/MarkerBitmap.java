/*
 * Copyright (C) 2009 Huan Erdao
 * Copyright (C) 2014 Martin Vennekamp
 * Copyright 2015 mapsforge.org
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

package org.mapsforge.applications.android.samples.markerclusterer;


import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;

import org.mapsforge.applications.android.samples.R;
import org.mapsforge.applications.android.samples.Utils;
import org.mapsforge.core.graphics.Bitmap;
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.model.Point;
import org.mapsforge.map.model.DisplayModel;

import java.util.HashMap;


/**
 * Utility Class to handle MarkerBitmap
 * it handles grid offset to display on the map with offset
 */
public class MarkerBitmap {

    private static HashMap<String, Bitmap> captionViews = new HashMap<String, Bitmap>();
    /**
     * bitmap object for normal state icon
     */
    protected final Bitmap mIconBmpNormal;
    /**
     * bitmap object for select state icon
     */
    protected final Bitmap mIconBmpSelect;
    /**
     * offset grid of icon in Point.
     * if you are using symmetric icon image, it should be half size of width&height.
     * adjust this parameter to offset the axis of the image.
     */
    protected Point mIconOffset;
    /**
     * maximum item size for the marker.
     */
    protected int mItemSizeMax;
    /**
     * text size for icon
     */
    protected float mTextSize;
    /**
     * Paint object for drawing icon
     */
    protected final Paint mPaint;
	private static Context mContext;
    
    /**
     * NOTE: src_nrm & src_sel must be same bitmap size.
     *
     * @param src_nrm  source Bitmap object for normal state
     * @param src_sel  source Bitmap object for select state
     * @param grid     grid point to be offset
     * @param textSize text size for icon
     * @param maxSize  icon size threshold
     */
    public MarkerBitmap(Context context, Bitmap src_nrm, Bitmap src_sel,
    		Point grid, float textSize, int maxSize, Paint paint) {
    	mContext = context;
        mIconBmpNormal = src_nrm;
        mIconBmpSelect = src_sel;
        mIconOffset = grid;
        mTextSize = textSize * DisplayModel.getDeviceScaleFactor();
        mItemSizeMax = maxSize;
        mPaint = paint;
        mPaint.setTextSize(getTextSize());
    }

    /**
     * @return bitmap object for normal state icon or for for select state icon
     */
    public final Bitmap getBitmap(boolean isSelected) {
        if (isSelected) {
            return mIconBmpSelect;
        }
        return mIconBmpNormal;
    }
   

    /**
     * @return get offset of the icon
     */
    public final Point getIconOffset() {
        return mIconOffset;
    }

    /**
     * @return text size already adjusted with DisplayModel.getDeviceScaleFactor(), i.e.
     * the scaling factor for fonts displayed on the display.
     */
    public final float getTextSize() {
        return mTextSize;
    }

    /**
     * @return icon size threshold
     */
    public final int getItemMax() {
        return mItemSizeMax;
    }
    /**
     * @return Paint object for drawing icon
     */
	public Paint getPaint(){
		return mPaint;
	}
	
	public static Bitmap getBitmapFromTitle(String title, Paint paint){
		if ( !captionViews.containsKey(title) ) {
			TextView bubbleView = new TextView(mContext);
			Utils.setBackground(bubbleView, mContext.getResources().getDrawable(R.drawable.caption_background));
			bubbleView.setGravity(Gravity.CENTER);
			bubbleView.setMaxEms(20);
			bubbleView.setTextSize(10); 
			bubbleView.setPadding(5, -2, 5, -2);
			bubbleView.setTextColor(android.graphics.Color.BLACK);
			bubbleView.setText(title);
			//Measure the view at the exact dimensions (otherwise the text won't center correctly)
	        int widthSpec = View.MeasureSpec.makeMeasureSpec(paint.getTextWidth(title), View.MeasureSpec.EXACTLY);
	        int heightSpec = View.MeasureSpec.makeMeasureSpec(paint.getTextHeight(title), View.MeasureSpec.EXACTLY);
	        bubbleView.measure(widthSpec, heightSpec);

	        //Layout the view at the width and height
	        bubbleView.layout(0, 0, paint.getTextWidth(title), paint.getTextHeight(title));
	        
	        captionViews.put(title, Utils.viewToBitmap(mContext, bubbleView));
	        captionViews.get(title).incrementRefCount(); // FIXME: is never reduced!
		}
		return captionViews.get(title);
	}

	protected static void clearCaptionBitmap() {
		for ( Bitmap bitmap: captionViews.values() ) {
			bitmap.decrementRefCount();
		}
		captionViews.clear();
	}
}


