/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright © 2014 Ludwig M Brinckmann
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
package org.mapsforge.map.layer.renderer;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.mapsforge.core.graphics.Bitmap;
import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.model.Point;
import org.mapsforge.core.model.Rectangle;
import org.mapsforge.core.model.Tile;
import org.mapsforge.map.model.DisplayModel;

/**
 * This class process the methods for the Dependency Cache. It's connected with the LabelPlacement class. The main goal
 * is, to remove double labels and symbols that are already rendered, from the actual object. Labels and symbols that,
 * would be rendered on an already drawn Tile, will be deleted too.
 */
class DependencyCache {
	/**
	 * The class holds the data for a symbol with dependencies on other tiles.
	 * 
	 * @param <T>
	 *            only two types are reasonable. The DependencySymbol or DependencyText class.
	 */
	private static class Dependency<T> {
		final Point point;
		final T value;

		Dependency(T value, Point point) {
			this.value = value;
			this.point = point;
		}
	}

	/**
	 * This class holds all the information off the possible dependencies on a object.
	 */
	private static class DependencyOnTile {
		boolean drawn;
		List<Dependency<DependencyText>> labels;
		List<Dependency<DependencySymbol>> symbols;

		/**
		 * Initialize label, symbol and drawn.
		 */
		DependencyOnTile() {
			this.labels = null;
			this.symbols = null;
			this.drawn = false;
		}

		/**
		 * @param toAdd
		 *            a dependency Symbol
		 */
		void addSymbol(Dependency<DependencySymbol> toAdd) {
			if (this.symbols == null) {
				this.symbols = new ArrayList<Dependency<DependencySymbol>>();
			}
			this.symbols.add(toAdd);
		}

		/**
		 * @param toAdd
		 *            a Dependency Text
		 */
		void addText(Dependency<DependencyText> toAdd) {
			if (this.labels == null) {
				this.labels = new ArrayList<Dependency<DependencyText>>();
			}
			this.labels.add(toAdd);
		}
	}

	/**
	 * The class holds the data for a symbol with dependencies on other tiles.
	 */
	private static class DependencySymbol {
		final Bitmap symbol;
		private final List<Tile> tiles;

		/**
		 * Creates a symbol dependency element for the dependency cache.
		 * 
		 * @param symbol
		 *            reference on the dependency symbol.
		 */
		DependencySymbol(Bitmap symbol, Tile tile) {
			this.symbol = symbol;
			this.tiles = new LinkedList<Tile>();
			this.tiles.add(tile);
		}

		/**
		 * Adds an additional object, which has an dependency with this symbol.
		 */
		void addTile(Tile tile) {
			this.tiles.add(tile);
		}
	}

	/**
	 * The class holds the data for a label with dependencies on other tiles.
	 */
	private static class DependencyText {
		final Rectangle boundary;
		final Paint paintBack;
		final Paint paintFront;
		final String text;
		final List<Tile> tiles;

		/**
		 * Creates a text dependency in the dependency cache.
		 * 
		 * @param paintFront
		 *            paint element from the front.
		 * @param paintBack
		 *            paint element form the background of the text.
		 * @param text
		 *            the text of the element.
		 * @param boundary
		 *            the fixed boundary with width and height.
		 */
		DependencyText(Paint paintFront, Paint paintBack, String text, Rectangle boundary, Tile tile) {
			this.paintFront = paintFront;
			this.paintBack = paintBack;
			this.text = text;
			this.tiles = new LinkedList<Tile>();
			this.tiles.add(tile);
			this.boundary = boundary;
		}

		void addTile(Tile tile) {
			this.tiles.add(tile);
		}
	}

	/**
	 * Hash table, that connects the Tiles with their entries in the dependency cache.
	 */
	final Map<Tile, DependencyOnTile> dependencyTable;
	//final DisplayModel displayModel;
	Dependency<DependencyText> depLabel;

	Rectangle rect1;
	Rectangle rect2;
	SymbolContainer smb;
	DependencyOnTile tmp;
	private DependencyOnTile currentDependencyOnTile;
	private Tile currentTile;

	/**
	 * Constructor for this class, that creates a hashtable for the dependencies.
	 */
	DependencyCache() {
		this.dependencyTable = new Hashtable<Tile, DependencyOnTile>(60);
	}

	/**
	 * This method fills the entries in the dependency cache of the tiles, if their dependencies.
	 * 
	 * @param labels
	 *            current labels, that will be displayed.
	 * @param symbols
	 *            current symbols, that will be displayed.
	 * @param areaLabels
	 *            current areaLabels, that will be displayed.
	 */
	void fillDependencyOnTile(List<PointTextContainer> labels, List<SymbolContainer> symbols,
			List<PointTextContainer> areaLabels, int tileSize) {
		this.currentDependencyOnTile.drawn = true;

		if ((!labels.isEmpty()) || (!symbols.isEmpty()) || (!areaLabels.isEmpty())) {
			fillDependencyOnTile2(labels, symbols, areaLabels, tileSize);
		}

		if (this.currentDependencyOnTile.labels != null) {
			addLabelsFromDependencyOnTile(labels);
		}
		if (this.currentDependencyOnTile.symbols != null) {
			addSymbolsFromDependencyOnTile(symbols);
		}
	}

