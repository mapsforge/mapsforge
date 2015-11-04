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

import org.junit.Before;
import org.junit.Test;

/**
 * This test case checks the functionality of the {@link DoubleLinkedPoiCategory} class. It tests the category
 * hierarchy.
 */
public class DoubleLinkedPoiCategoryTest {
	private PoiCategory root;
	private PoiCategory restaurants;
	private PoiCategory shops;
	private PoiCategory fastFood;
	private PoiCategory bars;
	private PoiCategory electronics;
	private PoiCategory clothes;

	/**
	 * Creates mock data.
	 */
	@Before
	public void init() {
		createCategoryTree();
		createCategoryIDs();
	}

	private void createCategoryTree() {
		this.root = new DoubleLinkedPoiCategory("root", null);

		this.restaurants = new DoubleLinkedPoiCategory("restaurants", this.root);
		this.shops = new DoubleLinkedPoiCategory("shops", this.root);

		this.fastFood = new DoubleLinkedPoiCategory("fastFood", this.restaurants);
		this.bars = new DoubleLinkedPoiCategory("bars", this.restaurants);

		this.electronics = new DoubleLinkedPoiCategory("electronics", this.shops);
		this.clothes = new DoubleLinkedPoiCategory("clothes", this.shops);
	}

	private void createCategoryIDs() {
		DoubleLinkedPoiCategory.calculateCategoryIDs((DoubleLinkedPoiCategory) this.root, 0);
	}

	/**
	 * Checks whether the categories are connected correctly.
	 */
	@Test
	public void checkCategoryHierarchy() {

		// Root category should not have any parents
		Assert.assertEquals(null, this.root.getParent());

		// Root category should have two direct children
		Assert.assertEquals(2, this.root.getChildren().size());

		// Root category's children should be "restaurants" and "shops"
		Assert.assertTrue(this.root.getChildren().contains(this.restaurants));
		Assert.assertTrue(this.root.getChildren().contains(this.shops));

		// "restaurants" and "shops" should have "root" as their parent category
		Assert.assertEquals(this.root, this.restaurants.getParent());
		Assert.assertEquals(this.root, this.shops.getParent());

		// "restaurants" should have two direct children
		Assert.assertEquals(2, this.restaurants.getChildren().size());

		// "shops" should have two direct children
		Assert.assertEquals(2, this.shops.getChildren().size());

		// "restaurants" children should be "fastFood" and "bars"
		Assert.assertTrue(this.restaurants.getChildren().contains(this.fastFood));
		Assert.assertTrue(this.restaurants.getChildren().contains(this.bars));

		// "shops" children should be "electronics" and "clothes"
		Assert.assertTrue(this.shops.getChildren().contains(this.electronics));
		Assert.assertTrue(this.shops.getChildren().contains(this.clothes));

		// "fastFood" and "bars" should have "restaurants" as their parent category
		Assert.assertEquals(this.restaurants, this.fastFood.getParent());
		Assert.assertEquals(this.restaurants, this.bars.getParent());

		// "electronic" and "clothes" should have "shops" as their parent category
		Assert.assertEquals(this.shops, this.electronics.getParent());
		Assert.assertEquals(this.shops, this.clothes.getParent());
	}

	/**
	 * Checks for correct assignment of category IDs.
	 */
	@Test
	public void checkCatgoryIDs() {
		Assert.assertEquals(0, this.fastFood.getID());
		Assert.assertEquals(1, this.bars.getID());
		Assert.assertEquals(2, this.restaurants.getID());
		Assert.assertEquals(3, this.electronics.getID());
		Assert.assertEquals(4, this.clothes.getID());
		Assert.assertEquals(5, this.shops.getID());
		Assert.assertEquals(6, this.root.getID());
	}
}
