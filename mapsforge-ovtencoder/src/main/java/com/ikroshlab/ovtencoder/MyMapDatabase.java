/*
 * Copyright 2010, 2011, 2012 mapsforge.org
 * Copyright 2013, 2014 Hannes Janetzek
 * Copyright 2014-2015 Ludwig M Brinckmann
 * Copyright 2016-2018 devemux86
 * Copyright 2016 Andrey Novikov
 * Copyright 2017-2018 Gustl22
 * Copyright 2018 Bezzu
 * Copyright 2018 ikroshlab
 *
 * This file is part of the OpenScienceMap project (http://www.opensciencemap.org).
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, eithcom.ikroshlab.ovtencoderer version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.ikroshlab.ovtencoder;



import static com.ikroshlab.ovtencoder.DeCoder.coder;

import org.oscim.backend.CanvasAdapter;
import org.oscim.core.BoundingBox;
import org.oscim.core.GeoPoint;
//import org.oscim.core.GeometryBuffer.GeometryType;
import com.ikroshlab.ovtencoder.GeometryBuffer.GeometryType;
//import org.oscim.core.MapElement;
import org.oscim.core.MercatorProjection;
import org.oscim.core.Tag;
import org.oscim.core.Tile;
import org.oscim.layers.tile.MapTile;
import org.oscim.layers.tile.buildings.BuildingLayer;
import org.oscim.tiling.ITileCache;
import org.oscim.tiling.ITileDataSink;
import org.oscim.tiling.ITileDataSource;
import org.oscim.tiling.TileSource;
import org.oscim.utils.Parameters;
//import org.oscim.utils.geom.TileClipper;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.ikroshlab.ovtencoder.GeometryBuffer.GeometryType.LINE;
import static com.ikroshlab.ovtencoder.GeometryBuffer.GeometryType.POLY;
import com.ikroshlab.ovtencoder.header.MapFileInfo;
import com.ikroshlab.ovtencoder.header.SubFileParameter;
import static org.oscim.tiling.QueryResult.FAILED;
import static org.oscim.tiling.QueryResult.SUCCESS;


/**
 * A class for reading binary map files.
 *
 * @see <a href="http://code.google.com/p/mapsforge/wiki/SpecificationBinaryMapFile">Specification</a>
 */
public class MyMapDatabase implements ITileDataSource {
    /**
     * Bitmask to extract the block offset from an index entry.
     */
    private static final long BITMASK_INDEX_OFFSET = 0x7FFFFFFFFFL;

    /**
     * Bitmask to extract the water information from an index entry.
     */
    private static final long BITMASK_INDEX_WATER = 0x8000000000L;

    /**
     * Debug message prefix for the block signature.
     */
    private static final String DEBUG_SIGNATURE_BLOCK = "block signature: ";

    /** Debug message prefix for the POI signature. */
    // private static final String DEBUG_SIGNATURE_POI = "POI signature: ";

    /**
     * Debug message prefix for the way signature.
     */
    private static final String DEBUG_SIGNATURE_WAY = "way signature: ";

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
     * For most use cases the standard settings should be sufficient
     */

    private boolean                 mUseCache                      = true;
    private int                     tX;
    private int                     tY;
    private int                     tZ;

    private ArrayList<MapElement>   ewaters;
    private ArrayList<MapElement>   epois;
    private ArrayList<MapElement>   eways;
    
    private long tags_total_bytes   = 0L;
    private long points_total_bytes = 0L;
    private long index_total_bytes  = 0L;




    public static boolean           wayFilterEnabled  = true;
    public static int               wayFilterDistance = 20;

    private long                    mFileSize;
    private boolean                 mDebugFile;
    private RandomAccessFile        mInputFile;
    private ReadBuffer              mReadBuffer;
    private String                  mSignatureBlock;
    private String                  mSignaturePoi;
    private String                  mSignatureWay;
    private int                     mTileLatitude;
    private int                     mTileLongitude;
    private int[]                   mIntBuffer;

    //private final MapElement        mElem = new MapElement();

    private int minDeltaLat, minDeltaLon;

    private final TileProjection    mTileProjection;
    private final TileClipper       mTileClipper;
    private final TileSeparator     mTileSeparator;

    private final MapFileTileSource mTileSource;

    private int                     zoomLevelMin = 0;
    private int                     zoomLevelMax = Byte.MAX_VALUE;



    public MyMapDatabase(MapFileTileSource tileSource) throws IOException {
        mTileSource = tileSource;
        try {
            /* open the file in read only mode */
            mInputFile  = new RandomAccessFile(tileSource.mapFile, "r");
            mFileSize   = mInputFile.length();
            mReadBuffer = new ReadBuffer(mInputFile);

        } catch (IOException e) {
            logDebug(e.getMessage());
            /* make sure that the file is closed */
            dispose();
            throw new IOException();
        }

        mTileProjection = new TileProjection();
        mTileClipper    = new TileClipper(0, 0, 0, 0);
        mTileSeparator  = new TileSeparator(0, 0, 0, 0);                       
    }