	/**
	 * This method must be called, before the dependencies will be handled correctly. Because it sets the actual Tile
	 * and looks if it has already dependencies.
	 */
	void generateTileAndDependencyOnTile(Tile tile) {
		this.currentTile = new Tile(tile.tileX, tile.tileY, tile.zoomLevel);
		this.currentDependencyOnTile = this.dependencyTable.get(this.currentTile);

		if (this.currentDependencyOnTile == null) {
			this.dependencyTable.put(this.currentTile, new DependencyOnTile());
			this.currentDependencyOnTile = this.dependencyTable.get(this.currentTile);
		}
	}

	/**
	 * Removes the are labels from the actual list, that would be rendered in a Tile that has already be drawn.
	 * 
	 * @param areaLabels
	 *            current area Labels, that will be displayed
	 */
	void removeAreaLabelsInAlreadyDrawnAreas(List<PointTextContainer> areaLabels, int tileSize) {
		long maxTileNumber = Tile.getMaxTileNumber(this.currentTile.zoomLevel);

		Tile lefttmp = null;
		if (this.currentTile.tileX > 0) {
			lefttmp = new Tile(this.currentTile.tileX - 1, this.currentTile.tileY, this.currentTile.zoomLevel);
		}

		Tile righttmp = null;
		if (this.currentTile.tileX < maxTileNumber) {
			righttmp = new Tile(this.currentTile.tileX + 1, this.currentTile.tileY, this.currentTile.zoomLevel);
		}

		Tile uptmp = null;
		if (this.currentTile.tileY > 0) {
			uptmp = new Tile(this.currentTile.tileX, this.currentTile.tileY - 1, this.currentTile.zoomLevel);
		}

		Tile downtmp = null;
		if (this.currentTile.tileY < maxTileNumber) {
			downtmp = new Tile(this.currentTile.tileX, this.currentTile.tileY + 1, this.currentTile.zoomLevel);
		}

		if (lefttmp == null) {
			this.tmp = null;
		} else {
			this.tmp = this.dependencyTable.get(lefttmp);
		}
		boolean left = this.tmp == null ? false : this.tmp.drawn;

		if (righttmp == null) {
			this.tmp = null;
		} else {
			this.tmp = this.dependencyTable.get(righttmp);
		}
		boolean right = this.tmp == null ? false : this.tmp.drawn;

		if (uptmp == null) {
			this.tmp = null;
		} else {
			this.tmp = this.dependencyTable.get(uptmp);
		}
		boolean up = this.tmp == null ? false : this.tmp.drawn;

		if (downtmp == null) {
			this.tmp = null;
		} else {
			this.tmp = this.dependencyTable.get(downtmp);
		}
		boolean down = this.tmp == null ? false : this.tmp.drawn;

		PointTextContainer label;

		for (int i = 0; i < areaLabels.size(); i++) {
			label = areaLabels.get(i);

			if (up && label.y - label.boundary.getHeight() < 0.0f) {
				areaLabels.remove(i);
				i--;
				continue;
			}

			if (down && label.y > tileSize) {
				areaLabels.remove(i);
				i--;
				continue;
			}
			if (left && label.x < 0.0f) {
				areaLabels.remove(i);
				i--;
				continue;
			}
			if (right && label.x + label.boundary.getWidth() > tileSize) {
				areaLabels.remove(i);
				i--;
				continue;
			}
		}
	}

	/**
	 * Removes all objects that overlaps with the objects from the dependency cache.
	 * 
	 * @param labels
	 *            labels from the current object
	 * @param areaLabels
	 *            area labels from the current object
	 * @param symbols
	 *            symbols from the current object
	 */
	void removeOverlappingObjectsWithDependencyOnTile(List<PointTextContainer> labels,
			List<PointTextContainer> areaLabels, List<SymbolContainer> symbols) {
		if (this.currentDependencyOnTile.labels != null && !this.currentDependencyOnTile.labels.isEmpty()) {
			removeOverlappingLabelsWithDependencyLabels(labels);
			removeOverlappingSymbolsWithDependencyLabels(symbols);
			removeOverlappingAreaLabelsWithDependencyLabels(areaLabels);
		}

		if (this.currentDependencyOnTile.symbols != null && !this.currentDependencyOnTile.symbols.isEmpty()) {
			removeOverlappingSymbolsWithDepencySymbols(symbols, 2);
			removeOverlappingAreaLabelsWithDependencySymbols(areaLabels);
		}
	}

