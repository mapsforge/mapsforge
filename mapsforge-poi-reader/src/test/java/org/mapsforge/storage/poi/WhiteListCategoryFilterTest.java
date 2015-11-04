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

import junit.framework.Assert;

import org.junit.Test;

import java.util.Collection;

/**
 * Tests the {@link WhitelistPoiCategoryFilter}.
 */
public class WhiteListCategoryFilterTest {
	@Test
	public void testAcceptedCategoriesForFlatHierarchy()
			throws UnknownPoiCategoryException {
		// ACCEPT ALL NODES
		PoiCategory root = CategoryTreeBuilder.createAndGetFlatConfiguration();
		DoubleLinkedPoiCategory.calculateCategoryIDs(
				(DoubleLinkedPoiCategory) root, 0);
		PoiCategoryFilter filter = new WhitelistPoiCategoryFilter();
		PoiCategoryManager cm = new CategoryManagerTest.MockPoiCategoryManager(
				root);

		filter.addCategory(root);
		Collection<PoiCategory> acceptedCategories = filter
				.getAcceptedCategories();
		Collection<PoiCategory> acceptedSuperCategories = filter
				.getAcceptedSuperCategories();

		// There should be 1+5 = 6 accepted categories
		Assert.assertEquals(6, acceptedCategories.size());

		// ACCEPT ONE CHILD NODE ONLY
		filter = new WhitelistPoiCategoryFilter();
		filter.addCategory(cm.getPoiCategoryByTitle("a"));
		acceptedCategories = filter.getAcceptedCategories();
		acceptedSuperCategories = filter.getAcceptedSuperCategories();

		// Only one category should be accepted
		Assert.assertEquals(1, acceptedCategories.size());

		// The accepted category should have title "a" and ID 0
		PoiCategory categoryA = null;
		for (PoiCategory c : acceptedCategories) {
			if (c.getTitle() == "a") {
				categoryA = c;
			}
		}
		Assert.assertEquals("a", categoryA.getTitle());
		Assert.assertEquals(0, categoryA.getID());

		// There should be one super category now (a)
		Assert.assertEquals(1, acceptedCategories.size());
		Assert.assertTrue(acceptedCategories.contains(categoryA));

		// ACCEPT TWO CHILDREN
		filter.addCategory(cm.getPoiCategoryByTitle("d"));
		acceptedCategories = filter.getAcceptedCategories();
		acceptedSuperCategories = filter.getAcceptedSuperCategories();

		// There should be two accepted categories
		Assert.assertEquals(2, acceptedCategories.size());

		// The accepted categories should have title "a" and "d" and ID 0 and 3
		PoiCategory categoryD = null;
		for (PoiCategory c : acceptedCategories) {
			if (c.getTitle() == "d") {
				categoryD = c;
			}
		}
		Assert.assertEquals("a", categoryA.getTitle());
		Assert.assertEquals(0, categoryA.getID());
		Assert.assertEquals("d", categoryD.getTitle());
		Assert.assertEquals(3, categoryD.getID());

		// There should be two super categories now (a,d)
		Assert.assertEquals(2, acceptedCategories.size());
		Assert.assertTrue(acceptedCategories.contains(categoryA));
		Assert.assertTrue(acceptedCategories.contains(categoryD));

		// ACCEPT ALL NOW
		filter.addCategory(root);
		acceptedCategories = filter.getAcceptedCategories();
		acceptedSuperCategories = filter.getAcceptedSuperCategories();

		// There should now be 6 accepted categories
		Assert.assertEquals(6, acceptedCategories.size());

		// There should only be one super category left now (root)
		Assert.assertEquals(1, acceptedSuperCategories.size());
		Assert.assertTrue(acceptedSuperCategories.contains(root));
	}
}