    public MapFileTileSource getTileSource() {
        return mTileSource;
    }


   
    @Override
    public void query(MapTile tile, ITileDataSink sink) {       

               
        if (mTileSource.fileHeader == null) {
            logDebug(">>>>>>         -- Bad file !");
            return;
        }

        if (mIntBuffer == null) mIntBuffer = new int[Short.MAX_VALUE * 2];

       
        mTileProjection.setTile(tile);
        //mTile = tile;

        /* size of tile in map coordinates; */
        double size = 1.0 / (1 << tile.zoomLevel);

        /* simplification tolerance */
        int pixel = (tile.zoomLevel > 11) ? 1 : 2;

        int simplify = Tile.SIZE / pixel;

        /* translate screen pixel for tile to latitude and longitude
         * tolerance for point reduction before projection. */
        minDeltaLat = (int) (Math.abs(MercatorProjection.toLatitude(tile.y + size)
                                    - MercatorProjection.toLatitude(tile.y)) * 1e6) / simplify;
        minDeltaLon = (int) (Math.abs(MercatorProjection.toLongitude(tile.x + size)
                                    - MercatorProjection.toLongitude(tile.x)) * 1e6) / simplify;

        QueryParameters queryParameters = new QueryParameters();
        queryParameters.queryZoomLevel  = mTileSource.fileHeader.getQueryZoomLevel(tile.zoomLevel);

        /* get and check the sub-file for the query zoom level */
        SubFileParameter subFileParameter = mTileSource.fileHeader.getSubFileParameter(queryParameters.queryZoomLevel);

        if (subFileParameter == null) {
            logDebug("no sub-file for zoom level: "   + queryParameters.queryZoomLevel);

            return;
        }

        QueryCalculations.calculateBaseTiles(queryParameters, tile, subFileParameter);
        QueryCalculations.calculateBlocks(queryParameters, subFileParameter);

        tags_total_bytes   = 0L;
        points_total_bytes = 0L;
        index_total_bytes  = 0L;
        ewaters = new ArrayList<>();
        epois   = new ArrayList<>();
        eways   = new ArrayList<>();
        tX      = tile.tileX;
        tY      = tile.tileY;
        tZ      = tile.zoomLevel;
        
        
        // 1. create tile elements
        try {
            processBlocks(sink, queryParameters, subFileParameter);
        } catch(Exception e) {
                logDebug(">>>>>> TILE -->  " + tX + "/" + tY + "/" + tZ + " - error: " + e.getMessage());
        }
             
        
        // 2. encode tile elements into byte array:
        byte[] rawTile = encodeTile();  // todo - added - calling tile ENCODER
        
        //logDebug(">>>>>> WATERS -->   " + tX + "/" + tY + "/" + tZ + "     size: " + ewaters.size());        
        //logDebug(">>>>>>    POI -->   " + tX + "/" + tY + "/" + tZ + "     size: " + epois.size());        
        //logDebug(">>>>>>   WAYS -->   " + tX + "/" + tY + "/" + tZ + "     size: " + eways.size());
        //logDebug(">>>>>>   tags -->   " + tX + "/" + tY + "/" + tZ + "    bytes: " + tags_total_bytes);
        //logDebug(">>>>>> points -->   " + tX + "/" + tY + "/" + tZ + "    bytes: " + points_total_bytes);
        //logDebug(">>>>>>  index -->   " + tX + "/" + tY + "/" + tZ + "    bytes: " + index_total_bytes);
        //logDebug(">>>>>>  TOTAL -->   " + tX + "/" + tY + "/" + tZ + "    bytes: " + rawTile.length); 
        
        // 3. store this tile into DB:         
        if (rawTile.length>0) {
            MapFileInfo fileInfo = mTileSource.fileHeader.getMapFileInfo();            
            long        tm       = fileInfo.mapDate;

            try {  // your own DAO code to store the tile data as raw byte array wherever you like.....
                
               // DAO.insertTile((byte)tZ, tX, tY, rawTile, ...........);               

            } catch(SQLException se) {
                logDebug(">>>>>> TILE -->  " + tX + "/" + tY + "/" + tZ + " - SQL error: " + se.getMessage());
            } catch(Exception ee) {
                logDebug(">>>>>> TILE -->  " + tX + "/" + tY + "/" + tZ + " - error: " + ee.getMessage());
            }
        
        }                

        ewaters = null;
        epois   = null;
        eways   = null;
    }



    @Override
    public void dispose() {
        mReadBuffer = null;
        if (mInputFile != null) {

            try {
                mInputFile.close();
                mInputFile = null;
            } catch (IOException e) {
                logDebug(e.getMessage());
            }
        }
    }

    @Override
    public void cancel() {
    }

    /**
     * Logs the debug signatures of the current way and block.
     */
    private void logDebugSignatures() {
        if (mDebugFile) {
            logDebug(DEBUG_SIGNATURE_WAY + mSignatureWay);
            logDebug(DEBUG_SIGNATURE_BLOCK + mSignatureBlock);
        }
    }



    private void setTileClipping(QueryParameters queryParameters, SubFileParameter subFileParameter, long currentRow, long currentCol) {

        long numRows = queryParameters.toBlockY - queryParameters.fromBlockY;
        long numCols = queryParameters.toBlockX - queryParameters.fromBlockX;

        //logDebugdebug(numCols + "/" + numRows + " " + currentCol + " " + currentRow);

        // At large query zoom levels use enlarged buffer
        int buffer;
        if (queryParameters.queryZoomLevel > 17)    buffer = Tile.SIZE / 2;
        else                                        buffer = (int) (16 * CanvasAdapter.getScale() + 0.5f);

        int xmin = -buffer;
        int ymin = -buffer;
        int xmax = Tile.SIZE + buffer;
        int ymax = Tile.SIZE + buffer;

        int xSmin = 0;
        int ySmin = 0;
        int xSmax = Tile.SIZE;
        int ySmax = Tile.SIZE;

        if (numRows > 0) {
            /* If blocks are at a border, sometimes too less blocks are requested,
             * so the divisor for tile dimensions is increased to base tile subdivision.
             */
            boolean isTopBorder  = queryParameters.fromBaseTileY < subFileParameter.boundaryTileTop;
            boolean isLeftBorder = queryParameters.fromBaseTileX < subFileParameter.boundaryTileLeft;
            long    numSubX      = queryParameters.toBaseTileX - queryParameters.fromBaseTileX;
            long    numSubY      = queryParameters.toBaseTileY - queryParameters.fromBaseTileY;
            long    numDifX      = numSubX - numCols; // 0 except at map borders
            long    numDifY      = numSubY - numRows; // 0 except at map borders

            int w = (int) (Tile.SIZE / (numSubX + 1));
            int h = (int) (Tile.SIZE / (numSubY + 1));

            if (currentCol > 0)        xSmin = xmin = (int) ((currentCol + (isLeftBorder ? numDifX : 0)) * w);
            if (currentCol < numCols)  xSmax = xmax = (int) ((currentCol + (isLeftBorder ? numDifX : 0)) * w + w);
            if (currentRow > 0)        ySmin = ymin = (int) ((currentRow + (isTopBorder  ? numDifY : 0)) * h);
            if (currentRow < numRows)  ySmax = ymax = (int) ((currentRow + (isTopBorder  ? numDifY : 0)) * h + h);
        }
        mTileClipper.setRect(xmin, ymin, xmax, ymax);
        mTileSeparator.setRect(xSmin, ySmin, xSmax, ySmax);
    }

    //private final static Tag mWaterTag = new Tag("natural", "water");





    /**
     * Map rendering.
     */
    private void processBlocks(ITileDataSink mapDataSink, QueryParameters queryParams,
                               SubFileParameter subFileParameter) throws IOException {
        processBlocks(mapDataSink, queryParams, subFileParameter, null, null, null);
    }

