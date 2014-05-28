package org.mapsforge.map.layer.renderer;

/*
 * Copyright 2014 Ludwig M Brinckmann
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
import org.mapsforge.core.model.Point;

import java.util.ArrayList;
import java.util.List;

class RendererUtils {

	/**
	 * Computes a polyline with distance dy parallel to given coordinates.
	 * http://objectmix.com/graphics/132987-draw-parallel-polyline-algorithm-needed.html
	 */
	static List<Point> parallelPath(List<Point> p, double dy) {
		int n = p.size() - 1;
		Point[] u = new Point[n];
		List<Point> h = new ArrayList<Point>(p.size());

		// Generate an array U[] of unity vectors of each direction
		for (int k = 0; k < n; ++k) {
			double c = p.get(k + 1).x - p.get(k).x;
			double s = p.get(k + 1).y - p.get(k).y;
			double l = Math.sqrt(c * c + s * s);
			u[k] = new Point(c / l, s / l);
		}

		// For the start point calculate the normal
		h.add(new Point(p.get(0).x - dy * u[0].y, p.get(0).y + dy * u[0].x));

		// For 1 to N-1 calculate the intersection of the offset lines
		for (int k = 1; k < n; k++) {
			double l = dy / (1 + u[k].x * u[k - 1].x + u[k].y * u[k - 1].y);
			h.add(new Point(p.get(k).x - l * (u[k].y + u[k - 1].y), p.get(k).y + l * (u[k].x + u[k - 1].x)));
		}

		// For the end point use the normal
		h.add(new Point(p.get(n).x - dy * u[n - 1].y, p.get(n).y + dy * u[n - 1].x));

		return h;
	}

	private RendererUtils() {
		throw new IllegalStateException();
	}

}
