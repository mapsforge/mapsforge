/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright 2014 Ludwig M Brinckmann
 * Copyright 2016-2019 devemux86
 * Copyright 2016 Andrey Novikov
 * Copyright 2019 Christian Pesch
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

import org.mapsforge.map.model.DisplayModel;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

/**
 * A thread-safe {@link Layer} list which does not allow {@code null} elements.
 */
public class Layers implements Iterable<Layer>, RandomAccess {

    private static final Logger LOGGER = Logger.getLogger(Layers.class.getName());

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

    private final DisplayModel displayModel;
    private final Map<Integer, Integer> groupIndex;
    private final List<Integer> groupList;
    private final List<Layer> layersList;
    private final Redrawer redrawer;

    Layers(Redrawer redrawer, DisplayModel displayModel) {
        this.redrawer = redrawer;
        this.displayModel = displayModel;

        this.groupIndex = new HashMap<>();
        this.groupList = new ArrayList<>();
        this.layersList = new CopyOnWriteArrayList<>();
    }

    /**
     * Adds a new layer at the specified position.
     * <p/>
     * Note: By default a redraw will take place afterwards.
     * To avoid this, use {@link #add(int, Layer, boolean)}.
     *
     * @param index The position at which to add the new layer (the lowest layer has position 0)
     * @param layer The new layer to add
     * @see List#add(int, Object)
     */
    public synchronized void add(int index, Layer layer) {
        add(index, layer, true);
    }

    /**
     * Adds a new layer at the specified position.
     *
     * @param index  The position at which to add the new layer (the lowest layer has position 0)
     * @param layer  The new layer to add
     * @param redraw Whether the map should be redrawn after adding the layer
     * @see List#add(int, Object)
     */
    public synchronized void add(int index, Layer layer, boolean redraw) {
        checkIsNull(layer);
        layer.setDisplayModel(this.displayModel);
        this.layersList.add(index, layer);
        layer.assign(this.redrawer);
        if (redraw) {
            this.redrawer.redrawLayers();
        }
    }

    /**
     * Adds a new layer on top.
     * <p/>
     * Note: By default a redraw will take place afterwards.
     * To avoid this, use {@link #add(Layer, boolean)}.
     *
     * @param layer The new layer to add
     * @see List#add(Object)
     */
    public synchronized void add(Layer layer) {
        add(layer, true);
    }

    /**
     * Adds a new layer on top.
     *
     * @param layer  The new layer to add
     * @param redraw Whether the map should be redrawn after adding the layer
     * @see List#add(Object)
     */
    public synchronized void add(Layer layer, boolean redraw) {
        checkIsNull(layer);
        layer.setDisplayModel(this.displayModel);

        this.layersList.add(layer);
        layer.assign(this.redrawer);
        if (redraw) {
            this.redrawer.redrawLayers();
        }
    }

    /**
     * Adds a new layer using layer groups.
     * <p/>
     * Note: By default a redraw will take place afterwards.
     * To avoid this, use {@link #add(Layer, int, boolean)}.
     *
     * @param layer The new layer to add
     * @param group The layer group
     * @see List#add(Object)
     */
    public synchronized void add(Layer layer, int group) {
        add(layer, group, true);
    }

    /**
     * Adds a new layer using layer groups.
     *
     * @param layer  The new layer to add
     * @param group  The layer group
     * @param redraw Whether the map should be redrawn after adding the layer
     * @see List#add(Object)
     */
    public synchronized void add(Layer layer, int group, boolean redraw) {
        int index = this.groupList.indexOf(group);
        if (index < 0) {
            LOGGER.warning("unknown / adding layer group" + group);
            addGroup(group);
        }

        index++;
        if (index == this.groupList.size()) {
            add(layer, redraw);
        } else {
            add(this.groupIndex.get(this.groupList.get(index)), layer, redraw);
            for (int i = index; i < this.groupList.size(); i++) {
                group = this.groupList.get(i);
                this.groupIndex.put(group, this.groupIndex.get(group) + 1);
            }
        }
    }

