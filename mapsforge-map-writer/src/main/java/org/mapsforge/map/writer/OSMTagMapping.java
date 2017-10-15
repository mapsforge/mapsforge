/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright 2016 Andrey Novikov
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
package org.mapsforge.map.writer;

import org.mapsforge.map.writer.model.OSMTag;
import org.mapsforge.map.writer.osmosis.MapFileWriterTask;
import org.mapsforge.map.writer.util.OSMUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import gnu.trove.map.hash.TShortIntHashMap;
import gnu.trove.procedure.TShortIntProcedure;
import gnu.trove.set.hash.TShortHashSet;

/**
 * Reorders and maps tag ids according to their frequency in the input data. Ids are remapped so that the most frequent
 * entities receive the lowest ids.
 */
public final class OSMTagMapping {
    private class HistogramEntry implements Comparable<HistogramEntry> {
        final int amount;
        final short id;

        public HistogramEntry(short id, int amount) {
            super();
            this.id = id;
            this.amount = amount;
        }

        /**
         * First order: amount Second order: id (reversed order).
         */
        @Override
        public int compareTo(HistogramEntry o) {
            if (this.amount > o.amount) {
                return 1;
            } else if (this.amount < o.amount) {
                return -1;
            } else {
                if (this.id < o.id) {
                    return 1;
                } else if (this.id > o.id) {
                    return -1;
                } else {
                    return 0;
                }
            }
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            HistogramEntry other = (HistogramEntry) obj;
            if (!getOuterType().equals(other.getOuterType())) {
                return false;
            }
            if (this.amount != other.amount) {
                return false;
            }
            if (this.id != other.id) {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + getOuterType().hashCode();
            result = prime * result + this.amount;
            result = prime * result + this.id;
            return result;
        }

        private OSMTagMapping getOuterType() {
            return OSMTagMapping.this;
        }
    }

    private static final Logger LOGGER = Logger.getLogger(OSMTagMapping.class.getName());

    private static OSMTagMapping mapping;
    private static final String XPATH_EXPRESSION_DEFAULT_ZOOM = "/tag-mapping/@default-zoom-appear";

    private static final String XPATH_EXPRESSION_POIS = "//pois/osm-tag["
            + "(../@enabled='true' or not(../@enabled)) and (./@enabled='true' or not(./@enabled)) "
            + "or (../@enabled='false' and ./@enabled='true')]";
    private static final String XPATH_EXPRESSION_WAYS = "//ways/osm-tag["
            + "(../@enabled='true' or not(../@enabled)) and (./@enabled='true' or not(./@enabled)) "
            + "or (../@enabled='false' and ./@enabled='true')]";

    /**
     * @return a new instance
     */
    public static synchronized OSMTagMapping getInstance() {
        if (mapping == null) {
            mapping = getInstance(MapFileWriterTask.class.getClassLoader().getResource("tag-mapping.xml"));
        }

        return mapping;
    }

    /**
     * @param tagConf the {@link URL} to a file that contains a tag configuration
     * @return a new instance
     */
    public static OSMTagMapping getInstance(URL tagConf) {
        if (mapping != null) {
            throw new IllegalStateException("mapping already initialized");
        }

        mapping = new OSMTagMapping(tagConf);
        return mapping;
    }

    private boolean hasTagValues = true;

    private final Map<Short, OSMTag> idToPoiTag = new LinkedHashMap<>();
    private final Map<Short, OSMTag> idToWayTag = new LinkedHashMap<>();

    private final Map<Short, Short> optimizedPoiIds = new LinkedHashMap<>();
    private final Map<Short, Short> optimizedWayIds = new LinkedHashMap<>();

    private short poiID = 0;

    private final Map<Short, Set<OSMTag>> poiZoomOverrides = new LinkedHashMap<>();

    // we use LinkedHashMaps as they guarantee to uphold the
    // insertion order when iterating over the key or value "set"
    private final Map<String, OSMTag> stringToPoiTag = new LinkedHashMap<>();

    private final Map<String, OSMTag> stringToWayTag = new LinkedHashMap<>();

    private short wayID = 0;

    private final Map<Short, Set<OSMTag>> wayZoomOverrides = new LinkedHashMap<>();

