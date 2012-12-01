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

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.DialogPreference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

/**
 * This abstract class provides all code for a seek bar preference. Deriving classes only need to set the current and
 * maximum value of the seek bar. An optional text message above the seek bar is also supported as well as an optional
 * current value message below the seek bar.
 */
abstract class SeekBarPreference extends DialogPreference implements OnSeekBarChangeListener {
	private TextView currentValueTextView;
	private Editor editor;
	private SeekBar preferenceSeekBar;

	/**
	 * How much the value should increase when the seek bar is moved.
	 */
	int increment = 1;

	/**
	 * The maximum value of the seek bar.
	 */
	int max;

	/**
	 * Optional text message to display on top of the seek bar.
	 */
	String messageText;

	/**
	 * The SharedPreferences instance that is used.
	 */
	final SharedPreferences preferencesDefault;

	/**
	 * The current value of the seek bar.
	 */
	int seekBarCurrentValue;

	/**
	 * Create a new seek bar preference.
	 * 
	 * @param context
	 *            the context of the seek bar preferences activity.
	 * @param attrs
	 *            A set of attributes (currently ignored).
	 */
	SeekBarPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.preferencesDefault = PreferenceManager.getDefaultSharedPreferences(context);
	}

	@Override
	public void onClick(DialogInterface dialog, int which) {
		// check if the "OK" button was pressed and the seek bar value has changed
		if (which == DialogInterface.BUTTON_POSITIVE
				&& this.seekBarCurrentValue != this.preferenceSeekBar.getProgress()) {
			// get the value of the seek bar and save it in the preferences
			this.seekBarCurrentValue = this.preferenceSeekBar.getProgress();
			this.editor = this.preferencesDefault.edit();
			this.editor.putInt(this.getKey(), this.seekBarCurrentValue);
			this.editor.commit();
		}
	}

	@Override
	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
		if (this.currentValueTextView != null) {
			this.currentValueTextView.setText(getCurrentValueText(progress));
		}
	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
		// do nothing
	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
		// do nothing
	}

	@Override
	protected View onCreateDialogView() {
		// create a layout for the optional text messageText and the seek bar
		LinearLayout linearLayout = new LinearLayout(getContext());
		linearLayout.setOrientation(LinearLayout.VERTICAL);
		linearLayout.setPadding(20, 10, 20, 10);

		// check if a text message should appear above the seek bar
		if (this.messageText != null) {
			// create a text view for the text messageText
			TextView messageTextView = new TextView(getContext());
			messageTextView.setText(this.messageText);
			messageTextView.setPadding(0, 0, 0, 20);
			// add the text message view to the layout
			linearLayout.addView(messageTextView);
		}

		// create the seek bar and set the maximum and current value
		this.preferenceSeekBar = new SeekBar(getContext());
		this.preferenceSeekBar.setOnSeekBarChangeListener(this);
		this.preferenceSeekBar.setMax(this.max);
		this.preferenceSeekBar.setProgress(Math.min(this.seekBarCurrentValue, this.max));
		this.preferenceSeekBar.setKeyProgressIncrement(this.increment);
		this.preferenceSeekBar.setPadding(0, 0, 0, 10);
		// add the seek bar to the layout
		linearLayout.addView(this.preferenceSeekBar);

		// create the text view for the current value below the seek bar
		this.currentValueTextView = new TextView(getContext());
		this.currentValueTextView.setText(getCurrentValueText(this.preferenceSeekBar.getProgress()));
		this.currentValueTextView.setGravity(Gravity.CENTER_HORIZONTAL);
		// add the current value text view to the layout
		linearLayout.addView(this.currentValueTextView);

		return linearLayout;
	}

	/**
	 * Get the current value text.
	 * 
	 * @param progress
	 *            the current progress level of the seek bar.
	 * @return the new current value text
	 */
	abstract String getCurrentValueText(int progress);
}
