/*
 * Copyright 2010, 2011 mapsforge.org
 * Copyright 2010, 2011 Karsten Groll
 * Copyright 2015 devemux86
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

import java.util.Collection;
import java.util.Stack;
import java.util.Vector;

/**
 * A POI category representation that stores a node, its parent node and its child nodes.
 */
public class DoubleLinkedPoiCategory implements PoiCategory {
	private final String title;
	private PoiCategory parent;

	private final Vector<PoiCategory> childCategories;

	// The category's id
	private int id;

	/**
	 * Creates a new category without knowing its position in the final category tree. If all categories
	 * have been created you have to call {@link #calculateCategoryIDs(DoubleLinkedPoiCategory, int)} to
	 * assign the IDs.
	 *
	 * @param title
	 *            The category's unique title.
	 * @param parent
	 *            The category's parent category. For creating a root node, set the parent to null.
	 */
	public DoubleLinkedPoiCategory(String title, PoiCategory parent) {
		this(title, parent, -1);
	}

	/**
	 * Creates a new category. This constructor should only be called from {@link PoiCategoryManager}
	 * when reading a category configuration from a database or XML file. Otherwise call
	 * {@link #DoubleLinkedPoiCategory(String, PoiCategory)}.
	 *
	 * @param title
	 *            The category's unique title.
	 * @param parent
	 *            The category's parent category. For creating a root node, set the parent to null.
	 * @param id
	 *            The category's position in the tree determined by left-order-dfs-traversal.
	 */
	public DoubleLinkedPoiCategory(String title, PoiCategory parent, int id) {
		this.title = title;
		this.parent = parent;
		this.id = id;

		this.childCategories = new Vector<>();

		if (parent != null) {
			((DoubleLinkedPoiCategory) parent).addChildNode(this);
		}
	}

	/**
	 * Adds a child node for this category.
	 *
	 * @param categoryNode
	 *            The node to be added.
	 */
	private void addChildNode(DoubleLinkedPoiCategory categoryNode) {
		this.childCategories.add(categoryNode);
	}

	/**
	 * This method calculates an unique ID for all nodes in the tree. For each node's 'n' ID named
	 * 'ID_'n at depth 'd' the following invariants must be true:
	 * <ul>
	 * <li>ID > max(ID of all child nodes)</li>
	 * <li>All nodes' IDs left of n must be < ID_n.</li>
	 * <li>All nodes' IDs right of n must be > ID_n.</li>
	 * </ul>
	 *
	 * @param rootNode
	 *            The tree's root node. (<strong>Any other node will result in invalid IDs!</strong>)
	 * @param maxValue
	 *            Global maximum ID.
	 * @return The root node's ID.
	 */
	public static int calculateCategoryIDs(DoubleLinkedPoiCategory rootNode, int maxValue) {
		int newMax = maxValue;
		for (PoiCategory c : rootNode.childCategories) {
			newMax = calculateCategoryIDs((DoubleLinkedPoiCategory) c, newMax);
		}

		rootNode.id = newMax;

		return newMax + 1;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Collection<PoiCategory> getChildren() {
		return this.childCategories;
	}

	/**
	 * Generates a GraphViz source representation as a tree having the current node as its root.
	 *
	 * @param rootNode
	 *            The resulting graph's root node. (You can use any sub node to get a sub-graph).
	 * @return a GraphViz source representation as a tree having the current node as its root.
	 */
	public static String getGraphVizString(DoubleLinkedPoiCategory rootNode) {
		StringBuilder sb = new StringBuilder();
		Stack<PoiCategory> stack = new Stack<>();
		stack.push(rootNode);

		DoubleLinkedPoiCategory currentNode;
		sb.append("// dot test.dot -Tpng > test.png\r\n");
		sb.append("digraph Categories {\r\n");
		sb.append("  graph [\r\nrankdir = \"LR\"\r\n]\r\n\r\nnode [\r\nshape = \"plaintext\"\r\n]");
		while (!stack.isEmpty()) {
			currentNode = (DoubleLinkedPoiCategory) stack.pop();
			for (PoiCategory childNode : currentNode.childCategories) {
				stack.push(childNode);
				sb.append("\t\"").append(currentNode.getTitle()).append(" (")
						.append(currentNode.getID()).append(")").append("\" -> \"")
						.append(childNode.getTitle()).append(" (").append(childNode.getID())
						.append(")").append("\"\r\n");
			}
		}

		sb.append("}\r\n");
		return sb.toString();
	}

	/**
	 * Make sure you call {@link #calculateCategoryIDs(DoubleLinkedPoiCategory, int)} first.
	 *
	 * @return The node's ID.
	 */
	@Override
	public int getID() {
		return this.id;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public PoiCategory getParent() {
		return this.parent;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getTitle() {
		return this.title;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setParent(PoiCategory parent) {
		this.parent = parent;
		parent.getChildren().add(this);
	}

	@Override
	public String toString() {
		return "DoubleLinked POICategory: ('" + this.title + "'," + this.id + ") @ "
				+ Integer.toHexString(this.hashCode());
	}
}