	/**
	 * When the LabelPlacement class generates potential label positions for an POI, there should be no possible
	 * positions, that collide with existing symbols or labels in the dependency Cache. This class implements this
	 * functionality.
	 * 
	 * @param refPos
	 *            possible label positions form the two or four point Greedy
	 */
	void removeReferencePointsFromDependencyCache(LabelPlacement.ReferencePosition[] refPos, int tileSize) {
		long maxTileNumber = Tile.getMaxTileNumber(this.currentTile.zoomLevel);

		Tile lefttmp = null;
		if (this.currentTile.tileX > 0) {
			lefttmp = new Tile(this.currentTile.tileX - 1, this.currentTile.tileY, this.currentTile.zoomLevel);
		}

		Tile righttmp = null;
		if (this.currentTile.tileX < maxTileNumber) {
			righttmp = new Tile(this.currentTile.tileX + 1, this.currentTile.tileY, this.currentTile.zoomLevel);
		}

		Tile uptmp = null;
		if (this.currentTile.tileY > 0) {
			uptmp = new Tile(this.currentTile.tileX, this.currentTile.tileY - 1, this.currentTile.zoomLevel);
		}

		Tile downtmp = null;
		if (this.currentTile.tileY < maxTileNumber) {
			downtmp = new Tile(this.currentTile.tileX, this.currentTile.tileY + 1, this.currentTile.zoomLevel);
		}

		if (lefttmp == null) {
			this.tmp = null;
		} else {
			this.tmp = this.dependencyTable.get(lefttmp);
		}
		boolean left = this.tmp == null ? false : this.tmp.drawn;

		if (righttmp == null) {
			this.tmp = null;
		} else {
			this.tmp = this.dependencyTable.get(righttmp);
		}
		boolean right = this.tmp == null ? false : this.tmp.drawn;

		if (uptmp == null) {
			this.tmp = null;
		} else {
			this.tmp = this.dependencyTable.get(uptmp);
		}
		boolean up = this.tmp == null ? false : this.tmp.drawn;

		if (downtmp == null) {
			this.tmp = null;
		} else {
			this.tmp = this.dependencyTable.get(downtmp);
		}
		boolean down = this.tmp == null ? false : this.tmp.drawn;

		LabelPlacement.ReferencePosition ref;

		for (int i = 0; i < refPos.length; i++) {
			ref = refPos[i];

			if (ref == null) {
				continue;
			}

			if (up && ref.y - ref.height < 0) {
				refPos[i] = null;
				continue;
			}

			if (down && ref.y >= tileSize) {
				refPos[i] = null;
				continue;
			}

			if (left && ref.x < 0) {
				refPos[i] = null;
				continue;
			}

			if (right && ref.x + ref.width > tileSize) {
				refPos[i] = null;
			}
		}

		// removes all Reverence Points that intersects with Labels from the Dependency Cache

		int dis = 2;
		if (this.currentDependencyOnTile != null) {
			if (this.currentDependencyOnTile.labels != null) {
				for (int i = 0; i < this.currentDependencyOnTile.labels.size(); i++) {
					this.depLabel = this.currentDependencyOnTile.labels.get(i);
					this.rect1 = new Rectangle((int) this.depLabel.point.x - dis,
							(int) (this.depLabel.point.y - this.depLabel.value.boundary.getHeight()) - dis,
							(int) (this.depLabel.point.x + this.depLabel.value.boundary.getWidth() + dis),
							(int) (this.depLabel.point.y + dis));

					for (int y = 0; y < refPos.length; y++) {
						if (refPos[y] != null) {
							this.rect2 = new Rectangle((int) refPos[y].x, (int) (refPos[y].y - refPos[y].height),
									(int) (refPos[y].x + refPos[y].width), (int) (refPos[y].y));

							if (this.rect2.intersects(this.rect1)) {
								refPos[y] = null;
							}
						}
					}
				}
			}
			if (this.currentDependencyOnTile.symbols != null) {
				for (Dependency<DependencySymbol> symbols2 : this.currentDependencyOnTile.symbols) {
					this.rect1 = new Rectangle((int) symbols2.point.x, (int) (symbols2.point.y),
							(int) (symbols2.point.x + symbols2.value.symbol.getWidth()),
							(int) (symbols2.point.y + symbols2.value.symbol.getHeight()));

					for (int y = 0; y < refPos.length; y++) {
						if (refPos[y] != null) {
							this.rect2 = new Rectangle((int) refPos[y].x, (int) (refPos[y].y - refPos[y].height),
									(int) (refPos[y].x + refPos[y].width), (int) (refPos[y].y));

							if (this.rect2.intersects(this.rect1)) {
								refPos[y] = null;
							}
						}
					}
				}
			}
		}
	}

