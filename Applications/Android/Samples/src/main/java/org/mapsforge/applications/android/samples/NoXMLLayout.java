/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
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

import org.mapsforge.map.android.view.MapView;

/**
 * A simple activity that illustrates that an XML layout file is not really needed
 * for the most simple map activities.
 */
public class NoXMLLayout extends RenderTheme4 {

	/**
	 * In this class we instantiate the MapView directly using this class,
	 * without using an XML layout file.
	 * @return
	 */
	@Override
	protected MapView getMapView() {
		MapView mv = new MapView(this);
		setContentView(mv);
		return mv;
	}

	@Override
	public void setContentView() {
		// no-op, we have already set the map view in getMapView()
	}
}
