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
package org.mapsforge.map.rendertheme.rule;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Stack;
import java.util.regex.Pattern;

import org.mapsforge.map.rendertheme.XmlUtils;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * A builder for {@link Rule} instances.
 */
public class RuleBuilder {
	private static final String CLOSED = "closed";
	private static final String E = "e";
	private static final String K = "k";
	private static final Pattern SPLIT_PATTERN = Pattern.compile("\\|");
	private static final String STRING_NEGATION = "~";
	private static final String STRING_WILDCARD = "*";
	private static final String V = "v";
	private static final String ZOOM_MAX = "zoom-max";
	private static final String ZOOM_MIN = "zoom-min";

	private static ClosedMatcher getClosedMatcher(Closed closed) {
		switch (closed) {
			case YES:
				return ClosedWayMatcher.INSTANCE;
			case NO:
				return LinearWayMatcher.INSTANCE;
			case ANY:
				return AnyMatcher.INSTANCE;
		}

		throw new IllegalArgumentException("unknown closed value: " + closed);
	}

	private static ElementMatcher getElementMatcher(Element element) {
		switch (element) {
			case NODE:
				return ElementNodeMatcher.INSTANCE;
			case WAY:
				return ElementWayMatcher.INSTANCE;
			case ANY:
				return AnyMatcher.INSTANCE;
		}

		throw new IllegalArgumentException("unknown element value: " + element);
	}

	private static AttributeMatcher getKeyMatcher(List<String> keyList) {
		if (STRING_WILDCARD.equals(keyList.get(0))) {
			return AnyMatcher.INSTANCE;
		}

		AttributeMatcher attributeMatcher = Rule.MATCHERS_CACHE_KEY.get(keyList);
		if (attributeMatcher == null) {
			attributeMatcher = new KeyMatcher(keyList);
			Rule.MATCHERS_CACHE_KEY.put(keyList, attributeMatcher);
		}
		return attributeMatcher;
	}

	private static AttributeMatcher getValueMatcher(List<String> valueList) {
		if (STRING_WILDCARD.equals(valueList.get(0))) {
			return AnyMatcher.INSTANCE;
		}

		AttributeMatcher attributeMatcher = Rule.MATCHERS_CACHE_VALUE.get(valueList);
		if (attributeMatcher == null) {
			attributeMatcher = new ValueMatcher(valueList);
			Rule.MATCHERS_CACHE_VALUE.put(valueList, attributeMatcher);
		}
		return attributeMatcher;
	}

	ClosedMatcher closedMatcher;
	ElementMatcher elementMatcher;
	byte zoomMax;
	byte zoomMin;
	private Closed closed;
	private Element element;
	private List<String> keyList;
	private String keys;
	private final Stack<Rule> ruleStack;
	private List<String> valueList;
	private String values;

	public RuleBuilder(String elementName, Attributes attributes, Stack<Rule> ruleStack) throws SAXException {
		this.ruleStack = ruleStack;

		this.closed = Closed.ANY;
		this.zoomMin = 0;
		this.zoomMax = Byte.MAX_VALUE;

		extractValues(elementName, attributes);
	}

	/**
	 * @return a new {@code Rule} instance.
	 */
	public Rule build() {
		if (this.valueList.remove(STRING_NEGATION)) {
			AttributeMatcher attributeMatcher = new NegativeMatcher(this.keyList, this.valueList);
			return new NegativeRule(this, attributeMatcher);
		}

		AttributeMatcher keyMatcher = getKeyMatcher(this.keyList);
		AttributeMatcher valueMatcher = getValueMatcher(this.valueList);

		keyMatcher = RuleOptimizer.optimize(keyMatcher, this.ruleStack);
		valueMatcher = RuleOptimizer.optimize(valueMatcher, this.ruleStack);

		return new PositiveRule(this, keyMatcher, valueMatcher);
	}

	private void extractValues(String elementName, Attributes attributes) throws SAXException {
		for (int i = 0; i < attributes.getLength(); ++i) {
			String name = attributes.getQName(i);
			String value = attributes.getValue(i);

			if (E.equals(name)) {
				this.element = Element.valueOf(value.toUpperCase(Locale.ENGLISH));
			} else if (K.equals(name)) {
				this.keys = value;
			} else if (V.equals(name)) {
				this.values = value;
			} else if (CLOSED.equals(name)) {
				this.closed = Closed.valueOf(value.toUpperCase(Locale.ENGLISH));
			} else if (ZOOM_MIN.equals(name)) {
				this.zoomMin = XmlUtils.parseNonNegativeByte(name, value);
			} else if (ZOOM_MAX.equals(name)) {
				this.zoomMax = XmlUtils.parseNonNegativeByte(name, value);
			} else {
				throw XmlUtils.createSAXException(elementName, name, value, i);
			}
		}

		validate(elementName);

		this.keyList = new ArrayList<String>(Arrays.asList(SPLIT_PATTERN.split(this.keys)));
		this.valueList = new ArrayList<String>(Arrays.asList(SPLIT_PATTERN.split(this.values)));

		this.elementMatcher = getElementMatcher(this.element);
		this.closedMatcher = getClosedMatcher(this.closed);

		this.elementMatcher = RuleOptimizer.optimize(this.elementMatcher, this.ruleStack);
		this.closedMatcher = RuleOptimizer.optimize(this.closedMatcher, this.ruleStack);
	}

	private void validate(String elementName) throws SAXException {
		XmlUtils.checkMandatoryAttribute(elementName, E, this.element);
		XmlUtils.checkMandatoryAttribute(elementName, K, this.keys);
		XmlUtils.checkMandatoryAttribute(elementName, V, this.values);

		if (this.zoomMin > this.zoomMax) {
			throw new SAXException('\'' + ZOOM_MIN + "' > '" + ZOOM_MAX + "': " + this.zoomMin + ' ' + this.zoomMax);
		}
	}
}
