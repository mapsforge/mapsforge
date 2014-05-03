/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
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
package org.mapsforge.map.writer.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import org.mapsforge.map.writer.OSMTagMapping;
import org.mapsforge.map.writer.util.OSMUtils;
import org.openstreetmap.osmosis.core.domain.v0_6.EntityType;
import org.openstreetmap.osmosis.core.domain.v0_6.Relation;
import org.openstreetmap.osmosis.core.domain.v0_6.RelationMember;

/**
 * Represents an OSM relation.
 */
public class TDRelation {
	private static final Logger LOGGER = Logger.getLogger(TDRelation.class.getName());

	/**
	 * Creates a new TDRelation from an osmosis entity using the given WayResolver.
	 * 
	 * @param relation
	 *            the relation
	 * @param resolver
	 *            the resolver
	 * @param preferredLanguage
	 *            the preferred language or null if no preference
	 * @return a new TDRelation if all members are valid and the relation is of a known type, null otherwise
	 */
	public static TDRelation fromRelation(Relation relation, WayResolver resolver, String preferredLanguage) {
		if (relation == null) {
			return null;
		}

		if (relation.getMembers().isEmpty()) {
			return null;
		}

		SpecialTagExtractionResult ster = OSMUtils.extractSpecialFields(relation, preferredLanguage);
		short[] knownWayTags = OSMUtils.extractKnownWayTags(relation);

		// special tags
		// TODO what about the layer of relations?

		// TODO exclude boundaries

		if (!knownRelationType(ster.getType())) {
			return null;
		}

		List<RelationMember> members = relation.getMembers();
		List<TDWay> wayMembers = new ArrayList<>();
		for (RelationMember relationMember : members) {
			if (relationMember.getMemberType() != EntityType.Way) {
				continue;
			}
			TDWay member = resolver.getWay(relationMember.getMemberId());
			if (member == null) {
				LOGGER.finest("relation is missing a member, rel-id: " + relation.getId() + " member id: "
						+ relationMember.getMemberId());
				continue;
			}
			wayMembers.add(member);
		}

		if (wayMembers.isEmpty()) {
			LOGGER.finest("relation has no valid members: " + relation.getId());
			return null;
		}

		return new TDRelation(relation.getId(), ster.getLayer(), ster.getName(), ster.getHousenumber(), ster.getRef(),
				knownWayTags, wayMembers.toArray(new TDWay[wayMembers.size()]));
	}

	/**
	 * @param type
	 *            the type attribute of a relation
	 * @return true if the type if known, currently only multipolygons are known
	 */
	// TODO adjust if more relations should be supported
	public static boolean knownRelationType(String type) {
		return type != null && "multipolygon".equals(type);
	}

	private final String houseNumber;
	private final long id;
	private final byte layer;
	private final TDWay[] memberWays;
	private final String name;

	private final String ref;

	private final short[] tags;

	/**
	 * Constructor.
	 * 
	 * @param id
	 *            the id
	 * @param layer
	 *            the layer
	 * @param name
	 *            the name
	 * @param houseNumber
	 *            the house number if existent
	 * @param ref
	 *            the ref attribute
	 * @param tags
	 *            the tags
	 * @param memberWays
	 *            the member ways
	 */
	TDRelation(long id, byte layer, String name, String houseNumber, String ref, short[] tags, TDWay[] memberWays) {
		this.id = id;
		this.layer = layer;
		this.name = name;
		this.houseNumber = houseNumber;
		this.ref = ref;
		this.tags = tags;
		this.memberWays = memberWays;
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
		TDRelation other = (TDRelation) obj;
		if (this.id != other.id) {
			return false;
		}
		return true;
	}

	/**
	 * @return the houseNumber
	 */
	public String getHouseNumber() {
		return this.houseNumber;
	}

	/**
	 * @return the id
	 */
	public long getId() {
		return this.id;
	}

	/**
	 * @return the layer
	 */
	public byte getLayer() {
		return this.layer;
	}

	/**
	 * @return the member ways
	 */
	public TDWay[] getMemberWays() {
		return this.memberWays;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * @return the ref
	 */
	public String getRef() {
		return this.ref;
	}

	/**
	 * @return the tags
	 */
	public short[] getTags() {
		return this.tags;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (this.id ^ (this.id >>> 32));
		return result;
	}

	/**
	 * @return true if the relation has associated tags
	 */
	public boolean hasTags() {
		return this.tags != null && this.tags.length > 0;
	}

	/**
	 * @return true if the relation represents a coastline
	 */
	public boolean isCoastline() {
		if (this.tags == null) {
			return false;
		}
		OSMTag tag;
		for (short tagID : this.tags) {
			tag = OSMTagMapping.getInstance().getWayTag(tagID);
			if (tag.isCoastline()) {
				return true;
			}
		}

		return false;
	}

	/**
	 * @return true if the relation is relevant for rendering
	 */
	public boolean isRenderRelevant() {
		return hasTags() || getName() != null && !getName().isEmpty() || getRef() != null && !getRef().isEmpty();
	}

	@Override
	public String toString() {
		return "TDRelation [id=" + this.id + ", layer=" + this.layer + ", name=" + this.name + ", ref=" + this.ref
				+ ", tags=" + Arrays.toString(this.tags) + "]";
	}
}
