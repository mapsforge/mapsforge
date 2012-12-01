/*
 * Copyright 2010, 2011, 2012 mapsforge.org
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
package org.mapsforge.applications.android.advancedmapviewer.preferences;

import org.mapsforge.applications.android.advancedmapviewer.AdvancedMapViewer;
import org.mapsforge.applications.android.advancedmapviewer.R;

import android.content.Context;
import android.util.AttributeSet;

/**
 * Preferences class for adjusting the move speed.
 */
public class MoveSpeedPreference extends SeekBarPreference {
	/**
	 * Construct a new move speed preference seek bar.
	 * 
	 * @param context
	 *            the context activity.
	 * @param attrs
	 *            A set of attributes (currently ignored).
	 */
	public MoveSpeedPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		// define the text message
		this.messageText = getContext().getString(R.string.preferences_move_speed_desc);

		// define the current and maximum value of the seek bar
		this.seekBarCurrentValue = this.preferencesDefault.getInt(this.getKey(), AdvancedMapViewer.MOVE_SPEED_DEFAULT);
		this.max = AdvancedMapViewer.MOVE_SPEED_MAX;
	}

	@Override
	String getCurrentValueText(int progress) {
		return String.format(getContext().getString(R.string.preferences_move_speed_value),
				Integer.valueOf(progress * 10));
	}
}
