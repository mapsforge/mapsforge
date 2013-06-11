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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.mapsforge.core.model.Tag;
import org.mapsforge.map.rendertheme.RenderCallback;
import org.mapsforge.map.rendertheme.renderinstruction.RenderInstruction;

abstract class Rule {
	static final Map<List<String>, AttributeMatcher> MATCHERS_CACHE_KEY = new HashMap<List<String>, AttributeMatcher>();
	static final Map<List<String>, AttributeMatcher> MATCHERS_CACHE_VALUE = new HashMap<List<String>, AttributeMatcher>();

	final ClosedMatcher closedMatcher;
	final ElementMatcher elementMatcher;
	final byte zoomMax;
	final byte zoomMin;
	private final ArrayList<RenderInstruction> renderInstructions;
	private final ArrayList<Rule> subRules;

	Rule(RuleBuilder ruleBuilder) {
		this.closedMatcher = ruleBuilder.closedMatcher;
		this.elementMatcher = ruleBuilder.elementMatcher;
		this.zoomMax = ruleBuilder.zoomMax;
		this.zoomMin = ruleBuilder.zoomMin;

		this.renderInstructions = new ArrayList<RenderInstruction>(4);
		this.subRules = new ArrayList<Rule>(4);
	}

	void addRenderingInstruction(RenderInstruction renderInstruction) {
		this.renderInstructions.add(renderInstruction);
	}

	void addSubRule(Rule rule) {
		this.subRules.add(rule);
	}

	abstract boolean matchesNode(List<Tag> tags, byte zoomLevel);

	abstract boolean matchesWay(List<Tag> tags, byte zoomLevel, Closed closed);

	void matchNode(RenderCallback renderCallback, List<Tag> tags, byte zoomLevel) {
		if (matchesNode(tags, zoomLevel)) {
			for (int i = 0, n = this.renderInstructions.size(); i < n; ++i) {
				this.renderInstructions.get(i).renderNode(renderCallback, tags);
			}
			for (int i = 0, n = this.subRules.size(); i < n; ++i) {
				this.subRules.get(i).matchNode(renderCallback, tags, zoomLevel);
			}
		}
	}

	void matchWay(RenderCallback renderCallback, List<Tag> tags, byte zoomLevel, Closed closed,
			List<RenderInstruction> matchingList) {
		if (matchesWay(tags, zoomLevel, closed)) {
			for (int i = 0, n = this.renderInstructions.size(); i < n; ++i) {
				this.renderInstructions.get(i).renderWay(renderCallback, tags);
				matchingList.add(this.renderInstructions.get(i));
			}
			for (int i = 0, n = this.subRules.size(); i < n; ++i) {
				this.subRules.get(i).matchWay(renderCallback, tags, zoomLevel, closed, matchingList);
			}
		}
	}

	void onComplete() {
		MATCHERS_CACHE_KEY.clear();
		MATCHERS_CACHE_VALUE.clear();

		this.renderInstructions.trimToSize();
		this.subRules.trimToSize();
		for (int i = 0, n = this.subRules.size(); i < n; ++i) {
			this.subRules.get(i).onComplete();
		}
	}

	void scaleStrokeWidth(float scaleFactor) {
		for (int i = 0, n = this.renderInstructions.size(); i < n; ++i) {
			this.renderInstructions.get(i).scaleStrokeWidth(scaleFactor);
		}
		for (int i = 0, n = this.subRules.size(); i < n; ++i) {
			this.subRules.get(i).scaleStrokeWidth(scaleFactor);
		}
	}

	void scaleTextSize(float scaleFactor) {
		for (int i = 0, n = this.renderInstructions.size(); i < n; ++i) {
			this.renderInstructions.get(i).scaleTextSize(scaleFactor);
		}
		for (int i = 0, n = this.subRules.size(); i < n; ++i) {
			this.subRules.get(i).scaleTextSize(scaleFactor);
		}
	}
}