    /**
     * Adds multiple new layers on top.
     * <p/>
     * Note: By default a redraw will take place afterwards.
     * To avoid this, use {@link #addAll(Collection, boolean)}.
     *
     * @param layers The new layers to add
     * @see List#addAll(Collection)
     */
    public synchronized boolean addAll(Collection<Layer> layers) {
        return addAll(layers, true);
    }

    /**
     * Adds multiple new layers on top.
     *
     * @param layers The new layers to add
     * @param redraw Whether the map should be redrawn after adding the layers
     * @see List#addAll(Collection)
     */
    public synchronized boolean addAll(Collection<Layer> layers, boolean redraw) {
        checkIsNull(layers);
        for (Layer layer : layers) {
            layer.setDisplayModel(this.displayModel);
        }
        if (this.layersList.addAll(layers)) {
            for (Layer layer : layers) {
                layer.assign(this.redrawer);
            }
            if (redraw) {
                this.redrawer.redrawLayers();
            }
            return true;
        }
        return false;
    }

    /**
     * Adds multiple new layers at the specified position.
     * <p/>
     * Note: By default a redraw will take place afterwards.
     * To avoid this, use {@link #addAll(int, Collection, boolean)}.
     *
     * @param index  The position at which to add the new layers (the lowest layer has position 0)
     * @param layers The new layers to add
     * @see List#addAll(int, Collection)
     */
    public synchronized boolean addAll(int index, Collection<Layer> layers) {
        return addAll(index, layers, true);
    }

    /**
     * Adds multiple new layers at the specified position.
     *
     * @param index  The position at which to add the new layers (the lowest layer has position 0)
     * @param layers The new layers to add
     * @param redraw Whether the map should be redrawn after adding the layers
     * @see List#addAll(int, Collection)
     */
    public synchronized boolean addAll(int index, Collection<Layer> layers, boolean redraw) {
        checkIsNull(layers);
        if (this.layersList.addAll(index, layers)) {
            for (Layer layer : layers) {
                layer.setDisplayModel(this.displayModel);
                layer.assign(this.redrawer);
            }
            if (redraw) {
                this.redrawer.redrawLayers();
            }
            return true;
        }
        return false;
    }

    /**
     * Adds multiple new layers using layer groups.
     * <p/>
     * Note: By default a redraw will take place afterwards.
     * To avoid this, use {@link #addAll(Collection, int, boolean)}.
     *
     * @param layers The new layers to add
     * @param group  The layer group
     * @see List#addAll(Collection)
     */
    public synchronized boolean addAll(Collection<Layer> layers, int group) {
        return addAll(layers, group, true);
    }

    /**
     * Adds multiple new layers using layer groups.
     *
     * @param layers The new layers to add
     * @param group  The layer group
     * @param redraw Whether the map should be redrawn after adding the layers
     * @see List#addAll(Collection)
     */
    public synchronized boolean addAll(Collection<Layer> layers, int group, boolean redraw) {
        int index = this.groupList.indexOf(group);
        if (index < 0) {
            LOGGER.warning("unknown / adding layer group" + group);
            addGroup(group);
        }

        index++;
        if (index == this.groupList.size()) {
            return addAll(layers, redraw);
        } else {
            if (addAll(this.groupIndex.get(this.groupList.get(index)), layers, redraw)) {
                for (int i = index; i < this.groupList.size(); i++) {
                    group = this.groupList.get(i);
                    this.groupIndex.put(group, this.groupIndex.get(group) + layers.size());
                }
                return true;
            }
        }
        return false;
    }

    /**
     * Adds a new layer group.
     *
     * @param group The layer group
     */
    public synchronized void addGroup(int group) {
        if (this.groupList.contains(group)) {
            LOGGER.warning("group " + group + " already exists");
            return;
        }

        this.groupList.add(group);
        this.groupIndex.put(group, this.layersList.size());
    }

    /**
     * Removes all layers.
     * <p/>
     * Note: By default a redraw will take place afterwards.
     * To avoid this, use {@link #clear(boolean)}.
     *
     * @see List#clear()
     */
    public synchronized void clear() {
        clear(true);
    }

    /**
     * Removes all layers.
     *
     * @param redraw Whether the map should be redrawn after removing the layers
     * @see List#clear()
     */
    public synchronized void clear(boolean redraw) {
        for (Layer layer : this.layersList) {
            layer.unassign();
        }
        this.layersList.clear();
        if (redraw) {
            this.redrawer.redrawLayers();
        }
    }

