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
package org.mapsforge.map.writer.model;

public class SpecialTagExtractionResult {
	private final short elevation;
	private final String housenumber;
	private final byte layer;
	private final String name;
	private final String ref;
	private final String type;

	/**
	 * @param name
	 *            the name
	 * @param ref
	 *            the ref
	 * @param housenumber
	 *            the housenumber
	 * @param layer
	 *            the layer
	 * @param elevation
	 *            the elevation
	 * @param type
	 *            the type
	 */
	public SpecialTagExtractionResult(String name, String ref, String housenumber, byte layer, short elevation,
			String type) {
		super();
		this.name = name;
		this.ref = ref;
		this.housenumber = housenumber;
		this.layer = layer;
		this.elevation = elevation;
		this.type = type;
	}

	/**
	 * @return the elevation
	 */
	public final short getElevation() {
		return this.elevation;
	}

	/**
	 * @return the housenumber
	 */
	public final String getHousenumber() {
		return this.housenumber;
	}

	/**
	 * @return the layer
	 */
	public final byte getLayer() {
		return this.layer;
	}

	/**
	 * @return the name
	 */
	public final String getName() {
		return this.name;
	}

	/**
	 * @return the ref
	 */
	public final String getRef() {
		return this.ref;
	}

	/**
	 * @return the type
	 */
	public final String getType() {
		return this.type;
	}
}
