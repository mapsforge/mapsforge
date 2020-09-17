/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright 2014-2015 Ludwig M Brinckmann
 * Copyright 2014-2020 devemux86
 * Copyright 2015-2016 lincomatic
 * Copyright 2016 bvgastel
 * Copyright 2017 linuskr
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
package org.mapsforge.map.reader;

import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Tag;
import org.mapsforge.core.model.Tile;
import org.mapsforge.core.util.LatLongUtils;
import org.mapsforge.core.util.MercatorProjection;
import org.mapsforge.core.util.Parameters;
import org.mapsforge.map.datastore.*;
import org.mapsforge.map.reader.header.MapFileException;
import org.mapsforge.map.reader.header.MapFileHeader;
import org.mapsforge.map.reader.header.MapFileInfo;
import org.mapsforge.map.reader.header.SubFileParameter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A class for reading binary map files.
 * <p/>
 * The readMapData method is now thread safe, but care should be taken that not too much data is
 * read at the same time (keep simultaneous requests to minimum)
 *
 * @see <a href="https://github.com/mapsforge/mapsforge/blob/master/docs/Specification-Binary-Map-File.md">Specification</a>
 */
public class MapFile extends MapDataStore {
    private static final Logger LOGGER = Logger.getLogger(MapFile.class.getName());
    /* Only for testing, an empty file. */
    public static final MapFile TEST_MAP_FILE = new MapFile();
    /**
     * Bitmask to extract the block offset from an index entry.
     */
    private static final long BITMASK_INDEX_OFFSET = 0x7FFFFFFFFFL;
    /**
     * Bitmask to extract the water information from an index entry.
     */
    private static final long BITMASK_INDEX_WATER = 0x8000000000L;
    /**
     * Default start zoom level.
     */
    private static final byte DEFAULT_START_ZOOM_LEVEL = 12;
    /**
     * Amount of cache blocks that the index cache should store.
     */
    private static final int INDEX_CACHE_SIZE = 64;
    /**
     * Error message for an invalid first way offset.
     */
    private static final String INVALID_FIRST_WAY_OFFSET = "invalid first way offset: ";
    /**
     * Bitmask for the optional POI feature "elevation".
     */
    private static final int POI_FEATURE_ELEVATION = 0x20;
    /**
     * Bitmask for the optional POI feature "house number".
     */
    private static final int POI_FEATURE_HOUSE_NUMBER = 0x40;
    /**
     * Bitmask for the optional POI feature "name".
     */
    private static final int POI_FEATURE_NAME = 0x80;
    /**
     * Bitmask for the POI layer.
     */
    private static final int POI_LAYER_BITMASK = 0xf0;
    /**
     * Bit shift for calculating the POI layer.
     */
    private static final int POI_LAYER_SHIFT = 4;
    /**
     * Bitmask for the number of POI tags.
     */
    private static final int POI_NUMBER_OF_TAGS_BITMASK = 0x0f;
    /**
     * Length of the debug signature at the beginning of each block.
     */
    private static final byte SIGNATURE_LENGTH_BLOCK = 32;
    /**
     * Length of the debug signature at the beginning of each POI.
     */
    private static final byte SIGNATURE_LENGTH_POI = 32;
    /**
     * Length of the debug signature at the beginning of each way.
     */
    private static final byte SIGNATURE_LENGTH_WAY = 32;
    /**
     * The key of the elevation OpenStreetMap tag.
     */
    private static final String TAG_KEY_ELE = "ele";
    /**
     * The key of the house number OpenStreetMap tag.
     */
    private static final String TAG_KEY_HOUSE_NUMBER = "addr:housenumber";
    /**
     * The key of the name OpenStreetMap tag.
     */
    private static final String TAG_KEY_NAME = "name";
    /**
     * The key of the reference OpenStreetMap tag.
     */
    private static final String TAG_KEY_REF = "ref";
    /**
     * Bitmask for the optional way data blocks byte.
     */
    private static final int WAY_FEATURE_DATA_BLOCKS_BYTE = 0x08;
    /**
     * Bitmask for the optional way double delta encoding.
     */
    private static final int WAY_FEATURE_DOUBLE_DELTA_ENCODING = 0x04;
    /**
     * Bitmask for the optional way feature "house number".
     */
    private static final int WAY_FEATURE_HOUSE_NUMBER = 0x40;
    /**
     * Bitmask for the optional way feature "label position".
     */
    private static final int WAY_FEATURE_LABEL_POSITION = 0x10;
    /**
     * Bitmask for the optional way feature "name".
     */
    private static final int WAY_FEATURE_NAME = 0x80;
    /**
     * Bitmask for the optional way feature "reference".
     */
    private static final int WAY_FEATURE_REF = 0x20;
    /**
     * Bitmask for the way layer.
     */
    private static final int WAY_LAYER_BITMASK = 0xf0;
    /**
     * Bit shift for calculating the way layer.
     */
    private static final int WAY_LAYER_SHIFT = 4;
    /**
     * Bitmask for the number of way tags.
     */
    private static final int WAY_NUMBER_OF_TAGS_BITMASK = 0x0f;
    /**
     * Way filtering reduces the number of ways returned to only those that are
     * relevant for the tile requested, leading to performance gains, but can
     * cause line clipping artifacts (particularly at higher zoom levels). The
     * risk of clipping can be reduced by either turning way filtering off or by
     * increasing the wayFilterDistance which governs how large an area surrounding
     * the requested tile will be returned.
     * For most use cases the standard settings should be sufficient.
     */
    public static boolean wayFilterEnabled = true;
    public static int wayFilterDistance = 20;

