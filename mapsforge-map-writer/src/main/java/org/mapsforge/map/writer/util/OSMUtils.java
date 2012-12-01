/*
 * Copyright 2010, 2011, 2012 mapsforge.org
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

import gnu.trove.list.array.TShortArrayList;

import java.util.Locale;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.mapsforge.map.writer.OSMTagMapping;
import org.mapsforge.map.writer.model.OSMTag;
import org.mapsforge.map.writer.model.SpecialTagExtractionResult;
import org.openstreetmap.osmosis.core.domain.v0_6.Entity;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;

/**
 * OpenStreetMap related utility methods.
 * 
 * @author bross
 */
public final class OSMUtils {
	private OSMUtils() {
	}

	private static final Logger LOGGER = Logger.getLogger(OSMUtils.class.getName());

	private static final Pattern NAME_LANGUAGE_PATTERN = Pattern.compile("(name)(:)([a-z]{2})");
	private static final int MAX_ELEVATION = 9000;

	/**
	 * Extracts special fields and returns their values as an array of strings.
	 * 
	 * @param entity
	 *            the entity
	 * @param preferredLanguage
	 *            the preferred language
	 * @return a string array, [0] = name, [1] = ref, [2} = housenumber, [3] layer, [4] elevation, [5] relationType
	 */
	public static SpecialTagExtractionResult extractSpecialFields(Entity entity, String preferredLanguage) {
		boolean foundPreferredLanguageName = false;
		String name = null;
		String ref = null;
		String housenumber = null;
		byte layer = 5;
		short elevation = 0;
		String relationType = null;

		if (entity.getTags() != null) {
			for (Tag tag : entity.getTags()) {
				String key = tag.getKey().toLowerCase(Locale.ENGLISH);
				if ("name".equals(key) && !foundPreferredLanguageName) {
					name = tag.getValue();
				} else if ("piste:name".equals(key) && name == null) {
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
					String strElevation = tag.getValue();
					strElevation = strElevation.replaceAll("m", "");
					strElevation = strElevation.replaceAll(",", ".");
					try {
						double testElevation = Double.parseDouble(strElevation);
						if (testElevation < MAX_ELEVATION) {
							elevation = (short) testElevation;
						}
					} catch (NumberFormatException e) {
						LOGGER.finest("could not parse elevation information to double type: " + tag.getValue()
								+ "\t entity-id: " + entity.getId() + "\tentity-type: " + entity.getType().name());
					}

				} else if ("type".equals(key)) {
					relationType = tag.getValue();
				} else if (preferredLanguage != null && !foundPreferredLanguageName) {
					Matcher matcher = NAME_LANGUAGE_PATTERN.matcher(key);
					if (matcher.matches()) {
						String language = matcher.group(3);
						if (language.equalsIgnoreCase(preferredLanguage)) {
							name = tag.getValue();
							foundPreferredLanguageName = true;
						}
					}
				}
			}
		}

		return new SpecialTagExtractionResult(name, ref, housenumber, layer, elevation, relationType);
	}

	/**
	 * Extracts known way tags and returns their ids.
	 * 
	 * @param entity
	 *            the way
	 * @return the ids of the identified tags
	 */
	public static short[] extractKnownWayTags(Entity entity) {
		TShortArrayList currentTags = new TShortArrayList();
		OSMTagMapping mapping = OSMTagMapping.getInstance();
		if (entity.getTags() != null) {
			for (Tag tag : entity.getTags()) {
				OSMTag wayTag = mapping.getWayTag(tag.getKey(), tag.getValue());
				if (wayTag != null) {
					currentTags.add(wayTag.getId());
				}
			}
		}
		return currentTags.toArray();
	}

	/**
	 * Extracts known POI tags and returns their ids.
	 * 
	 * @param entity
	 *            the node
	 * @return the ids of the identified tags
	 */
	public static short[] extractKnownPOITags(Entity entity) {
		TShortArrayList currentTags = new TShortArrayList();
		OSMTagMapping mapping = OSMTagMapping.getInstance();
		if (entity.getTags() != null) {
			for (Tag tag : entity.getTags()) {
				OSMTag wayTag = mapping.getPoiTag(tag.getKey(), tag.getValue());
				if (wayTag != null) {
					currentTags.add(wayTag.getId());
				}
			}
		}
		return currentTags.toArray();
	}
}