    /**
     * Map data reading.
     */
    private void processBlocks(QueryParameters queryParams,
                               SubFileParameter subFileParameter, BoundingBox boundingBox,
                               Selector selector, MapReadResult mapReadResult) throws IOException {
        processBlocks(null, queryParams, subFileParameter, boundingBox, selector, mapReadResult);
    }



    private void processBlocks(ITileDataSink mapDataSink, QueryParameters queryParams,
                               SubFileParameter subFileParameter, BoundingBox boundingBox,
                               Selector selector, MapReadResult mapReadResult) throws IOException {

        /* read and process all blocks from top to bottom and from left to right */
        for (long row = queryParams.fromBlockY; row <= queryParams.toBlockY; row++) {
            for (long column = queryParams.fromBlockX; column <= queryParams.toBlockX; column++) {

                setTileClipping(queryParams, subFileParameter,
                        row    - queryParams.fromBlockY,
                         column - queryParams.fromBlockX);

                // calculate the actual block number of the needed block in the file
                long blockNumber = row * subFileParameter.blocksWidth + column;

                /* get the current index entry */
                long blockIndexEntry = mTileSource.databaseIndexCache.getIndexEntry(subFileParameter, blockNumber);

                /* check the water flag of the block in its index entry */
                if ((blockIndexEntry & BITMASK_INDEX_WATER) != 0) {
                    // Deprecate water tiles rendering
                    /*MapElement e = mElem;
                    e.clear();
                    e.tags.clear();
                    e.tags.add(mWaterTag);
                    e.startPolygon();
                    e.addPoint(xmin, ymin);
                    e.addPoint(xmax, ymin);
                    e.addPoint(xmax, ymax);
                    e.addPoint(xmin, ymax);
                    mapDataSink.process(e);*/
                }

                /* get and check the current block pointer */
                long blockPointer = blockIndexEntry & BITMASK_INDEX_OFFSET;
                if (blockPointer < 1 || blockPointer > subFileParameter.subFileSize) {
                    logDebug("invalid current block pointer: " + blockPointer);
                    logDebug("subFileSize: " + subFileParameter.subFileSize);
                    return;
                }

                long nextBlockPointer;
                /* check if the current block is the last block in the file */
                if (blockNumber + 1 == subFileParameter.numberOfBlocks) {
                    /* set the next block pointer to the end of the file */
                    nextBlockPointer = subFileParameter.subFileSize;
                } else {
                    /* get and check the next block pointer */
                    nextBlockPointer = mTileSource.databaseIndexCache.getIndexEntry(subFileParameter,blockNumber + 1);
                    nextBlockPointer &= BITMASK_INDEX_OFFSET;

                    if (nextBlockPointer < 1 || nextBlockPointer > subFileParameter.subFileSize) {
                        logDebug("invalid next block pointer: " + nextBlockPointer);
                        logDebug("sub-file size: " + subFileParameter.subFileSize);
                        return;
                    }
                }

                /* calculate the size of the current block */
                int blockSize = (int) (nextBlockPointer - blockPointer);
                if (blockSize < 0) {
                    logDebug("current block size must not be negative: " + blockSize);
                    return;
                } else if (blockSize == 0) {
                    /* the current block is empty, continue with the next block */
                    continue;
                } else if (blockSize > Parameters.MAXIMUM_BUFFER_SIZE) {
                    /* the current block is too large, continue with the next
                     * block */
                    logDebug("current block size too large: " + blockSize);
                    continue;
                } else if (blockPointer + blockSize > mFileSize) {
                    logDebug("current block larger than file size: " + blockSize);
                    return;
                }

                /* seek to the current block in the map file */
                mInputFile.seek(subFileParameter.startAddress + blockPointer);

                /* read the current block into the buffer */
                if (!mReadBuffer.readFromFile(blockSize)) {
                    /* skip the current block */
                    logDebug("reading current block has failed: " + blockSize);
                    return;
                }

                /* calculate the top-left coordinates of the underlying tile */
                double tileLatitudeDeg  = Projection.tileYToLatitude(subFileParameter.boundaryTileTop   + row,    subFileParameter.baseZoomLevel);
                double tileLongitudeDeg = Projection.tileXToLongitude(subFileParameter.boundaryTileLeft + column, subFileParameter.baseZoomLevel);

                mTileLatitude  = (int) (tileLatitudeDeg  * 1E6);
                mTileLongitude = (int) (tileLongitudeDeg * 1E6);

                processBlock(queryParams, subFileParameter, mapDataSink, boundingBox, selector, mapReadResult);
            }
        }
    }




    /**
     * Processes a single block and executes the callback functions on all map elements.
     *
     * @param queryParameters  the parameters of the current query.
     * @param subFileParameter the parameters of the current map file.
     * @param mapDataSink      the callback which handles the extracted map elements.
     */
    private void processBlock(QueryParameters queryParameters,
                              SubFileParameter subFileParameter, ITileDataSink mapDataSink,
                              BoundingBox boundingBox, Selector selector,
                              MapReadResult mapReadResult) {

        if (!processBlockSignature())  return;

        int[][] zoomTable = readZoomTable(subFileParameter);
        if (zoomTable == null) return;

        int zoomTableRow         = queryParameters.queryZoomLevel - subFileParameter.zoomLevelMin;
        int poisOnQueryZoomLevel = zoomTable[zoomTableRow][0];
        int waysOnQueryZoomLevel = zoomTable[zoomTableRow][1];

        /* get the relative offset to the first stored way in the block */
        int firstWayOffset = mReadBuffer.readUnsignedInt();
        if (firstWayOffset < 0) {
            logDebug(INVALID_FIRST_WAY_OFFSET + firstWayOffset);
            if (mDebugFile) {
                logDebug(DEBUG_SIGNATURE_BLOCK + mSignatureBlock);
            }
            return;
        }

        /* add the current buffer position to the relative first way offset */
        firstWayOffset += mReadBuffer.getBufferPosition();
        if (firstWayOffset > mReadBuffer.getBufferSize()) {
            logDebug(INVALID_FIRST_WAY_OFFSET + firstWayOffset);
            if (mDebugFile) {
                logDebug(DEBUG_SIGNATURE_BLOCK + mSignatureBlock);
            }
            return;
        }

        boolean filterRequired = queryParameters.queryZoomLevel > subFileParameter.baseZoomLevel;

        List<PointOfInterest> pois = null;
        if (mapReadResult != null)  pois = new ArrayList<>();

        if (!processPOIs(mapDataSink, poisOnQueryZoomLevel, boundingBox, filterRequired, pois)) {
            return;
        }

        /* finished reading POIs, check if the current buffer position is valid */
        if (mReadBuffer.getBufferPosition() > firstWayOffset) {
            logDebug("invalid buffer position: " + mReadBuffer.getBufferPosition());
            if (mDebugFile) {
                logDebug(DEBUG_SIGNATURE_BLOCK + mSignatureBlock);
            }
            return;
        }

        /* move the pointer to the first way */
        mReadBuffer.setBufferPosition(firstWayOffset);



        List<Way> ways = null;
        if (mapReadResult != null && Selector.POIS != selector)  ways = new ArrayList<>();

        if (!processWays(queryParameters, mapDataSink, waysOnQueryZoomLevel, boundingBox, filterRequired, selector, ways)) {
            return;
        }

        if (mapReadResult != null) {
            if (Selector.POIS == selector)  ways = Collections.emptyList();

            mapReadResult.add(new PoiWayBundle(pois, ways));
        }
    }







