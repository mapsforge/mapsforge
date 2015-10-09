/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright 2014-2015 Ludwig M Brinckmann
 * Copyright 2014, 2015 devemux86
 * Copyright 2015 lincomatic
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

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Tag;
import org.mapsforge.core.model.Tile;
import org.mapsforge.core.util.LatLongUtils;
import org.mapsforge.core.util.MercatorProjection;
import org.mapsforge.map.datastore.MapDataStore;
import org.mapsforge.map.datastore.MapReadResult;
import org.mapsforge.map.datastore.PoiWayBundle;
import org.mapsforge.map.datastore.PointOfInterest;
import org.mapsforge.map.datastore.Way;
import org.mapsforge.map.reader.header.MapFileException;
import org.mapsforge.map.reader.header.MapFileHeader;
import org.mapsforge.map.reader.header.MapFileInfo;
import org.mapsforge.map.reader.header.SubFileParameter;

/**
 * A class for reading binary map files.
 * <p>
 * The readMapData method is now thread safe, but care should be taken that not too much data is
 * read at the same time (keep simultaneous requests to minimum)
 *
 * @see <a href="https://github.com/mapsforge/mapsforge/blob/master/docs/Specification-Binary-Map-File.md">Specification</a>
 */
public class MapFile extends MapDataStore {

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
	private static final Byte DEFAULT_START_ZOOM_LEVEL = Byte.valueOf((byte) 12);

	/**
	 * Amount of cache blocks that the index cache should store.
	 */
	private static final int INDEX_CACHE_SIZE = 64;

	/**
	 * Error message for an invalid first way offset.
	 */
	private static final String INVALID_FIRST_WAY_OFFSET = "invalid first way offset: ";

	private static final Logger LOGGER = Logger.getLogger(MapFile.class.getName());

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

	private static final String READ_ONLY_MODE = "r";

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

	/* Only for testing, an empty file. */
	public static final MapFile TEST_MAP_FILE = new MapFile();
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
	private final RandomAccessFile inputFile;

	private final MapFileHeader mapFileHeader;

	private final ReadBuffer readBuffer;
	private final long timestamp;

