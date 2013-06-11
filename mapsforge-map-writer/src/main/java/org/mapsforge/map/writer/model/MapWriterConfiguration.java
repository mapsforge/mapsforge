/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
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
package org.mapsforge.map.writer.model;

import java.io.File;
import java.net.MalformedURLException;

import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.util.LatLongUtils;
import org.mapsforge.map.writer.OSMTagMapping;

/**
 * Configuration for the map file writer.
 */
public class MapWriterConfiguration {
	private BoundingBox bboxConfiguration;
	private int bboxEnlargement;
	private String comment;

	private String dataProcessorType;
	private long date;

	private boolean debugStrings;
	private EncodingChoice encodingChoice;
	private int fileSpecificationVersion;

	private boolean labelPosition;
	private LatLong mapStartPosition;
	private int mapStartZoomLevel;
	private File outputFile;
	private boolean polygonClipping;
	private String preferredLanguage;

	private double simplification;

	private boolean skipInvalidRelations;

	private OSMTagMapping tagMapping;
	private boolean wayClipping;

	private String writerVersion;
	private ZoomIntervalConfiguration zoomIntervalConfiguration;

	/**
	 * Convenience method.
	 * 
	 * @param bbox
	 *            the bounding box specification in format minLat, minLon, maxLat, maxLon in exactly this order as
	 *            degrees
	 */
	public void addBboxConfiguration(String bbox) {
		if (bbox != null) {
			setBboxConfiguration(org.mapsforge.core.model.BoundingBox.fromString(bbox));
		}
	}

	/**
	 * Convenience method.
	 * 
	 * @param encoding
	 *            the choice for the encoding, either auto, single or double are valid parameters
	 */
	public void addEncodingChoice(String encoding) {
		if (encoding != null) {
			setEncodingChoice(EncodingChoice.fromString(encoding));
		}
	}

	/**
	 * Convenience method.
	 * 
	 * @param position
	 *            the map start position in format latitude, longitude
	 */
	public void addMapStartPosition(String position) {
		if (position != null) {
			setMapStartPosition(LatLongUtils.fromString(position));
		}
	}