    /**
     * Processes the block signature, if present.
     *
     * @return true if the block signature could be processed successfully,
     * false otherwise.
     */
    private boolean processBlockSignature() {
        if (mDebugFile) {
            /* get and check the block signature */
            mSignatureBlock = mReadBuffer.readUTF8EncodedString(SIGNATURE_LENGTH_BLOCK);
            if (!mSignatureBlock.startsWith("###TileStart")) {
                logDebug("invalid block signature: " + mSignatureBlock);
                return false;
            }
        }
        return true;
    }







    /**
     * Processes the given number of POIs.
     *
     * @param mapDataSink  the callback which handles the extracted POIs.
     * @param numberOfPois how many POIs should be processed.
     * @return             true if the POIs could be processed successfully, false otherwise.
     */
    private boolean processPOIs(ITileDataSink mapDataSink, int numberOfPois, BoundingBox boundingBox,
                                boolean filterRequired, List<PointOfInterest> pois) {

        Tag[] poiTags = mTileSource.fileInfo.poiTags;
        //MapElement e = mElem;

        for (int elementCounter = numberOfPois; elementCounter != 0; --elementCounter) {
            MapElement e = new MapElement();

            /* reset to common tag position */
            //e.tags.clear();

            if (mDebugFile) {
                /* get and check the POI signature */
                mSignaturePoi = mReadBuffer.readUTF8EncodedString(SIGNATURE_LENGTH_POI);
                if (!mSignaturePoi.startsWith("***POIStart")) {
                    logDebug("invalid POI signature: " + mSignaturePoi);
                    logDebug(DEBUG_SIGNATURE_BLOCK + mSignatureBlock);
                    return false;
                }
            }

            /* get the POI latitude offset (VBE-S) */
            int latitude = mTileLatitude + mReadBuffer.readSignedInt();
            /* get the POI longitude offset (VBE-S) */
            int longitude = mTileLongitude + mReadBuffer.readSignedInt();

            /* get the special byte which encodes multiple flags */
            byte specialByte = mReadBuffer.readByte();

            /* bit 1-4 represent the layer */
            byte layer = (byte) ((specialByte & POI_LAYER_BITMASK) >>> POI_LAYER_SHIFT);

            /* bit 5-8 represent the number of tag IDs */
            byte numberOfTags = (byte) (specialByte & POI_NUMBER_OF_TAGS_BITMASK);

            if (numberOfTags != 0) {
                if (!mReadBuffer.readTags(e.tags, poiTags, numberOfTags))  return false;
            }

            /* get the feature bitmask (1 byte) */
            byte featureByte = mReadBuffer.readByte();

            /* bit 1-3 enable optional features
             * check if the POI has a name */
            if ((featureByte & POI_FEATURE_NAME) != 0) {
                String str = mTileSource.extractLocalized(mReadBuffer.readUTF8EncodedString());
                e.tags.add(new Tag(Tag.KEY_NAME, str, false));
            }

            /* check if the POI has a house number */
            if ((featureByte & POI_FEATURE_HOUSE_NUMBER) != 0) {
                String str = mReadBuffer.readUTF8EncodedString();
                e.tags.add(new Tag(Tag.KEY_HOUSE_NUMBER, str, false));
            }

            /* check if the POI has an elevation */
            if ((featureByte & POI_FEATURE_ELEVATION) != 0) {
                String str = Integer.toString(mReadBuffer.readSignedInt());
                e.tags.add(new Tag(Tag.KEY_ELE, str, false));
            }
            mTileProjection.projectPoint(latitude, longitude, e);

            e.setLayer(layer);

            List<Tag> tags = new ArrayList<>();
            if (pois != null) {
                for (int i = 0; i < e.tags.size(); i++) {
                    tags.add(e.tags.get(i));
                }

                GeoPoint position = new GeoPoint(latitude, longitude);
                // depending on the zoom level configuration the poi can lie outside
                // the tile requested, we filter them out here
                if (!filterRequired || boundingBox.contains(position)) {
                    pois.add(new PointOfInterest(layer, tags, position));  // todo - add to epois???
                }
            }


            // filter tags
            List<Tag> tset = new ArrayList<>();
            for (Tag t : e.tags.asArray()) {
                if (t != null) tset.add(t);
            }
            Tag[] tarr = new Tag[tset.size()];
            for (int i=0; i<tset.size(); i++)
                tarr[i] = tset.get(i);

            e.tags.set(tarr);
            //e.tags.numTags = tarr.length;

            epois.add(e);  // add this POI to main array

            //if (mapDataSink != null) mapDataSink.process(e);  todo - to remove
        }
        return true;
    }














    private int stringOffset = -1;

