/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright 2014-2015 Ludwig M Brinckmann
 * Copyright 2014-2016 devemux86
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
package org.mapsforge.map.rendertheme.renderinstruction;

import org.mapsforge.core.graphics.*;
import org.mapsforge.map.datastore.PointOfInterest;
import org.mapsforge.map.layer.hills.HillsContext;
import org.mapsforge.map.layer.renderer.PolylineContainer;
import org.mapsforge.map.model.DisplayModel;
import org.mapsforge.map.rendertheme.RenderCallback;
import org.mapsforge.map.rendertheme.RenderContext;

/**
 * Represents a closed polygon on the map.
 */
public class Hillshading extends RenderInstruction {
    private final int level;
    private final String relativePathPrefix;
    private final Paint fill;
//    private final Map<Byte, Paint> fills;
    private boolean bitmapInvalid;
    private Scale scale = Scale.STROKE;
    private Bitmap shaderBitmap;

    public Hillshading(GraphicFactory graphicFactory, DisplayModel displayModel, int level, String relativePathPrefix) {
        super(graphicFactory, displayModel);
        this.level = level;
        this.relativePathPrefix = relativePathPrefix;


        this.fill = graphicFactory.createPaint();
        this.fill.setColor(Color.TRANSPARENT);
        this.fill.setStyle(Style.FILL);

//        this.fills = new HashMap<>();
    }

    @Override
    public void destroy() {
        // no-op
    }


    @Override
    public void renderNode(RenderCallback renderCallback, final RenderContext renderContext, PointOfInterest poi) {
        // do nothing
    }

    @Override
    public void renderWay(RenderCallback renderCallback, final RenderContext renderContext, PolylineContainer way) {
        HillsContext hillsContext = renderContext.hillsContext;
        if( ! hillsContext.hillsActive(renderContext)) {
            return;
        }
        if(hillsContext.level <= level) {
            return;
        }
        hillsContext.level = level;
        Paint fillPaint = fill;
        fillPaint.setBitmapShaderShift(way.getUpperLeft().getOrigin());
        renderCallback.renderHillshading(renderContext, fillPaint, level, way);
    }

    @Override
    public void scaleStrokeWidth(float scaleFactor, byte zoomLevel) {

    }

    @Override
    public void scaleTextSize(float scaleFactor, byte zoomLevel) {
        // do nothing
    }
}
