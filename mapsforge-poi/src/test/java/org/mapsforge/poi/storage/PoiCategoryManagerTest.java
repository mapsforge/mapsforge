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
package org.mapsforge.poi.storage;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Stack;
import java.util.TreeMap;

/**
 * Tests the {@link AbstractPoiCategoryManager}'s functionality
 * (finding categories by their ID / name).
 */
public class PoiCategoryManagerTest {
    private PoiCategory root;
    private PoiCategory restaurants;
    private PoiCategory shops;
    private PoiCategory fastFood;
    private PoiCategory bars;
    private PoiCategory electronics;
    private PoiCategory clothes;

    private PoiCategoryManager cm;

    /**
     * Tests the <code>getPoiCategoryByID()</code> method.
     */
    @Test
    public void getCategoriesByID() {
        try {
            Assert.assertEquals(this.fastFood, this.cm.getPoiCategoryByID(0));
            Assert.assertEquals(this.bars, this.cm.getPoiCategoryByID(1));
            Assert.assertEquals(this.restaurants, this.cm.getPoiCategoryByID(2));
            Assert.assertEquals(this.electronics, this.cm.getPoiCategoryByID(3));
            Assert.assertEquals(this.clothes, this.cm.getPoiCategoryByID(4));
            Assert.assertEquals(this.shops, this.cm.getPoiCategoryByID(5));
            Assert.assertEquals(this.root, this.cm.getPoiCategoryByID(6));
        } catch (UnknownPoiCategoryException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void getRootCategory() {
        try {
            Assert.assertEquals(this.root, this.cm.getRootCategory());
        } catch (UnknownPoiCategoryException e) {
            e.printStackTrace();
        }
    }

    /**
     * Creates a simple category tree and adds it to a mock category manager.
     */
    @Before
    public void init() {
        this.root = new DoubleLinkedPoiCategory("root", null);

        this.restaurants = new DoubleLinkedPoiCategory("restaurants", this.root);
        this.shops = new DoubleLinkedPoiCategory("shops", this.root);

        this.fastFood = new DoubleLinkedPoiCategory("fastFood", this.restaurants);
        this.bars = new DoubleLinkedPoiCategory("bars", this.restaurants);

        this.electronics = new DoubleLinkedPoiCategory("electronics", this.shops);
        this.clothes = new DoubleLinkedPoiCategory("clothes", this.shops);

        DoubleLinkedPoiCategory.calculateCategoryIDs((DoubleLinkedPoiCategory) this.root, 0);

        this.cm = new MockPoiCategoryManager(this.root);
    }

    /**
     * Throws an exception for a non existent category.
     *
     * @throws UnknownPoiCategoryException expected exception.
     */
    @Test(expected = UnknownPoiCategoryException.class)
    public void throwExceptionForUnknownCategoryID() throws UnknownPoiCategoryException {
        this.cm.getPoiCategoryByID(42);
    }

    /**
     * Throws an exception for a non existent category.
     *
     * @throws UnknownPoiCategoryException expected exception.
     */
    @Test(expected = UnknownPoiCategoryException.class)
    public void throwExceptionForUnknownCategoryName() throws UnknownPoiCategoryException {
        this.cm.getPoiCategoryByTitle("doesnotexist");
    }

    /**
     * A database-less {@link PoiCategoryManager} implementation that reads a category configuration from a root
     * category.
     */
    static class MockPoiCategoryManager extends AbstractPoiCategoryManager {
        private PoiCategory localRoot;

        MockPoiCategoryManager(PoiCategory root) {
            this.rootCategory = root;
            this.localRoot = root;
            this.categoryMap = new TreeMap<>();

            loadCategoryHierarchy();
        }

        /**
         * Transforms a category tree given by a root category into a TreeMap.
         */
        private void loadCategoryHierarchy() {
            Stack<PoiCategory> stack = new Stack<>();
            stack.push(this.localRoot);

            while (!stack.isEmpty()) {
                PoiCategory current = stack.pop();
                this.categoryMap.put(current.getID(), current);

                for (PoiCategory c : current.getChildren()) {
                    stack.push(c);
                }
            }
        }
    }
}
