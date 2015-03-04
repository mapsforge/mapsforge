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
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.mapsforge.core.model.Tile;
import org.mapsforge.core.util.LRUCache;
import org.mapsforge.map.layer.renderer.PolylineContainer;
import org.mapsforge.map.reader.PointOfInterest;
import org.mapsforge.map.rendertheme.RenderCallback;
import org.mapsforge.map.rendertheme.RenderContext;
import org.mapsforge.map.rendertheme.renderinstruction.RenderInstruction;

/**
 * A RenderTheme defines how ways and nodes are drawn.
 */
public class RenderTheme {
	private static final int MATCHING_CACHE_SIZE = 1024;

	private final float baseStrokeWidth;
	private final float baseTextSize;
	private final boolean hasBackgroundOutside;
	private int levels;
	private final int mapBackground;
	private final int mapBackgroundOutside;
	private final LRUCache<MatchingCacheKey, List<RenderInstruction>> wayMatchingCache;
	private final LRUCache<MatchingCacheKey, List<RenderInstruction>> poiMatchingCache;
	private final AtomicInteger refCount = new AtomicInteger();
	private final ArrayList<Rule> rulesList; // NOPMD we need specific interface
	private float textScale;
	private float strokeWidthScale;

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
		if (this.refCount.decrementAndGet() < 0) {
			this.poiMatchingCache.clear();
			this.wayMatchingCache.clear();
			for (Rule r : this.rulesList) {
				r.destroy();
			}
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


	public void incrementRefCount() {
		this.refCount.incrementAndGet();
	}

	/**
	 * Matches a closed way with the given parameters against this RenderTheme.
	 *  @param renderCallback
	 *            the callback implementation which will be executed on each match.
	 * @param renderContext
	 * @param way
	 */
	public void matchClosedWay(RenderCallback renderCallback, final RenderContext renderContext, PolylineContainer way) {
		matchWay(renderCallback, renderContext, Closed.YES, way);
	}

	/**
	 * Matches a linear way with the given parameters against this RenderTheme.
	 *  @param renderCallback
	 *            the callback implementation which will be executed on each match.
	 * @param renderContext
	 * @param way
	 */
	public void matchLinearWay(RenderCallback renderCallback, final RenderContext renderContext, PolylineContainer way) {
		matchWay(renderCallback, renderContext, Closed.NO, way);
	}

	/**
	 * Matches a node with the given parameters against this RenderTheme.
	 *  @param renderCallback
	 *            the callback implementation which will be executed on each match.
	 * @param renderContext
	 * @param tile
	 * @param poi
 *            the point of interest.
	 */
	public void matchNode(RenderCallback renderCallback, final RenderContext renderContext, Tile tile, PointOfInterest poi) {
		MatchingCacheKey matchingCacheKey = new MatchingCacheKey(poi.tags, tile.zoomLevel, Closed.NO);

		List<RenderInstruction> matchingList = this.poiMatchingCache.get(matchingCacheKey);
		if (matchingList != null) {
			// cache hit
			for (int i = 0, n = matchingList.size(); i < n; ++i) {
				matchingList.get(i).renderNode(renderCallback, renderContext, tile, poi);
			}
			return;
		}

		// cache miss
		matchingList = new ArrayList<RenderInstruction>();

		for (int i = 0, n = this.rulesList.size(); i < n; ++i) {
			this.rulesList.get(i).matchNode(renderCallback, renderContext, tile, matchingList, poi);
		}
		this.poiMatchingCache.put(matchingCacheKey, matchingList);
	}

	/**
	 * Scales the stroke width of this RenderTheme by the given factor.
	 * 
	 * @param scaleFactor
	 *            the factor by which the stroke width should be scaled.
	 */
	public void scaleStrokeWidth(float scaleFactor) {
		if (this.strokeWidthScale != scaleFactor) {
			for (int i = 0, n = this.rulesList.size(); i < n; ++i) {
				this.rulesList.get(i).scaleStrokeWidth(scaleFactor * this.baseStrokeWidth);
			}
			this.strokeWidthScale = scaleFactor;
		}
	}

	/**
	 * Scales the text size of this RenderTheme by the given factor.
	 * 
	 * @param scaleFactor
	 *            the factor by which the text size should be scaled.
	 */
	public void scaleTextSize(float scaleFactor) {
		if (this.textScale != scaleFactor) {
			for (int i = 0, n = this.rulesList.size(); i < n; ++i) {
				this.rulesList.get(i).scaleTextSize(scaleFactor * this.baseTextSize);
			}
			this.textScale = scaleFactor;
		}
	}

	void addRule(Rule rule) {
		this.rulesList.add(rule);
	}

	void complete() {
		this.rulesList.trimToSize();
		for (int i = 0, n = this.rulesList.size(); i < n; ++i) {
			this.rulesList.get(i).onComplete();
		}
	}

	void setLevels(int levels) {
		this.levels = levels;
	}

	private void matchWay(RenderCallback renderCallback, final RenderContext renderContext, Closed closed, PolylineContainer way) {
		MatchingCacheKey matchingCacheKey = new MatchingCacheKey(way.getTags(), way.getTile().zoomLevel, closed);

		List<RenderInstruction> matchingList = this.wayMatchingCache.get(matchingCacheKey);
		if (matchingList != null) {
			// cache hit
			for (int i = 0, n = matchingList.size(); i < n; ++i) {
				matchingList.get(i).renderWay(renderCallback, renderContext, way);
			}
			return;
		}

		// cache miss
		matchingList = new ArrayList<RenderInstruction>();
		for (int i = 0, n = this.rulesList.size(); i < n; ++i) {
			this.rulesList.get(i).matchWay(renderCallback, way, way.getTile(), closed, matchingList, renderContext);
		}

		this.wayMatchingCache.put(matchingCacheKey, matchingList);
	}
}
