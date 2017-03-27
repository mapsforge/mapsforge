package org.mapsforge.map.layer.overlay;

import java.util.ArrayList;
import java.util.List;

import org.mapsforge.core.graphics.Canvas;
import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Point;
import org.mapsforge.map.layer.Layer;
import org.mapsforge.map.util.MapViewProjection;

/**
 * Maintain a set of pins and make all pins trigger onTap() event in single selection mode.
 * 
 * @author 小璋丸 <virus.warnning@gmail.com>
 */
public class PinGroup extends Layer {
	
	private String category;
	private GraphicFactory gf;
	private MapViewProjection proj;
	
	private int darkColor = 0xff900000;
	private int brightColor = 0xffff0000;
	
	private List<Pin> pinPool = new ArrayList<>();
	
	/**
	 * Create a PinGroup
	 * 
	 * @param category text in the pin
	 * @param gf GraphicFactory
	 * @param proj Projection
	 */
	public PinGroup(String category, GraphicFactory gf, MapViewProjection proj) {
		this.category = category;
		this.gf = gf;
		this.proj = proj;
	}

	/**
	 * Add a pin into group.
	 * 
	 * @param latLong position
	 * @param label text below the pin
	 */
	public void add(LatLong latLong, String label) {
		Pin p = new Pin(latLong, category, label, gf);
		p.setPinColors(darkColor, brightColor);
		pinPool.add(p);
	}
	
	/**
	 * Remove all pins in the group.
	 */
	public void clear() {
		pinPool.clear();
		requestRedraw();
	}
	
	/**
	 * Set colors of all pins in the group.
	 *  
	 * @param darkColor unselected state color
	 * @param brightColor selected state color
	 */
	public synchronized void setPinColors(int darkColor, int brightColor) {
    	this.darkColor = darkColor;
    	this.brightColor = brightColor;
    	
    	if (pinPool.size() > 0) {
    		for (Pin p : pinPool) {
        		p.setPinColors(darkColor, brightColor);
        	}
        	requestRedraw();
    	}
    }
	
	@Override
	public boolean onTap(LatLong tapLatLong, Point layerXY, Point tapXY) {
		Pin bingo = null;

		// trigger bottom first
		for (int i = pinPool.size() - 1; i >= 0; i--) {
			Pin p = pinPool.get(i);
			Point pinXY = proj.toPixels(p.getPosition());
			if (p.onTap(tapLatLong, pinXY, tapXY)) {
				bingo = p;
				break;
			}
		}
		
		// deselect others & move bingo to the top.
		if (bingo != null) {
			pinPool.remove(bingo);
			for (Pin p : pinPool) {
				p.setSelected(false);
			}
			pinPool.add(0, bingo);
			requestRedraw();
		}
		
		return false;
	}

	@Override
	public synchronized void draw(BoundingBox boundingBox, byte zoomLevel, Canvas canvas, Point topLeftPoint) {
		// draw bottom first
		for (int i = pinPool.size() - 1; i >= 0; i--) {
			Pin p = pinPool.get(i);
			p.setDisplayModel(getDisplayModel());
			p.draw(boundingBox, zoomLevel, canvas, topLeftPoint);
		}
	}
	
}