	void removeSymbolsFromDrawnAreas(List<SymbolContainer> symbols, int tileSize) {
		long maxTileNumber = Tile.getMaxTileNumber(this.currentTile.zoomLevel);

		Tile lefttmp = null;
		if (this.currentTile.tileX > 0) {
			lefttmp = new Tile(this.currentTile.tileX - 1, this.currentTile.tileY, this.currentTile.zoomLevel);
		}

		Tile righttmp = null;
		if (this.currentTile.tileX < maxTileNumber) {
			righttmp = new Tile(this.currentTile.tileX + 1, this.currentTile.tileY, this.currentTile.zoomLevel);
		}

		Tile uptmp = null;
		if (this.currentTile.tileY > 0) {
			uptmp = new Tile(this.currentTile.tileX, this.currentTile.tileY - 1, this.currentTile.zoomLevel);
		}

		Tile downtmp = null;
		if (this.currentTile.tileY < maxTileNumber) {
			downtmp = new Tile(this.currentTile.tileX, this.currentTile.tileY + 1, this.currentTile.zoomLevel);
		}

		if (lefttmp == null) {
			this.tmp = null;
		} else {
			this.tmp = this.dependencyTable.get(lefttmp);
		}
		boolean left = this.tmp == null ? false : this.tmp.drawn;

		if (righttmp == null) {
			this.tmp = null;
		} else {
			this.tmp = this.dependencyTable.get(righttmp);
		}
		boolean right = this.tmp == null ? false : this.tmp.drawn;

		if (uptmp == null) {
			this.tmp = null;
		} else {
			this.tmp = this.dependencyTable.get(uptmp);
		}
		boolean up = this.tmp == null ? false : this.tmp.drawn;

		if (downtmp == null) {
			this.tmp = null;
		} else {
			this.tmp = this.dependencyTable.get(downtmp);
		}
		boolean down = this.tmp == null ? false : this.tmp.drawn;

		SymbolContainer ref;

		for (int i = 0; i < symbols.size(); i++) {
			ref = symbols.get(i);

			if (up && ref.point.y < 0) {
				symbols.remove(i);
				i--;
				continue;
			}

			if (down && ref.point.y + ref.symbol.getHeight() > tileSize) {
				symbols.remove(i);
				i--;
				continue;
			}
			if (left && ref.point.x < 0) {
				symbols.remove(i);
				i--;
				continue;
			}
			if (right && ref.point.x + ref.symbol.getWidth() > tileSize) {
				symbols.remove(i);
				i--;
				continue;
			}
		}
	}

	private void addLabelsFromDependencyOnTile(List<PointTextContainer> labels) {
		for (int i = 0; i < this.currentDependencyOnTile.labels.size(); i++) {
			this.depLabel = this.currentDependencyOnTile.labels.get(i);
			if (this.depLabel.value.paintBack != null) {
				labels.add(new PointTextContainer(this.depLabel.value.text, this.depLabel.point.x,
						this.depLabel.point.y, this.depLabel.value.paintFront, this.depLabel.value.paintBack));
			} else {
				labels.add(new PointTextContainer(this.depLabel.value.text, this.depLabel.point.x,
						this.depLabel.point.y, this.depLabel.value.paintFront));
			}
		}
	}

	private void addSymbolsFromDependencyOnTile(List<SymbolContainer> symbols) {
		for (Dependency<DependencySymbol> depSmb : this.currentDependencyOnTile.symbols) {
			symbols.add(new SymbolContainer(depSmb.value.symbol, depSmb.point));
		}
	}

