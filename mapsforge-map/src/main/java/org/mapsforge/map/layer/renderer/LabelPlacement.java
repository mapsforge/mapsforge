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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;

import org.mapsforge.core.model.Rectangle;
import org.mapsforge.core.model.Tile;

/**
 * This class place the labels form POIs, area labels and normal labels. The main target is avoiding collisions of these
 * different labels.
 */
class LabelPlacement {
	/**
	 * This class holds the reference positions for the two and four point greedy algorithms.
	 */
	static class ReferencePosition {
		double height;
		final int nodeNumber;
		double width;
		final double x;
		final double y;

		ReferencePosition(double x, double y, int nodeNumber, double width, double height) {
			this.x = x;
			this.y = y;
			this.nodeNumber = nodeNumber;
			this.width = width;
			this.height = height;
		}
	}

	static final class ReferencePositionHeightComparator implements Comparator<ReferencePosition>, Serializable {
		static final ReferencePositionHeightComparator INSTANCE = new ReferencePositionHeightComparator();
		private static final long serialVersionUID = 1L;

		private ReferencePositionHeightComparator() {
			// do nothing
		}

		@Override
		public int compare(ReferencePosition x, ReferencePosition y) {
			if (x.y - x.height < y.y - y.height) {
				return -1;
			}

			if (x.y - x.height > y.y - y.height) {
				return 1;
			}
			return 0;
		}
	}

	static final class ReferencePositionWidthComparator implements Comparator<ReferencePosition>, Serializable {
		static final ReferencePositionWidthComparator INSTANCE = new ReferencePositionWidthComparator();
		private static final long serialVersionUID = 1L;

		private ReferencePositionWidthComparator() {
			// do nothing
		}

		@Override
		public int compare(ReferencePosition x, ReferencePosition y) {
			if (x.x + x.width < y.x + y.width) {
				return -1;
			}

			if (x.x + x.width > y.x + y.width) {
				return 1;
			}

			return 0;
		}
	}

	static final class ReferencePositionXComparator implements Comparator<ReferencePosition>, Serializable {
		static final ReferencePositionXComparator INSTANCE = new ReferencePositionXComparator();
		private static final long serialVersionUID = 1L;

		private ReferencePositionXComparator() {
			// do nothing
		}

		@Override
		public int compare(ReferencePosition x, ReferencePosition y) {
			if (x.x < y.x) {
				return -1;
			}

			if (x.x > y.x) {
				return 1;
			}

			return 0;
		}
	}

	static final class ReferencePositionYComparator implements Comparator<ReferencePosition>, Serializable {
		static final ReferencePositionYComparator INSTANCE = new ReferencePositionYComparator();
		private static final long serialVersionUID = 1L;

		private ReferencePositionYComparator() {
			// do nothing
		}

		@Override
		public int compare(ReferencePosition x, ReferencePosition y) {
			if (x.y < y.y) {
				return -1;
			}

			if (x.y > y.y) {
				return 1;
			}

			return 0;
		}
	}

	private static final int LABEL_DISTANCE_TO_LABEL = 2;
	private static final int LABEL_DISTANCE_TO_SYMBOL = 2;
	private static final int START_DISTANCE_TO_SYMBOLS = 4;
	private static final int SYMBOL_DISTANCE_TO_SYMBOL = 2;

	final DependencyCache dependencyCache;
	PointTextContainer label;
	Rectangle rect1;
	Rectangle rect2;
	ReferencePosition referencePosition;
	SymbolContainer symbolContainer;

	LabelPlacement() {
		this.dependencyCache = new DependencyCache();
	}