    /**
     * Processes the given number of ways.
     *
     * @param queryParameters the parameters of the current query.
     * @param mapDataSink     the callback which handles the extracted ways.
     * @param numberOfWays    how many ways should be processed.
     * @return                true if the ways could be processed successfully, false
     * otherwise.
     */
    private boolean processWays(QueryParameters queryParameters, ITileDataSink mapDataSink,
                                int numberOfWays, BoundingBox boundingBox, boolean filterRequired,
                                Selector selector, List<Way> ways) {

        Tag[] wayTags = mTileSource.fileInfo.wayTags;
        //MapElement e = mElem;

        int wayDataBlocks;

        // skip string block
        int stringsSize = 0;
        stringOffset    = 0;

        if (mTileSource.experimental) {
            stringsSize = mReadBuffer.readUnsignedInt();
            stringOffset = mReadBuffer.getBufferPosition();
            mReadBuffer.skipBytes(stringsSize);
        }

        //setTileClipping(queryParameters);

        for (int elementCounter = numberOfWays; elementCounter != 0; --elementCounter) {

            MapElement e = new MapElement();
            /* reset to common tag position */
            //e.tags.clear();

            if (mDebugFile) {
                // get and check the way signature
                mSignatureWay = mReadBuffer.readUTF8EncodedString(SIGNATURE_LENGTH_WAY);
                if (!mSignatureWay.startsWith("---WayStart")) {
                    logDebug("invalid way signature: " + mSignatureWay);
                    logDebug(DEBUG_SIGNATURE_BLOCK + mSignatureBlock);
                    return false;
                }
            }

            if (queryParameters.useTileBitmask) {

                elementCounter = mReadBuffer.skipWays(queryParameters.queryTileBitmask, elementCounter);

                if (elementCounter == 0) return true;
                if (elementCounter < 0)  return false;

                if (mTileSource.experimental && mReadBuffer.lastTagPosition > 0) {
                    int pos = mReadBuffer.getBufferPosition();
                    mReadBuffer.setBufferPosition(mReadBuffer.lastTagPosition);

                    byte numberOfTags = (byte) (mReadBuffer.readByte() & WAY_NUMBER_OF_TAGS_BITMASK);
                    if (!mReadBuffer.readTags(e.tags, wayTags, numberOfTags))   return false;

                    mReadBuffer.setBufferPosition(pos);
                }
            } else {
                int wayDataSize = mReadBuffer.readUnsignedInt();
                if (wayDataSize < 0) {
                    logDebug("invalid way data size: " + wayDataSize);
                    if (mDebugFile) {
                        logDebug(DEBUG_SIGNATURE_BLOCK + mSignatureBlock);
                    }
                    logDebug("BUG way 2");
                    return false;
                }

                /* ignore the way tile bitmask (2 bytes) */
                mReadBuffer.skipBytes(2);
            }

            /* get the special byte which encodes multiple flags */
            byte specialByte = mReadBuffer.readByte();

            /* bit 1-4 represent the layer */
            byte layer = (byte) ((specialByte & WAY_LAYER_BITMASK) >>> WAY_LAYER_SHIFT);
            /* bit 5-8 represent the number of tag IDs */
            byte numberOfTags = (byte) (specialByte & WAY_NUMBER_OF_TAGS_BITMASK);

            if (numberOfTags != 0) {
                if (!mReadBuffer.readTags(e.tags, wayTags, numberOfTags))  return false;
            }

            /* get the feature bitmask (1 byte) */
            byte featureByte = mReadBuffer.readByte();

            /* bit 1-6 enable optional features */
            boolean featureWayDoubleDeltaEncoding = (featureByte & WAY_FEATURE_DOUBLE_DELTA_ENCODING) != 0;

            boolean hasName    = (featureByte & WAY_FEATURE_NAME)         != 0;
            boolean hasHouseNr = (featureByte & WAY_FEATURE_HOUSE_NUMBER) != 0;
            boolean hasRef     = (featureByte & WAY_FEATURE_REF)          != 0;

            if (mTileSource.experimental) {
                if (hasName) {
                    int textPos = mReadBuffer.readUnsignedInt();
                    String str = mTileSource.extractLocalized(mReadBuffer.readUTF8EncodedStringAt(stringOffset + textPos));
                    e.tags.add(new Tag(Tag.KEY_NAME, str, false));
                }
                if (hasHouseNr) {
                    int textPos = mReadBuffer.readUnsignedInt();
                    String str = mReadBuffer.readUTF8EncodedStringAt(stringOffset + textPos);
                    e.tags.add(new Tag(Tag.KEY_HOUSE_NUMBER, str, false));
                }
                if (hasRef) {
                    int textPos = mReadBuffer.readUnsignedInt();
                    String str = mReadBuffer.readUTF8EncodedStringAt(stringOffset + textPos);
                    e.tags.add(new Tag(Tag.KEY_REF, str, false));
                }
            } else {
                if (hasName) {
                    String str = mTileSource.extractLocalized(mReadBuffer.readUTF8EncodedString());
                    e.tags.add(new Tag(Tag.KEY_NAME, str, false));
                }
                if (hasHouseNr) {
                    String str = mReadBuffer.readUTF8EncodedString();
                    e.tags.add(new Tag(Tag.KEY_HOUSE_NUMBER, str, false));
                }
                if (hasRef) {
                    String str = mReadBuffer.readUTF8EncodedString();
                    e.tags.add(new Tag(Tag.KEY_REF, str, false));
                }
            }

            int[] labelPosition = null;
            if ((featureByte & WAY_FEATURE_LABEL_POSITION) != 0) {
                labelPosition = readOptionalLabelPosition();
            }

            if ((featureByte & WAY_FEATURE_DATA_BLOCKS_BYTE) != 0) {
                wayDataBlocks = mReadBuffer.readUnsignedInt();

                if (wayDataBlocks < 1) {
                    logDebug("invalid number of way data blocks: " + wayDataBlocks);
                    logDebugSignatures();
                    return false;
                }
            } else {
                wayDataBlocks = 1;
            }

            /* some guessing if feature is a line or a polygon */
            boolean linearFeature = !OSMUtils.isArea(e);


            // todo - filter tags and thus elements to be added to main array:
            boolean toAdd = false;
            if (tZ <= 8) {
                Tag t = e.tags.asArray()[0];   // analyze only the first tag fo these zoom levels

                if      (t.key.equals("area")     &&  t.value.equals("yes"))              toAdd = true;
                else if (t.key.equals("highway")  && (
                        t.value.equals("primary")
                                ||  t.value.equals("motorway")
                                ||  t.value.equals("trunk")
                ))                                   toAdd = true;
                else if (t.key.equals("natural")  && (
                        t.value.equals("wood")
                        // ||  t.value.equals("water")
                ))                                   toAdd = true;
            }

            else if (tZ > 8 && tZ < 11) {
                Tag t = e.tags.asArray()[0];   // analyze only the first tag fo these zoom levels
                if      (t.key.equals("area")     &&  t.value.equals("yes"))              toAdd = true;
                else if (t.key.equals("highway")  && (
                        t.value.equals("primary")
                                ||  t.value.equals("motorway")
                                ||  t.value.equals("trunk")
                ))                                   toAdd = true;
                else if (t.key.equals("natural")  && (
                        t.value.equals("wood")
                                ||  t.value.equals("water")))           toAdd = true;
                else if (t.key.equals("waterway") && (
                        t.value.equals("riverbank")
                                ||  t.value.equals("river")
                ))                                   toAdd = true;
                //else if (t.key.equals("boundary") &&  t.value.equals("administrative")) toAdd = true;
                //else if (t.key.equals("landuse")  &&  t.value.equals("forest"))         toAdd = true;
            }



            for (int wayDataBlock = 0; wayDataBlock < wayDataBlocks; wayDataBlock++) {
                //e.clear();  // todo - commented

                List<GeoPoint[]> wayNodes = null;
                if (ways != null)  wayNodes = new ArrayList<>();

                if (!processWayDataBlock(e, featureWayDoubleDeltaEncoding, linearFeature, wayNodes))  return false;

                /* drop invalid outer ring */
                if (e.isPoly() && e.index[0] < 6)  continue;

                if (labelPosition != null && wayDataBlock == 0)
                    e.setLabelPosition(e.points[0] + labelPosition[0], e.points[1] + labelPosition[1]);

                mTileProjection.project(e);

                // Avoid clipping for buildings, which slows rendering.
                // But clip everything if buildings are displayed.
                if (   !e.tags.containsKey(Tag.KEY_BUILDING)
                    && !e.tags.containsKey(Tag.KEY_BUILDING_PART)) {
                    if (!mTileClipper.clip(e))   continue;
                }
                else if (queryParameters.queryZoomLevel >= 17) {
                    if (!mTileSeparator.separate(e))  continue;
                }

                e.simplify(1, true);

                e.setLayer(layer);

                List<Tag> tags = new ArrayList<>();
                if (ways != null) {
                    BoundingBox  wayFilterBbox = boundingBox.extendMeters(wayFilterDistance);
                    GeoPoint[][] wayNodesArray = wayNodes.toArray(new GeoPoint[wayNodes.size()][]);

                    if (!filterRequired || !wayFilterEnabled || wayFilterBbox.intersectsArea(wayNodesArray)) {

                        for (int i = 0; i < e.tags.size(); i++)
                            tags.add(e.tags.get(i));

                        if (Selector.ALL == selector || hasName || hasHouseNr || hasRef || wayAsLabelTagFilter(tags)) {
                            GeoPoint labelPos = e.labelPosition != null ? new GeoPoint(e.labelPosition.y / 1E6, e.labelPosition.x / 1E6) : null;
                            ways.add(new Way(layer, tags, wayNodesArray, labelPos, e.type));
                        }
                    }
                }


                // filter tags
                List<Tag> tset = new ArrayList<>();
                for (Tag t : e.tags.asArray()) {
                    if (t != null) tset.add(t);
                }
                Tag[] tarr = new Tag[tset.size()];
                for (int i=0; i<tset.size(); i++)
                    tarr[i] = tset.get(i);

                e.tags.set(tarr);
                //e.tags.numTags = tarr.length;

                if (tZ < 11) {
                    if (toAdd) {
                        eways.add(e);
                    }
                }
                else {
                   eways.add(e);
                }

                //if (mapDataSink != null)  mapDataSink.process(e);
            }
        }

        return true;
    }