    private OSMTagMapping(URL tagConf) {
        try {
            byte defaultZoomAppear;

            // ---- Parse XML file ----
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(tagConf.openStream());

            XPath xpath = XPathFactory.newInstance().newXPath();

            XPathExpression xe = xpath.compile(XPATH_EXPRESSION_DEFAULT_ZOOM);
            defaultZoomAppear = Byte.parseByte((String) xe.evaluate(document, XPathConstants.STRING));

            final HashMap<Short, Set<String>> tmpPoiZoomOverrides = new HashMap<>();
            final HashMap<Short, Set<String>> tmpWayZoomOverrides = new HashMap<>();

            // ---- Get list of poi nodes ----
            xe = xpath.compile(XPATH_EXPRESSION_POIS);
            NodeList pois = (NodeList) xe.evaluate(document, XPathConstants.NODESET);

            for (int i = 0; i < pois.getLength(); i++) {
                NamedNodeMap attributes = pois.item(i).getAttributes();
                String key = attributes.getNamedItem("key").getTextContent();
                String value = attributes.getNamedItem("value").getTextContent();

                String[] equivalentValues = null;
                if (attributes.getNamedItem("equivalent-values") != null) {
                    equivalentValues = attributes.getNamedItem("equivalent-values").getTextContent().split(",");
                }

                byte zoom = attributes.getNamedItem("zoom-appear") == null ? defaultZoomAppear : Byte
                        .parseByte(attributes.getNamedItem("zoom-appear").getTextContent());

                boolean renderable = attributes.getNamedItem("renderable") == null ? true : Boolean
                        .parseBoolean(attributes.getNamedItem("renderable").getTextContent());

                boolean forcePolygonLine = attributes.getNamedItem("force-polygon-line") == null ? false : Boolean
                        .parseBoolean(attributes.getNamedItem("force-polygon-line").getTextContent());

                boolean labelPosition = attributes.getNamedItem("label-position") == null ? false : Boolean
                        .parseBoolean(attributes.getNamedItem("label-position").getTextContent());

                OSMTag osmTag = new OSMTag(this.poiID, key, value, zoom, renderable, forcePolygonLine, labelPosition);
                if (!addPoiTag(osmTag, equivalentValues)) continue;

                // check if this tag overrides the zoom level spec of another tag
                NodeList zoomOverrideNodes = pois.item(i).getChildNodes();
                for (int j = 0; j < zoomOverrideNodes.getLength(); j++) {
                    Node overriddenNode = zoomOverrideNodes.item(j);
                    if (overriddenNode instanceof Element) {
                        String keyOverridden = overriddenNode.getAttributes().getNamedItem("key").getTextContent();
                        String valueOverridden = overriddenNode.getAttributes().getNamedItem("value").getTextContent();
                        Set<String> s = tmpPoiZoomOverrides.get(Short.valueOf(this.poiID));
                        if (s == null) {
                            s = new HashSet<>();
                            tmpPoiZoomOverrides.put(Short.valueOf(this.poiID), s);
                        }
                        s.add(OSMTag.tagKey(keyOverridden, valueOverridden));
                    }
                }

                this.poiID++;
            }

            // ---- Get list of way nodes ----
            xe = xpath.compile(XPATH_EXPRESSION_WAYS);
            NodeList ways = (NodeList) xe.evaluate(document, XPathConstants.NODESET);

            for (int i = 0; i < ways.getLength(); i++) {
                NamedNodeMap attributes = ways.item(i).getAttributes();
                String key = attributes.getNamedItem("key").getTextContent();
                String value = attributes.getNamedItem("value").getTextContent();

                String[] equivalentValues = null;
                if (attributes.getNamedItem("equivalent-values") != null) {
                    equivalentValues = attributes.getNamedItem("equivalent-values").getTextContent().split(",");
                }

                byte zoom = attributes.getNamedItem("zoom-appear") == null ? defaultZoomAppear : Byte
                        .parseByte(attributes.getNamedItem("zoom-appear").getTextContent());

                boolean renderable = attributes.getNamedItem("renderable") == null ? true : Boolean
                        .parseBoolean(attributes.getNamedItem("renderable").getTextContent());

                boolean forcePolygonLine = attributes.getNamedItem("force-polygon-line") == null ? false : Boolean
                        .parseBoolean(attributes.getNamedItem("force-polygon-line").getTextContent());

                boolean labelPosition = attributes.getNamedItem("label-position") == null ? false : Boolean
                        .parseBoolean(attributes.getNamedItem("label-position").getTextContent());

                OSMTag osmTag = new OSMTag(this.wayID, key, value, zoom, renderable, forcePolygonLine, labelPosition);
                if (!addWayTag(osmTag, equivalentValues)) continue;

                // check if this tag overrides the zoom level spec of another tag
                NodeList zoomOverrideNodes = ways.item(i).getChildNodes();
                for (int j = 0; j < zoomOverrideNodes.getLength(); j++) {
                    Node overriddenNode = zoomOverrideNodes.item(j);
                    if (overriddenNode instanceof Element) {
                        String keyOverridden = overriddenNode.getAttributes().getNamedItem("key").getTextContent();
                        String valueOverridden = overriddenNode.getAttributes().getNamedItem("value").getTextContent();
                        Set<String> s = tmpWayZoomOverrides.get(Short.valueOf(this.wayID));
                        if (s == null) {
                            s = new HashSet<>();
                            tmpWayZoomOverrides.put(Short.valueOf(this.wayID), s);
                        }
                        s.add(OSMTag.tagKey(keyOverridden, valueOverridden));
                    }
                }

                this.wayID++;
            }

            // copy temporary values from zoom-override data sets
            for (Entry<Short, Set<String>> entry : tmpPoiZoomOverrides.entrySet()) {
                Set<OSMTag> overriddenTags = new HashSet<>();
                for (String tagString : entry.getValue()) {
                    OSMTag tag = this.stringToPoiTag.get(tagString);
                    if (tag != null) {
                        overriddenTags.add(tag);
                    }
                }
                if (!overriddenTags.isEmpty()) {
                    this.poiZoomOverrides.put(entry.getKey(), overriddenTags);
                }
            }

            for (Entry<Short, Set<String>> entry : tmpWayZoomOverrides.entrySet()) {
                Set<OSMTag> overriddenTags = new HashSet<>();
                for (String tagString : entry.getValue()) {
                    OSMTag tag = this.stringToWayTag.get(tagString);
                    if (tag != null) {
                        overriddenTags.add(tag);
                    }
                }
                if (!overriddenTags.isEmpty()) {
                    this.wayZoomOverrides.put(entry.getKey(), overriddenTags);
                }
            }

            // ---- Error handling ----
        } catch (SAXParseException spe) {
            LOGGER.severe("\n** Parsing error, line " + spe.getLineNumber() + ", uri " + spe.getSystemId());
            throw new IllegalStateException(spe);
        } catch (SAXException sxe) {
            throw new IllegalStateException(sxe);
        } catch (ParserConfigurationException pce) {
            throw new IllegalStateException(pce);
        } catch (IOException ioe) {
            throw new IllegalStateException(ioe);
        } catch (XPathExpressionException e) {
            throw new IllegalStateException(e);
        }
    }

