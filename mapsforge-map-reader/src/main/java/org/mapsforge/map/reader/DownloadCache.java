package org.mapsforge.map.reader; 

import java.io.File;
import java.io.IOException;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.FileWriter;
import org.mapsforge.core.model.Tile;

public class DownloadCache {

    File dir;

    public DownloadCache(File dir) {
        this.dir=dir;
    }

	public InputStream getInputStream(Tile tile) throws IOException {
		return new FileInputStream(getFilename(tile));
	}

    public void write (Tile tile, String data) throws IOException {
		File f = new File(getDir(tile));
		if(!f.exists())
			f.mkdirs();
        FileWriter fw = new FileWriter(f+"/"+tile.tileY+".json");
        fw.write(data, 0, data.length());
		fw.close();
    }    

    public boolean inCache(Tile tile) {
        return new File(getFilename(tile)).exists();
    }

	private String getFilename(Tile tile) {
		return dir.getAbsolutePath() + "/" + tile.zoomLevel 
                + "/" + tile.tileX + "/" + tile.tileY + ".json";
	}

	private String getDir(Tile tile) {
		return dir.getAbsolutePath() + "/" + tile.zoomLevel 
                + "/" + tile.tileX + "/";
	}
}