    private boolean processWayDataBlock(MapElement e, boolean doubleDeltaEncoding, boolean isLine, List<GeoPoint[]> wayCoordinates) {
        /* get and check the number of way coordinate blocks (VBE-U) */
        int numBlocks = mReadBuffer.readUnsignedInt();
        if (numBlocks < 1 || numBlocks > Short.MAX_VALUE) {
            logDebug("invalid number of way coordinate blocks: " + numBlocks);
            return false;
        }

        int[] wayLengths = e.ensureIndexSize(numBlocks, false);
        if (wayLengths.length > numBlocks)  wayLengths[numBlocks] = -1;

        /* read the way coordinate blocks */
        for (int coordinateBlock = 0; coordinateBlock < numBlocks; ++coordinateBlock) {
            int numWayNodes = mReadBuffer.readUnsignedInt();

            if (numWayNodes < 2 || numWayNodes > Short.MAX_VALUE) {
                logDebug("invalid number of way nodes: " + numWayNodes);
                logDebugSignatures();
                return false;
            }

            /* each way node consists of latitude and longitude */
            int len = numWayNodes * 2;

            wayLengths[coordinateBlock] = decodeWayNodes(doubleDeltaEncoding, e, len, isLine);

            if (wayCoordinates != null) {
                // create the array which will store the current way segment
                GeoPoint[] waySegment = new GeoPoint[e.getNumPoints()];
                for (int i = 0; i < e.getNumPoints(); i++)
                    waySegment[i] = new GeoPoint(e.getPointY(i) / 1E6, e.getPointX(i) / 1E6);

                wayCoordinates.add(waySegment);
            }
        }

        return true;
    }


    private int decodeWayNodes(boolean doubleDelta, MapElement e, int length, boolean isLine) {
        int[] buffer = mIntBuffer;
        mReadBuffer.readSignedInt(buffer, length);

        float[] outBuffer = e.ensurePointSize(e.pointNextPos + length, true);
        int     outPos    = e.pointNextPos;
        int lat, lon;

        /* first node latitude single-delta offset */
        int firstLat = lat = mTileLatitude  + buffer[0];
        int firstLon = lon = mTileLongitude + buffer[1];

        outBuffer[outPos++] = lon;
        outBuffer[outPos++] = lat;
        int cnt = 2;

        int deltaLat = 0;
        int deltaLon = 0;

        for (int pos = 2; pos < length; pos += 2) {
            if (doubleDelta) {
                deltaLat = buffer[pos] + deltaLat;
                deltaLon = buffer[pos + 1] + deltaLon;
            }
            else {
                deltaLat = buffer[pos];
                deltaLon = buffer[pos + 1];
            }
            lat += deltaLat;
            lon += deltaLon;

            if (pos == length - 2) {
                boolean line = isLine || (lon != firstLon || lat != firstLat);

                if (line) {
                    outBuffer[outPos++] = lon;
                    outBuffer[outPos++] = lat;
                    cnt += 2;
                }

                if (e.type == GeometryType.NONE)  e.type = line ? LINE : POLY;

            }
            else /*if ((deltaLon > minDeltaLon || deltaLon < -minDeltaLon
                    || deltaLat > minDeltaLat || deltaLat < -minDeltaLat)
                    || e.tags.contains("natural", "nosea"))*/ {
                // Avoid additional simplification
                // https://github.com/mapsforge/vtm/issues/39
                outBuffer[outPos++] = lon;
                outBuffer[outPos++] = lat;
                cnt += 2;
            }
        }

        e.pointNextPos = outPos;

        return cnt;
    }











