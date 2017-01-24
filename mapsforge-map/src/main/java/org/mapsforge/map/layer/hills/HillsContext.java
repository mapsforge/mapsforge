package org.mapsforge.map.layer.hills;

import org.mapsforge.map.rendertheme.RenderContext;

/**
 * Created by usrusr on 17.01.2017.
 */
public class HillsContext {
    private HillsRenderConfig hillsRenderer;
    public int level = Integer.MAX_VALUE;

    public boolean hillsActive(RenderContext renderContext){
        return true;
    }

    public void render(RenderContext renderContext) {
//        renderContext.addToCurrentDrawingLayer();
    }
}