    private final IndexCache databaseIndexCache;
    private final long fileSize;
    private final FileChannel inputChannel;
    private final MapFileHeader mapFileHeader;
    private final long timestamp;

    private byte zoomLevelMin = 0;
    private byte zoomLevelMax = Byte.MAX_VALUE;

    private MapFile() {
        // only to create a dummy empty file.
        databaseIndexCache = null;
        fileSize = 0;
        inputChannel = null;
        mapFileHeader = null;
        timestamp = System.currentTimeMillis();
    }

    /**
     * Opens the given map file, reads its header data and validates them. Uses default language.
     *
     * @param mapFile the map file.
     * @throws MapFileException if the given map file is null or invalid.
     */
    public MapFile(File mapFile) {
        this(mapFile, null);
    }

    /**
     * Opens the given map file, reads its header data and validates them.
     *
     * @param mapFile  the map file.
     * @param language the language to use (may be null).
     * @throws MapFileException if the given map file is null or invalid.
     */
    public MapFile(File mapFile, String language) {
        super(language);
        if (mapFile == null) {
            throw new MapFileException("mapFile must not be null");
        }
        try {
            // check if the file exists and is readable
            if (!mapFile.exists()) {
                throw new MapFileException("file does not exist: " + mapFile);
            } else if (!mapFile.isFile()) {
                throw new MapFileException("not a file: " + mapFile);
            } else if (!mapFile.canRead()) {
                throw new MapFileException("cannot read file: " + mapFile);
            }

            // false positive: stream gets closed when the channel is closed
            // see e.g. http://bugs.java.com/bugdatabase/view_bug.do?bug_id=4796385
            FileInputStream fis = new FileInputStream(mapFile);
            this.inputChannel = fis.getChannel();
            this.fileSize = this.inputChannel.size();

            ReadBuffer readBuffer = new ReadBuffer(this.inputChannel);
            this.mapFileHeader = new MapFileHeader();
            this.mapFileHeader.readHeader(readBuffer, this.fileSize);
            this.databaseIndexCache = new IndexCache(this.inputChannel, INDEX_CACHE_SIZE);

            this.timestamp = mapFile.lastModified();
        } catch (Exception e) {
            // make sure that the channel is closed
            closeFileChannel();
            throw new MapFileException(e.getMessage());
        }
    }

    /**
     * Opens the given map file input stream, reads its header data and validates them.
     *
     * @param mapFileInputStream the map file input stream.
     * @throws MapFileException if the given map file is null or invalid.
     */
    public MapFile(FileInputStream mapFileInputStream) {
        this(mapFileInputStream, null);
    }

    /**
     * Opens the given map file input stream, reads its header data and validates them.
     *
     * @param mapFileInputStream the map file input stream.
     * @param language           the language to use (may be null).
     * @throws MapFileException if the given map file is null or invalid.
     */
    public MapFile(FileInputStream mapFileInputStream, String language) {
        this(mapFileInputStream, System.currentTimeMillis(), language);
    }

    /**
     * Opens the given map file input stream, reads its header data and validates them.
     *
     * @param mapFileInputStream the map file input stream.
     * @param language           the language to use (may be null).
     * @throws MapFileException if the given map file is null or invalid.
     */
    public MapFile(FileInputStream mapFileInputStream, long lastModified, String language) {
        super(language);
        if (mapFileInputStream == null) {
            throw new MapFileException("mapFileInputStream must not be null");
        }
        try {
            this.inputChannel = mapFileInputStream.getChannel();
            this.fileSize = this.inputChannel.size();

            ReadBuffer readBuffer = new ReadBuffer(this.inputChannel);
            this.mapFileHeader = new MapFileHeader();
            this.mapFileHeader.readHeader(readBuffer, this.fileSize);
            this.databaseIndexCache = new IndexCache(this.inputChannel, INDEX_CACHE_SIZE);

            this.timestamp = lastModified;
        } catch (Exception e) {
            // make sure that the channel is closed
            closeFileChannel();
            throw new MapFileException(e.getMessage());
        }
    }

    /**
     * Opens the given map file channel, reads its header data and validates them.
     *
     * @param mapFileChannel the map file channel.
     * @throws MapFileException if the given map file channel is null or invalid.
     */
    public MapFile(FileChannel mapFileChannel) {
        this(mapFileChannel, null);
    }

    /**
     * Opens the given map file channel, reads its header data and validates them.
     *
     * @param mapFileChannel the map file channel.
     * @param language       the language to use (may be null).
     * @throws MapFileException if the given map file channel is null or invalid.
     */
    public MapFile(FileChannel mapFileChannel, String language) {
        this(mapFileChannel, System.currentTimeMillis(), language);
    }