	/**
	 * The inputs are all the label and symbol objects of the current object. The output is overlap free label and
	 * symbol placement with the greedy strategy. The placement model is either the two fixed point or the four fixed
	 * point model.
	 * 
	 * @param labels
	 *            labels from the current object.
	 * @param symbols
	 *            symbols of the current object.
	 * @param areaLabels
	 *            area labels from the current object.
	 * @param cT
	 *            current object with the x,y- coordinates and the zoom level.
	 * @return the processed list of labels.
	 */
	List<PointTextContainer> placeLabels(List<PointTextContainer> labels, List<SymbolContainer> symbols,
			List<PointTextContainer> areaLabels, Tile cT, int tileSize) {
		List<PointTextContainer> returnLabels = labels;
		this.dependencyCache.generateTileAndDependencyOnTile(cT);

		preprocessAreaLabels(areaLabels, tileSize);

		preprocessLabels(returnLabels, tileSize);

		preprocessSymbols(symbols, tileSize);

		removeEmptySymbolReferences(returnLabels, symbols);

		removeOverlappingSymbolsWithAreaLabels(symbols, areaLabels);

		this.dependencyCache.removeOverlappingObjectsWithDependencyOnTile(returnLabels, areaLabels, symbols);

		if (!returnLabels.isEmpty()) {
			returnLabels = processFourPointGreedy(returnLabels, symbols, areaLabels, tileSize);
		}

		this.dependencyCache.fillDependencyOnTile(returnLabels, symbols, areaLabels, tileSize);

		return returnLabels;
	}

	/**
	 * Centers the labels.
	 * 
	 * @param labels
	 *            labels to center
	 */
	private void centerLabels(List<PointTextContainer> labels) {
		for (int i = 0; i < labels.size(); i++) {
			this.label = labels.get(i);
			this.label.x = this.label.x - this.label.boundary.getWidth() / 2;
		}
	}

	private void preprocessAreaLabels(List<PointTextContainer> areaLabels, int tileSize) {
		centerLabels(areaLabels);

		removeOutOfTileAreaLabels(areaLabels, tileSize);

		removeOverlappingAreaLabels(areaLabels);

		if (!areaLabels.isEmpty()) {
			this.dependencyCache.removeAreaLabelsInAlreadyDrawnAreas(areaLabels, tileSize);
		}
	}

	private void preprocessLabels(List<PointTextContainer> labels, int tileSize) {
		removeOutOfTileLabels(labels, tileSize);
	}

	private void preprocessSymbols(List<SymbolContainer> symbols, int tileSize) {
		removeOutOfTileSymbols(symbols, tileSize);
		removeOverlappingSymbols(symbols);
		this.dependencyCache.removeSymbolsFromDrawnAreas(symbols, tileSize);
	}

