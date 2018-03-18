/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright 2015-2018 devemux86
 * Copyright 2015-2016 lincomatic
 * Copyright 2017 Gustl22
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
package org.mapsforge.map.writer.util;

import org.mapsforge.map.writer.OSMTagMapping;
import org.mapsforge.map.writer.model.OSMTag;
import org.mapsforge.map.writer.model.SpecialTagExtractionResult;
import org.openstreetmap.osmosis.core.domain.v0_6.Entity;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * OpenStreetMap related utility methods.
 */
public final class OSMUtils {
    private static final Logger LOGGER = Logger.getLogger(OSMUtils.class.getName());

    private static final int MAX_ELEVATION = 9000;

    private static final Pattern COLOR_PATTERN = Pattern.compile("#([A-Fa-f0-9]{6})");
    //private static final String COLOR_EXTENDED_PATTERN = "(#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{8}|[A-Fa-f0-9]{3}))";
    private static final Pattern DIGIT_PATTERN = Pattern.compile("-?\\d+(\\.\\d+)?");
    private static final Pattern NAME_LANGUAGE_PATTERN = Pattern.compile("(name)(:)([a-zA-Z]{1,3}(?:[-_][a-zA-Z0-9]{1,8})*)");

    private static int nBytes, nFloats, nIntegers, nShorts, nStrings = 0;

    /**
     * Extracts known POI tags and returns their ids.
     *
     * @param entity the node
     * @return the ids of the identified tags
     */
    public static Map<Short, Object> extractKnownPOITags(Entity entity) {
        Map<Short, Object> tagMap = new HashMap<>();
        OSMTagMapping mapping = OSMTagMapping.getInstance();
        if (entity.getTags() != null) {
            for (Tag tag : entity.getTags()) {
                OSMTag poiTag = mapping.getPoiTag(tag.getKey(), tag.getValue());
                if (poiTag != null) {
                    String wildcard = poiTag.getValue();
                    tagMap.put(poiTag.getId(), getObjectFromWildcardAndValue(wildcard, tag.getValue()));
                }
            }
        }
        return tagMap;
    }

    /**
     * Extracts known way tags and returns their ids.
     *
     * @param entity the way
     * @return the ids of the identified tags
     */
    public static Map<Short, Object> extractKnownWayTags(Entity entity) {
        Map<Short, Object> tagMap = new HashMap<>();
        OSMTagMapping mapping = OSMTagMapping.getInstance();
        if (entity.getTags() != null) {
            for (Tag tag : entity.getTags()) {
                OSMTag wayTag = mapping.getWayTag(tag.getKey(), tag.getValue());
                if (wayTag != null) {
                    String wildcard = wayTag.getValue();
                    tagMap.put(wayTag.getId(), getObjectFromWildcardAndValue(wildcard, tag.getValue()));
                }
            }
        }
        return tagMap;
    }