	/**
	 * Fills the dependency entry from the object and the neighbor tiles with the dependency information, that are
	 * necessary for drawing. To do that every label and symbol that will be drawn, will be checked if it produces
	 * dependencies with other tiles.
	 * 
	 * @param pTC
	 *            list of the labels
	 */
	private void fillDependencyLabels(List<PointTextContainer> pTC, int tileSize) {
		Tile left = new Tile(this.currentTile.tileX - 1, this.currentTile.tileY, this.currentTile.zoomLevel);
		Tile right = new Tile(this.currentTile.tileX + 1, this.currentTile.tileY, this.currentTile.zoomLevel);
		Tile up = new Tile(this.currentTile.tileX, this.currentTile.tileY - 1, this.currentTile.zoomLevel);
		Tile down = new Tile(this.currentTile.tileX, this.currentTile.tileY + 1, this.currentTile.zoomLevel);

		Tile leftup = new Tile(this.currentTile.tileX - 1, this.currentTile.tileY - 1, this.currentTile.zoomLevel);
		Tile leftdown = new Tile(this.currentTile.tileX - 1, this.currentTile.tileY + 1, this.currentTile.zoomLevel);
		Tile rightup = new Tile(this.currentTile.tileX + 1, this.currentTile.tileY - 1, this.currentTile.zoomLevel);
		Tile rightdown = new Tile(this.currentTile.tileX + 1, this.currentTile.tileY + 1, this.currentTile.zoomLevel);

		PointTextContainer label;
		DependencyOnTile linkedDep;
		DependencyText toAdd;

		for (int i = 0; i < pTC.size(); i++) {
			label = pTC.get(i);

			toAdd = null;

			// up
			if ((label.y - label.boundary.getHeight() < 0.0f) && (!this.dependencyTable.get(up).drawn)) {
				linkedDep = this.dependencyTable.get(up);

				toAdd = new DependencyText(label.paintFront, label.paintBack, label.text, label.boundary,
						this.currentTile);

				this.currentDependencyOnTile
						.addText(new Dependency<DependencyText>(toAdd, new Point(label.x, label.y)));

				linkedDep.addText(new Dependency<DependencyText>(toAdd, new Point(label.x, label.y + tileSize)));

				toAdd.addTile(up);

				if ((label.x < 0.0f) && (!this.dependencyTable.get(leftup).drawn)) {
					linkedDep = this.dependencyTable.get(leftup);

					linkedDep.addText(new Dependency<DependencyText>(toAdd, new Point(label.x + tileSize, label.y
							+ tileSize)));

					toAdd.addTile(leftup);
				}

				if ((label.x + label.boundary.getWidth() > tileSize)
						&& (!this.dependencyTable.get(rightup).drawn)) {
					linkedDep = this.dependencyTable.get(rightup);

					linkedDep.addText(new Dependency<DependencyText>(toAdd, new Point(label.x - tileSize, label.y
							+ tileSize)));

					toAdd.addTile(rightup);
				}
			}

			// down
			if ((label.y > tileSize) && (!this.dependencyTable.get(down).drawn)) {
				linkedDep = this.dependencyTable.get(down);

				if (toAdd == null) {
					toAdd = new DependencyText(label.paintFront, label.paintBack, label.text, label.boundary,
							this.currentTile);

					this.currentDependencyOnTile.addText(new Dependency<DependencyText>(toAdd, new Point(label.x,
							label.y)));
				}

				linkedDep.addText(new Dependency<DependencyText>(toAdd, new Point(label.x, label.y - tileSize)));

				toAdd.addTile(down);

				if ((label.x < 0.0f) && (!this.dependencyTable.get(leftdown).drawn)) {
					linkedDep = this.dependencyTable.get(leftdown);

					linkedDep.addText(new Dependency<DependencyText>(toAdd, new Point(label.x + tileSize, label.y
							- tileSize)));

					toAdd.addTile(leftdown);
				}

				if ((label.x + label.boundary.getWidth() > tileSize)
						&& (!this.dependencyTable.get(rightdown).drawn)) {
					linkedDep = this.dependencyTable.get(rightdown);

					linkedDep.addText(new Dependency<DependencyText>(toAdd, new Point(label.x - tileSize, label.y
							- tileSize)));

					toAdd.addTile(rightdown);
				}
			}
			// left

			if ((label.x < 0.0f) && (!this.dependencyTable.get(left).drawn)) {
				linkedDep = this.dependencyTable.get(left);

				if (toAdd == null) {
					toAdd = new DependencyText(label.paintFront, label.paintBack, label.text, label.boundary,
							this.currentTile);

					this.currentDependencyOnTile.addText(new Dependency<DependencyText>(toAdd, new Point(label.x,
							label.y)));
				}

				linkedDep.addText(new Dependency<DependencyText>(toAdd, new Point(label.x + tileSize, label.y)));

				toAdd.addTile(left);
			}
			// right
			if ((label.x + label.boundary.getWidth() > tileSize) && (!this.dependencyTable.get(right).drawn)) {
				linkedDep = this.dependencyTable.get(right);

				if (toAdd == null) {
					toAdd = new DependencyText(label.paintFront, label.paintBack, label.text, label.boundary,
							this.currentTile);

					this.currentDependencyOnTile.addText(new Dependency<DependencyText>(toAdd, new Point(label.x,
							label.y)));
				}

				linkedDep.addText(new Dependency<DependencyText>(toAdd, new Point(label.x - tileSize, label.y)));

				toAdd.addTile(right);
			}

			// check symbols

			if ((label.symbol != null) && (toAdd == null)) {
				if ((label.symbol.point.y <= 0.0f) && (!this.dependencyTable.get(up).drawn)) {
					linkedDep = this.dependencyTable.get(up);

					toAdd = new DependencyText(label.paintFront, label.paintBack, label.text, label.boundary,
							this.currentTile);

					this.currentDependencyOnTile.addText(new Dependency<DependencyText>(toAdd, new Point(label.x,
							label.y)));

					linkedDep.addText(new Dependency<DependencyText>(toAdd,
							new Point(label.x, label.y + tileSize)));

					toAdd.addTile(up);

					if ((label.symbol.point.x < 0.0f) && (!this.dependencyTable.get(leftup).drawn)) {
						linkedDep = this.dependencyTable.get(leftup);

						linkedDep.addText(new Dependency<DependencyText>(toAdd, new Point(label.x + tileSize,
								label.y + tileSize)));

						toAdd.addTile(leftup);
					}

					if ((label.symbol.point.x + label.symbol.symbol.getWidth() > tileSize)
							&& (!this.dependencyTable.get(rightup).drawn)) {
						linkedDep = this.dependencyTable.get(rightup);

						linkedDep.addText(new Dependency<DependencyText>(toAdd, new Point(label.x - tileSize,
								label.y + tileSize)));

						toAdd.addTile(rightup);
					}
				}

				if ((label.symbol.point.y + label.symbol.symbol.getHeight() >= tileSize)
						&& (!this.dependencyTable.get(down).drawn)) {
					linkedDep = this.dependencyTable.get(down);

					if (toAdd == null) {
						toAdd = new DependencyText(label.paintFront, label.paintBack, label.text, label.boundary,
								this.currentTile);

						this.currentDependencyOnTile.addText(new Dependency<DependencyText>(toAdd, new Point(label.x,
								label.y)));
					}

					linkedDep.addText(new Dependency<DependencyText>(toAdd,
							new Point(label.x, label.y + tileSize)));

					toAdd.addTile(up);

					if ((label.symbol.point.x < 0.0f) && (!this.dependencyTable.get(leftdown).drawn)) {
						linkedDep = this.dependencyTable.get(leftdown);

						linkedDep.addText(new Dependency<DependencyText>(toAdd, new Point(label.x + tileSize,
								label.y - tileSize)));

						toAdd.addTile(leftdown);
					}

					if ((label.symbol.point.x + label.symbol.symbol.getWidth() > tileSize)
							&& (!this.dependencyTable.get(rightdown).drawn)) {
						linkedDep = this.dependencyTable.get(rightdown);

						linkedDep.addText(new Dependency<DependencyText>(toAdd, new Point(label.x - tileSize,
								label.y - tileSize)));

						toAdd.addTile(rightdown);
					}
				}

				if ((label.symbol.point.x <= 0.0f) && (!this.dependencyTable.get(left).drawn)) {
					linkedDep = this.dependencyTable.get(left);

					if (toAdd == null) {
						toAdd = new DependencyText(label.paintFront, label.paintBack, label.text, label.boundary,
								this.currentTile);

						this.currentDependencyOnTile.addText(new Dependency<DependencyText>(toAdd, new Point(label.x,
								label.y)));
					}

					linkedDep.addText(new Dependency<DependencyText>(toAdd,
							new Point(label.x - tileSize, label.y)));

					toAdd.addTile(left);
				}

				if ((label.symbol.point.x + label.symbol.symbol.getWidth() >= tileSize)
						&& (!this.dependencyTable.get(right).drawn)) {
					linkedDep = this.dependencyTable.get(right);

					if (toAdd == null) {
						toAdd = new DependencyText(label.paintFront, label.paintBack, label.text, label.boundary,
								this.currentTile);

						this.currentDependencyOnTile.addText(new Dependency<DependencyText>(toAdd, new Point(label.x,
								label.y)));
					}

					linkedDep.addText(new Dependency<DependencyText>(toAdd,
							new Point(label.x + tileSize, label.y)));

					toAdd.addTile(right);
				}
			}
		}
	}