	/**
	 * This method uses an adapted greedy strategy for the fixed four position model, above, under left and right form
	 * the point of interest. It uses no priority search tree, because it will not function with symbols only with
	 * points. Instead it uses two minimum heaps. They work similar to a sweep line algorithm but have not a O(n log n
	 * +k) runtime. To find the rectangle that has the top edge, I use also a minimum Heap. The rectangles are sorted by
	 * their y coordinates.
	 * 
	 * @param labels
	 *            label positions and text
	 * @param symbols
	 *            symbol positions
	 * @param areaLabels
	 *            area label positions and text
	 * @return list of labels without overlaps with symbols and other labels by the four fixed position greedy strategy
	 */
	private List<PointTextContainer> processFourPointGreedy(List<PointTextContainer> labels,
			List<SymbolContainer> symbols, List<PointTextContainer> areaLabels, int tileSize) {
		List<PointTextContainer> resolutionSet = new ArrayList<PointTextContainer>();

		// Array for the generated reference positions around the points of interests
		ReferencePosition[] refPos = new ReferencePosition[(labels.size()) * 4];

		// lists that sorts the reference points after the minimum top edge y position
		PriorityQueue<ReferencePosition> priorUp = new PriorityQueue<ReferencePosition>(labels.size() * 4 * 2
				+ labels.size() / 10 * 2, ReferencePositionYComparator.INSTANCE);
		// lists that sorts the reference points after the minimum bottom edge y position
		PriorityQueue<ReferencePosition> priorDown = new PriorityQueue<ReferencePosition>(labels.size() * 4 * 2
				+ labels.size() / 10 * 2, ReferencePositionHeightComparator.INSTANCE);

		PointTextContainer tmp;
		int dis = START_DISTANCE_TO_SYMBOLS;

		// creates the reference positions
		for (int z = 0; z < labels.size(); z++) {
			if (labels.get(z) != null) {
				if (labels.get(z).symbol != null) {
					tmp = labels.get(z);

					// up
					refPos[z * 4] = new ReferencePosition(tmp.x - (float) tmp.boundary.getWidth() / 2, tmp.y
							- (float) tmp.symbol.symbol.getHeight() / 2 - dis, z, tmp.boundary.getWidth(),
							tmp.boundary.getHeight());
					// down
					refPos[z * 4 + 1] = new ReferencePosition(tmp.x - (float) tmp.boundary.getWidth() / 2, tmp.y
							+ (float) tmp.symbol.symbol.getHeight() / 2 + (float) tmp.boundary.getHeight() + dis, z,
							tmp.boundary.getWidth(), tmp.boundary.getHeight());
					// left
					refPos[z * 4 + 2] = new ReferencePosition(tmp.x - (float) tmp.symbol.symbol.getWidth() / 2
							- tmp.boundary.getWidth() - dis, tmp.y + (float) tmp.boundary.getHeight() / 2, z,
							tmp.boundary.getWidth(), tmp.boundary.getHeight());
					// right
					refPos[z * 4 + 3] = new ReferencePosition(tmp.x + (float) tmp.symbol.symbol.getWidth() / 2 + dis, tmp.y
							+ (float) tmp.boundary.getHeight() / 2 - 0.1f, z, tmp.boundary.getWidth(), tmp.boundary.getHeight());
				} else {
					refPos[z * 4] = new ReferencePosition(labels.get(z).x - (((float) labels.get(z).boundary.getWidth()) / 2),
							labels.get(z).y, z, labels.get(z).boundary.getWidth(), labels.get(z).boundary.getHeight());
					refPos[z * 4 + 1] = null;
					refPos[z * 4 + 2] = null;
					refPos[z * 4 + 3] = null;
				}
			}
		}

		removeNonValidateReferencePosition(refPos, symbols, areaLabels, tileSize);

		// do while it gives reference positions
		for (int i = 0; i < refPos.length; i++) {
			this.referencePosition = refPos[i];
			if (this.referencePosition != null) {
				priorUp.add(this.referencePosition);
				priorDown.add(this.referencePosition);
			}
		}

		while (priorUp.size() != 0) {
			this.referencePosition = priorUp.remove();

			this.label = labels.get(this.referencePosition.nodeNumber);

			resolutionSet.add(new PointTextContainer(this.label.text, this.referencePosition.x,
					this.referencePosition.y, this.label.paintFront, this.label.paintBack, this.label.symbol));

			if (priorUp.size() == 0) {
				return resolutionSet;
			}

			priorUp.remove(refPos[this.referencePosition.nodeNumber * 4 + 0]);
			priorUp.remove(refPos[this.referencePosition.nodeNumber * 4 + 1]);
			priorUp.remove(refPos[this.referencePosition.nodeNumber * 4 + 2]);
			priorUp.remove(refPos[this.referencePosition.nodeNumber * 4 + 3]);

			priorDown.remove(refPos[this.referencePosition.nodeNumber * 4 + 0]);
			priorDown.remove(refPos[this.referencePosition.nodeNumber * 4 + 1]);
			priorDown.remove(refPos[this.referencePosition.nodeNumber * 4 + 2]);
			priorDown.remove(refPos[this.referencePosition.nodeNumber * 4 + 3]);

			LinkedList<ReferencePosition> linkedRef = new LinkedList<ReferencePosition>();

			while (priorDown.size() != 0) {
				if (priorDown.peek().x < this.referencePosition.x + this.referencePosition.width) {
					linkedRef.add(priorDown.remove());
				} else {
					break;
				}
			}
			// brute Force collision test (faster then sweep line for a small amount of
			// objects)
			for (int i = 0; i < linkedRef.size(); i++) {
				if ((linkedRef.get(i).x <= this.referencePosition.x + this.referencePosition.width)
						&& (linkedRef.get(i).y >= this.referencePosition.y - linkedRef.get(i).height)
						&& (linkedRef.get(i).y <= this.referencePosition.y + linkedRef.get(i).height)) {
					priorUp.remove(linkedRef.get(i));
					linkedRef.remove(i);
					i--;
				}
			}
			priorDown.addAll(linkedRef);
		}

		return resolutionSet;
	}