    private boolean addPoiTag(OSMTag osmTag, String[] equivalentValues) {
        if (this.stringToPoiTag.containsKey(osmTag.tagKey())) {
            LOGGER.warning("duplicate osm-tag found in tag-mapping configuration (ignoring): " + osmTag);
            return false;
        }
        LOGGER.finest("adding poi: " + osmTag);
        this.stringToPoiTag.put(osmTag.tagKey(), osmTag);
        if (equivalentValues != null) {
            for (String equivalentValue : equivalentValues) {
                this.stringToPoiTag.put(OSMTag.tagKey(osmTag.getKey(), equivalentValue), osmTag);
            }
        }
        this.idToPoiTag.put(Short.valueOf(this.poiID), osmTag);

        // also fill optimization mapping with identity
        this.optimizedPoiIds.put(Short.valueOf(this.poiID), Short.valueOf(this.poiID));

        return true;
    }

    private boolean addWayTag(OSMTag osmTag, String[] equivalentValues) {
        if (this.stringToWayTag.containsKey(osmTag.tagKey())) {
            LOGGER.warning("duplicate osm-tag found in tag-mapping configuration (ignoring): " + osmTag);
            return false;
        }
        LOGGER.finest("adding way: " + osmTag);
        this.stringToWayTag.put(osmTag.tagKey(), osmTag);
        if (equivalentValues != null) {
            for (String equivalentValue : equivalentValues) {
                this.stringToWayTag.put(OSMTag.tagKey(osmTag.getKey(), equivalentValue), osmTag);
            }
        }
        this.idToWayTag.put(Short.valueOf(this.wayID), osmTag);

        // also fill optimization mapping with identity
        this.optimizedWayIds.put(Short.valueOf(this.wayID), Short.valueOf(this.wayID));

        return true;
    }

    /**
     * @return a mapping that maps original tag ids to the optimized ones
     */
    public Map<Short, Short> getOptimizedPoiIds() {
        return this.optimizedPoiIds;
    }

    /**
     * @return a mapping that maps original tag ids to the optimized ones
     */
    public Map<Short, Short> getOptimizedWayIds() {
        return this.optimizedWayIds;
    }