	private void fillDependencyOnTile2(List<PointTextContainer> labels, List<SymbolContainer> symbols,
			List<PointTextContainer> areaLabels, int tileSize) {
		Tile left = new Tile(this.currentTile.tileX - 1, this.currentTile.tileY, this.currentTile.zoomLevel);
		Tile right = new Tile(this.currentTile.tileX + 1, this.currentTile.tileY, this.currentTile.zoomLevel);
		Tile up = new Tile(this.currentTile.tileX, this.currentTile.tileY - 1, this.currentTile.zoomLevel);
		Tile down = new Tile(this.currentTile.tileX, this.currentTile.tileY + 1, this.currentTile.zoomLevel);

		Tile leftup = new Tile(this.currentTile.tileX - 1, this.currentTile.tileY - 1, this.currentTile.zoomLevel);
		Tile leftdown = new Tile(this.currentTile.tileX - 1, this.currentTile.tileY + 1, this.currentTile.zoomLevel);
		Tile rightup = new Tile(this.currentTile.tileX + 1, this.currentTile.tileY - 1, this.currentTile.zoomLevel);
		Tile rightdown = new Tile(this.currentTile.tileX + 1, this.currentTile.tileY + 1, this.currentTile.zoomLevel);

		if (this.dependencyTable.get(up) == null) {
			this.dependencyTable.put(up, new DependencyOnTile());
		}
		if (this.dependencyTable.get(down) == null) {
			this.dependencyTable.put(down, new DependencyOnTile());
		}
		if (this.dependencyTable.get(left) == null) {
			this.dependencyTable.put(left, new DependencyOnTile());
		}
		if (this.dependencyTable.get(right) == null) {
			this.dependencyTable.put(right, new DependencyOnTile());
		}
		if (this.dependencyTable.get(leftdown) == null) {
			this.dependencyTable.put(leftdown, new DependencyOnTile());
		}
		if (this.dependencyTable.get(rightup) == null) {
			this.dependencyTable.put(rightup, new DependencyOnTile());
		}
		if (this.dependencyTable.get(leftup) == null) {
			this.dependencyTable.put(leftup, new DependencyOnTile());
		}
		if (this.dependencyTable.get(rightdown) == null) {
			this.dependencyTable.put(rightdown, new DependencyOnTile());
		}

		fillDependencyLabels(labels, tileSize);
		fillDependencyLabels(areaLabels, tileSize);

		DependencyOnTile linkedDep;
		DependencySymbol addSmb;

		for (SymbolContainer symbol : symbols) {
			addSmb = null;

			// up
			if ((symbol.point.y < 0.0f) && (!this.dependencyTable.get(up).drawn)) {
				linkedDep = this.dependencyTable.get(up);

				addSmb = new DependencySymbol(symbol.symbol, this.currentTile);
				this.currentDependencyOnTile.addSymbol(new Dependency<DependencySymbol>(addSmb, new Point(
						symbol.point.x, symbol.point.y)));

				linkedDep.addSymbol(new Dependency<DependencySymbol>(addSmb, new Point(symbol.point.x, symbol.point.y
						+ tileSize)));
				addSmb.addTile(up);

				if ((symbol.point.x < 0.0f) && (!this.dependencyTable.get(leftup).drawn)) {
					linkedDep = this.dependencyTable.get(leftup);

					linkedDep.addSymbol(new Dependency<DependencySymbol>(addSmb, new Point(symbol.point.x
							+ tileSize, symbol.point.y + tileSize)));
					addSmb.addTile(leftup);
				}

				if ((symbol.point.x + symbol.symbol.getWidth() > tileSize)
						&& (!this.dependencyTable.get(rightup).drawn)) {
					linkedDep = this.dependencyTable.get(rightup);

					linkedDep.addSymbol(new Dependency<DependencySymbol>(addSmb, new Point(symbol.point.x
							- tileSize, symbol.point.y + tileSize)));
					addSmb.addTile(rightup);
				}
			}

			// down
			if ((symbol.point.y + symbol.symbol.getHeight() > tileSize)
					&& (!this.dependencyTable.get(down).drawn)) {
				linkedDep = this.dependencyTable.get(down);

				if (addSmb == null) {
					addSmb = new DependencySymbol(symbol.symbol, this.currentTile);
					this.currentDependencyOnTile.addSymbol(new Dependency<DependencySymbol>(addSmb, new Point(
							symbol.point.x, symbol.point.y)));
				}

				linkedDep.addSymbol(new Dependency<DependencySymbol>(addSmb, new Point(symbol.point.x, symbol.point.y
						- tileSize)));
				addSmb.addTile(down);

				if ((symbol.point.x < 0.0f) && (!this.dependencyTable.get(leftdown).drawn)) {
					linkedDep = this.dependencyTable.get(leftdown);

					linkedDep.addSymbol(new Dependency<DependencySymbol>(addSmb, new Point(symbol.point.x
							+ tileSize, symbol.point.y - tileSize)));
					addSmb.addTile(leftdown);
				}

				if ((symbol.point.x + symbol.symbol.getWidth() > tileSize)
						&& (!this.dependencyTable.get(rightdown).drawn)) {
					linkedDep = this.dependencyTable.get(rightdown);

					linkedDep.addSymbol(new Dependency<DependencySymbol>(addSmb, new Point(symbol.point.x
							- tileSize, symbol.point.y - tileSize)));
					addSmb.addTile(rightdown);
				}
			}

			// left
			if ((symbol.point.x < 0.0f) && (!this.dependencyTable.get(left).drawn)) {
				linkedDep = this.dependencyTable.get(left);

				if (addSmb == null) {
					addSmb = new DependencySymbol(symbol.symbol, this.currentTile);
					this.currentDependencyOnTile.addSymbol(new Dependency<DependencySymbol>(addSmb, new Point(
							symbol.point.x, symbol.point.y)));
				}

				linkedDep.addSymbol(new Dependency<DependencySymbol>(addSmb, new Point(symbol.point.x + tileSize,
						symbol.point.y)));
				addSmb.addTile(left);
			}

			// right
			if ((symbol.point.x + symbol.symbol.getWidth() > tileSize)
					&& (!this.dependencyTable.get(right).drawn)) {
				linkedDep = this.dependencyTable.get(right);
				if (addSmb == null) {
					addSmb = new DependencySymbol(symbol.symbol, this.currentTile);
					this.currentDependencyOnTile.addSymbol(new Dependency<DependencySymbol>(addSmb, new Point(
							symbol.point.x, symbol.point.y)));
				}

				linkedDep.addSymbol(new Dependency<DependencySymbol>(addSmb, new Point(symbol.point.x - tileSize,
						symbol.point.y)));
				addSmb.addTile(right);
			}
		}
	}