    /**
     * Opens the given map file channel, reads its header data and validates them.
     *
     * @param mapFileChannel the map file channel.
     * @param language       the language to use (may be null).
     * @throws MapFileException if the given map file channel is null or invalid.
     */
    public MapFile(FileChannel mapFileChannel, long lastModified, String language) {
        super(language);
        if (mapFileChannel == null) {
            throw new MapFileException("mapFileChannel must not be null");
        }
        try {
            this.inputChannel = mapFileChannel;
            this.fileSize = this.inputChannel.size();

            ReadBuffer readBuffer = new ReadBuffer(this.inputChannel);
            this.mapFileHeader = new MapFileHeader();
            this.mapFileHeader.readHeader(readBuffer, this.fileSize);
            this.databaseIndexCache = new IndexCache(this.inputChannel, INDEX_CACHE_SIZE);

            this.timestamp = lastModified;
        } catch (Exception e) {
            // make sure that the channel is closed
            closeFileChannel();
            throw new MapFileException(e.getMessage());
        }
    }

    /**
     * Opens the given map file, reads its header data and validates them. Uses default language.
     *
     * @param mapPath the path of the map file.
     * @throws MapFileException if the given map file is null or invalid.
     */
    public MapFile(String mapPath) {
        this(mapPath, null);
    }

    /**
     * Opens the given map file, reads its header data and validates them.
     *
     * @param mapPath  the path of the map file.
     * @param language the language to use (may be null).
     * @throws MapFileException if the given map file is null or invalid or IOException if the file
     *                          cannot be opened.
     */
    public MapFile(String mapPath, String language) {
        this(new File(mapPath), language);
    }

    @Override
    public BoundingBox boundingBox() {
        return getMapFileInfo().boundingBox;
    }

    @Override
    public void close() {
        closeFileChannel();
    }

