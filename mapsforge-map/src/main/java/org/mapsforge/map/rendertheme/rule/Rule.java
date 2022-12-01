/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright 2014-2015 Ludwig M Brinckmann
 * Copyright 2016 devemux86
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

import org.mapsforge.core.model.Tag;
import org.mapsforge.core.model.Tile;
import org.mapsforge.map.datastore.PointOfInterest;
import org.mapsforge.map.layer.renderer.PolylineContainer;
import org.mapsforge.map.rendertheme.RenderCallback;
import org.mapsforge.map.rendertheme.RenderContext;
import org.mapsforge.map.rendertheme.renderinstruction.RenderInstruction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class Rule {

    static final Map<List<String>, AttributeMatcher> MATCHERS_CACHE_KEY = new HashMap<>();
    static final Map<List<String>, AttributeMatcher> MATCHERS_CACHE_VALUE = new HashMap<>();

    public static class RuleVisitor {
        public void apply(Rule r) {
            for (Rule subRule : r.subRules) {
                this.apply(subRule);
            }
        }
    }

    String cat;
    final ClosedMatcher closedMatcher;
    final ElementMatcher elementMatcher;
    final byte zoomMax;
    final byte zoomMin;
    // conversion to Array[] tested, but performance difference was not measurable
    private final ArrayList<RenderInstruction> renderInstructions; // NOSONAR NOPMD we need specific interface
    private final ArrayList<Rule> subRules; // NOSONAR NOPMD we need specific interface

    Rule(RuleBuilder ruleBuilder) {
        this.cat = ruleBuilder.cat;
        this.closedMatcher = ruleBuilder.closedMatcher;
        this.elementMatcher = ruleBuilder.elementMatcher;
        this.zoomMax = ruleBuilder.zoomMax;
        this.zoomMin = ruleBuilder.zoomMin;

        this.renderInstructions = new ArrayList<>(4);
        this.subRules = new ArrayList<>(4);
    }

    void addRenderingInstruction(RenderInstruction renderInstruction) {
        this.renderInstructions.add(renderInstruction);
    }

    void addSubRule(Rule rule) {
        this.subRules.add(rule);
    }

    void apply(RuleVisitor v) {
        v.apply(this);
    }

    void destroy() {
        for (RenderInstruction ri : this.renderInstructions) {
            ri.destroy();
        }
        for (Rule sr : this.subRules) {
            sr.destroy();
        }
    }

    abstract boolean matchesNode(List<Tag> tags, byte zoomLevel);

    abstract boolean matchesWay(List<Tag> tags, byte zoomLevel, Closed closed);

    void matchNode(RenderCallback renderCallback, final RenderContext renderContext, List<RenderInstruction> matchingList, PointOfInterest pointOfInterest) {
        if (matchesNode(pointOfInterest.tags, renderContext.rendererJob.tile.zoomLevel)) {
            for (int i = 0, n = this.renderInstructions.size(); i < n; ++i) {
                this.renderInstructions.get(i).renderNode(renderCallback, renderContext, pointOfInterest);
                matchingList.add(this.renderInstructions.get(i));
            }
            for (int i = 0, n = this.subRules.size(); i < n; ++i) {
                this.subRules.get(i).matchNode(renderCallback, renderContext, matchingList, pointOfInterest);
            }
        }
    }

    void matchWay(RenderCallback renderCallback, PolylineContainer way, Tile tile, Closed closed,
                  List<RenderInstruction> matchingList, final RenderContext renderContext) {
        if (matchesWay(way.getTags(), tile.zoomLevel, closed)) {
            for (int i = 0, n = this.renderInstructions.size(); i < n; ++i) {
                this.renderInstructions.get(i).renderWay(renderCallback, renderContext, way);
                matchingList.add(this.renderInstructions.get(i));
            }
            for (int i = 0, n = this.subRules.size(); i < n; ++i) {
                this.subRules.get(i).matchWay(renderCallback, way, tile, closed, matchingList, renderContext);
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

    void scaleStrokeWidth(float scaleFactor, byte zoomLevel) {
        for (int i = 0, n = this.renderInstructions.size(); i < n; ++i) {
            this.renderInstructions.get(i).scaleStrokeWidth(scaleFactor, zoomLevel);
        }
        for (int i = 0, n = this.subRules.size(); i < n; ++i) {
            this.subRules.get(i).scaleStrokeWidth(scaleFactor, zoomLevel);
        }
    }

    void scaleTextSize(float scaleFactor, byte zoomLevel) {
        for (int i = 0, n = this.renderInstructions.size(); i < n; ++i) {
            this.renderInstructions.get(i).scaleTextSize(scaleFactor, zoomLevel);
        }
        for (int i = 0, n = this.subRules.size(); i < n; ++i) {
            this.subRules.get(i).scaleTextSize(scaleFactor, zoomLevel);
        }
    }
}