	private void removeOverlappingAreaLabelsWithDependencyLabels(List<PointTextContainer> areaLabels) {
		PointTextContainer pTC;

		for (int i = 0; i < this.currentDependencyOnTile.labels.size(); i++) {
			this.depLabel = this.currentDependencyOnTile.labels.get(i);
			this.rect1 = new Rectangle((int) (this.depLabel.point.x),
					(int) (this.depLabel.point.y - this.depLabel.value.boundary.getHeight()),
					(int) (this.depLabel.point.x + this.depLabel.value.boundary.getWidth()),
					(int) (this.depLabel.point.y));

			for (int x = 0; x < areaLabels.size(); x++) {
				pTC = areaLabels.get(x);

				this.rect2 = new Rectangle((int) pTC.x, (int) pTC.y - pTC.boundary.getHeight(), (int) pTC.x
						+ pTC.boundary.getWidth(), (int) pTC.y);

				if (this.rect2.intersects(this.rect1)) {
					areaLabels.remove(x);
					x--;
				}
			}
		}
	}

	private void removeOverlappingAreaLabelsWithDependencySymbols(List<PointTextContainer> areaLabels) {
		PointTextContainer label;

		for (Dependency<DependencySymbol> depSmb : this.currentDependencyOnTile.symbols) {
			this.rect1 = new Rectangle((int) depSmb.point.x, (int) depSmb.point.y, (int) depSmb.point.x
					+ depSmb.value.symbol.getWidth(), (int) depSmb.point.y + depSmb.value.symbol.getHeight());

			for (int x = 0; x < areaLabels.size(); x++) {
				label = areaLabels.get(x);

				this.rect2 = new Rectangle((int) (label.x), (int) (label.y - label.boundary.getHeight()),
						(int) (label.x + label.boundary.getWidth()), (int) (label.y));

				if (this.rect2.intersects(this.rect1)) {
					areaLabels.remove(x);
					x--;
				}
			}
		}
	}

