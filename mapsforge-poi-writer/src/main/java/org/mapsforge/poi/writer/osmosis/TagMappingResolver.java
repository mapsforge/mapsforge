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
package org.mapsforge.poi.writer.osmosis;

import org.mapsforge.poi.writer.osmosis.jaxb.Category;
import org.mapsforge.poi.writer.osmosis.jaxb.Mapping;
import org.mapsforge.storage.poi.PoiCategory;
import org.mapsforge.storage.poi.PoiCategoryManager;
import org.mapsforge.storage.poi.UnknownPoiCategoryException;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

/**
 * This class maps a given tag (e.g. amenity=restaurant) to a certain {@link PoiCategory}.
 * The mapping configuration is read from a XML file.
 */
class TagMappingResolver {
	private static final Logger LOGGER = Logger.getLogger(TagMappingResolver.class.getName());

	private final PoiCategoryManager categoryManager;

	/** Maps a tag to a category's title */
	private Map<String, String> tagMap;

	private Set<String> mappingTags;

	/**
	 * @param configFilePath
	 *            Path to the XML file containing the tag to POI mappings.
	 * @param categoryManager
	 *            The category manager for loading a category tree.
	 */
	TagMappingResolver(URL configFilePath, PoiCategoryManager categoryManager) {
		this.categoryManager = categoryManager;
		this.tagMap = new HashMap<>();
		this.mappingTags = new TreeSet<>();

		// Read root category from XML
		JAXBContext ctx;
		Unmarshaller um;
		Category xmlRootCategory = null;
		try {
			ctx = JAXBContext.newInstance(Category.class);
			um = ctx.createUnmarshaller();
			xmlRootCategory = (Category) um.unmarshal(configFilePath);
		} catch (JAXBException e) {
			LOGGER.log(Level.SEVERE, "Could not load POI category configuration from XML.", e);
		}

		LOGGER.info("Adding tag mappings...");
		Stack<Category> categories = new Stack<>();
		categories.push(xmlRootCategory);

		// Add mappings from tag to category title
		while (!categories.isEmpty()) {
			for (Category c : categories.pop().getCategory()) {
				categories.push(c);

				for (Mapping m : c.getMapping()) {
					LOGGER.finer("'" + m.getTag() + "' --> '" + c.getTitle() + "'");
					this.tagMap.put(m.getTag(), c.getTitle());
				}
			}
		}

		// For each mapping's tag: Split and save key (uniquely)
		for (String tag : this.tagMap.keySet()) {
			this.mappingTags.add(tag.split("=")[0]);
		}
		LOGGER.info("Tag mappings have been added.");
	}

	PoiCategory getCategoryFromTag(String tag) throws UnknownPoiCategoryException {
		String categoryName = this.tagMap.get(tag);

		// Tag not found?
		if (categoryName == null) {
			return null;
		}

		return this.categoryManager.getPoiCategoryByTitle(categoryName);
	}

	Set<String> getMappingTags() {
		return this.mappingTags;
	}
}
