/*
 * Copyright (C) 2009 Huan Erdao
 * Copyright (C) 2014 Martin Vennekamp
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *	  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mapsforge.applications.android.samples.markerclusterer;

import org.mapsforge.core.model.LatLong;

/**
 * Utility Class to handle GeoItem for ClusterMarker
 * 
 */
public interface GeoItem {
	/**
	 * getLatLong
	 * 
	 * @return item location in LatLong.
	 */
	public LatLong getLatLong();

	/**
	 * getTitle
	 * 
	 * @return Title of the item, might be used as Caption text.
	 */
	public String getTitle();
}