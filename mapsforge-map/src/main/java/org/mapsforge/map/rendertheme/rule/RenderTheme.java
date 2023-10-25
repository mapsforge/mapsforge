/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright 2015 Ludwig M Brinckmann
 * Copyright 2016 devemux86
 * Copyright 2017 usrusr
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
import org.mapsforge.core.util.LRUCache;
import org.mapsforge.core.util.Utils;
import org.mapsforge.map.datastore.PointOfInterest;
import org.mapsforge.map.layer.renderer.PolylineContainer;
import org.mapsforge.map.layer.renderer.StandardRenderer;
import org.mapsforge.map.model.DisplayModel;
import org.mapsforge.map.rendertheme.RenderCallback;
import org.mapsforge.map.rendertheme.RenderContext;
import org.mapsforge.map.rendertheme.renderinstruction.Hillshading;
import org.mapsforge.map.rendertheme.renderinstruction.RenderInstruction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A RenderTheme defines how ways and nodes are drawn.
 */
public class RenderTheme {

    private static final int MATCHING_CACHE_SIZE = 8192;

    private final float baseStrokeWidth;
    private final float baseTextSize;
    private final boolean hasBackgroundOutside;
    private int levels;
    private final int mapBackground;
    private final int mapBackgroundOutside;
    private final LRUCache<Integer, RenderInstruction[]> wayMatchingCache;
    private final LRUCache<Integer, RenderInstruction[]> poiMatchingCache;
    private final ArrayList<Rule> rulesList; // NOPMD we need specific interface
    private ArrayList<Hillshading> hillShadings = new ArrayList<>(); // NOPMD specific interface for trimToSize

    private final Map<Byte, Float> strokeScales = new HashMap<>();
    private final Map<Byte, Float> textScales = new HashMap<>();

    RenderTheme(RenderThemeBuilder renderThemeBuilder) {
        this.baseStrokeWidth = renderThemeBuilder.baseStrokeWidth;
        this.baseTextSize = renderThemeBuilder.baseTextSize;
        this.hasBackgroundOutside = renderThemeBuilder.hasBackgroundOutside;
        this.mapBackground = renderThemeBuilder.mapBackground;
        this.mapBackgroundOutside = renderThemeBuilder.mapBackgroundOutside;
        this.rulesList = new ArrayList<>();
        this.poiMatchingCache = new LRUCache<>(MATCHING_CACHE_SIZE);
        this.wayMatchingCache = new LRUCache<>(MATCHING_CACHE_SIZE);
    }

    /**
     * Must be called when this RenderTheme gets destroyed to clean up and free resources.
     */
    public void destroy() {
        this.poiMatchingCache.clear();
        this.wayMatchingCache.clear();
        for (Rule r : this.rulesList) {
            r.destroy();
        }
    }

    /**
     * @return the number of distinct drawing levels required by this RenderTheme.
     */
    public int getLevels() {
        return this.levels;
    }

    /**
     * @return the map background color of this RenderTheme.
     */
    public int getMapBackground() {
        return this.mapBackground;
    }

    /**
     * @return the background color that applies to areas outside the map.
     */
    public int getMapBackgroundOutside() {
        return this.mapBackgroundOutside;
    }

    /**
     * @return true if map color is defined for outside areas.
     */
    public boolean hasMapBackgroundOutside() {
        return this.hasBackgroundOutside;
    }

    /**
     * Matches a closed way with the given parameters against this RenderTheme.
     *
     * @param renderCallback the callback implementation which will be executed on each match.
     * @param renderContext
     * @param way
     */
    public void matchClosedWay(RenderCallback renderCallback, final RenderContext renderContext, PolylineContainer way) {
        matchWay(renderCallback, renderContext, Closed.YES, way);
    }

    /**
     * Matches a linear way with the given parameters against this RenderTheme.
     *
     * @param renderCallback the callback implementation which will be executed on each match.
     * @param renderContext
     * @param way
     */
    public void matchLinearWay(RenderCallback renderCallback, final RenderContext renderContext, PolylineContainer way) {
        matchWay(renderCallback, renderContext, Closed.NO, way);
    }

    /**
     * Matches a node with the given parameters against this RenderTheme.
     *
     * @param renderCallback the callback implementation which will be executed on each match.
     * @param renderContext
     * @param poi            the point of interest.
     */
    public synchronized void matchNode(RenderCallback renderCallback, final RenderContext renderContext, PointOfInterest poi) {
        // check cached instructions
        int matchingCacheKey = computeMatchingCacheKey(poi.tags, renderContext.rendererJob.tile.zoomLevel, Closed.NO);
        RenderInstruction[] instructions = this.poiMatchingCache.get(matchingCacheKey);
        if (instructions != null) {
            //noinspection ForLoopReplaceableByForEach
            for (int i = 0; i < instructions.length; ++i) {
                instructions[i].renderNode(renderCallback, renderContext, poi);
            }
            return;
        }

        // cache miss
        List<RenderInstruction> matchingList = new ArrayList<>();
        for (int i = 0, n = rulesList.size(); i < n; i++) {
            rulesList.get(i).matchNode(renderCallback, renderContext, matchingList, poi);
        }
        RenderInstruction[] matchingListA = new RenderInstruction[matchingList.size()];
        matchingList.toArray(matchingListA);
        this.poiMatchingCache.put(matchingCacheKey, matchingListA);
    }