    /**
     * Reads only labels for tile.
     *
     * @param tile tile for which data is requested.
     * @return     label data for the tile.
     */
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
     * @return           map data for the tile.
     */
    public MapReadResult readLabels(Tile upperLeft, Tile lowerRight) {
        return readMapData(upperLeft, lowerRight, Selector.LABELS);
    }

    /**
     * Reads all map data for the area covered by the given tile at the tile zoom level.
     *
     * @param tile defines area and zoom level of read map data.
     * @return     the read map data.
     */
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
     * @return           map data for the tile.
     */
    public MapReadResult readMapData(Tile upperLeft, Tile lowerRight) {
        return readMapData(upperLeft, lowerRight, Selector.ALL);
    }




    private MapReadResult readMapData(Tile upperLeft, Tile lowerRight, Selector selector) {
        if (mTileSource.fileHeader == null)  return null;

        MapReadResult mapReadResult = new MapReadResult();

        if (mIntBuffer == null)   mIntBuffer = new int[Short.MAX_VALUE * 2];

        try {
            mTileProjection.setTile(upperLeft);

            QueryParameters queryParameters = new QueryParameters();
            queryParameters.queryZoomLevel  = mTileSource.fileHeader.getQueryZoomLevel(upperLeft.zoomLevel);

            /* get and check the sub-file for the query zoom level */
            SubFileParameter subFileParameter = mTileSource.fileHeader.getSubFileParameter(queryParameters.queryZoomLevel);

            if (subFileParameter == null) {
                logDebug("no sub-file for zoom level: " + queryParameters.queryZoomLevel);

                return null;
            }

            QueryCalculations.calculateBaseTiles(queryParameters, upperLeft, lowerRight, subFileParameter);
            QueryCalculations.calculateBlocks(queryParameters, subFileParameter);

            processBlocks(queryParameters, subFileParameter, Tile.getBoundingBox(upperLeft, lowerRight), selector, mapReadResult);

        } catch (IOException e) {
            logDebug(e.getMessage());
            return null;
        }

        return mapReadResult;
    }





    private int[] readOptionalLabelPosition() {
        int[] labelPosition = new int[2];

        /* get the label position latitude offset (VBE-S) */
        labelPosition[1] = mReadBuffer.readSignedInt();

        /* get the label position longitude offset (VBE-S) */
        labelPosition[0] = mReadBuffer.readSignedInt();

        return labelPosition;
    }

    /**
     * Reads only POI data for tile.
     *
     * @param tile tile for which data is requested.
     * @return POI data for the tile.
     */
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
    public MapReadResult readPoiData(Tile upperLeft, Tile lowerRight) {
        return readMapData(upperLeft, lowerRight, Selector.POIS);
    }

