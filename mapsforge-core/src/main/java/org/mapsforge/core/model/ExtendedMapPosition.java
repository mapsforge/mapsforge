package org.mapsforge.core.model;

/**
 * Created by Mike on 11/10/2017.
 */

public class ExtendedMapPosition extends MapPosition {

    private LatLong pivot;

    private double scaleFactor;

    /**
     * @param latLong   the geographical coordinates of the map center.
     * @param zoomLevel the zoom level of the map.
     * @throws IllegalArgumentException if {@code latLong} is null or {@code zoomLevel} is negative.
     */
    public ExtendedMapPosition(LatLong latLong, byte zoomLevel, LatLong pivot, double scaleFactor) {
        super(latLong, zoomLevel);
        this.pivot = pivot;
        this.scaleFactor = scaleFactor;
    }

    public LatLong getPivot() {
        return pivot;
    }

    public double getScaleFactor() {
        return scaleFactor;
    }

}
