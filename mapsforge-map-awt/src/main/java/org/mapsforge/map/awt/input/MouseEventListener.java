/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright 2015-2016 devemux86
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
package org.mapsforge.map.awt.input;

import org.mapsforge.core.model.LatLong;
import org.mapsforge.map.layer.Layer;
import org.mapsforge.map.model.IMapViewPosition;
import org.mapsforge.map.view.MapView;

import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.ListIterator;

import javax.swing.SwingUtilities;

public class MouseEventListener extends MouseAdapter {
    private final MapView mapView;

    private Point lastDragPoint;

    public MouseEventListener(MapView mapView) {
        this.mapView = mapView;
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if (SwingUtilities.isLeftMouseButton(e)) {
            LatLong tapLatLong = this.mapView.getMapViewProjection().fromPixels(e.getX(), e.getY());
            if (tapLatLong != null) {
                org.mapsforge.core.model.Point tapXY = new org.mapsforge.core.model.Point(e.getX(), e.getY());
                // need an iterator because the list content may change while in the loop
                ListIterator<Layer> listIterator = mapView.getLayerManager().getLayers().reverseiterator();
                while (listIterator.hasPrevious()) {
                    Layer layer = listIterator.previous();
                    org.mapsforge.core.model.Point layerXY = this.mapView.getMapViewProjection().toPixels(layer.getPosition());
                    if (layer.onTap(tapLatLong, layerXY, tapXY)) {
                        break;
                    }
                }
            }
        }
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (SwingUtilities.isLeftMouseButton(e)) {
            Point point = e.getPoint();
            if (this.lastDragPoint != null) {
                mapView.manualMoveStarted();

                int moveHorizontal = point.x - this.lastDragPoint.x;
                int moveVertical = point.y - this.lastDragPoint.y;
                this.mapView.getModel().mapViewPosition.moveCenter(moveHorizontal, moveVertical);
            }
            this.lastDragPoint = point;
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (SwingUtilities.isLeftMouseButton(e)) {
            this.lastDragPoint = e.getPoint();
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        this.lastDragPoint = null;
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        byte zoomLevelDiff = (byte) -e.getWheelRotation();
        IMapViewPosition mapViewPosition = mapView.getModel().mapViewPosition;
        //System.out.println("zoomDiff: " + zoomLevelDiff + ", min/max: " + mapViewPosition.getZoomLevelMin() + "/" + mapViewPosition.getZoomLevelMax() + ", current: " + mapViewPosition.getZoomLevel());
        if ((mapViewPosition.getZoomLevel() < 127 && zoomLevelDiff > 0 && mapViewPosition.getZoomLevel() + zoomLevelDiff <= mapViewPosition.getZoomLevelMax())
                || (mapViewPosition.getZoomLevel() > 0 && zoomLevelDiff < 0 && mapViewPosition.getZoomLevel() - zoomLevelDiff >= mapViewPosition.getZoomLevelMin())) {

            mapView.manualZoomStarted();

            LatLong pivot = this.mapView.getMapViewProjection().fromPixels(e.getX(), e.getY());

            org.mapsforge.core.model.Point center = this.mapView.getModel().mapViewDimension.getDimension().getCenter();
            double moveHorizontal = (center.x - e.getX()) / Math.pow(2, zoomLevelDiff);
            double moveVertical = (center.y - e.getY()) / Math.pow(2, zoomLevelDiff);

            if (zoomLevelDiff < 0) {
                // there is a problem when zooming out using the pivot
                mapViewPosition.setPivot(null);
                mapViewPosition.moveCenterAndZoom(moveHorizontal, moveVertical, zoomLevelDiff);
                mapViewPosition.animateToPivot(null, zoomLevelDiff);
            } else {
                mapViewPosition.setPivot(pivot);
                mapViewPosition.moveCenterAndZoom(moveHorizontal, moveVertical, zoomLevelDiff);
                mapViewPosition.animateToPivot(pivot, zoomLevelDiff);
            }
        }
    }
}
