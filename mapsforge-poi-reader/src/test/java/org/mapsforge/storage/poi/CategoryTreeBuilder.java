/*
 * Copyright 2010, 2011, 2012 mapsforge.org
 * Copyright 2010, 2011, 2012 Karsten Groll
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
 * This helper class provides templates for category configurations.
 */
public class CategoryTreeBuilder {
	static PoiCategory createAndGetBalancedConfiguration() {
		// Level 0
		PoiCategory root = new DoubleLinkedPoiCategory("root", null);

		// Level 1
		PoiCategory l1_1 = new DoubleLinkedPoiCategory("l1_1", root);
		PoiCategory l1_2 = new DoubleLinkedPoiCategory("l1_2", root);
		PoiCategory l1_3 = new DoubleLinkedPoiCategory("l1_3", root);

		// Level 2
		new DoubleLinkedPoiCategory("l1_1_l2_1", l1_1);
		new DoubleLinkedPoiCategory("l1_1_l2_2", l1_1);
		new DoubleLinkedPoiCategory("l1_1_l2_3", l1_1);

		new DoubleLinkedPoiCategory("l1_2_l2_1", l1_2);
		new DoubleLinkedPoiCategory("l1_2_l2_2", l1_2);
		new DoubleLinkedPoiCategory("l1_2_l2_3", l1_2);

		new DoubleLinkedPoiCategory("l1_3_l2_1", l1_3);
		new DoubleLinkedPoiCategory("l1_3_l2_2", l1_3);
		new DoubleLinkedPoiCategory("l1_3_l2_3", l1_3);

		DoubleLinkedPoiCategory.calculateCategoryIDs((DoubleLinkedPoiCategory) root, 0);
		return root;
	}

	static PoiCategory createAndGetFlatConfiguration() {
		PoiCategory root = new DoubleLinkedPoiCategory("root", null);
		new DoubleLinkedPoiCategory("a", root);
		new DoubleLinkedPoiCategory("b", root);
		new DoubleLinkedPoiCategory("c", root);
		new DoubleLinkedPoiCategory("d", root);
		new DoubleLinkedPoiCategory("e", root);

		DoubleLinkedPoiCategory.calculateCategoryIDs((DoubleLinkedPoiCategory) root, 0);
		return root;
	}
}
