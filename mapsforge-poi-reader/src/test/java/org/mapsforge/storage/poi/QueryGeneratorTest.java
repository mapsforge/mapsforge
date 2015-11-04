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

import org.junit.Before;
import org.junit.Test;

/**
 * This class tests the {@link PoiCategoryRangeQueryGenerator} class for common use cases.
 */
public class QueryGeneratorTest {
	private PoiCategory flatRoot;
	private PoiCategory balancedRoot;
	private PoiCategoryManager flatCm;
	private PoiCategoryManager balancedCm;

	@Before
	public void init() {
		this.flatRoot = CategoryTreeBuilder.createAndGetFlatConfiguration();
		this.flatCm = new CategoryManagerTest.MockPoiCategoryManager(this.flatRoot);

		this.balancedRoot = CategoryTreeBuilder.createAndGetBalancedConfiguration();
		this.balancedCm = new CategoryManagerTest.MockPoiCategoryManager(this.balancedRoot);

		System.out.println("=====8<=====");
		System.out.println(DoubleLinkedPoiCategory.getGraphVizString((DoubleLinkedPoiCategory) this.balancedRoot));
		System.out.println("============");
	}

	/**
	 * Select all categories by adding the root category to a whitelist filter.
	 */
	// @Test
	public void selectAllFromFlatHierarchy() {
		PoiCategoryFilter filter = new WhitelistPoiCategoryFilter();
		filter.addCategory(this.flatRoot);

		String query = PoiCategoryRangeQueryGenerator.getSQLSelectString(filter);

		System.out.println("Query: " + query);

		// TODO add assertions
	}

	/**
	 * Select all categories by adding the root category to a whitelist filter.
	 * 
	 * @throws UnknownPoiCategoryException
	 *             if a category cannot be found by its name or ID.
	 */
	@Test
	public void selectTwoFromBalancedHierarchy() throws UnknownPoiCategoryException {
		PoiCategoryFilter filter = new WhitelistPoiCategoryFilter();
		filter.addCategory(this.balancedCm.getPoiCategoryByTitle("l1_1"));
		filter.addCategory(this.balancedCm.getPoiCategoryByTitle("l1_2"));

		String query = PoiCategoryRangeQueryGenerator.getSQLSelectString(filter);
		System.out.println("Query: " + query);

		// TODO add assertions
	}
}