    /**
     * Closes the map file channel and destroys all internal caches.
     * Has no effect if no map file channel is currently opened.
     */
    private void closeFileChannel() {
        try {
            if (this.databaseIndexCache != null) {
                this.databaseIndexCache.destroy();
            }
            if (this.inputChannel != null) {
                this.inputChannel.close();
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }
    }

    private void decodeWayNodesDoubleDelta(LatLong[] waySegment, double tileLatitude, double tileLongitude, ReadBuffer readBuffer) {
        // get the first way node latitude offset (VBE-S)
        double wayNodeLatitude = tileLatitude
                + LatLongUtils.microdegreesToDegrees(readBuffer.readSignedInt());

        // get the first way node longitude offset (VBE-S)
        double wayNodeLongitude = tileLongitude
                + LatLongUtils.microdegreesToDegrees(readBuffer.readSignedInt());

        // store the first way node
        waySegment[0] = new LatLong(wayNodeLatitude, wayNodeLongitude);

        double previousSingleDeltaLatitude = 0;
        double previousSingleDeltaLongitude = 0;

        for (int wayNodesIndex = 1; wayNodesIndex < waySegment.length; ++wayNodesIndex) {
            // get the way node latitude double-delta offset (VBE-S)
            double doubleDeltaLatitude = LatLongUtils.microdegreesToDegrees(readBuffer.readSignedInt());

            // get the way node longitude double-delta offset (VBE-S)
            double doubleDeltaLongitude = LatLongUtils.microdegreesToDegrees(readBuffer.readSignedInt());

            double singleDeltaLatitude = doubleDeltaLatitude + previousSingleDeltaLatitude;
            double singleDeltaLongitude = doubleDeltaLongitude + previousSingleDeltaLongitude;

            wayNodeLatitude = wayNodeLatitude + singleDeltaLatitude;
            wayNodeLongitude = wayNodeLongitude + singleDeltaLongitude;

            // Decoding near international date line can return values slightly outside valid [-180°, 180°] due to calculation precision
            if (wayNodeLongitude < LatLongUtils.LONGITUDE_MIN
                    && (LatLongUtils.LONGITUDE_MIN - wayNodeLongitude) < 0.001) {
                wayNodeLongitude = LatLongUtils.LONGITUDE_MIN;
            } else if (wayNodeLongitude > LatLongUtils.LONGITUDE_MAX
                    && (wayNodeLongitude - LatLongUtils.LONGITUDE_MAX) < 0.001) {
                wayNodeLongitude = LatLongUtils.LONGITUDE_MAX;
            }

            waySegment[wayNodesIndex] = new LatLong(wayNodeLatitude, wayNodeLongitude);

            previousSingleDeltaLatitude = singleDeltaLatitude;
            previousSingleDeltaLongitude = singleDeltaLongitude;
        }
    }

    private void decodeWayNodesSingleDelta(LatLong[] waySegment, double tileLatitude, double tileLongitude, ReadBuffer readBuffer) {
        // get the first way node latitude single-delta offset (VBE-S)
        double wayNodeLatitude = tileLatitude
                + LatLongUtils.microdegreesToDegrees(readBuffer.readSignedInt());

        // get the first way node longitude single-delta offset (VBE-S)
        double wayNodeLongitude = tileLongitude
                + LatLongUtils.microdegreesToDegrees(readBuffer.readSignedInt());

        // store the first way node
        waySegment[0] = new LatLong(wayNodeLatitude, wayNodeLongitude);

        for (int wayNodesIndex = 1; wayNodesIndex < waySegment.length; ++wayNodesIndex) {
            // get the way node latitude offset (VBE-S)
            wayNodeLatitude = wayNodeLatitude + LatLongUtils.microdegreesToDegrees(readBuffer.readSignedInt());

            // get the way node longitude offset (VBE-S)
            wayNodeLongitude = wayNodeLongitude + LatLongUtils.microdegreesToDegrees(readBuffer.readSignedInt());

            // Decoding near international date line can return values slightly outside valid [-180°, 180°] due to calculation precision
            if (wayNodeLongitude < LatLongUtils.LONGITUDE_MIN
                    && (LatLongUtils.LONGITUDE_MIN - wayNodeLongitude) < 0.001) {
                wayNodeLongitude = LatLongUtils.LONGITUDE_MIN;
            } else if (wayNodeLongitude > LatLongUtils.LONGITUDE_MAX
                    && (wayNodeLongitude - LatLongUtils.LONGITUDE_MAX) < 0.001) {
                wayNodeLongitude = LatLongUtils.LONGITUDE_MAX;
            }

            waySegment[wayNodesIndex] = new LatLong(wayNodeLatitude, wayNodeLongitude);
        }
    }

    /**
     * Returns the creation timestamp of the map file.
     *
     * @param tile not used, as all tiles will shared the same creation date.
     * @return the creation timestamp inside the map file.
     */
    @Override
    public long getDataTimestamp(Tile tile) {
        return this.timestamp;
    }

    /**
     * @return the header data for the current map file.
     */
    public MapFileHeader getMapFileHeader() {
        return this.mapFileHeader;
    }

    /**
     * @return the metadata for the current map file.
     */
    public MapFileInfo getMapFileInfo() {
        return this.mapFileHeader.getMapFileInfo();
    }

    /**
     * @return the map file supported languages (may be null).
     */
    public String[] getMapLanguages() {
        String languagesPreference = getMapFileInfo().languagesPreference;
        if (languagesPreference != null && !languagesPreference.trim().isEmpty()) {
            return languagesPreference.split(",");
        }
        return null;
    }

    private PoiWayBundle processBlock(QueryParameters queryParameters, SubFileParameter subFileParameter,
                                      BoundingBox boundingBox, double tileLatitude, double tileLongitude,
                                      Selector selector, ReadBuffer readBuffer) {
        if (!processBlockSignature(readBuffer)) {
            return null;
        }

        int[][] zoomTable = readZoomTable(subFileParameter, readBuffer);
        int zoomTableRow = queryParameters.queryZoomLevel - subFileParameter.zoomLevelMin;
        int poisOnQueryZoomLevel = zoomTable[zoomTableRow][0];
        int waysOnQueryZoomLevel = zoomTable[zoomTableRow][1];

        // get the relative offset to the first stored way in the block
        int firstWayOffset = readBuffer.readUnsignedInt();
        if (firstWayOffset < 0) {
            LOGGER.warning(INVALID_FIRST_WAY_OFFSET + firstWayOffset);
            return null;
        }

        // add the current buffer position to the relative first way offset
        firstWayOffset += readBuffer.getBufferPosition();
        if (firstWayOffset > readBuffer.getBufferSize()) {
            LOGGER.warning(INVALID_FIRST_WAY_OFFSET + firstWayOffset);
            return null;
        }

        boolean filterRequired = queryParameters.queryZoomLevel > subFileParameter.baseZoomLevel;

        List<PointOfInterest> pois = processPOIs(tileLatitude, tileLongitude, poisOnQueryZoomLevel, boundingBox, filterRequired, readBuffer);
        if (pois == null) {
            return null;
        }

        List<Way> ways;
        if (Selector.POIS == selector) {
            ways = Collections.emptyList();
        } else {
            // finished reading POIs, check if the current buffer position is valid
            if (readBuffer.getBufferPosition() > firstWayOffset) {
                LOGGER.warning("invalid buffer position: " + readBuffer.getBufferPosition());
                return null;
            }

            // move the pointer to the first way
            readBuffer.setBufferPosition(firstWayOffset);

            ways = processWays(queryParameters, waysOnQueryZoomLevel, boundingBox,
                    filterRequired, tileLatitude, tileLongitude, selector, readBuffer);
            if (ways == null) {
                return null;
            }
        }

        return new PoiWayBundle(pois, ways);
    }

    /**
     * Processes the block signature, if present.
     *
     * @return true if the block signature could be processed successfully, false otherwise.
     */
    private boolean processBlockSignature(ReadBuffer readBuffer) {
        if (this.mapFileHeader.getMapFileInfo().debugFile) {
            // get and check the block signature
            String signatureBlock = readBuffer.readUTF8EncodedString(SIGNATURE_LENGTH_BLOCK);
            if (!signatureBlock.startsWith("###TileStart")) {
                LOGGER.warning("invalid block signature: " + signatureBlock);
                return false;
            }
        }
        return true;
    }

    private MapReadResult processBlocks(QueryParameters queryParameters, SubFileParameter subFileParameter,
                                        BoundingBox boundingBox, Selector selector) throws IOException {
        boolean queryIsWater = true;
        boolean queryReadWaterInfo = false;

        MapReadResult mapFileReadResult = new MapReadResult();

        // read and process all blocks from top to bottom and from left to right
        for (long row = queryParameters.fromBlockY; row <= queryParameters.toBlockY; ++row) {
            for (long column = queryParameters.fromBlockX; column <= queryParameters.toBlockX; ++column) {
                // calculate the actual block number of the needed block in the file
                long blockNumber = row * subFileParameter.blocksWidth + column;

                // get the current index entry
                long currentBlockIndexEntry = this.databaseIndexCache.getIndexEntry(subFileParameter, blockNumber);

                // check if the current query would still return a water tile
                if (queryIsWater) {
                    // check the water flag of the current block in its index entry
                    queryIsWater &= (currentBlockIndexEntry & BITMASK_INDEX_WATER) != 0;
                    queryReadWaterInfo = true;
                }

                // get and check the current block pointer
                long currentBlockPointer = currentBlockIndexEntry & BITMASK_INDEX_OFFSET;
                if (currentBlockPointer < 1 || currentBlockPointer > subFileParameter.subFileSize) {
                    LOGGER.warning("invalid current block pointer: " + currentBlockPointer);
                    LOGGER.warning("subFileSize: " + subFileParameter.subFileSize);
                    return null;
                }

                long nextBlockPointer;
                // check if the current block is the last block in the file
                if (blockNumber + 1 == subFileParameter.numberOfBlocks) {
                    // set the next block pointer to the end of the file
                    nextBlockPointer = subFileParameter.subFileSize;
                } else {
                    // get and check the next block pointer
                    nextBlockPointer = this.databaseIndexCache.getIndexEntry(subFileParameter, blockNumber + 1)
                            & BITMASK_INDEX_OFFSET;
                    if (nextBlockPointer > subFileParameter.subFileSize) {
                        LOGGER.warning("invalid next block pointer: " + nextBlockPointer);
                        LOGGER.warning("sub-file size: " + subFileParameter.subFileSize);
                        return null;
                    }
                }

                // calculate the size of the current block
                int currentBlockSize = (int) (nextBlockPointer - currentBlockPointer);
                if (currentBlockSize < 0) {
                    LOGGER.warning("current block size must not be negative: " + currentBlockSize);
                    return null;
                } else if (currentBlockSize == 0) {
                    // the current block is empty, continue with the next block
                    continue;
                } else if (currentBlockSize > Parameters.MAXIMUM_BUFFER_SIZE) {
                    // the current block is too large, continue with the next block
                    LOGGER.warning("current block size too large: " + currentBlockSize);
                    continue;
                } else if (currentBlockPointer + currentBlockSize > this.fileSize) {
                    LOGGER.warning("current block largher than file size: " + currentBlockSize);
                    return null;
                }

                // seek to the current block in the map file
                // read the current block into the buffer
                ReadBuffer readBuffer = new ReadBuffer(inputChannel);
                if (!readBuffer.readFromFile(subFileParameter.startAddress + currentBlockPointer, currentBlockSize)) {
                    // skip the current block
                    LOGGER.warning("reading current block has failed: " + currentBlockSize);
                    return null;
                }

                // calculate the top-left coordinates of the underlying tile
                double tileLatitude = MercatorProjection.tileYToLatitude(subFileParameter.boundaryTileTop + row,
                        subFileParameter.baseZoomLevel);
                double tileLongitude = MercatorProjection.tileXToLongitude(subFileParameter.boundaryTileLeft + column,
                        subFileParameter.baseZoomLevel);

                try {
                    PoiWayBundle poiWayBundle = processBlock(queryParameters, subFileParameter, boundingBox,
                            tileLatitude, tileLongitude, selector, readBuffer);
                    if (poiWayBundle != null) {
                        mapFileReadResult.add(poiWayBundle);
                    }
                } catch (ArrayIndexOutOfBoundsException e) {
                    LOGGER.log(Level.SEVERE, e.getMessage(), e);
                }
            }
        }

        // the query is finished, was the water flag set for all blocks?
        if (queryIsWater && queryReadWaterInfo) {
            // Deprecate water tiles rendering
            //mapFileReadResult.isWater = true;
        }

        return mapFileReadResult;
    }

    private List<PointOfInterest> processPOIs(double tileLatitude, double tileLongitude, int numberOfPois, BoundingBox boundingBox, boolean filterRequired, ReadBuffer readBuffer) {
        List<PointOfInterest> pois = new ArrayList<>();
        Tag[] poiTags = this.mapFileHeader.getMapFileInfo().poiTags;

        for (int elementCounter = numberOfPois; elementCounter != 0; --elementCounter) {
            if (this.mapFileHeader.getMapFileInfo().debugFile) {
                // get and check the POI signature
                String signaturePoi = readBuffer.readUTF8EncodedString(SIGNATURE_LENGTH_POI);
                if (!signaturePoi.startsWith("***POIStart")) {
                    LOGGER.warning("invalid POI signature: " + signaturePoi);
                    return null;
                }
            }

            // get the POI latitude offset (VBE-S)
            double latitude = tileLatitude + LatLongUtils.microdegreesToDegrees(readBuffer.readSignedInt());

            // get the POI longitude offset (VBE-S)
            double longitude = tileLongitude + LatLongUtils.microdegreesToDegrees(readBuffer.readSignedInt());

            // get the special byte which encodes multiple flags
            byte specialByte = readBuffer.readByte();

            // bit 1-4 represent the layer
            byte layer = (byte) ((specialByte & POI_LAYER_BITMASK) >>> POI_LAYER_SHIFT);
            // bit 5-8 represent the number of tag IDs
            byte numberOfTags = (byte) (specialByte & POI_NUMBER_OF_TAGS_BITMASK);

            // get the tags from IDs (VBE-U)
            List<Tag> tags = readBuffer.readTags(poiTags, numberOfTags);
            if (tags == null) {
                return null;
            }

            // get the feature bitmask (1 byte)
            byte featureByte = readBuffer.readByte();

            // bit 1-3 enable optional features
            boolean featureName = (featureByte & POI_FEATURE_NAME) != 0;
            boolean featureHouseNumber = (featureByte & POI_FEATURE_HOUSE_NUMBER) != 0;
            boolean featureElevation = (featureByte & POI_FEATURE_ELEVATION) != 0;

            // check if the POI has a name
            if (featureName) {
                tags.add(new Tag(TAG_KEY_NAME, extractLocalized(readBuffer.readUTF8EncodedString())));
            }

            // check if the POI has a house number
            if (featureHouseNumber) {
                tags.add(new Tag(TAG_KEY_HOUSE_NUMBER, readBuffer.readUTF8EncodedString()));
            }

            // check if the POI has an elevation
            if (featureElevation) {
                tags.add(new Tag(TAG_KEY_ELE, Integer.toString(readBuffer.readSignedInt())));
            }

            LatLong position = new LatLong(latitude, longitude);
            // depending on the zoom level configuration the poi can lie outside
            // the tile requested, we filter them out here
            if (!filterRequired || boundingBox.contains(position)) {
                pois.add(new PointOfInterest(layer, tags, position));
            }
        }

        return pois;
    }

    private LatLong[][] processWayDataBlock(double tileLatitude, double tileLongitude, boolean doubleDeltaEncoding, ReadBuffer readBuffer) {
        // get and check the number of way coordinate blocks (VBE-U)
        int numberOfWayCoordinateBlocks = readBuffer.readUnsignedInt();
        if (numberOfWayCoordinateBlocks < 1 || numberOfWayCoordinateBlocks > Short.MAX_VALUE) {
            LOGGER.warning("invalid number of way coordinate blocks: " + numberOfWayCoordinateBlocks);
            return null;
        }

        // create the array which will store the different way coordinate blocks
        LatLong[][] wayCoordinates = new LatLong[numberOfWayCoordinateBlocks][];

        // read the way coordinate blocks
        for (int coordinateBlock = 0; coordinateBlock < numberOfWayCoordinateBlocks; ++coordinateBlock) {
            // get and check the number of way nodes (VBE-U)
            int numberOfWayNodes = readBuffer.readUnsignedInt();
            if (numberOfWayNodes < 2 || numberOfWayNodes > Short.MAX_VALUE) {
                LOGGER.warning("invalid number of way nodes: " + numberOfWayNodes);
                // returning null here will actually leave the tile blank as the
                // position on the ReadBuffer will not be advanced correctly. However,
                // it will not crash the app.
                return null;
            }

            // create the array which will store the current way segment
            LatLong[] waySegment = new LatLong[numberOfWayNodes];

            if (doubleDeltaEncoding) {
                decodeWayNodesDoubleDelta(waySegment, tileLatitude, tileLongitude, readBuffer);
            } else {
                decodeWayNodesSingleDelta(waySegment, tileLatitude, tileLongitude, readBuffer);
            }

            wayCoordinates[coordinateBlock] = waySegment;
        }

        return wayCoordinates;
    }

    private List<Way> processWays(QueryParameters queryParameters, int numberOfWays, BoundingBox boundingBox,
                                  boolean filterRequired, double tileLatitude, double tileLongitude,
                                  Selector selector, ReadBuffer readBuffer) {
        List<Way> ways = new ArrayList<>();
        Tag[] wayTags = this.mapFileHeader.getMapFileInfo().wayTags;

        BoundingBox wayFilterBbox = boundingBox.extendMeters(wayFilterDistance);

        for (int elementCounter = numberOfWays; elementCounter != 0; --elementCounter) {
            if (this.mapFileHeader.getMapFileInfo().debugFile) {
                // get and check the way signature
                String signatureWay = readBuffer.readUTF8EncodedString(SIGNATURE_LENGTH_WAY);
                if (!signatureWay.startsWith("---WayStart")) {
                    LOGGER.warning("invalid way signature: " + signatureWay);
                    return null;
                }
            }

            // get the size of the way (VBE-U)
            int wayDataSize = readBuffer.readUnsignedInt();
            if (wayDataSize < 0) {
                LOGGER.warning("invalid way data size: " + wayDataSize);
                return null;
            }

            if (queryParameters.useTileBitmask) {
                // get the way tile bitmask (2 bytes)
                int tileBitmask = readBuffer.readShort();
                // check if the way is inside the requested tile
                if ((queryParameters.queryTileBitmask & tileBitmask) == 0) {
                    // skip the rest of the way and continue with the next way
                    readBuffer.skipBytes(wayDataSize - 2);
                    continue;
                }
            } else {
                // ignore the way tile bitmask (2 bytes)
                readBuffer.skipBytes(2);
            }

            // get the special byte which encodes multiple flags
            byte specialByte = readBuffer.readByte();

            // bit 1-4 represent the layer
            byte layer = (byte) ((specialByte & WAY_LAYER_BITMASK) >>> WAY_LAYER_SHIFT);
            // bit 5-8 represent the number of tag IDs
            byte numberOfTags = (byte) (specialByte & WAY_NUMBER_OF_TAGS_BITMASK);

            // get the tags from IDs (VBE-U)
            List<Tag> tags = readBuffer.readTags(wayTags, numberOfTags);
            if (tags == null) {
                return null;
            }

            // get the feature bitmask (1 byte)
            byte featureByte = readBuffer.readByte();

            // bit 1-6 enable optional features
            boolean featureName = (featureByte & WAY_FEATURE_NAME) != 0;
            boolean featureHouseNumber = (featureByte & WAY_FEATURE_HOUSE_NUMBER) != 0;
            boolean featureRef = (featureByte & WAY_FEATURE_REF) != 0;
            boolean featureLabelPosition = (featureByte & WAY_FEATURE_LABEL_POSITION) != 0;
            boolean featureWayDataBlocksByte = (featureByte & WAY_FEATURE_DATA_BLOCKS_BYTE) != 0;
            boolean featureWayDoubleDeltaEncoding = (featureByte & WAY_FEATURE_DOUBLE_DELTA_ENCODING) != 0;

            // check if the way has a name
            if (featureName) {
                tags.add(new Tag(TAG_KEY_NAME, extractLocalized(readBuffer.readUTF8EncodedString())));
            }

            // check if the way has a house number
            if (featureHouseNumber) {
                tags.add(new Tag(TAG_KEY_HOUSE_NUMBER, readBuffer.readUTF8EncodedString()));
            }

            // check if the way has a reference
            if (featureRef) {
                tags.add(new Tag(TAG_KEY_REF, readBuffer.readUTF8EncodedString()));
            }

            int[] labelPosition = null;
            if (featureLabelPosition) {
                labelPosition = readOptionalLabelPosition(readBuffer);
            }

            int wayDataBlocks = readOptionalWayDataBlocksByte(featureWayDataBlocksByte, readBuffer);
            if (wayDataBlocks < 1) {
                LOGGER.warning("invalid number of way data blocks: " + wayDataBlocks);
                return null;
            }

            for (int wayDataBlock = 0; wayDataBlock < wayDataBlocks; ++wayDataBlock) {
                LatLong[][] wayNodes = processWayDataBlock(tileLatitude, tileLongitude, featureWayDoubleDeltaEncoding, readBuffer);
                if (wayNodes != null) {
                    if (filterRequired && wayFilterEnabled && !wayFilterBbox.intersectsArea(wayNodes)) {
                        continue;
                    }
                    if (Selector.ALL == selector || featureName || featureHouseNumber || featureRef || wayAsLabelTagFilter(tags)) {
                        LatLong labelLatLong = null;
                        if (labelPosition != null) {
                            labelLatLong = new LatLong(wayNodes[0][0].latitude + LatLongUtils.microdegreesToDegrees(labelPosition[1]),
                                    wayNodes[0][0].longitude + LatLongUtils.microdegreesToDegrees(labelPosition[0]));
                        }
                        ways.add(new Way(layer, tags, wayNodes, labelLatLong));
                    }
                }
            }
        }

        return ways;
    }

    /**
     * Reads only labels for tile.
     *
     * @param tile tile for which data is requested.
     * @return label data for the tile.
     */
    @Override
    public MapReadResult readLabels(Tile tile) {
        return readMapData(tile, tile, Selector.LABELS);
    }

    /**
     * Reads data for an area defined by the tile in the upper left and the tile in
     * the lower right corner.
     * Precondition: upperLeft.tileX <= lowerRight.tileX && upperLeft.tileY <= lowerRight.tileY
     *
     * @param upperLeft  tile that defines the upper left corner of the requested area.
     * @param lowerRight tile that defines the lower right corner of the requested area.
     * @return map data for the tile.
     */
    @Override
    public MapReadResult readLabels(Tile upperLeft, Tile lowerRight) {
        return readMapData(upperLeft, lowerRight, Selector.LABELS);
    }

    /**
     * Reads all map data for the area covered by the given tile at the tile zoom level.
     *
     * @param tile defines area and zoom level of read map data.
     * @return the read map data.
     */
    @Override
    public MapReadResult readMapData(Tile tile) {
        return readMapData(tile, tile, Selector.ALL);
    }

    /**
     * Reads data for an area defined by the tile in the upper left and the tile in
     * the lower right corner.
     * Precondition: upperLeft.tileX <= lowerRight.tileX && upperLeft.tileY <= lowerRight.tileY
     *
     * @param upperLeft  tile that defines the upper left corner of the requested area.
     * @param lowerRight tile that defines the lower right corner of the requested area.
     * @return map data for the tile.
     */
    @Override
    public MapReadResult readMapData(Tile upperLeft, Tile lowerRight) {
        return readMapData(upperLeft, lowerRight, Selector.ALL);
    }

    private MapReadResult readMapData(Tile upperLeft, Tile lowerRight, Selector selector) {
        if (upperLeft.tileX > lowerRight.tileX || upperLeft.tileY > lowerRight.tileY) {
            new IllegalArgumentException("upperLeft tile must be above and left of lowerRight tile");
        }

        try {
            QueryParameters queryParameters = new QueryParameters();
            queryParameters.queryZoomLevel = this.mapFileHeader.getQueryZoomLevel(upperLeft.zoomLevel);

            // get and check the sub-file for the query zoom level
            SubFileParameter subFileParameter = this.mapFileHeader.getSubFileParameter(queryParameters.queryZoomLevel);
            if (subFileParameter == null) {
                LOGGER.warning("no sub-file for zoom level: " + queryParameters.queryZoomLevel);
                return null;
            }

            queryParameters.calculateBaseTiles(upperLeft, lowerRight, subFileParameter);
            queryParameters.calculateBlocks(subFileParameter);

            // we enlarge the bounding box for the tile slightly in order to retain any data that
            // lies right on the border, some of this data needs to be drawn as the graphics will
            // overlap onto this tile.
            return processBlocks(queryParameters, subFileParameter, Tile.getBoundingBox(upperLeft, lowerRight), selector);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
            return null;
        }
    }

    private int[] readOptionalLabelPosition(ReadBuffer readBuffer) {
        int[] labelPosition = new int[2];

        // get the label position latitude offset (VBE-S)
        labelPosition[1] = readBuffer.readSignedInt();

        // get the label position longitude offset (VBE-S)
        labelPosition[0] = readBuffer.readSignedInt();

        return labelPosition;
    }

    private int readOptionalWayDataBlocksByte(boolean featureWayDataBlocksByte, ReadBuffer readBuffer) {
        if (featureWayDataBlocksByte) {
            // get and check the number of way data blocks (VBE-U)
            return readBuffer.readUnsignedInt();
        }
        // only one way data block exists
        return 1;
    }

    /**
     * Reads only POI data for tile.
     *
     * @param tile tile for which data is requested.
     * @return POI data for the tile.
     */
    @Override
    public MapReadResult readPoiData(Tile tile) {
        return readMapData(tile, tile, Selector.POIS);
    }

    /**
     * Reads POI data for an area defined by the tile in the upper left and the tile in
     * the lower right corner.
     * This implementation takes the data storage of a MapFile into account for greater efficiency.
     *
     * @param upperLeft  tile that defines the upper left corner of the requested area.
     * @param lowerRight tile that defines the lower right corner of the requested area.
     * @return map data for the tile.
     */
    @Override
    public MapReadResult readPoiData(Tile upperLeft, Tile lowerRight) {
        return readMapData(upperLeft, lowerRight, Selector.POIS);
    }

    private int[][] readZoomTable(SubFileParameter subFileParameter, ReadBuffer readBuffer) {
        int rows = subFileParameter.zoomLevelMax - subFileParameter.zoomLevelMin + 1;
        int[][] zoomTable = new int[rows][2];

        int cumulatedNumberOfPois = 0;
        int cumulatedNumberOfWays = 0;

        for (int row = 0; row < rows; ++row) {
            cumulatedNumberOfPois += readBuffer.readUnsignedInt();
            cumulatedNumberOfWays += readBuffer.readUnsignedInt();

            zoomTable[row][0] = cumulatedNumberOfPois;
            zoomTable[row][1] = cumulatedNumberOfWays;
        }

        return zoomTable;
    }

    /**
     * Restricts returns of data to zoom level range specified. This can be used to restrict
     * the use of this map data base when used in MultiMapDatabase settings.
     *
     * @param minZoom minimum zoom level supported
     * @param maxZoom maximum zoom level supported
     */
    public void restrictToZoomRange(byte minZoom, byte maxZoom) {
        this.zoomLevelMax = maxZoom;
        this.zoomLevelMin = minZoom;
    }

    @Override
    public LatLong startPosition() {
        if (null != getMapFileInfo().startPosition) {
            return getMapFileInfo().startPosition;
        }
        return getMapFileInfo().boundingBox.getCenterPoint();
    }

    @Override
    public Byte startZoomLevel() {
        if (null != getMapFileInfo().startZoomLevel) {
            return getMapFileInfo().startZoomLevel;
        }
        return DEFAULT_START_ZOOM_LEVEL;
    }

    @Override
    public boolean supportsTile(Tile tile) {
        return tile.getBoundingBox().intersects(getMapFileInfo().boundingBox)
                && (tile.zoomLevel >= this.zoomLevelMin && tile.zoomLevel <= this.zoomLevelMax);
    }

    /**
     * The Selector enum is used to specify which data subset is to be retrieved from a MapFile:
     * ALL: all data (as in version 0.6.0)
     * POIS: only poi data, no ways (new after 0.6.0)
     * LABELS: poi data and ways that have a name (new after 0.6.0)
     */
    private enum Selector {
        ALL, POIS, LABELS
    }
}