	private void removeEmptySymbolReferences(List<PointTextContainer> nodes, List<SymbolContainer> symbols) {
		for (int i = 0; i < nodes.size(); i++) {
			this.label = nodes.get(i);
			if (!symbols.contains(this.label.symbol)) {
				this.label.symbol = null;
			}
		}
	}

	/**
	 * The greedy algorithms need possible label positions, to choose the best among them. This method removes the
	 * reference points, that are not validate. Not validate means, that the Reference overlap with another symbol or
	 * label or is outside of the object.
	 * 
	 * @param refPos
	 *            list of the potential positions
	 * @param symbols
	 *            actual list of the symbols
	 * @param areaLabels
	 *            actual list of the area labels
	 */
	private void removeNonValidateReferencePosition(ReferencePosition[] refPos, List<SymbolContainer> symbols,
			List<PointTextContainer> areaLabels, int tileSize) {
		int distance = LABEL_DISTANCE_TO_SYMBOL;

		for (int i = 0; i < symbols.size(); i++) {
			this.symbolContainer = symbols.get(i);
			this.rect1 = new Rectangle((int) this.symbolContainer.point.x - distance,
					(int) this.symbolContainer.point.y - distance, (int) this.symbolContainer.point.x
							+ this.symbolContainer.symbol.getWidth() + distance, (int) this.symbolContainer.point.y
							+ this.symbolContainer.symbol.getHeight() + distance);

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

		distance = LABEL_DISTANCE_TO_LABEL;

		for (PointTextContainer areaLabel : areaLabels) {
			this.rect1 = new Rectangle((int) areaLabel.x - distance, (int) areaLabel.y - areaLabel.boundary.getHeight()
					- distance, (int) areaLabel.x + areaLabel.boundary.getWidth() + distance, (int) areaLabel.y
					+ distance);

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

		this.dependencyCache.removeReferencePointsFromDependencyCache(refPos, tileSize);
	}

	/**
	 * This method removes the area labels, that are not visible in the actual object.
	 * 
	 * @param areaLabels
	 *            area Labels from the actual object
	 */
	private void removeOutOfTileAreaLabels(List<PointTextContainer> areaLabels, int tileSize) {
		for (int i = 0; i < areaLabels.size(); i++) {
			this.label = areaLabels.get(i);

			if (this.label.x > tileSize) {
				areaLabels.remove(i);

				i--;
			} else if (this.label.y - this.label.boundary.getHeight() > tileSize) {
				areaLabels.remove(i);

				i--;
			} else if (this.label.x + this.label.boundary.getWidth() < 0.0f) {
				areaLabels.remove(i);

				i--;
			} else if (this.label.y + this.label.boundary.getHeight() < 0.0f) {
				areaLabels.remove(i);

				i--;
			}
		}
	}

	/**
	 * This method removes the labels, that are not visible in the actual object.
	 * 
	 * @param labels
	 *            Labels from the actual object
	 */
	private void removeOutOfTileLabels(List<PointTextContainer> labels, int tileSize) {
		for (int i = 0; i < labels.size();) {
			this.label = labels.get(i);

			if (this.label.x - this.label.boundary.getWidth() / 2 > tileSize) {
				labels.remove(i);
				this.label = null;
			} else if (this.label.y - this.label.boundary.getHeight() > tileSize) {
				labels.remove(i);
				this.label = null;
			} else if ((this.label.x - this.label.boundary.getWidth() / 2 + this.label.boundary.getWidth()) < 0.0f) {
				labels.remove(i);
				this.label = null;
			} else if (this.label.y < 0.0f) {
				labels.remove(i);
				this.label = null;
			} else {
				i++;
			}
		}
	}

	/**
	 * This method removes the Symbols, that are not visible in the actual object.
	 * 
	 * @param symbols
	 *            Symbols from the actual object
	 */
	private void removeOutOfTileSymbols(List<SymbolContainer> symbols, int tileSize) {
		for (int i = 0; i < symbols.size();) {
			this.symbolContainer = symbols.get(i);

			if (this.symbolContainer.point.x > tileSize) {
				symbols.remove(i);
			} else if (this.symbolContainer.point.y > tileSize) {
				symbols.remove(i);
			} else if (this.symbolContainer.point.x + this.symbolContainer.symbol.getWidth() < 0.0f) {
				symbols.remove(i);
			} else if (this.symbolContainer.point.y + this.symbolContainer.symbol.getHeight() < 0.0f) {
				symbols.remove(i);
			} else {
				i++;
			}
		}
	}

	/**
	 * This method removes all the area labels, that overlap each other. So that the output is collision free
	 * 
	 * @param areaLabels
	 *            area labels from the actual object
	 */
	private void removeOverlappingAreaLabels(List<PointTextContainer> areaLabels) {
		int dis = LABEL_DISTANCE_TO_LABEL;

		for (int x = 0; x < areaLabels.size(); x++) {
			this.label = areaLabels.get(x);
			this.rect1 = new Rectangle((int) this.label.x - dis, (int) this.label.y - dis,
					(int) (this.label.x + this.label.boundary.getWidth()) + dis, (int) (this.label.y
							+ this.label.boundary.getHeight() + dis));

			for (int y = x + 1; y < areaLabels.size(); y++) {
				if (y != x) {
					this.label = areaLabels.get(y);
					this.rect2 = new Rectangle((int) this.label.x, (int) this.label.y,
							(int) (this.label.x + this.label.boundary.getWidth()),
							(int) (this.label.y + this.label.boundary.getHeight()));

					if (this.rect1.intersects(this.rect2)) {
						areaLabels.remove(y);

						y--;
					}
				}
			}
		}
	}

	/**
	 * This method removes all the Symbols, that overlap each other. So that the output is collision free.
	 * 
	 * @param symbols
	 *            symbols from the actual object
	 */
	private void removeOverlappingSymbols(List<SymbolContainer> symbols) {
		int dis = SYMBOL_DISTANCE_TO_SYMBOL;

		for (int x = 0; x < symbols.size(); x++) {
			this.symbolContainer = symbols.get(x);
			this.rect1 = new Rectangle((int) this.symbolContainer.point.x - dis, (int) this.symbolContainer.point.y
					- dis, (int) this.symbolContainer.point.x + this.symbolContainer.symbol.getWidth() + dis,
					(int) this.symbolContainer.point.y + this.symbolContainer.symbol.getHeight() + dis);

			for (int y = x + 1; y < symbols.size(); y++) {
				if (y != x) {
					this.symbolContainer = symbols.get(y);
					this.rect2 = new Rectangle((int) this.symbolContainer.point.x, (int) this.symbolContainer.point.y,
							(int) this.symbolContainer.point.x + this.symbolContainer.symbol.getWidth(),
							(int) this.symbolContainer.point.y + this.symbolContainer.symbol.getHeight());

					if (this.rect2.intersects(this.rect1)) {
						symbols.remove(y);
						y--;
					}
				}
			}
		}
	}

	/**
	 * Removes the the symbols that overlap with area labels.
	 * 
	 * @param symbols
	 *            list of symbols
	 * @param pTC
	 *            list of labels
	 */
	private void removeOverlappingSymbolsWithAreaLabels(List<SymbolContainer> symbols, List<PointTextContainer> pTC) {
		int dis = LABEL_DISTANCE_TO_SYMBOL;

		for (int x = 0; x < pTC.size(); x++) {
			this.label = pTC.get(x);

			this.rect1 = new Rectangle((int) this.label.x - dis, (int) (this.label.y - this.label.boundary.getHeight())
					- dis, (int) (this.label.x + this.label.boundary.getWidth() + dis), (int) (this.label.y + dis));

			for (int y = 0; y < symbols.size(); y++) {
				this.symbolContainer = symbols.get(y);

				this.rect2 = new Rectangle((int) this.symbolContainer.point.x, (int) this.symbolContainer.point.y,
						(int) (this.symbolContainer.point.x + this.symbolContainer.symbol.getWidth()),
						(int) (this.symbolContainer.point.y + this.symbolContainer.symbol.getHeight()));

				if (this.rect1.intersects(this.rect2)) {
					symbols.remove(y);
					y--;
				}
			}
		}
	}
}