    /**
     * @param id the id
     * @return the corresponding {@link OSMTag}
     */
    public OSMTag getPoiTag(short id) {
        return this.idToPoiTag.get(Short.valueOf(id));
    }

    /**
     * @param tagIds the ids
     * @return the corresponding {@link OSMTag}s
     */
    public List<OSMTag> getPoiTags(short[] tagIds) {
        List<OSMTag> tags = new ArrayList<>();
        for (short tagId : tagIds) {
            tags.add(getPoiTag(tagId));
        }
        return tags;
    }

    /**
     * @param key   the key
     * @param value the value
     * @return the corresponding {@link OSMTag}
     */
    public OSMTag getPoiTag(String key, String value) {
        OSMTag tag;
        if ((tag = this.stringToPoiTag.get(OSMTag.tagKey(key, value))) != null) return tag;
        if (!hasTagValues) return null;
        String vType = OSMUtils.getValueType(key, value);

        if (vType.charAt(1) != 's') {
            if ((tag = this.stringToPoiTag.get(OSMTag.tagKey(key, "%f"))) == null) return null;
            tag = checkAlternativePoiTag(key, vType, tag);
        } else {
            tag = this.stringToPoiTag.get(OSMTag.tagKey(key, vType));
            if (tag != null)
                LOGGER.fine(key + ":\t" + value);
        }

        return tag;
    }

    private OSMTag checkAlternativePoiTag(String key, String value, OSMTag original) {
        assert original != null;
        OSMTag tag = this.stringToPoiTag.get(OSMTag.tagKey(key, value));
        if (tag == null) {
            tag = new OSMTag(this.poiID, original.getKey(), value, original.getZoomAppear(), original.isRenderable(), original.isForcePolygonLine(), original.isLabelPosition());
            if (addPoiTag(tag, null)) {
                this.poiID++;
            } else {
                tag = original;
            }
        }
        return tag;
    }

    /**
     * @param id the id
     * @return the corresponding {@link OSMTag}
     */
    public OSMTag getWayTag(short id) {
        return this.idToWayTag.get(Short.valueOf(id));
    }

    /**
     * @param tagIds the ids
     * @return the corresponding {@link OSMTag}s
     */
    public List<OSMTag> getWayTags(Set<Short> tagIds) {
        List<OSMTag> tags = new ArrayList<>();
        for (short tagId : tagIds) {
            tags.add(getWayTag(tagId));
        }
        return tags;
    }

    // /**
    // * @param tags
    // * the tags
    // * @return
    // */
    // private static short[] tagIDsFromList(List<OSMTag> tags) {
    // short[] tagIDs = new short[tags.size()];
    // int i = 0;
    // for (OSMTag tag : tags) {
    // tagIDs[i++] = tag.getId();
    // }
    //
    // return tagIDs;
    // }

    /**
     * @param key   the key
     * @param value the value
     * @return the corresponding {@link OSMTag}
     */
    public OSMTag getWayTag(String key, String value) {
        OSMTag tag;
        if ((tag = this.stringToWayTag.get(OSMTag.tagKey(key, value))) != null) return tag;
        if (!hasTagValues) return null;
        String vType = OSMUtils.getValueType(key, value);

        if (vType.charAt(1) != 's') {
            if ((tag = this.stringToWayTag.get(OSMTag.tagKey(key, "%f"))) == null) return null;
            tag = checkAlternativeWayTag(key, vType, tag);
        } else {
            tag = this.stringToWayTag.get(OSMTag.tagKey(key, vType));
            if (tag != null)
                LOGGER.fine(key + ":\t" + value);
        }

        return tag;
    }

    private OSMTag checkAlternativeWayTag(String key, String value, OSMTag original) {
        assert original != null;
        OSMTag tag = this.stringToWayTag.get(OSMTag.tagKey(key, value));
        if (tag == null) {
            tag = new OSMTag(this.wayID, original.getKey(), value, original.getZoomAppear(), original.isRenderable(), original.isForcePolygonLine(), original.isLabelPosition());
            if (addWayTag(tag, null)) {
                this.wayID++;
            } else {
                tag = original;
            }
        }
        return tag;
    }