	private void removeOverlappingLabelsWithDependencyLabels(List<PointTextContainer> labels) {
		for (int i = 0; i < this.currentDependencyOnTile.labels.size(); i++) {
			for (int x = 0; x < labels.size(); x++) {
				if ((labels.get(x).text.equals(this.currentDependencyOnTile.labels.get(i).value.text))
						&& (labels.get(x).paintFront
								.equals(this.currentDependencyOnTile.labels.get(i).value.paintFront))
						&& (labels.get(x).paintBack.equals(this.currentDependencyOnTile.labels.get(i).value.paintBack))) {
					labels.remove(x);
					i--;
					break;
				}
			}
		}
	}

	private void removeOverlappingSymbolsWithDepencySymbols(List<SymbolContainer> symbols, int dis) {
		SymbolContainer symbolContainer;
		Dependency<DependencySymbol> sym2;

		for (int x = 0; x < this.currentDependencyOnTile.symbols.size(); x++) {
			sym2 = this.currentDependencyOnTile.symbols.get(x);
			this.rect1 = new Rectangle((int) sym2.point.x - dis, (int) sym2.point.y - dis, (int) sym2.point.x
					+ sym2.value.symbol.getWidth() + dis, (int) sym2.point.y + sym2.value.symbol.getHeight() + dis);

			for (int y = 0; y < symbols.size(); y++) {
				symbolContainer = symbols.get(y);
				this.rect2 = new Rectangle((int) symbolContainer.point.x, (int) symbolContainer.point.y,
						(int) symbolContainer.point.x + symbolContainer.symbol.getWidth(),
						(int) symbolContainer.point.y + symbolContainer.symbol.getHeight());

				if (this.rect2.intersects(this.rect1)) {
					symbols.remove(y);
					y--;
				}
			}
		}
	}

	private void removeOverlappingSymbolsWithDependencyLabels(List<SymbolContainer> symbols) {
		for (int i = 0; i < this.currentDependencyOnTile.labels.size(); i++) {
			this.depLabel = this.currentDependencyOnTile.labels.get(i);
			this.rect1 = new Rectangle((int) (this.depLabel.point.x),
					(int) (this.depLabel.point.y - this.depLabel.value.boundary.getHeight()),
					(int) (this.depLabel.point.x + this.depLabel.value.boundary.getWidth()),
					(int) (this.depLabel.point.y));

			for (int x = 0; x < symbols.size(); x++) {
				this.smb = symbols.get(x);

				this.rect2 = new Rectangle((int) this.smb.point.x, (int) this.smb.point.y, (int) this.smb.point.x
						+ this.smb.symbol.getWidth(), (int) this.smb.point.y + this.smb.symbol.getHeight());

				if (this.rect2.intersects(this.rect1)) {
					symbols.remove(x);
					x--;
				}
			}
		}
	}
}