    /**
     * Scales the stroke width of this RenderTheme by the given factor for a given zoom level
     *
     * @param scaleFactor the factor by which the stroke width should be scaled.
     * @param zoomLevel   the zoom level to which this is applied.
     */
    public synchronized void scaleStrokeWidth(float scaleFactor, byte zoomLevel) {
        if (!strokeScales.containsKey(zoomLevel) || scaleFactor != strokeScales.get(zoomLevel)) {
            for (int i = 0, n = this.rulesList.size(); i < n; ++i) {
                Rule rule = this.rulesList.get(i);
                if (rule.zoomMin <= zoomLevel && rule.zoomMax >= zoomLevel) {
                    rule.scaleStrokeWidth(scaleFactor * this.baseStrokeWidth * DisplayModel.lineScale, zoomLevel);
                }
            }
            strokeScales.put(zoomLevel, scaleFactor);
        }
    }

    /**
     * Scales the text size of this RenderTheme by the given factor for a given zoom level.
     *
     * @param scaleFactor the factor by which the text size should be scaled.
     * @param zoomLevel   the zoom level to which this is applied.
     */
    public synchronized void scaleTextSize(float scaleFactor, byte zoomLevel) {
        if (!textScales.containsKey(zoomLevel) || scaleFactor != textScales.get(zoomLevel)) {
            for (int i = 0, n = this.rulesList.size(); i < n; ++i) {
                Rule rule = this.rulesList.get(i);
                if (rule.zoomMin <= zoomLevel && rule.zoomMax >= zoomLevel) {
                    rule.scaleTextSize(scaleFactor * this.baseTextSize * DisplayModel.textScale, zoomLevel);
                }
            }
            textScales.put(zoomLevel, scaleFactor);
        }
    }

    void addRule(Rule rule) {
        this.rulesList.add(rule);
    }

    void addHillShadings(Hillshading hillshading) {
        this.hillShadings.add(hillshading);
    }

    void complete() {
        this.rulesList.trimToSize();
        this.hillShadings.trimToSize();
        for (int i = 0, n = this.rulesList.size(); i < n; ++i) {
            this.rulesList.get(i).onComplete();
        }
    }

    void setLevels(int levels) {
        this.levels = levels;
    }

    private synchronized void matchWay(RenderCallback renderCallback, final RenderContext renderContext, Closed closed, PolylineContainer way) {
        // check cached instructions
        int matchingCacheKey = computeMatchingCacheKey(way.getTags(), way.getUpperLeft().zoomLevel, closed);
        RenderInstruction[] instructions = this.wayMatchingCache.get(matchingCacheKey);
        if (instructions != null) {
            //noinspection ForLoopReplaceableByForEach
            for (int i = 0; i < instructions.length; ++i) {
                instructions[i].renderWay(renderCallback, renderContext, way);
            }
            return;
        }

        // cache miss
        List<RenderInstruction> matchingList = new ArrayList<>();
        for (int i = 0, n = rulesList.size(); i < n; i++) {
            rulesList.get(i).matchWay(renderCallback, way, way.getUpperLeft(), closed, matchingList, renderContext);
        }
        RenderInstruction[] matchingListA = new RenderInstruction[matchingList.size()];
        matchingList.toArray(matchingListA);
        this.wayMatchingCache.put(matchingCacheKey, matchingListA);
    }

    public void traverseRules(Rule.RuleVisitor visitor) {
        for (Rule rule : this.rulesList) {
            rule.apply(visitor);
        }
    }

    public void matchHillShadings(StandardRenderer renderer, RenderContext renderContext) {
        for (Hillshading hillShading : hillShadings)
            hillShading.render(renderContext, renderer.hillsRenderConfig);
    }

    private static final int keyCodeName = Utils.hashTagParameter("name");

    /**
     * Compute key for matching cached set of instructions.
     */
    private Integer computeMatchingCacheKey(List<Tag> tags, byte zoomLevel, Closed closed) {
        int result = 1;
        result = 31 * result + ((closed == null) ? 0 : closed.hashCode());
        if (tags != null) {
            for (int i = 0, n = tags.size(); i < n; i++) {
                Tag tag = tags.get(i);
                if (keyCodeName != tag.keyCode) {
                    result = 31 * result + tag.hashCode();
                }
            }
        }
        result = 31 * result + zoomLevel;
        return result;
    }
}