	private MapFile() {
		// only to create a dummy empty file.
		databaseIndexCache = null;
		fileSize = 0;
		inputFile = null;
		mapFileHeader = null;
		readBuffer = null;
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
	 * @param mapFile the map file.
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

			// open the file in read only mode
			this.inputFile = new RandomAccessFile(mapFile, READ_ONLY_MODE);
			this.fileSize = this.inputFile.length();

			this.readBuffer = new ReadBuffer(this.inputFile);
			this.mapFileHeader = new MapFileHeader();
			this.mapFileHeader.readHeader(this.readBuffer, this.fileSize);
			this.databaseIndexCache = new IndexCache(this.inputFile, INDEX_CACHE_SIZE);

			this.timestamp = mapFile.lastModified();
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, e.getMessage(), e);
			// make sure that the file is closed
			closeFile();
			throw new MapFileException(e.getMessage());
		}
	}

	/**
	 * Opens the given map file, reads its header data and validates them.
	 *
	 * @param mapFileName the path of the map file.
	 * @param language the language to use (may be null).
	 * @throws MapFileException if the given map file is null or invalid or IOException if the file
	 * cannot be opened.
	 */
	public MapFile(String mapFileName, String language) {
		this(new File(mapFileName), language);
	}

	@Override
	public BoundingBox boundingBox() {
		return getMapFileInfo().boundingBox;
	}

	@Override
	public void close() {
		closeFile();
	}

	/**
	 * Closes the map file and destroys all internal caches. Has no effect if no map file is currently opened.
	 */
	private void closeFile() {
		try {
			this.databaseIndexCache.destroy();
			this.inputFile.close();
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, e.getMessage(), e);
		}
	}

	private void decodeWayNodesDoubleDelta(LatLong[] waySegment, double tileLatitude, double tileLongitude) {
		// get the first way node latitude offset (VBE-S)
		double wayNodeLatitude = tileLatitude
				+ LatLongUtils.microdegreesToDegrees(this.readBuffer.readSignedInt());

		// get the first way node longitude offset (VBE-S)
		double wayNodeLongitude = tileLongitude
				+ LatLongUtils.microdegreesToDegrees(this.readBuffer.readSignedInt());

		// store the first way node
		waySegment[0] = new LatLong(wayNodeLatitude, wayNodeLongitude);

		double previousSingleDeltaLatitude = 0;
		double previousSingleDeltaLongitude = 0;

		for (int wayNodesIndex = 1; wayNodesIndex < waySegment.length; ++wayNodesIndex) {
			// get the way node latitude double-delta offset (VBE-S)
			double doubleDeltaLatitude = LatLongUtils.microdegreesToDegrees(this.readBuffer.readSignedInt());

			// get the way node longitude double-delta offset (VBE-S)
			double doubleDeltaLongitude = LatLongUtils.microdegreesToDegrees(this.readBuffer.readSignedInt());

			double singleDeltaLatitude = doubleDeltaLatitude + previousSingleDeltaLatitude;
			double singleDeltaLongitude = doubleDeltaLongitude + previousSingleDeltaLongitude;

			wayNodeLatitude = wayNodeLatitude + singleDeltaLatitude;
			wayNodeLongitude = wayNodeLongitude + singleDeltaLongitude;

			waySegment[wayNodesIndex] = new LatLong(wayNodeLatitude, wayNodeLongitude);

			previousSingleDeltaLatitude = singleDeltaLatitude;
			previousSingleDeltaLongitude = singleDeltaLongitude;
		}
	}

	private void decodeWayNodesSingleDelta(LatLong[] waySegment, double tileLatitude, double tileLongitude) {
		// get the first way node latitude single-delta offset (VBE-S)
		double wayNodeLatitude = tileLatitude
				+ LatLongUtils.microdegreesToDegrees(this.readBuffer.readSignedInt());

		// get the first way node longitude single-delta offset (VBE-S)
		double wayNodeLongitude = tileLongitude
				+ LatLongUtils.microdegreesToDegrees(this.readBuffer.readSignedInt());

		// store the first way node
		waySegment[0] = new LatLong(wayNodeLatitude, wayNodeLongitude);

		for (int wayNodesIndex = 1; wayNodesIndex < waySegment.length; ++wayNodesIndex) {
			// get the way node latitude offset (VBE-S)
			wayNodeLatitude = wayNodeLatitude + LatLongUtils.microdegreesToDegrees(this.readBuffer.readSignedInt());

			// get the way node longitude offset (VBE-S)
			wayNodeLongitude = wayNodeLongitude + LatLongUtils.microdegreesToDegrees(this.readBuffer.readSignedInt());

			waySegment[wayNodesIndex] = new LatLong(wayNodeLatitude, wayNodeLongitude);
		}
	}

	/**
	 * Returns the creation timestamp of the map file.
	 * @param tile not used, as all tiles will shared the same creation date.
	 * @return the creation timestamp inside the map file.
	 */
	@Override
	public long getDataTimestamp(Tile tile) {
		return this.timestamp;
	}

	/**
	 * @return the metadata for the current map file.
	 * @throws IllegalStateException
	 *             if no map is currently opened.
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
			BoundingBox boundingBox, double tileLatitude, double tileLongitude) {
		if (!processBlockSignature()) {
			return null;
		}

		int[][] zoomTable = readZoomTable(subFileParameter);
		int zoomTableRow = queryParameters.queryZoomLevel - subFileParameter.zoomLevelMin;
		int poisOnQueryZoomLevel = zoomTable[zoomTableRow][0];
		int waysOnQueryZoomLevel = zoomTable[zoomTableRow][1];

		// get the relative offset to the first stored way in the block
		int firstWayOffset = this.readBuffer.readUnsignedInt();
		if (firstWayOffset < 0) {
			LOGGER.warning(INVALID_FIRST_WAY_OFFSET + firstWayOffset);
			return null;
		}

		// add the current buffer position to the relative first way offset
		firstWayOffset += this.readBuffer.getBufferPosition();
		if (firstWayOffset > this.readBuffer.getBufferSize()) {
			LOGGER.warning(INVALID_FIRST_WAY_OFFSET + firstWayOffset);
			return null;
		}

		boolean filterRequired = queryParameters.queryZoomLevel > subFileParameter.baseZoomLevel;

		List<PointOfInterest> pois = processPOIs(tileLatitude, tileLongitude, poisOnQueryZoomLevel, boundingBox, filterRequired);
		if (pois == null) {
			return null;
		}

		// finished reading POIs, check if the current buffer position is valid
		if (this.readBuffer.getBufferPosition() > firstWayOffset) {
			LOGGER.warning("invalid buffer position: " + this.readBuffer.getBufferPosition());
			return null;
		}

		// move the pointer to the first way
		this.readBuffer.setBufferPosition(firstWayOffset);

		List<Way> ways = processWays(queryParameters, waysOnQueryZoomLevel, boundingBox, filterRequired, tileLatitude, tileLongitude);
		if (ways == null) {
			return null;
		}

		return new PoiWayBundle(pois, ways);
	}

	private MapReadResult processBlocks(QueryParameters queryParameters, SubFileParameter subFileParameter, BoundingBox boundingBox)
			throws IOException {
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
				} else if (currentBlockSize > ReadBuffer.getMaximumBufferSize()) {
					// the current block is too large, continue with the next block
					LOGGER.warning("current block size too large: " + currentBlockSize);
					continue;
				} else if (currentBlockPointer + currentBlockSize > this.fileSize) {
					LOGGER.warning("current block largher than file size: " + currentBlockSize);
					return null;
				}

				// seek to the current block in the map file
				this.inputFile.seek(subFileParameter.startAddress + currentBlockPointer);

				// read the current block into the buffer
				if (!this.readBuffer.readFromFile(currentBlockSize)) {
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
					PoiWayBundle poiWayBundle = processBlock(queryParameters, subFileParameter, boundingBox, tileLatitude, tileLongitude);
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
			mapFileReadResult.isWater = true;
		}

		return mapFileReadResult;
	}

	/**
	 * Processes the block signature, if present.
	 *
	 * @return true if the block signature could be processed successfully, false otherwise.
	 */
	private boolean processBlockSignature() {
		if (this.mapFileHeader.getMapFileInfo().debugFile) {
			// get and check the block signature
			String signatureBlock = this.readBuffer.readUTF8EncodedString(SIGNATURE_LENGTH_BLOCK);
			if (!signatureBlock.startsWith("###TileStart")) {
				LOGGER.warning("invalid block signature: " + signatureBlock);
				return false;
			}
		}
		return true;
	}

	private List<PointOfInterest> processPOIs(double tileLatitude, double tileLongitude, int numberOfPois, BoundingBox boundingBox, boolean filterRequired) {
		List<PointOfInterest> pois = new ArrayList<PointOfInterest>();
		Tag[] poiTags = this.mapFileHeader.getMapFileInfo().poiTags;

		for (int elementCounter = numberOfPois; elementCounter != 0; --elementCounter) {
			if (this.mapFileHeader.getMapFileInfo().debugFile) {
				// get and check the POI signature
				String signaturePoi = this.readBuffer.readUTF8EncodedString(SIGNATURE_LENGTH_POI);
				if (!signaturePoi.startsWith("***POIStart")) {
					LOGGER.warning("invalid POI signature: " + signaturePoi);
					return null;
				}
			}

			// get the POI latitude offset (VBE-S)
			double latitude = tileLatitude + LatLongUtils.microdegreesToDegrees(this.readBuffer.readSignedInt());

			// get the POI longitude offset (VBE-S)
			double longitude = tileLongitude + LatLongUtils.microdegreesToDegrees(this.readBuffer.readSignedInt());

			// get the special byte which encodes multiple flags
			byte specialByte = this.readBuffer.readByte();

			// bit 1-4 represent the layer
			byte layer = (byte) ((specialByte & POI_LAYER_BITMASK) >>> POI_LAYER_SHIFT);
			// bit 5-8 represent the number of tag IDs
			byte numberOfTags = (byte) (specialByte & POI_NUMBER_OF_TAGS_BITMASK);

			List<Tag> tags = new ArrayList<Tag>();

			// get the tag IDs (VBE-U)
			for (byte tagIndex = numberOfTags; tagIndex != 0; --tagIndex) {
				int tagId = this.readBuffer.readUnsignedInt();
				if (tagId < 0 || tagId >= poiTags.length) {
					LOGGER.warning("invalid POI tag ID: " + tagId);
					return null;
				}
				tags.add(poiTags[tagId]);
			}

			// get the feature bitmask (1 byte)
			byte featureByte = this.readBuffer.readByte();

			// bit 1-3 enable optional features
			boolean featureName = (featureByte & POI_FEATURE_NAME) != 0;
			boolean featureHouseNumber = (featureByte & POI_FEATURE_HOUSE_NUMBER) != 0;
			boolean featureElevation = (featureByte & POI_FEATURE_ELEVATION) != 0;

			// check if the POI has a name
			if (featureName) {
				tags.add(new Tag(TAG_KEY_NAME, extractLocalized(this.readBuffer.readUTF8EncodedString())));
			}

			// check if the POI has a house number
			if (featureHouseNumber) {
				tags.add(new Tag(TAG_KEY_HOUSE_NUMBER, this.readBuffer.readUTF8EncodedString()));
			}

			// check if the POI has an elevation
			if (featureElevation) {
				tags.add(new Tag(TAG_KEY_ELE, Integer.toString(this.readBuffer.readSignedInt())));
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

	private LatLong[][] processWayDataBlock(double tileLatitude, double tileLongitude, boolean doubleDeltaEncoding) {
		// get and check the number of way coordinate blocks (VBE-U)
		int numberOfWayCoordinateBlocks = this.readBuffer.readUnsignedInt();
		if (numberOfWayCoordinateBlocks < 1 || numberOfWayCoordinateBlocks > Short.MAX_VALUE) {
			LOGGER.warning("invalid number of way coordinate blocks: " + numberOfWayCoordinateBlocks);
			return null;
		}

		// create the array which will store the different way coordinate blocks
		LatLong[][] wayCoordinates = new LatLong[numberOfWayCoordinateBlocks][];

		// read the way coordinate blocks
		for (int coordinateBlock = 0; coordinateBlock < numberOfWayCoordinateBlocks; ++coordinateBlock) {
			// get and check the number of way nodes (VBE-U)
			int numberOfWayNodes = this.readBuffer.readUnsignedInt();
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
				decodeWayNodesDoubleDelta(waySegment, tileLatitude, tileLongitude);
			} else {
				decodeWayNodesSingleDelta(waySegment, tileLatitude, tileLongitude);
			}

			wayCoordinates[coordinateBlock] = waySegment;
		}

		return wayCoordinates;
	}

	private List<Way> processWays(QueryParameters queryParameters, int numberOfWays,
			BoundingBox boundingBox, boolean filterRequired, double tileLatitude, double tileLongitude) {
		List<Way> ways = new ArrayList<Way>();
		Tag[] wayTags = this.mapFileHeader.getMapFileInfo().wayTags;

		BoundingBox wayFilterBbox = boundingBox.extendMeters(wayFilterDistance);

		for (int elementCounter = numberOfWays; elementCounter != 0; --elementCounter) {
			if (this.mapFileHeader.getMapFileInfo().debugFile) {
				// get and check the way signature
				String signatureWay = this.readBuffer.readUTF8EncodedString(SIGNATURE_LENGTH_WAY);
				if (!signatureWay.startsWith("---WayStart")) {
					LOGGER.warning("invalid way signature: " + signatureWay);
					return null;
				}
			}

			// get the size of the way (VBE-U)
			int wayDataSize = this.readBuffer.readUnsignedInt();
			if (wayDataSize < 0) {
				LOGGER.warning("invalid way data size: " + wayDataSize);
				return null;
			}

			if (queryParameters.useTileBitmask) {
				// get the way tile bitmask (2 bytes)
				int tileBitmask = this.readBuffer.readShort();
				// check if the way is inside the requested tile
				if ((queryParameters.queryTileBitmask & tileBitmask) == 0) {
					// skip the rest of the way and continue with the next way
					this.readBuffer.skipBytes(wayDataSize - 2);
					continue;
				}
			} else {
				// ignore the way tile bitmask (2 bytes)
				this.readBuffer.skipBytes(2);
			}

			// get the special byte which encodes multiple flags
			byte specialByte = this.readBuffer.readByte();

			// bit 1-4 represent the layer
			byte layer = (byte) ((specialByte & WAY_LAYER_BITMASK) >>> WAY_LAYER_SHIFT);
			// bit 5-8 represent the number of tag IDs
			byte numberOfTags = (byte) (specialByte & WAY_NUMBER_OF_TAGS_BITMASK);

			List<Tag> tags = new ArrayList<Tag>();

			for (byte tagIndex = numberOfTags; tagIndex != 0; --tagIndex) {
				int tagId = this.readBuffer.readUnsignedInt();
				if (tagId < 0 || tagId >= wayTags.length) {
					LOGGER.warning("invalid way tag ID: " + tagId);
					return null;
				}
				tags.add(wayTags[tagId]);
			}

			// get the feature bitmask (1 byte)
			byte featureByte = this.readBuffer.readByte();

			// bit 1-6 enable optional features
			boolean featureName = (featureByte & WAY_FEATURE_NAME) != 0;
			boolean featureHouseNumber = (featureByte & WAY_FEATURE_HOUSE_NUMBER) != 0;
			boolean featureRef = (featureByte & WAY_FEATURE_REF) != 0;
			boolean featureLabelPosition = (featureByte & WAY_FEATURE_LABEL_POSITION) != 0;
			boolean featureWayDataBlocksByte = (featureByte & WAY_FEATURE_DATA_BLOCKS_BYTE) != 0;
			boolean featureWayDoubleDeltaEncoding = (featureByte & WAY_FEATURE_DOUBLE_DELTA_ENCODING) != 0;

			// check if the way has a name
			if (featureName) {
				tags.add(new Tag(TAG_KEY_NAME, extractLocalized(this.readBuffer.readUTF8EncodedString())));
			}

			// check if the way has a house number
			if (featureHouseNumber) {
				tags.add(new Tag(TAG_KEY_HOUSE_NUMBER, this.readBuffer.readUTF8EncodedString()));
			}

			// check if the way has a reference
			if (featureRef) {
				tags.add(new Tag(TAG_KEY_REF, this.readBuffer.readUTF8EncodedString()));
			}

			LatLong labelPosition = readOptionalLabelPosition(tileLatitude, tileLongitude, featureLabelPosition);

			int wayDataBlocks = readOptionalWayDataBlocksByte(featureWayDataBlocksByte);
			if (wayDataBlocks < 1) {
				LOGGER.warning("invalid number of way data blocks: " + wayDataBlocks);
				return null;
			}

			for (int wayDataBlock = 0; wayDataBlock < wayDataBlocks; ++wayDataBlock) {
				LatLong[][] wayNodes = processWayDataBlock(tileLatitude, tileLongitude, featureWayDoubleDeltaEncoding);
				if (wayNodes != null) {
					if (filterRequired && wayFilterEnabled && !wayFilterBbox.intersectsArea(wayNodes)) {
						continue;
					}
					ways.add(new Way(layer, tags, wayNodes, labelPosition));
				}
			}
		}

		return ways;
	}

	/**
	 * Reads all map data for the area covered by the given tile at the tile zoom level.
	 *
	 * @param tile
	 *            defines area and zoom level of read map data.
	 * @return the read map data.
	 */
	@Override
	public synchronized MapReadResult readMapData(Tile tile) {
		try {
			QueryParameters queryParameters = new QueryParameters();
			queryParameters.queryZoomLevel = this.mapFileHeader.getQueryZoomLevel(tile.zoomLevel);

			// get and check the sub-file for the query zoom level
			SubFileParameter subFileParameter = this.mapFileHeader.getSubFileParameter(queryParameters.queryZoomLevel);
			if (subFileParameter == null) {
				LOGGER.warning("no sub-file for zoom level: " + queryParameters.queryZoomLevel);
				return null;
			}

			queryParameters.calculateBaseTiles(tile, subFileParameter);
			queryParameters.calculateBlocks(subFileParameter);

			// we enlarge the bounding box for the tile slightly in order to retain any data that
			// lies right on the border, some of this data needs to be drawn as the graphics will
			// overlap onto this tile.
			return processBlocks(queryParameters, subFileParameter, tile.getBoundingBox());
		} catch (IOException e) {
			LOGGER.log(Level.SEVERE, e.getMessage(), e);
			return null;
		}
	}

	private LatLong readOptionalLabelPosition(double tileLatitude, double tileLongitude, boolean featureLabelPosition) {
		if (featureLabelPosition) {
			// get the label position latitude offset (VBE-S)
			double latitude = tileLatitude + LatLongUtils.microdegreesToDegrees(this.readBuffer.readSignedInt());

			// get the label position longitude offset (VBE-S)
			double longitude = tileLongitude + LatLongUtils.microdegreesToDegrees(this.readBuffer.readSignedInt());

			return new LatLong(latitude, longitude);
		}

		return null;
	}

	private int readOptionalWayDataBlocksByte(boolean featureWayDataBlocksByte) {
		if (featureWayDataBlocksByte) {
			// get and check the number of way data blocks (VBE-U)
			return this.readBuffer.readUnsignedInt();
		}
		// only one way data block exists
		return 1;
	}

	private int[][] readZoomTable(SubFileParameter subFileParameter) {
		int rows = subFileParameter.zoomLevelMax - subFileParameter.zoomLevelMin + 1;
		int[][] zoomTable = new int[rows][2];

		int cumulatedNumberOfPois = 0;
		int cumulatedNumberOfWays = 0;

		for (int row = 0; row < rows; ++row) {
			cumulatedNumberOfPois += this.readBuffer.readUnsignedInt();
			cumulatedNumberOfWays += this.readBuffer.readUnsignedInt();

			zoomTable[row][0] = cumulatedNumberOfPois;
			zoomTable[row][1] = cumulatedNumberOfWays;
		}

		return zoomTable;
	}

	/**
	 * Restricts returns of data to zoom level range specified. This can be used to restrict
	 * the use of this map data base when used in MultiMapDatabase settings.
	 * @param minZoom minimum zoom level supported
	 * @param maxZoom maximum zoom level supported
	 */
	public void restrictToZoomRange(byte minZoom, byte maxZoom) {
		this.getMapFileInfo().zoomLevelMax = maxZoom;
		this.getMapFileInfo().zoomLevelMin = minZoom;
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
		return tile.getBoundingBox().intersects(getMapFileInfo().boundingBox);
	}
}
