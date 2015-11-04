/*
 * Copyright 2010, 2011 mapsforge.org
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
package org.mapsforge.poi.writer.osmosis;

import org.mapsforge.poi.writer.osmosis.jaxb.Category;
import org.mapsforge.storage.poi.DoubleLinkedPoiCategory;
import org.mapsforge.storage.poi.PoiCategory;
import org.mapsforge.storage.poi.PoiCategoryManager;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.logging.Logger;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

class XMLPoiCategoryManager implements PoiCategoryManager {
	private static final Logger LOGGER = Logger.getLogger(XMLPoiCategoryManager.class.getName());

	private DoubleLinkedPoiCategory root = null;
	private HashMap<String, DoubleLinkedPoiCategory> titleMap = null;

	/**
	 * @param configFilePath
	 *            Path to POI category XML file containing the category tree configuration.
	 */
	XMLPoiCategoryManager(String configFilePath) {
		LOGGER.info("Loading POI categories from XML...");

		this.titleMap = new HashMap<String, DoubleLinkedPoiCategory>();

		// Read root category from XML
		final File f = new File(configFilePath);

		JAXBContext ctx = null;
		Unmarshaller um = null;
		Category xmlRoot = null;
		try {
			ctx = JAXBContext.newInstance(Category.class);
			um = ctx.createUnmarshaller();
			xmlRoot = (Category) um.unmarshal(f);

		} catch (JAXBException e) {
			e.printStackTrace();
			LOGGER.severe("Could not load POI category configuration from XML.");
		}

		LinkedList<Category> currentXMLNode = new LinkedList<Category>();
		DoubleLinkedPoiCategory parent = null;
		DoubleLinkedPoiCategory child = null;
		// Create categories
		currentXMLNode.push(xmlRoot);
		while (!currentXMLNode.isEmpty()) {

			parent = createOrGetPoiCategory(currentXMLNode.getFirst().getTitle());
			titleMap.put(parent.getTitle(), parent);

			// Set root node
			if (currentXMLNode.getFirst() == xmlRoot) {
				this.root = parent;
			}

			for (Category c : currentXMLNode.pop().getCategory()) {
				child = createOrGetPoiCategory(c.getTitle());
				child.setParent(parent);

				currentXMLNode.add(c);
			}
		}

		// DEBUG
		// this.root = new DoubleLinkedPoiCategory("root", null);
		//
		// PoiCategory amr = createOrGetPoiCategory("amenity root");
		// createOrGetPoiCategory("amenity root").setParent(this.root);
		//
		// PoiCategory anr = createOrGetPoiCategory("another root");
		// createOrGetPoiCategory("another root").setParent(this.root);
		//
		// PoiCategory rest = createOrGetPoiCategory("restaurants");
		// rest.setParent(amr);
		//
		// PoiCategory blubb = createOrGetPoiCategory("blubb");
		// blubb.setParent(anr);
		//
		// PoiCategory ff = createOrGetPoiCategory("ff");
		// ff.setParent(rest);
		//
		// PoiCategory sushi = createOrGetPoiCategory("sushi");
		// sushi.setParent(rest);

		DoubleLinkedPoiCategory.calculateCategoryIDs(this.root, 0);
		// System.out.println(DoubleLinkedPoiCategory.getGraphVizString(this.root));
	}

	private DoubleLinkedPoiCategory createOrGetPoiCategory(String title) {
		DoubleLinkedPoiCategory ret = this.titleMap.get(title);

		// Category does not exist -> create it
		if (ret == null) {
			ret = new DoubleLinkedPoiCategory(title, null);
			LOGGER.finer("Added category: " + ret);
			this.titleMap.put(title, ret);
		}

		return ret;
	}

	@Override
	public PoiCategory getPoiCategoryByID(int id) {
		PoiCategory ret = null;

		for (String key : this.titleMap.keySet()) {
			if ((ret = this.titleMap.get(key)).getID() == id) {
				break;
			}
		}

		return ret;
	}

	@Override
	public PoiCategory getPoiCategoryByTitle(String title) {
		return this.titleMap.get(title);
	}

	@Override
	public PoiCategory getRootCategory() {
		return this.root;
	}
}
