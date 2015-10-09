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
package org.mapsforge.applications.android.samples;

/**
 * Illustrates the ability to set different tile sizes and background colors to
 * MapViews.
 */

public class DualMapViewerWithDifferentDisplayModels extends DualMapViewer {

	@Override
	protected void createMapViews() {
		super.createMapViews();

		// change tile size and background colour for the second view
		mapView2.getModel().displayModel.setUserScaleFactor(0.3f);
		mapView2.getModel().displayModel.setBackgroundColor(0xffff0000); // red background
	}
}