    /**
     * @see List#contains(Object)
     */
    public synchronized boolean contains(Layer layer) {
        checkIsNull(layer);
        return this.layersList.contains(layer);
    }

    /**
     * @see List#contains(Object)
     */
    public synchronized boolean containsGroup(int group) {
        return this.groupList.contains(group);
    }

    /**
     * @see List#get(int)
     */
    public synchronized Layer get(int index) {
        return this.layersList.get(index);
    }

    /**
     * Get layer groups.
     */
    public synchronized List<Integer> getGroups() {
        return this.groupList;
    }

    /**
     * Not synchronized, read only.
     */
    public List<Layer> getLayers() {
        return this.layersList;
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
     * Removes a layer.
     * <p/>
     * Note: By default a redraw will take place afterwards.
     * To avoid this, use {@link #remove(int, boolean)}.
     *
     * @param index The index of the layer to remove
     * @see List#remove(int)
     */
    public synchronized Layer remove(int index) {
        return remove(index, true);
    }

    /**
     * Removes a layer.
     *
     * @param index  The index of the layer to remove
     * @param redraw Whether the map should be redrawn after removing the layer
     * @see List#remove(int)
     */
    public synchronized Layer remove(int index, boolean redraw) {
        Layer layer = this.layersList.remove(index);
        layer.unassign();

        // update layer group pointers
        for (Integer group : this.groupIndex.keySet()) {
            int pointer = this.groupIndex.get(group);
            if (pointer > index) {
                this.groupIndex.put(group, pointer - 1);
            }
        }

        if (redraw) {
            this.redrawer.redrawLayers();
        }
        return layer;
    }

    /**
     * Removes a layer.
     * <p/>
     * Note: By default a redraw will take place afterwards.
     * To avoid this, use {@link #remove(Layer, boolean)}.
     *
     * @param layer The layer to remove
     * @see List#remove(Object)
     */
    public synchronized boolean remove(Layer layer) {
        return remove(layer, true);
    }

    /**
     * Removes a layer.
     *
     * @param layer  The layer to remove
     * @param redraw Whether the map should be redrawn after removing the layer
     * @see List#remove(Object)
     */
    public synchronized boolean remove(Layer layer, boolean redraw) {
        checkIsNull(layer);
        int index = this.layersList.indexOf(layer);
        if (this.layersList.remove(layer)) {
            layer.unassign();

            // update layer group pointers
            for (Integer group : this.groupIndex.keySet()) {
                int pointer = this.groupIndex.get(group);
                if (pointer > index) {
                    this.groupIndex.put(group, pointer - 1);
                }
            }

            if (redraw) {
                this.redrawer.redrawLayers();
            }
            return true;
        }
        return false;
    }

    /**
     * Removes multiple layers.
     * <p/>
     * Note: By default a redraw will take place afterwards.
     * To avoid this, use {@link #removeAll(Collection, boolean)}.
     *
     * @param layers The layers to remove
     * @see List#removeAll(Collection)
     */
    public synchronized boolean removeAll(Collection<Layer> layers) {
        return removeAll(layers, true);
    }

    /**
     * Removes multiple layers.
     *
     * @param layers The layers to remove
     * @param redraw Whether the map should be redrawn after removing the layers
     * @see List#removeAll(Collection)
     */
    public synchronized boolean removeAll(Collection<Layer> layers, boolean redraw) {
        checkIsNull(layers);
        int[] indexes = new int[layers.size()];
        int i = 0;
        for (Layer layer : layers) {
            indexes[i++] = this.layersList.indexOf(layer);
        }
        if (this.layersList.removeAll(layers)) {
            for (Layer layer : layers) {
                layer.unassign();
            }

            // update layer group pointers
            for (int index : indexes) {
                for (Integer group : this.groupIndex.keySet()) {
                    int pointer = this.groupIndex.get(group);
                    if (pointer > index) {
                        this.groupIndex.put(group, pointer - 1);
                    }
                }
            }

            if (redraw) {
                this.redrawer.redrawLayers();
            }
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
