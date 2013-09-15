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
package org.mapsforge.map.layer;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.RandomAccess;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * A thread-safe {@link Layer} list which does not allow {@code null} elements.
 */
public class Layers implements Iterable<Layer>, RandomAccess {
	private static void checkIsNull(Collection<Layer> layers) {
		if (layers == null) {
			throw new IllegalArgumentException("layers must not be null");
		}

		for (Layer layer : layers) {
			checkIsNull(layer);
		}
	}

	private static void checkIsNull(Layer layer) {
		if (layer == null) {
			throw new IllegalArgumentException("layer must not be null");
		}
	}

	private final List<Layer> layersList;
	private final Redrawer redrawer;

	Layers(Redrawer redrawer) {
		this.redrawer = redrawer;

		this.layersList = new CopyOnWriteArrayList<Layer>();
	}

	/**
	 * @see List#add(int, Object)
	 */
	public synchronized void add(int index, Layer layer) {
		checkIsNull(layer);
		this.layersList.add(index, layer);
		layer.assign(this.redrawer);
	}

	/**
	 * @see List#add(Object)
	 */
	public synchronized void add(Layer layer) {
		checkIsNull(layer);
		this.layersList.add(layer);
		layer.assign(this.redrawer);
	}

	/**
	 * @see List#addAll(Collection)
	 */
	public synchronized void addAll(Collection<Layer> layers) {
		checkIsNull(layers);
		this.layersList.addAll(layers);
		for (Layer layer : layers) {
			layer.assign(this.redrawer);
		}
	}

	/**
	 * @see List#addAll(int, Collection)
	 */
	public synchronized void addAll(int index, Collection<Layer> layers) {
		checkIsNull(layers);
		this.layersList.addAll(index, layers);
		for (Layer layer : layers) {
			layer.assign(this.redrawer);
		}
	}

	/**
	 * @see List#clear()
	 */
	public synchronized void clear() {
		for (Layer layer : this.layersList) {
			layer.unassign();
		}
		this.layersList.clear();
	}

	/**
	 * @see List#contains(Object)
	 */
	public synchronized boolean contains(Layer layer) {
		checkIsNull(layer);
		return this.layersList.contains(layer);
	}

	/**
	 * @see List#get(int)
	 */
	public synchronized Layer get(int index) {
		return this.layersList.get(index);
	}

	/**
	 * @see List#indexOf(Object)
	 */
	public synchronized int indexOf(Layer layer) {
		checkIsNull(layer);
		return this.layersList.indexOf(layer);
	}

	/**
	 * @see List#isEmpty()
	 */
	public synchronized boolean isEmpty() {
		return this.layersList.isEmpty();
	}

	/**
	 * @see List#iterator()
	 */
	@Override
	public synchronized Iterator<Layer> iterator() {
		return this.layersList.iterator();
	}

	/**
	 * @see List#remove(int)
	 */
	public synchronized Layer remove(int index) {
		Layer layer = this.layersList.remove(index);
		layer.unassign();
		return layer;
	}

	/**
	 * @see List#remove(Object)
	 */
	public synchronized boolean remove(Layer layer) {
		checkIsNull(layer);
		if (this.layersList.remove(layer)) {
			layer.unassign();
			return true;
		}
		return false;
	}

	/**
	 * @see List#size()
	 */
	public synchronized int size() {
		return this.layersList.size();
	}
}