	/**
	 * Convenience method.
	 * 
	 * @param zoom
	 *            the map start zoom level
	 */
	public void addMapStartZoom(String zoom) {
		if (zoom != null) {
			try {
				int intZoom = Integer.parseInt(zoom);
				if (intZoom < 0 || intZoom > 21) {
					throw new IllegalArgumentException("not a valid map start zoom: " + zoom);
				}
				setMapStartZoomLevel(intZoom);
			} catch (NumberFormatException e) {
				throw new IllegalArgumentException("not a valid map start zoom: " + zoom, e);
			}
		} else {
			setMapStartZoomLevel(-1);
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
	 * Convenience method.
	 * 
	 * @param zoomIntervalConfiguaration
	 *            the zoom interval configuration
	 */
	public void addZoomIntervalConfiguration(String zoomIntervalConfiguaration) {
		if (zoomIntervalConfiguaration != null) {
			setZoomIntervalConfiguration(ZoomIntervalConfiguration.fromString(zoomIntervalConfiguaration));
		} else {
			setZoomIntervalConfiguration(ZoomIntervalConfiguration.getStandardConfiguration());
		}
	}

	/**
	 * @return the bboxConfiguration
	 */
	public BoundingBox getBboxConfiguration() {
		return this.bboxConfiguration;
	}

	/**
	 * @return the bboxEnlargement
	 */
	public int getBboxEnlargement() {
		return this.bboxEnlargement;
	}

	/**
	 * @return the comment
	 */
	public String getComment() {
		return this.comment;
	}

	/**
	 * @return the dataProcessorType
	 */
	public String getDataProcessorType() {
		return this.dataProcessorType;
	}

	/**
	 * @return the date
	 */
	public long getDate() {
		return this.date;
	}

	/**
	 * @return the encodingChoice
	 */
	public EncodingChoice getEncodingChoice() {
		return this.encodingChoice;
	}

	/**
	 * @return the fileSpecificationVersion
	 */
	public int getFileSpecificationVersion() {
		return this.fileSpecificationVersion;
	}

	/**
	 * @return the mapStartPosition
	 */
	public LatLong getMapStartPosition() {
		return this.mapStartPosition;
	}

	/**
	 * @return the mapStartZoomLevel
	 */
	public int getMapStartZoomLevel() {
		return this.mapStartZoomLevel;
	}

	/**
	 * @return the outputFile
	 */
	public File getOutputFile() {
		return this.outputFile;
	}

	/**
	 * @return the preferredLanguage
	 */
	public String getPreferredLanguage() {
		return this.preferredLanguage;
	}

	/**
	 * @return the simplification
	 */
	public double getSimplification() {
		return this.simplification;
	}

	/**
	 * @return the tagMapping
	 */
	public OSMTagMapping getTagMapping() {
		return this.tagMapping;
	}

	/**
	 * @return the writerVersion
	 */
	public String getWriterVersion() {
		return this.writerVersion;
	}

	/**
	 * @return the zoomIntervalConfiguration
	 */
	public ZoomIntervalConfiguration getZoomIntervalConfiguration() {
		return this.zoomIntervalConfiguration;
	}

	/**
	 * Convenience method.
	 * 
	 * @return true if map start zoom level is set
	 */
	public boolean hasMapStartZoomLevel() {
		return getMapStartZoomLevel() >= 0;
	}

	/**
	 * @return the debugStrings
	 */
	public boolean isDebugStrings() {
		return this.debugStrings;
	}

	/**
	 * @return the labelPosition
	 */
	public boolean isLabelPosition() {
		return this.labelPosition;
	}

	/**
	 * @return the polygonClipping
	 */
	public boolean isPolygonClipping() {
		return this.polygonClipping;
	}

	/**
	 * @return the skipInvalidRelations
	 */
	public boolean isSkipInvalidRelations() {
		return this.skipInvalidRelations;
	}

	/**
	 * @return the wayClipping
	 */
	public boolean isWayClipping() {
		return this.wayClipping;
	}

	/**
	 * Convenience method.
	 * 
	 * @param file
	 *            the path to the output file
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
				this.tagMapping = OSMTagMapping.getInstance(f.toURI().toURL());
			} catch (MalformedURLException e) {
				throw new RuntimeException(e);
			}
		} else {
			this.tagMapping = OSMTagMapping.getInstance();
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
	 * @param bboxEnlargement
	 *            the bboxEnlargement to set
	 */
	public void setBboxEnlargement(int bboxEnlargement) {
		this.bboxEnlargement = bboxEnlargement;
	}

	/**
	 * @param comment
	 *            the comment to set
	 */
	public void setComment(String comment) {
		if (comment != null && !comment.isEmpty()) {
			this.comment = comment;
		}
	}

	/**
	 * @param dataProcessorType
	 *            the dataProcessorType to set
	 */
	public void setDataProcessorType(String dataProcessorType) {
		this.dataProcessorType = dataProcessorType;
	}

	/**
	 * @param date
	 *            the date to set
	 */
	public void setDate(long date) {
		this.date = date;
	}

	/**
	 * @param debugStrings
	 *            the debugStrings to set
	 */
	public void setDebugStrings(boolean debugStrings) {
		this.debugStrings = debugStrings;
	}

	/**
	 * @param encodingChoice
	 *            the encodingChoice to set
	 */
	public void setEncodingChoice(EncodingChoice encodingChoice) {
		this.encodingChoice = encodingChoice;
	}

	/**
	 * @param fileSpecificationVersion
	 *            the fileSpecificationVersion to set
	 */
	public void setFileSpecificationVersion(int fileSpecificationVersion) {
		this.fileSpecificationVersion = fileSpecificationVersion;
	}

	/**
	 * @param labelPosition
	 *            the labelPosition to set
	 */
	public void setLabelPosition(boolean labelPosition) {
		this.labelPosition = labelPosition;
	}

	/**
	 * @param mapStartPosition
	 *            the mapStartPosition to set
	 */
	public void setMapStartPosition(LatLong mapStartPosition) {
		this.mapStartPosition = mapStartPosition;
	}

	/**
	 * @param mapStartZoomLevel
	 *            the mapStartZoomLevel to set
	 */
	public void setMapStartZoomLevel(int mapStartZoomLevel) {
		this.mapStartZoomLevel = mapStartZoomLevel;
	}

	/**
	 * @param outputFile
	 *            the outputFile to set
	 */
	public void setOutputFile(File outputFile) {
		this.outputFile = outputFile;
	}

	/**
	 * @param polygonClipping
	 *            the polygonClipping to set
	 */
	public void setPolygonClipping(boolean polygonClipping) {
		this.polygonClipping = polygonClipping;
	}

	/**
	 * @param preferredLanguage
	 *            the preferredLanguage to set
	 */
	public void setPreferredLanguage(String preferredLanguage) {
		if (preferredLanguage != null && !preferredLanguage.isEmpty()) {
			this.preferredLanguage = preferredLanguage;
		}
	}

	/**
	 * @param simplification
	 *            the simplification to set
	 */
	public void setSimplification(double simplification) {
		if (simplification < 0) {
			throw new RuntimeException("simplification must be >= 0");
		}

		this.simplification = simplification;
	}

	/**
	 * @param skipInvalidRelations
	 *            the skipInvalidRelations to set
	 */
	public void setSkipInvalidRelations(boolean skipInvalidRelations) {
		this.skipInvalidRelations = skipInvalidRelations;
	}

	/**
	 * @param wayClipping
	 *            the wayClipping to set
	 */
	public void setWayClipping(boolean wayClipping) {
		this.wayClipping = wayClipping;
	}

	/**
	 * @param writerVersion
	 *            the writerVersion to set
	 */
	public void setWriterVersion(String writerVersion) {
		this.writerVersion = writerVersion;
	}

	/**
	 * @param zoomIntervalConfiguration
	 *            the zoomIntervalConfiguration to set
	 */
	public void setZoomIntervalConfiguration(ZoomIntervalConfiguration zoomIntervalConfiguration) {
		this.zoomIntervalConfiguration = zoomIntervalConfiguration;
	}

	/**
	 * Validates this configuration.
	 * 
	 * @throws IllegalArgumentException
	 *             thrown if configuration is invalid
	 */
	public void validate() {
		if (this.mapStartPosition != null && this.bboxConfiguration != null
				&& !this.bboxConfiguration.contains(this.mapStartPosition)) {
			throw new IllegalArgumentException(
					"map start position is not valid, must be included in bounding box of the map, bbox: "
							+ this.bboxConfiguration.toString() + " - map start position: "
							+ this.mapStartPosition.toString());
		}
	}
}
