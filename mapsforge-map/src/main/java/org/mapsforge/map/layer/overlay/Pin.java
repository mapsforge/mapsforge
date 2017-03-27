package org.mapsforge.map.layer.overlay;

import org.mapsforge.core.graphics.Canvas;
import org.mapsforge.core.graphics.Color;
import org.mapsforge.core.graphics.FontFamily;
import org.mapsforge.core.graphics.FontStyle;
import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.graphics.Path;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Point;
import org.mapsforge.core.util.MercatorProjection;
import org.mapsforge.map.layer.Layer;

/**
 * Pin of POI
 * 
 * This Layer draw a pin and some text on the map directly.
 * The pin has selected and unselected state, using two different colors to represent.
 * Under selected state, a label string appears below the pin to explain what it is. 
 * 
 * @author 小璋丸 <virus.warnning@gmail.com>
 */
public class Pin extends Layer {

    private LatLong latLong;
    private String category;
    private String label;
    private GraphicFactory gf;
    private boolean selected = false;
    
    private int darkColor = 0xff900000;
    private int brightColor = 0xffff0000;

    /**
     * Create a pin without category or label.
     * 
     * @param latLong position
     * @param gf GraphicFactory
     */
    public Pin(LatLong latLong, GraphicFactory gf) {
        this(latLong, "", "", gf);
    }

    /** 
     * Create a pin with category and label.
     * 
     * @param latLong position
     * @param category text in the pin
     * @param label text below the pin
     * @param gf GraphicFactory
     */
    public Pin(LatLong latLong, String category, String label, GraphicFactory gf) {
        super();

        this.latLong = latLong;
        this.category = category;
        this.label = label;
        this.gf = gf;
    }

    @Override
	public boolean onTap(LatLong tapLatLong, Point layerXY, Point tapXY) {
    	double cx = layerXY.x;
    	double cy = layerXY.y - 35;
    	double dx = Math.abs(tapXY.x - cx);
    	double dy = Math.abs(tapXY.y - cy);
    	
    	if (dx <= 20 && dy <= 35) {
    		selected = !selected;
    		requestRedraw();
    		return true;
    	}

		return false;
	}

	@Override
    public synchronized void draw(BoundingBox boundingBox, byte zoomLevel, Canvas canvas, Point topLeftPoint) {
        long mapSize = MercatorProjection.getMapSize(zoomLevel, displayModel.getTileSize());
    	int pixelX = (int)(MercatorProjection.longitudeToPixelX(latLong.longitude, mapSize) - topLeftPoint.x);
        int pixelY = (int)(MercatorProjection.latitudeToPixelY(latLong.latitude, mapSize) - topLeftPoint.y);
        
        Paint p = gf.createPaint();
        if (selected) {
        	p.setColor(brightColor);	
        } else {
        	p.setColor(darkColor);
        }

        // Draw shape.
        Path path = gf.createPath();
        path.moveTo(pixelX, pixelY);
        path.lineTo(pixelX + 15, pixelY - 50);
        path.lineTo(pixelX - 15, pixelY - 50);
        path.close();
        canvas.drawPath(path, p);
        canvas.drawCircle(pixelX, pixelY - 50, 20, p);
        
        // Draw category.
        // TODO: Use Align.CENTER instead of xoff.
        p.setColor(Color.WHITE);
        p.setTextSize(20.0f);
        // p.setTextAlign(Align.CENTER); // <-- It's invalid.
        p.setTypeface(FontFamily.MONOSPACE, FontStyle.BOLD);
        int xoff = -p.getTextWidth(category) / 2;
        canvas.drawText(category, pixelX + xoff, pixelY - 44, p);
        
        // Draw label
        if (selected) {
        	p.setColor(Color.BLACK);
            p.setTextSize(16.0f);
            xoff = -p.getTextWidth(label) / 2;
            canvas.drawText(label, pixelX + xoff, pixelY + 20, p);	
        }
    }

	/**
     * Return label of the pin.
     * 
     * @return label
     */
    public synchronized String getLabel() {
    	return label;
    } 
    
    @Override
    public synchronized LatLong getPosition() {
        return this.latLong;
    }
    
    /**
     * Set colors of the pin.  
     * 
     * @param darkColor unselected state color
     * @param brightColor selected state color
     */
    public synchronized void setPinColors(int darkColor, int brightColor) {
    	this.darkColor = darkColor;
    	this.brightColor = brightColor;
    }
    
    /**
     * Set position of the pin.
     * 
     * @param latLong position
     */
    public synchronized void setPosition(LatLong latLong) {
        this.latLong = latLong;
    }
    
    /**
     * Set selection state of the pin.
     * 
     * @param selected
     */
    public synchronized void setSelected(boolean selected) {
    	this.selected = selected;
    }
    
}

