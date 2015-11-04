/*
 * Copyright 2010, 2011 mapsforge.org
 * Copyright 2010, 2011 Karsten Groll
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
package org.mapsforge.storage.poi;

/**
 * This class generates a prepared SQL query for retrieving POIs filtered by a given
 * {@link PoiCategoryFilter}.
 */
public class PoiCategoryRangeQueryGenerator {
	private PoiCategoryFilter filter;

	PoiCategoryRangeQueryGenerator(PoiCategoryFilter filter) {
		this.filter = filter;
	}

	String getSQLSelectString() {
		StringBuilder sb = new StringBuilder(
				"SELECT poi_index.id, poi_index.minLat, poi_index.minLon, poi_data.data, poi_data.category "
						+ "FROM poi_index "
						+ "JOIN poi_data ON poi_index.id = poi_data.id "
						+ "WHERE "
						+ "minLat <= ? AND "
						+ "minLon <= ? AND "
						+ "minLat >= ? AND "
						+ "minLon >= ?");

		sb.append(getSQLWhereClauseString());

		return sb.toString();
	}

	/**
	 * Gets the WHERE clause for the SQL query that looks up POI entries.
	 * 
	 * @return A string like <code>WHERE id BETWEEN 2 AND 5 OR BETWEEN 10 AND 12</code>.
	 */
	private String getSQLWhereClauseString() {
		int[] intervals = getCategoryIDIntervals();

		if (intervals.length == 0) {
			return "";
		}

		StringBuilder sb = new StringBuilder();
		sb.append(" AND (");
		// foreach interval
		for (int i = 0; i < intervals.length; i += 2) {
			sb.append("id BETWEEN ").append(intervals[i]).append(" AND ").append(intervals[i + 1]);

			// append OR if it is not the last interval
			if (i != intervals.length - 2) {
				sb.append(" OR ");
			}
		}

		sb.append(')');

		return sb.toString();
	}

	private int[] getCategoryIDIntervals() {
		int[] ret = new int[this.filter.getAcceptedCategories().size() * 2];

		int i = 0;
		PoiCategory[] siblings = null;

		for (PoiCategory c : this.filter.getAcceptedCategories()) {
			siblings = getSiblings(c);
			ret[i] = siblings[0].getID();
			ret[i + 1] = siblings[1].getID();
			i += 2;
		}

		return ret;
	}

	/**
	 * @param c1
	 *            Category whose siblings should be returned.
	 * @return Array: [left sibling, right sibling]
	 */
	private PoiCategory[] getSiblings(PoiCategory c1) {
		PoiCategory ret[] = new PoiCategory[2];
		ret[0] = c1;
		ret[1] = c1;

		if (c1.getParent() == null) {
			return ret;
		}

		for (PoiCategory c : c1.getParent().getChildren()) {

			if (c.getID() < c1.getID()) {
				ret[0] = c;
			}

			if (c.getID() > c1.getID()) {
				ret[1] = c;
			}
		}

		return ret;
	}
}