    private int[][] readZoomTable(SubFileParameter subFileParameter) {
        int rows = subFileParameter.zoomLevelMax - subFileParameter.zoomLevelMin + 1;
        int[][] zoomTable = new int[rows][2];

        int cumulatedNumberOfPois = 0;
        int cumulatedNumberOfWays = 0;

        for (int row = 0; row < rows; row++) {
            cumulatedNumberOfPois += mReadBuffer.readUnsignedInt();
            cumulatedNumberOfWays += mReadBuffer.readUnsignedInt();

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
    public void restrictToZoomRange(int minZoom, int maxZoom) {
        this.zoomLevelMax = maxZoom;
        this.zoomLevelMin = minZoom;
    }

    /**
     * Returns true if MapDatabase contains tile.
     *
     * @param tile tile to be rendered.
     * @return true if tile is part of database.
     */
    public boolean supportsTile(Tile tile) {
        return tile.getBoundingBox().intersects(mTileSource.getMapInfo().boundingBox)
                && (tile.zoomLevel >= this.zoomLevelMin && tile.zoomLevel <= this.zoomLevelMax);
    }

    /**
     * Returns true if a way should be included in the result set for readLabels()
     * By default only ways with names, house numbers or a ref are included in the result set
     * of readLabels(). This is to reduce the set of ways as much as possible to save memory.
     *
     * @param tags the tags associated with the way
     * @return true if the way should be included in the result set
     */
    public boolean wayAsLabelTagFilter(List<Tag> tags) {
        return false;
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

    static class TileProjection {
        private static final double COORD_SCALE = 1000000.0;

        long dx, dy;
        double divx, divy;

        void setTile(Tile tile) {
            /* tile position in pixels at tile zoom */
            long x = tile.tileX * Tile.SIZE;
            long y = tile.tileY * Tile.SIZE + Tile.SIZE;

            /* size of the map in pixel at tile zoom */
            long mapExtents = Tile.SIZE << tile.zoomLevel;

            /* offset relative to lat/lon == 0 */
            dx = (x - (mapExtents >> 1));
            dy = (y - (mapExtents >> 1));

            /* scales longitude(1e6) to map-pixel */
            divx = (180.0 * COORD_SCALE) / (mapExtents >> 1);

            /* scale latitude to map-pixel */
            divy = (Math.PI * 2.0) / (mapExtents >> 1);
        }

        public void projectPoint(int lat, int lon, MapElement out) {
            out.clear();
            out.startPoints();
            out.addPoint(projectLon(lon), projectLat(lat));
        }

        public float projectLat(double lat) {
            double s = Math.sin(lat * ((Math.PI / 180) / COORD_SCALE));
            double r = Math.log((1.0 + s) / (1.0 - s));

            return Tile.SIZE - (float) (r / divy + dy);
        }

        public float projectLon(double lon) {
            return (float) (lon / divx - dx);
        }

        void project(MapElement e) {

            float[] coords  = e.points;
            int[]   indices = e.index;

            int inPos  = 0;
            int outPos = 0;

            boolean isPoly = e.isPoly();

            for (int idx = 0, m = indices.length; idx < m; idx++) {
                int len = indices[idx];
                if (len == 0)
                    continue;
                if (len < 0)
                    break;

                float lat, lon, pLon = 0, pLat = 0;
                int cnt = 0, first = outPos;

                for (int end = inPos + len; inPos < end; inPos += 2) {
                    lon = projectLon(coords[inPos]);
                    lat = projectLat(coords[inPos + 1]);

                    if (cnt != 0) {
                        /* drop small distance intermediate nodes */
                        if (lat == pLat && lon == pLon) {
                            //logDebugdebug("drop zero delta ");
                            continue;
                        }
                    }
                    coords[outPos++] = pLon = lon;
                    coords[outPos++] = pLat = lat;
                    cnt += 2;
                }

                if (isPoly && coords[first] == pLon && coords[first + 1] == pLat) {
                    /* remove identical start/end point */
                    //logDebugdebug("drop closing point {}", e);
                    indices[idx] = (short) (cnt - 2);
                    outPos -= 2;
                } else {
                    indices[idx] = (short) cnt;
                }
            }
            if (e.labelPosition != null) {
                e.labelPosition.x = projectLon(e.labelPosition.x);
                e.labelPosition.y = projectLat(e.labelPosition.y);
            }
        }
    }









    //  TODO  ///////////////////  added tile ENCODER for MapsForge format /////////////////////////
    /////////////////////////////  encode tile into byte array  ////////////////////////////////////
    private byte[] encodeTile() {

        ByteArrayOutputStream data = new ByteArrayOutputStream();
        DataOutputStream dos  = new DataOutputStream(data);

        try {
            dos.writeInt(tX);
            dos.writeInt(tY);
            dos.writeInt(tZ);
            dos.writeInt(mTileLatitude);
            dos.writeInt(mTileLongitude);
            dos.writeInt(ewaters.size());
            dos.writeInt(epois.size());
            dos.writeInt(eways.size());

            for (int i=0; i<ewaters.size(); i++) {  // waters
                MapElement e = ewaters.get(i);

                // 1. meta
                dos.writeInt(e.layer);
                //dos.writeInt(e.tags.numTags);
                dos.writeInt(e.tags.size());
                dos.writeInt(e.points.length);
                dos.writeInt(e.index.length);
                dos.writeInt(e.pointNextPos);
                dos.writeInt(e.indexCurrentPos);
                dos.writeInt(e.getTypeInt());
                //dos.writeFloat(e.mTmpPoint.getX());
                //dos.writeFloat(e.mTmpPoint.getY());
                //dos.writeInt(e.pointLimit);
                //dos.writeFloat(e.labelPosition.getX());
                //dos.writeFloat(e.labelPosition.getY());

                // 2. tags
                Tag[] tags = e.tags.asArray();
                for (Tag t : tags ) {
                    if (t == null)  continue;
                    dos.writeUTF(t.key);
                    dos.writeUTF(t.value);
                }

                // 3. points array
                float[] pts = e.points;
                for (float f : pts) {
                    dos.writeFloat(f);
                }

                // 4. index array
                int[] idx = e.index;
                for (int ix : idx) {
                    dos.writeInt(ix);
                }
            }

            for (int i=0; i<epois.size(); i++) {  // pois
                MapElement e = epois.get(i);

                // 1. meta
                dos.writeInt(e.layer);
                //dos.writeInt(e.tags.numTags);
                dos.writeInt(e.tags.size());
                dos.writeInt(e.points.length);
                dos.writeInt(e.index.length);
                dos.writeInt(e.pointNextPos);
                dos.writeInt(e.indexCurrentPos);
                dos.writeInt(e.getTypeInt());
                //dos.writeFloat(e.mTmpPoint.getX());
                //dos.writeFloat(e.mTmpPoint.getY());
                //dos.writeInt(e.pointLimit);
                //dos.writeFloat(e.labelPosition.getX());
                //dos.writeFloat(e.labelPosition.getY());

                // 2. tags
                Tag[] tags = e.tags.asArray();
                for (Tag t : tags ) {
                    if (t == null)  continue;
                    dos.writeUTF(t.key);
                    dos.writeUTF(t.value);
                }

                // 3. points array
                float[] pts = e.points;
                for (float f : pts) {
                    dos.writeFloat(f);
                }

                // 4. index array
                int[] idx = e.index;
                for (int ix : idx) {
                    dos.writeInt(ix);
                }
            }

            for (int i=0; i<eways.size(); i++) {  // ways
                MapElement e = eways.get(i);

                // 1. meta
                dos.writeInt(e.layer);
                if (tZ<11) dos.writeInt(1);
                else       dos.writeInt(e.tags.size()); //todo
                dos.writeInt(e.points.length);
                dos.writeInt(e.index.length);
                dos.writeInt(e.pointNextPos);
                dos.writeInt(e.indexCurrentPos);
                dos.writeInt(e.getTypeInt());
                //dos.writeFloat(e.mTmpPoint.getX());
                //dos.writeFloat(e.mTmpPoint.getY());
                //dos.writeInt(e.pointLimit);
                //dos.writeFloat(e.labelPosition.getX());
                //dos.writeFloat(e.labelPosition.getY());

                // 2. tags
                Tag[] tags = e.tags.asArray();
                if (tZ<11) { // todo
                    if (coder.get(tags[0].key)   != null) {
                        dos.writeBoolean(true);
                        dos.writeByte(coder.get(tags[0].key));
                    }
                    else {
                        dos.writeBoolean(false);
                        dos.writeUTF(tags[0].key);
                    }

                    if (coder.get(tags[0].value) != null)  {
                        dos.writeBoolean(true);
                        dos.writeByte(coder.get(tags[0].value));
                    }
                    else {
                        dos.writeBoolean(false);
                        dos.writeUTF(tags[0].value);
                    }
                }
                else {
                    for (Tag t : tags) {
                        if (t == null)  continue;

                        if (coder.get(t.key)   != null) {
                            dos.writeBoolean(true);
                            dos.writeByte(coder.get(t.key));
                        }
                        else {
                            dos.writeBoolean(false);
                            dos.writeUTF(t.key);
                        }

                        if (coder.get(t.value) != null)  {
                            dos.writeBoolean(true);
                            dos.writeByte(coder.get(t.value));
                        }
                        else {
                            dos.writeBoolean(false);
                            dos.writeUTF(t.value);
                        }
                    }
                }

                // 3. points array
                float[] pts = e.points;
                for (float f : pts) {
                    dos.writeFloat(f);
                }

                // 4. index array
                int[] idx = e.index;
                for (int ix : idx) {
                    dos.writeInt(ix);
                }
            }

            dos.flush();

            byte[] rawTile = data.toByteArray();

            data.close();

            return rawTile;

        } catch (IOException ex)     {
            ex.printStackTrace();
            return null;
        }

    }


   



    // handling Logging:
    private void logDebug(String msg) { System.out.println("VTM" + " ### " + getClass().getName() + " ###  " + msg); }
}