    /**
     * @param tagSet the tag set
     * @return the minimum zoom level of all tags in the tag set
     */
    public byte getZoomAppearPOI(Set<Short> tagSet) {
        if (tagSet == null || tagSet.size() == 0) {
            return Byte.MAX_VALUE;
        }

        TShortHashSet tmp = new TShortHashSet(tagSet);

        if (!this.poiZoomOverrides.isEmpty()) {
            for (short s : tagSet) {
                Set<OSMTag> overriddenTags = this.poiZoomOverrides.get(Short.valueOf(s));
                if (overriddenTags != null) {
                    for (OSMTag osmTag : overriddenTags) {
                        tmp.remove(osmTag.getId());
                    }
                }
            }

            if (tmp.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                for (short s : tagSet) {
                    sb.append(this.idToPoiTag.get(Short.valueOf(s)).tagKey() + "; ");
                }
                LOGGER.severe("ERROR: You have a cycle in your zoom-override definitions. Look for these tags: "
                        + sb.toString());
            }
        }

        byte zoomAppear = Byte.MAX_VALUE;
        for (short s : tmp.toArray()) {
            OSMTag tag = this.idToPoiTag.get(Short.valueOf(s));
            if (tag.isRenderable()) {
                zoomAppear = (byte) Math.min(zoomAppear, tag.getZoomAppear());
            }
        }

        return zoomAppear;
    }

    /**
     * @param tagSet the tag set
     * @return the minimum zoom level of all the tags in the set
     */
    public byte getZoomAppearWay(Set<Short> tagSet) {
        if (tagSet == null || tagSet.size() == 0) {
            return Byte.MAX_VALUE;
        }

        TShortHashSet tmp = new TShortHashSet(tagSet);

        if (!this.wayZoomOverrides.isEmpty()) {
            for (short s : tagSet) {
                Set<OSMTag> overriddenTags = this.wayZoomOverrides.get(Short.valueOf(s));
                if (overriddenTags != null) {
                    for (OSMTag osmTag : overriddenTags) {
                        tmp.remove(osmTag.getId());
                    }
                }
            }

            if (tmp.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                for (short s : tagSet) {
                    sb.append(this.idToWayTag.get(Short.valueOf(s)).tagKey() + "; ");
                }
                LOGGER.severe("ERROR: You have a cycle in your zoom-override definitions. Look for these tags: "
                        + sb.toString());
            }
        }
        byte zoomAppear = Byte.MAX_VALUE;
        for (short s : tmp.toArray()) {
            OSMTag tag = this.idToWayTag.get(Short.valueOf(s));
            if (tag.isRenderable()) {
                zoomAppear = (byte) Math.min(zoomAppear, tag.getZoomAppear());
            }
        }

        return zoomAppear;
    }

    /**
     * @param histogram a histogram that represents the frequencies of tags
     */
    public void optimizePoiOrdering(TShortIntHashMap histogram) {
        this.optimizedPoiIds.clear();
        final TreeSet<HistogramEntry> poiOrdering = new TreeSet<>();

        histogram.forEachEntry(new TShortIntProcedure() {
            @Override
            public boolean execute(short tag, int amount) {
                poiOrdering.add(new HistogramEntry(tag, amount));
                return true;
            }
        });

        short tmpPoiID = 0;

        OSMTag currentTag = null;
        for (HistogramEntry histogramEntry : poiOrdering.descendingSet()) {
            currentTag = this.idToPoiTag.get(Short.valueOf(histogramEntry.id));
            this.optimizedPoiIds.put(Short.valueOf(histogramEntry.id), Short.valueOf(tmpPoiID));
            LOGGER.finer("adding poi tag: " + currentTag.tagKey() + " id:" + tmpPoiID + " amount: "
                    + histogramEntry.amount);
            tmpPoiID++;
        }
    }

    /**
     * @param histogram a histogram that represents the frequencies of tags
     */
    public void optimizeWayOrdering(TShortIntHashMap histogram) {
        this.optimizedWayIds.clear();
        final TreeSet<HistogramEntry> wayOrdering = new TreeSet<>();

        histogram.forEachEntry(new TShortIntProcedure() {
            @Override
            public boolean execute(short tag, int amount) {
                wayOrdering.add(new HistogramEntry(tag, amount));
                return true;
            }
        });
        short tmpWayID = 0;

        OSMTag currentTag = null;
        for (HistogramEntry histogramEntry : wayOrdering.descendingSet()) {
            currentTag = this.idToWayTag.get(Short.valueOf(histogramEntry.id));
            this.optimizedWayIds.put(Short.valueOf(histogramEntry.id), Short.valueOf(tmpWayID));
            LOGGER.finer("adding way tag: " + currentTag.tagKey() + " id:" + tmpWayID + " amount: "
                    + histogramEntry.amount);
            tmpWayID++;
        }
    }

    /**
     * @param hasTagValues if true optional tag values are stored
     */
    public void storeTagValues(boolean hasTagValues) {
        this.hasTagValues = hasTagValues;
    }
}
