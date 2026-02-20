/*
 * Copyright 2025 Sublimis
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
package org.mapsforge.map.elevation;

import org.mapsforge.core.graphics.Bitmap;
import org.mapsforge.core.graphics.Canvas;
import org.mapsforge.core.graphics.Color;
import org.mapsforge.core.graphics.Display;
import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.graphics.HillshadingBitmap;
import org.mapsforge.core.graphics.Matrix;
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.graphics.Path;
import org.mapsforge.core.graphics.Position;
import org.mapsforge.core.graphics.ResourceBitmap;
import org.mapsforge.core.graphics.TileBitmap;
import org.mapsforge.core.mapelements.PointTextContainer;
import org.mapsforge.core.mapelements.SymbolContainer;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.Point;
import org.mapsforge.map.layer.hills.HgtFileInfo;

import java.io.IOException;
import java.io.InputStream;

public class ElevationFactory implements GraphicFactory {

    @Override
    public HillshadingBitmap createMonoBitmap(int width, int height, byte[] buffer, int padding, BoundingBox area, int color) {
        final ElevationBitmap elevationBitmap = new ElevationBitmap();

        elevationBitmap.buffer = buffer;
        elevationBitmap.width = width;
        elevationBitmap.height = height;

        return elevationBitmap;
    }

    // ***************************
    // Methods below are not used.
    // ***************************

    @Override
    public Bitmap createBitmap(int width, int height) {
        return null;
    }

    @Override
    public Bitmap createBitmap(int width, int height, boolean isTransparent) {
        return null;
    }

    @Override
    public Canvas createCanvas() {
        return null;
    }

    @Override
    public int createColor(Color color) {
        return 0;
    }

    @Override
    public int createColor(int alpha, int red, int green, int blue) {
        return 0;
    }

    @Override
    public Matrix createMatrix() {
        return null;
    }

    @Override
    public Paint createPaint() {
        return null;
    }

    @Override
    public Paint createPaint(Paint paint) {
        return null;
    }

    @Override
    public Path createPath() {
        return null;
    }

    @Override
    public PointTextContainer createPointTextContainer(Point xy, double horizontalOffset, double verticalOffset, Display display, int priority, String text, Paint paintFront, Paint paintBack, SymbolContainer symbolContainer, Position position, int maxTextWidth) {
        return null;
    }

    @Override
    public ResourceBitmap createResourceBitmap(InputStream inputStream, float scaleFactor, int width, int height, int percent, int hash) throws IOException {
        return null;
    }

    @Override
    public TileBitmap createTileBitmap(InputStream inputStream, int tileSize, boolean isTransparent) throws IOException {
        return null;
    }

    @Override
    public TileBitmap createTileBitmap(int tileSize, boolean isTransparent) {
        return null;
    }

    @Override
    public InputStream platformSpecificSources(String relativePathPrefix, String src) throws IOException {
        return null;
    }

    @Override
    public ResourceBitmap renderSvg(InputStream inputStream, float scaleFactor, int width, int height, int percent, int hash) throws IOException {
        return null;
    }
}
