/*
 * Copyright 2015 devemux86
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
package org.mapsforge.poi.writer.model;

import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.poi.writer.osmosis.PoiWriterTask;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Configuration for the POI writer.
 */
public class PoiWriterConfiguration {
	private BoundingBox bboxConfiguration;
	private File outputFile;
	private String preferredLanguage;
	private URL tagMapping;

	/**
	 * Convenience method.
	 *
	 * @param bbox
	 *            the bounding box specification in format minLat, minLon, maxLat, maxLon in exactly this order as
	 *            degrees
	 */
	public void addBboxConfiguration(String bbox) {
		if (bbox != null) {
			setBboxConfiguration(BoundingBox.fromString(bbox));
		}
	}

	/**
	 * Convenience method.
	 *
	 * @param file
	 *            the path to the output file
	 */
	public void addOutputFile(String file) {
		if (file != null) {
			File f = new File(file);
			if (f.isDirectory()) {
				throw new IllegalArgumentException("output file parameter points to a directory, must be a file");
			} else if (f.exists() && !f.canWrite()) {
				throw new IllegalArgumentException(
						"output file parameter points to a file we have no write permissions");
			}

			setOutputFile(f);
		}
	}

	/**
	 * @return the bboxConfiguration
	 */
	public BoundingBox getBboxConfiguration() {
		return this.bboxConfiguration;
	}

	/**
	 * @return the output file
	 */
	public File getOutputFile() {
		return this.outputFile;
	}

	/**
	 * @return the preferred language
	 */
	public String getPreferredLanguage() {
		return this.preferredLanguage;
	}

	/**
	 * @return the tag mapping
	 */
	public URL getTagMapping() {
		return this.tagMapping;
	}

	/**
	 * Convenience method.
	 *
	 * @param file
	 *            the path to the tag mapping
	 */
	public void loadTagMappingFile(String file) {
		if (file != null) {
			File f = new File(file);
			if (!f.exists()) {
				throw new IllegalArgumentException("tag mapping file parameter points to a file that does not exist");
			}
			if (f.isDirectory()) {
				throw new IllegalArgumentException("tag mapping file parameter points to a directory, must be a file");
			} else if (!f.canRead()) {
				throw new IllegalArgumentException(
						"tag mapping file parameter points to a file we have no read permissions");
			}

			try {
				this.tagMapping = f.toURI().toURL();
			} catch (MalformedURLException e) {
				throw new RuntimeException(e);
			}
		} else {
			this.tagMapping = PoiWriterTask.class.getClassLoader().getResource("poi-mapping.xml");
		}
	}

	/**
	 * @param bboxConfiguration
	 *            the bboxConfiguration to set
	 */
	public void setBboxConfiguration(BoundingBox bboxConfiguration) {
		this.bboxConfiguration = bboxConfiguration;
	}

	/**
	 * @param outputFile
	 *            the output file to set
	 */
	public void setOutputFile(File outputFile) {
		this.outputFile = outputFile;
	}

	/**
	 * @param preferredLanguage
	 *            the preferred language to set
	 */
	public void setPreferredLanguage(String preferredLanguage) {
		if (preferredLanguage != null && !preferredLanguage.isEmpty()) {
			this.preferredLanguage = preferredLanguage;
		}
	}
}
