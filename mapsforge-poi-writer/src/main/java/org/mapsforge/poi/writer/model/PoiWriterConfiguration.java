/*
 * Copyright 2015-2017 devemux86
 * Copyright 2017 Gustl22
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
    private boolean allTags;
    private BoundingBox bboxConfiguration;
    private String comment;
    private boolean geoTags;
    private int fileSpecificationVersion;
    private boolean filterCategories;
    private boolean names;
    private File outputFile;
    private String preferredLanguage;
    private URL tagMapping;
    private boolean ways;
    private String writerVersion;
    private boolean autoGeoTags;

    /**
     * Convenience method.
     *
     * @param bbox the bounding box specification in format minLat, minLon, maxLat, maxLon in exactly this order as
     *             degrees
     */
    public void addBboxConfiguration(String bbox) {
        if (bbox != null) {
            setBboxConfiguration(BoundingBox.fromString(bbox));
        }
    }

    /**
     * Convenience method.
     *
     * @param file the path to the output file
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
     * @return the bounding box configuration
     */
    public BoundingBox getBboxConfiguration() {
        return this.bboxConfiguration;
    }

    /**
     * @return the comment
     */
    public String getComment() {
        return this.comment;
    }

    /**
     * @return the file specification version
     */
    public int getFileSpecificationVersion() {
        return this.fileSpecificationVersion;
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
     * @return the writer version
     */
    public String getWriterVersion() {
        return this.writerVersion;
    }

    /**
     * @return the all tags
     */
    public boolean isAllTags() {
        return allTags;
    }

    /**
     * @return the filter categories
     */
    public boolean isFilterCategories() {
        return filterCategories;
    }

    /**
     * @return if add additional tags to data, to resolve geolocation
     */
    public boolean isGeoTags() {
        return this.geoTags;
    }

    /**
     * @return the names
     */
    public boolean isNames() {
        return names;
    }

    /**
     * @return the ways
     */
    public boolean isWays() {
        return ways;
    }

    /**
     * Add additional tags to data, to resolve geolocation
     * @return boolean, if it's enabled
     */
    public boolean isAutoGeoTags() {
        return autoGeoTags;
    }

    /**
     * Convenience method.
     *
     * @param file the path to the tag mapping
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
     * @param allTags the all tags to set
     */
    public void setAllTags(boolean allTags) {
        this.allTags = allTags;
    }

    /**
     * @param bboxConfiguration the bounding box configuration to set
     */
    public void setBboxConfiguration(BoundingBox bboxConfiguration) {
        this.bboxConfiguration = bboxConfiguration;
    }

    /**
     * @param comment the comment to set
     */
    public void setComment(String comment) {
        if (comment != null && !comment.isEmpty()) {
            this.comment = comment;
        }
    }

    /**
     * @param fileSpecificationVersion the file specification version to set
     */
    public void setFileSpecificationVersion(int fileSpecificationVersion) {
        this.fileSpecificationVersion = fileSpecificationVersion;
    }

    /**
     * @param filterCategories the filter categories to set
     */
    public void setFilterCategories(boolean filterCategories) {
        this.filterCategories = filterCategories;
    }

    /**
     * @param geoTags if add additional tags to data, to resolve geolocation
     */
    public void setGeoTags(boolean geoTags) {
        this.geoTags = geoTags;
    }

    /**
     * @param names the names to set
     */
    public void setNames(boolean names) {
        this.names = names;
    }

    /**
     * @param outputFile the output file to set
     */
    public void setOutputFile(File outputFile) {
        this.outputFile = outputFile;
    }

    /**
     * @param preferredLanguage the preferred language to set
     */
    public void setPreferredLanguage(String preferredLanguage) {
        if (preferredLanguage != null && !preferredLanguage.isEmpty()) {
            this.preferredLanguage = preferredLanguage;
        }
    }

    /**
     * @param ways the ways to set
     */
    public void setWays(boolean ways) {
        this.ways = ways;
    }

    /**
     * @param writerVersion the writer version to set
     */
    public void setWriterVersion(String writerVersion) {
        this.writerVersion = writerVersion;
    }

    /**
     * Sets configuration for autoGeoTags
     * @param autoGeoTags true: enable geoTags, else false
     */
    public void setAutoGeoTags(boolean autoGeoTags) {
        this.autoGeoTags = autoGeoTags;
    }
}