    /**
     * Extracts special fields and returns their values as an array of strings.
     * <p/>
     * Use '\r' delimiter among names and '\b' delimiter between each language and name.
     *
     * @param entity             the entity
     * @param preferredLanguages the preferred language(s)
     * @return a string array, [0] = name, [1] = ref, [2} = housenumber, [3] layer, [4] elevation, [5] relationType
     */
    public static SpecialTagExtractionResult extractSpecialFields(Entity entity, List<String> preferredLanguages) {
        String name = null;
        String ref = null;
        String housenumber = null;
        byte layer = 5;
        short elevation = 0;
        String relationType = null;

        if (entity.getTags() != null) {
            // Process 'name' tags
            if (preferredLanguages != null && preferredLanguages.size() > 1) { // Multilingual map
                // Convert tag collection to list and sort it
                // i.e. making sure default 'name' comes first
                List<Tag> tags = new ArrayList<Tag>(entity.getTags());
                Collections.sort(tags);

                String defaultName = null;
                List<String> restPreferredLanguages = new ArrayList<String>(preferredLanguages);
                for (Tag tag : tags) {
                    String key = tag.getKey().toLowerCase(Locale.ENGLISH);
                    if ("name".equals(key)) { // Default 'name'
                        defaultName = tag.getValue();
                        name = defaultName;
                    } else { // Localized name
                        Matcher matcher = NAME_LANGUAGE_PATTERN.matcher(key);
                        if (!matcher.matches()) {
                            continue;
                        }
                        if (tag.getValue().equals(defaultName)) { // Same with default 'name'?
                            continue;
                        }
                        String language = matcher.group(3).toLowerCase(Locale.ENGLISH).replace('_', '-');
                        if (preferredLanguages.contains(language)) {
                            restPreferredLanguages.remove(language);
                            name = (name != null ? name + '\r' : "") + language + '\b' + tag.getValue();
                        }
                    }
                }

                // Check rest preferred languages for falling back to base
                if (!restPreferredLanguages.isEmpty()) {
                    Map<String, String> fallbacks = new HashMap<String, String>();
                    for (String preferredLanguage : restPreferredLanguages) {
                        for (Tag tag : tags) {
                            String key = tag.getKey().toLowerCase(Locale.ENGLISH);
                            Matcher matcher = NAME_LANGUAGE_PATTERN.matcher(key);
                            if (!matcher.matches()) {
                                continue;
                            }
                            if (tag.getValue().equals(defaultName)) { // Same with default 'name'?
                                continue;
                            }
                            String language = matcher.group(3).toLowerCase(Locale.ENGLISH).replace('_', '-');
                            if (!fallbacks.containsKey(language) && !language.contains("-") && (preferredLanguage.contains("-") || preferredLanguage.contains("_"))
                                    && preferredLanguage.toLowerCase(Locale.ENGLISH).startsWith(language)) {
                                fallbacks.put(language, tag.getValue());
                            }
                        }
                    }
                    for (String language : fallbacks.keySet()) {
                        name = (name != null ? name + '\r' : "") + language + '\b' + fallbacks.get(language);
                    }
                }
            } else { // Non multilingual map
                boolean foundPreferredLanguageName = false;
                for (Tag tag : entity.getTags()) {
                    String key = tag.getKey().toLowerCase(Locale.ENGLISH);
                    if ("name".equals(key) && !foundPreferredLanguageName) {
                        name = tag.getValue();
                    } else if (preferredLanguages != null && !foundPreferredLanguageName) {
                        Matcher matcher = NAME_LANGUAGE_PATTERN.matcher(key);
                        if (matcher.matches()) {
                            String language = matcher.group(3);
                            if (language.equalsIgnoreCase(preferredLanguages.get(0))) {
                                name = tag.getValue();
                                foundPreferredLanguageName = true;
                            }
                        }
                    }
                }
            }

            // Process rest tags
            for (Tag tag : entity.getTags()) {
                String key = tag.getKey().toLowerCase(Locale.ENGLISH);
                if ("piste:name".equals(key) && name == null) {
                    name = tag.getValue();
                } else if ("addr:housenumber".equals(key)) {
                    housenumber = tag.getValue();
                } else if ("ref".equals(key)) {
                    ref = tag.getValue();
                } else if ("layer".equals(key)) {
                    String l = tag.getValue();
                    try {
                        byte testLayer = Byte.parseByte(l);
                        if (testLayer >= -5 && testLayer <= 5) {
                            testLayer += 5;
                        }
                        layer = testLayer;
                    } catch (NumberFormatException e) {
                        LOGGER.finest("could not parse layer information to byte type: " + tag.getValue()
                                + "\t entity-id: " + entity.getId() + "\tentity-type: " + entity.getType().name());
                    }
                } else if ("ele".equals(key)) {
                    Double floatElevation = parseDoubleUnit(tag.getValue());
                    if (floatElevation != null) {
                        if (floatElevation < MAX_ELEVATION) {
                            elevation = floatElevation.shortValue();
                        }
                    } else {
                        LOGGER.finest("could not parse elevation information to double type: " + tag.getValue()
                                + "\t entity-id: " + entity.getId() + "\tentity-type: " + entity.getType().name());
                    }
                } else if ("type".equals(key)) {
                    relationType = tag.getValue();
                }
            }
        }

        return new SpecialTagExtractionResult(name, ref, housenumber, layer, elevation, relationType);
    }


    /**
     * @param value string represented numerical value
     * @return value as Byte
     */
    public static Byte getByteValue(String value) {
        nBytes++;
        return parseDoubleUnit(value).byteValue();
    }

    /**
     * @param value string represented numerical value
     * @return value as Float
     */
    public static Float getFloatValue(String value) {
        nFloats++;
        return parseDoubleUnit(value).floatValue();
    }

    /**
     * @param value numerical or color-representing value as string
     * @return corresponding Integer
     */
    public static Integer getIntegerValue(String value) {
        nIntegers++;
        Integer integer;
        if (Character.isLetter(value.charAt(0))) {
            integer = ColorsCSS.get(value);
            if (integer != null) {
                LOGGER.finest("ColorNam: #" + Integer.toHexString(integer));
                return integer;
            }
        }
        Matcher matcher = COLOR_PATTERN.matcher(value);
        if (matcher.matches()) {
            // TODO convert alpha colors too
            try {
                integer = Color.decode(value).getRGB();
                LOGGER.finest("ColorHex: #" + Integer.toHexString(integer));
            } catch (NumberFormatException e) {
                integer = 0;
                LOGGER.warning("Color conversion failed: " + value + "\n" + e.getMessage());
            }
        } else {
            integer = OSMUtils.parseDoubleUnit(value).intValue();
        }
        return integer;
    }

