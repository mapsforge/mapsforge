/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright 2015-2016 devemux86
 * Copyright 2018 mikes222
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
import org.mapsforge.map.awt.view.MapView;
import org.mapsforge.map.layer.Layer;

import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

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
                for (int i = this.mapView.getLayerManager().getLayers().size() - 1; i >= 0; --i) {
                    Layer layer = this.mapView.getLayerManager().getLayers().get(i);
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
                this.mapView.onMoveEvent();
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
        this.mapView.onZoomEvent();
        byte zoomLevelDiff = (byte) -e.getWheelRotation();
        this.mapView.getModel().mapViewPosition.zoom(zoomLevelDiff);
    }
}
