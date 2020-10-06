/*
 * Copyright 2014 Ludwig M Brinckmann
 * Copyright 2015 devemux86
 * Copyright 2020 mg4gh
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

import org.mapsforge.core.model.Point;

class RendererUtils {

    private static final double ANGLE_LIMIT = 170;
    private static final double ANGLE_LIMIT_COS = Math.cos(Math.toRadians(ANGLE_LIMIT));

    /**
     * Computes a polyline with distance dy parallel to given coordinates.
     * http://objectmix.com/graphics/132987-draw-parallel-polyline-algorithm-needed.html
     */
    static Point[] parallelPath(Point[] p, double dy) {
        int n = p.length - 1;
        Point[] u = new Point[n];
        Point[] h = new Point[p.length];

        // Generate an array u[] of unity vectors of each direction
        for (int k = 0; k < n; ++k) {
            double c = p[k + 1].x - p[k].x;
            double s = p[k + 1].y - p[k].y;
            double l = Math.sqrt(c * c + s * s);
            if (l == 0) {
                u[k] = new Point(0, 0);
            } else {
                u[k] = new Point(c / l, s / l);
            }

            // Detect angles above the allowed limit - return original path in this case
            if (k == 0) {
                continue;
            }
            if (u[k].x * u[k - 1].x + u[k].y * u[k - 1].y < ANGLE_LIMIT_COS) {
                return p;
            }
        }

        // For the start point calculate the normal
        h[0] = new Point(p[0].x - dy * u[0].y, p[0].y + dy * u[0].x);

        // For 1 to N-1 calculate the intersection of the offset lines
        for (int k = 1; k < n; k++) {
            double l = dy / (1 + u[k].x * u[k - 1].x + u[k].y * u[k - 1].y);
            h[k] = new Point(p[k].x - l * (u[k].y + u[k - 1].y), p[k].y + l * (u[k].x + u[k - 1].x));
        }

        // For the end point use the normal
        h[n] = new Point(p[n].x - dy * u[n - 1].y, p[n].y + dy * u[n - 1].x);

        return h;
    }

    private RendererUtils() {
        throw new IllegalStateException();
    }

}