    /**
     * @param wildcard the type of value as wildcard
     * @param value    the value as string
     * @return an object that represents value
     */
    public static Object getObjectFromWildcardAndValue(String wildcard, String value) {
        if (wildcard.length() == 2 && wildcard.charAt(0) == '%') {
            Character format = wildcard.charAt(1);
            if (format == 'b') {
                return getByteValue(value);
            } else if (format == 'i') {
                return getIntegerValue(value);
            } else if (format == 'f') {
                return getFloatValue(value);
            } else if (format == 'h') {
                return getShortValue(value);
            } else if (format == 's') {
                return getStringValue(value);
            }
        }
        return null;
    }

    /**
     * @param value string represented numerical value
     * @return value as Short
     */
    public static Short getShortValue(String value) {
        nShorts++;
        return parseDoubleUnit(value).shortValue();
    }

    /**
     * @param value string
     * @return formatted value
     */
    public static String getStringValue(String value) {
        // Do some string formation
        nStrings++;
        return value;
    }

    /**
     * @param value string represented value
     * @return a string represented primitive type
     */
    public static String getValueType(String key, String value) {
        Double f = OSMUtils.parseDoubleUnit(value);
        if (f != null) {
            if (Math.round(f) == f) {
                if (f.byteValue() == f) {
                    return "%b";
                } else if (f.shortValue() == f) {
                    return "%h";
                } else {
                    return "%i";
                }
            }
            return "%f";
        }
        if (key.contains("colour")) {
            Matcher matcher = COLOR_PATTERN.matcher(value); // Encode color as integer
            if (matcher.matches() || ColorsCSS.get(value) != null) {
                return "%i";
            }
        }
        return "%s";
    }

    /**
     * Heuristic to determine from attributes if a way is likely to be an area.
     * Precondition for this call is that the first and last node of a way are the
     * same, so that this method should only return false if it is known that the
     * feature should not be an area even if the geometry is a polygon.
     * <p/>
     * Determining what is an area is neigh impossible in OSM, this method inspects tag elements
     * to give a likely answer. See http://wiki.openstreetmap.org/wiki/The_Future_of_Areas and
     * http://wiki.openstreetmap.org/wiki/Way
     *
     * @param way the way (which is assumed to be closed and have enough nodes to be an area)
     * @return true if tags indicate this is an area, otherwise false.
     */
    public static boolean isArea(Way way) {
        boolean result = true;
        if (way.getTags() != null) {
            for (Tag tag : way.getTags()) {
                String key = tag.getKey().toLowerCase(Locale.ENGLISH);
                String value = tag.getValue().toLowerCase(Locale.ENGLISH);
                if ("area".equals(key)) {
                    // obvious result
                    if (("yes").equals(value) || ("y").equals(value) || ("true").equals(value)) {
                        return true;
                    }
                    if (("no").equals(value) || ("n").equals(value) || ("false").equals(value)) {
                        return false;
                    }
                }
                // as specified by http://wiki.openstreetmap.org/wiki/Key:area
                if ("aeroway".equals(key) || "building".equals(key) || "landuse".equals(key) || "leisure".equals(key) || "natural".equals(key) || "amenity".equals(key)) {
                    return true;
                }
                if ("highway".equals(key) || "barrier".equals(key)) {
                    // false unless something else overrides this.
                    result = false;
                }
                if ("railway".equals(key)) {
                    // there is more to the railway tag then just rails, this excludes the
                    // most common railway lines from being detected as areas if they are closed.
                    // Since this method is only called if the first and last node are the same
                    // this should be safe
                    if ("rail".equals(value) || "tram".equals(value) || "subway".equals(value)
                            || "monorail".equals(value) || "narrow_gauge".equals(value) || "preserved".equals(value)
                            || "light_rail".equals(value) || "construction".equals(value)) {
                        result = false;
                    }
                }
            }
        }
        return result;
    }

    public static String logValueTypeCount() {
        return "Bytes:\t" + nBytes + "\nShorts:\t" + nShorts + "\nIntegers:\t" + nIntegers
                + "\nFloats:\t" + nFloats + "\nStrings:\t" + nStrings;
    }

    /**
     * @param value value as string
     * @return parsed number if numerical, else null
     */
    public static Double parseDoubleUnit(String value) {
        value = value.replaceAll("m", "").replaceAll(",", ".");
        Matcher matcher = DIGIT_PATTERN.matcher(value);
        Double res = null;
        if (matcher.matches()) {
            try {
                res = Double.parseDouble(value);
            } catch (NumberFormatException ignored) {
            }
        }
        return res;
    }

    private OSMUtils() {
    }
}
