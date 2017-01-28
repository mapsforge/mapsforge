package org.mapsforge.map.rendertheme.rule;

import org.mapsforge.core.model.Tag;
import org.mapsforge.map.rendertheme.renderinstruction.RenderInstruction;

import java.util.List;

/**
 * Created by usrusr on 21.01.2017.
 */
public class HillShadingRule extends Rule {

    HillShadingRule(String cat, byte _zoomMax, byte _zoomMin, RenderInstruction hillShadingRule) {
        super(cat, _zoomMax, _zoomMin, hillShadingRule);
    }

    @Override
    public boolean matchesNode(List<Tag> tags, byte zoomLevel) {
        return false;
    }

    @Override
    public boolean matchesWay(List<Tag> tags, byte zoomLevel, Closed closed) {
        return true;
    }
}
