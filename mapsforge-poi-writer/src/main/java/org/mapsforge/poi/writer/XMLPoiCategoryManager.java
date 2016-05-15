/*
 * Copyright 2010, 2011 mapsforge.org
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
package org.mapsforge.poi.writer;

import org.mapsforge.poi.storage.DoubleLinkedPoiCategory;
import org.mapsforge.poi.storage.PoiCategory;
import org.mapsforge.poi.storage.PoiCategoryManager;
import org.mapsforge.poi.writer.jaxb.Category;

import java.net.URL;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

/**
 * A {@link PoiCategoryManager} implementation that reads a category configuration from an XML file.
 */
class XMLPoiCategoryManager implements PoiCategoryManager {
    private static final Logger LOGGER = Logger.getLogger(XMLPoiCategoryManager.class.getName());

    /**
     * Maps a category's title to a category
     */
    private final Map<String, DoubleLinkedPoiCategory> titleMap;

    private DoubleLinkedPoiCategory root = null;

    /**
     * @param configFilePath Path to POI category XML file containing the category tree configuration.
     */
    XMLPoiCategoryManager(URL configFilePath) {
        this.titleMap = new HashMap<>();

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

        // Create categories
        LinkedList<Category> currentXMLNode = new LinkedList<>();
        currentXMLNode.push(xmlRootCategory);
        DoubleLinkedPoiCategory parent, child;
        while (!currentXMLNode.isEmpty()) {
            parent = createOrGetPoiCategory(currentXMLNode.getFirst().getTitle());
            titleMap.put(parent.getTitle(), parent);

            // Set root node
            if (currentXMLNode.getFirst() == xmlRootCategory) {
                this.root = parent;
            }

            for (Category c : currentXMLNode.pop().getCategory()) {
                child = createOrGetPoiCategory(c.getTitle());
                child.setParent(parent);

                currentXMLNode.add(c);
            }
        }

        // Calculate a unique ID for all nodes in the tree
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

    /**
     * {@inheritDoc}
     */
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

    /**
     * {@inheritDoc}
     */
    @Override
    public PoiCategory getPoiCategoryByTitle(String title) {
        return this.titleMap.get(title);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PoiCategory getRootCategory() {
        return this.root;
    }
}
