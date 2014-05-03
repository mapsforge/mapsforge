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
package org.mapsforge.map.writer;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.util.LatLongUtils;
import org.mapsforge.core.util.MercatorProjection;
import org.mapsforge.map.writer.model.Encoding;
import org.mapsforge.map.writer.model.MapWriterConfiguration;
import org.mapsforge.map.writer.model.OSMTag;
import org.mapsforge.map.writer.model.TDNode;
import org.mapsforge.map.writer.model.TDWay;
import org.mapsforge.map.writer.model.TileBasedDataProcessor;
import org.mapsforge.map.writer.model.TileCoordinate;
import org.mapsforge.map.writer.model.TileData;
import org.mapsforge.map.writer.model.TileInfo;
import org.mapsforge.map.writer.model.WayDataBlock;
import org.mapsforge.map.writer.model.ZoomIntervalConfiguration;
import org.mapsforge.map.writer.util.Constants;
import org.mapsforge.map.writer.util.GeoUtils;
import org.mapsforge.map.writer.util.JTSUtils;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.CacheStats;
import com.google.common.cache.LoadingCache;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

/**
 * Writes the binary file format for mapsforge maps.
 */
public final class MapFileWriter {
	private static class JTSGeometryCacheLoader extends CacheLoader<TDWay, Geometry> {
		private final TileBasedDataProcessor datastore;

		JTSGeometryCacheLoader(TileBasedDataProcessor datastore) {
			super();
			this.datastore = datastore;
		}

		@Override
		public Geometry load(TDWay way) throws Exception {
			if (way.isInvalid()) {
				throw new Exception("way is known to be invalid: " + way.getId());
			}
			List<TDWay> innerWaysOfMultipolygon = this.datastore.getInnerWaysOfMultipolygon(way.getId());
			Geometry geometry = JTSUtils.toJtsGeometry(way, innerWaysOfMultipolygon);
			if (geometry == null) {
				way.setInvalid(true);
				throw new Exception("cannot create geometry for way with id: " + way.getId());
			}
			return geometry;
		}
	}

	private static class WayPreprocessingCallable implements Callable<WayPreprocessingResult> {
		private final MapWriterConfiguration configuration;
		private final LoadingCache<TDWay, Geometry> jtsGeometryCache;
		private final byte maxZoomInterval;
		private final TileCoordinate tile;
		private final TDWay way;

		/**
		 * @param way
		 *            the {@link TDWay}
		 * @param tile
		 *            the {@link TileCoordinate}
		 * @param maxZoomInterval
		 *            the maximum zoom
		 * @param jtsGeometryCache
		 *            the {@link LoadingCache} for {@link Geometry} objects
		 * @param configuration
		 *            the {@link MapWriterConfiguration}
		 */
		WayPreprocessingCallable(TDWay way, TileCoordinate tile, byte maxZoomInterval,
				LoadingCache<TDWay, Geometry> jtsGeometryCache, MapWriterConfiguration configuration) {
			super();
			this.way = way;
			this.tile = tile;
			this.maxZoomInterval = maxZoomInterval;
			this.jtsGeometryCache = jtsGeometryCache;
			this.configuration = configuration;
		}

		@Override
		public WayPreprocessingResult call() {
			// TODO more sophisticated clipping of polygons needed
			// we have a problem when clipping polygons which border needs to be
			// rendered
			// the problem does not occur with polygons that do not have a border
			// imagine an administrative border, such a polygon is not filled, but its
			// border is rendered
			// in case the polygon spans multiple base zoom tiles, clipping
			// introduces connections between
			// nodes that haven't existed before (exactly at the borders of a base
			// tile)
			// in case of filled polygons we do not care about these connections
			// polygons that represent a border must be clipped as simple ways and
			// not as polygons

			Geometry originalGeometry;
			try {
				originalGeometry = this.jtsGeometryCache.get(this.way);
			} catch (ExecutionException e) {
				this.way.setInvalid(true);
				return null;
			}

			Geometry processedGeometry = originalGeometry;
			if (originalGeometry instanceof Polygon && this.configuration.isPolygonClipping()
					|| (originalGeometry instanceof LineString || originalGeometry instanceof MultiLineString)
					&& this.configuration.isWayClipping()) {
				processedGeometry = GeoUtils.clipToTile(this.way, originalGeometry, this.tile,
						this.configuration.getBboxEnlargement());
				if (processedGeometry == null) {
					return null;
				}
			}

			// TODO is this the right place to simplify, or is it better before clipping?
			if (this.configuration.getSimplification() > 0
					&& this.tile.getZoomlevel() <= Constants.MAX_SIMPLIFICATION_BASE_ZOOM) {
				processedGeometry = GeoUtils.simplifyGeometry(this.way, processedGeometry, this.maxZoomInterval,
						tileSize, this.configuration.getSimplification());
				if (processedGeometry == null) {
					return null;
				}
			}

			List<WayDataBlock> blocks = GeoUtils.toWayDataBlockList(processedGeometry);
			if (blocks == null) {
				return null;
			}
			if (blocks.isEmpty()) {
				LOGGER.finer("empty list of way data blocks after preprocessing way: " + this.way.getId());
				return null;
			}
			short subtileMask = GeoUtils.computeBitmask(processedGeometry, this.tile,
					this.configuration.getBboxEnlargement());

			// check if the original polygon is completely contained in the current tile
			// in that case we do not try to compute a label position
			// this is left to the renderer for more flexibility

			// in case the polygon covers multiple tiles, we compute the centroid of the unclipped polygon
			// if the computed centroid is within the current tile, we add it as label position
			// this way, we can make sure that a label position is attached only once to a clipped polygon
			LatLong centroidCoordinate = null;
			if (this.configuration.isLabelPosition() && this.way.isValidClosedLine()
					&& !GeoUtils.coveredByTile(originalGeometry, this.tile, this.configuration.getBboxEnlargement())) {
				Point centroidPoint = originalGeometry.getCentroid();
				if (GeoUtils.coveredByTile(centroidPoint, this.tile, this.configuration.getBboxEnlargement())) {
					centroidCoordinate = new LatLong(centroidPoint.getY(), centroidPoint.getX());
				}
			}

			switch (this.configuration.getEncodingChoice()) {
				case SINGLE:
					blocks = DeltaEncoder.encode(blocks, Encoding.DELTA);
					break;
				case DOUBLE:
					blocks = DeltaEncoder.encode(blocks, Encoding.DOUBLE_DELTA);
					break;
				case AUTO:
					List<WayDataBlock> blocksDelta = DeltaEncoder.encode(blocks, Encoding.DELTA);
					List<WayDataBlock> blocksDoubleDelta = DeltaEncoder.encode(blocks, Encoding.DOUBLE_DELTA);
					int simDelta = DeltaEncoder.simulateSerialization(blocksDelta);
					int simDoubleDelta = DeltaEncoder.simulateSerialization(blocksDoubleDelta);
					if (simDelta <= simDoubleDelta) {
						blocks = blocksDelta;
					} else {
						blocks = blocksDoubleDelta;
					}
					break;
			}

			return new WayPreprocessingResult(this.way, blocks, centroidCoordinate, subtileMask);
		}
	}

	private static class WayPreprocessingResult {
		final LatLong labelPosition;
		final short subtileMask;
		final TDWay way;
		final List<WayDataBlock> wayDataBlocks;

		WayPreprocessingResult(TDWay way, List<WayDataBlock> wayDataBlocks, LatLong labelPosition, short subtileMask) {
			super();
			this.way = way;
			this.wayDataBlocks = wayDataBlocks;
			this.labelPosition = labelPosition;
			this.subtileMask = subtileMask;
		}

		LatLong getLabelPosition() {
			return this.labelPosition;
		}

		short getSubtileMask() {
			return this.subtileMask;
		}

		TDWay getWay() {
			return this.way;
		}

		List<WayDataBlock> getWayDataBlocks() {
			return this.wayDataBlocks;
		}
	}

	// IO
	static final int HEADER_BUFFER_SIZE = 0x100000; // 1MB

	static final Logger LOGGER = Logger.getLogger(MapFileWriter.class.getName());

	static final int MIN_TILE_BUFFER_SIZE = 0xF00000; // 15MB

	static final int POI_DATA_BUFFER_SIZE = 0x100000; // 1MB

	static final int TILE_BUFFER_SIZE = 0xA00000; // 10MB

	// private static final int PIXEL_COMPRESSION_MAX_DELTA = 5;

	static final int TILES_BUFFER_SIZE = 0x3200000; // 50MB

	static final int WAY_BUFFER_SIZE = 0x100000; // 10MB

	static final int WAY_DATA_BUFFER_SIZE = 0xA00000; // 10MB

	// private static final CoastlineHandler COASTLINE_HANDLER = new
	// CoastlineHandler();

	private static final short BITMAP_COMMENT = 8;
	private static final short BITMAP_CREATED_WITH = 4;
	// bitmap flags for file features
	private static final short BITMAP_DEBUG = 128;
	// bitmap flags for pois
	private static final short BITMAP_ELEVATION = 32;
	private static final short BITMAP_ENCODING = 4;
	private static final short BITMAP_HOUSENUMBER = 64;

	private static final int BITMAP_INDEX_ENTRY_WATER = 0x80;
	private static final short BITMAP_LABEL = 16;

	private static final short BITMAP_MAP_START_POSITION = 64;

	private static final short BITMAP_MAP_START_ZOOM = 32;
	private static final short BITMAP_MULTIPLE_WAY_BLOCKS = 8;
	// bitmap flags for pois and ways
	private static final short BITMAP_NAME = 128;
	private static final short BITMAP_PREFERRED_LANGUAGE = 16;

	// bitmap flags for ways
	private static final short BITMAP_REF = 32;
	private static final int BYTE_AMOUNT_SUBFILE_INDEX_PER_TILE = 5;
	private static final int BYTES_INT = 4;
	private static final int DEBUG_BLOCK_SIZE = 32;
	private static final String DEBUG_INDEX_START_STRING = "+++IndexStart+++";
	// DEBUG STRINGS
	private static final String DEBUG_STRING_POI_HEAD = "***POIStart";

	private static final String DEBUG_STRING_POI_TAIL = "***";

	private static final String DEBUG_STRING_TILE_HEAD = "###TileStart";

	private static final String DEBUG_STRING_TILE_TAIL = "###";

	private static final String DEBUG_STRING_WAY_HEAD = "---WayStart";

	private static final String DEBUG_STRING_WAY_TAIL = "---";

	private static final int DUMMY_INT = 0xf0f0f0f0;

	private static final long DUMMY_LONG = 0xf0f0f0f0f0f0f0f0L;

	private static final ExecutorService EXECUTOR_SERVICE = Executors.newFixedThreadPool(Runtime.getRuntime()
			.availableProcessors());
	private static final int JTS_GEOMETRY_CACHE_SIZE = 50000;
	private static final String MAGIC_BYTE = "mapsforge binary OSM";
	private static final int OFFSET_FILE_SIZE = 28;
	private static final float PROGRESS_PERCENT_STEP = 10f;
	private static final String PROJECTION = "Mercator";
	private static final int SIZE_ZOOMINTERVAL_CONFIGURATION = 19;

	private static final TileInfo TILE_INFO = TileInfo.getInstance();

	private static final int tileSize = 256; // needed for optimal simplification, but set to constant here TODO

	private static final Charset UTF8_CHARSET = Charset.forName("utf8");

	/**
	 * Writes the map file according to the given configuration using the given data processor.
	 * 
	 * @param configuration
	 *            the configuration
	 * @param dataProcessor
	 *            the data processor
	 * @throws IOException
	 *             thrown if any IO error occurs
	 */
	public static void writeFile(MapWriterConfiguration configuration, TileBasedDataProcessor dataProcessor)
			throws IOException {
		RandomAccessFile randomAccessFile = new RandomAccessFile(configuration.getOutputFile(), "rw");

		int amountOfZoomIntervals = dataProcessor.getZoomIntervalConfiguration().getNumberOfZoomIntervals();
		ByteBuffer containerHeaderBuffer = ByteBuffer.allocate(HEADER_BUFFER_SIZE);
		// CONTAINER HEADER
		int totalHeaderSize = writeHeaderBuffer(configuration, dataProcessor, containerHeaderBuffer);

		// set to mark where zoomIntervalConfig starts
		containerHeaderBuffer.reset();

		final LoadingCache<TDWay, Geometry> jtsGeometryCache = CacheBuilder.newBuilder()
				.maximumSize(JTS_GEOMETRY_CACHE_SIZE).concurrencyLevel(Runtime.getRuntime().availableProcessors() * 2)
				.build(new JTSGeometryCacheLoader(dataProcessor));

		// SUB FILES
		// for each zoom interval write a sub file
		long currentFileSize = totalHeaderSize;
		for (int i = 0; i < amountOfZoomIntervals; i++) {
			// SUB FILE INDEX AND DATA
			long subfileSize = writeSubfile(currentFileSize, i, dataProcessor, jtsGeometryCache, randomAccessFile,
					configuration);
			// SUB FILE META DATA IN CONTAINER HEADER
			writeSubfileMetaDataToContainerHeader(dataProcessor.getZoomIntervalConfiguration(), i, currentFileSize,
					subfileSize, containerHeaderBuffer);
			currentFileSize += subfileSize;
		}

		randomAccessFile.seek(0);
		randomAccessFile.write(containerHeaderBuffer.array(), 0, totalHeaderSize);

		// WRITE FILE SIZE TO HEADER
		long fileSize = randomAccessFile.length();
		randomAccessFile.seek(OFFSET_FILE_SIZE);
		randomAccessFile.writeLong(fileSize);

		randomAccessFile.close();

		CacheStats stats = jtsGeometryCache.stats();
		LOGGER.info("JTS Geometry cache hit rate: " + stats.hitRate());
		LOGGER.info("JTS Geometry total load time: " + stats.totalLoadTime() / 1000);

		LOGGER.info("Finished writing file.");
	}

	static byte infoByteOptmizationParams(MapWriterConfiguration configuration) {
		byte infoByte = 0;

		if (configuration.isDebugStrings()) {
			infoByte |= BITMAP_DEBUG;
		}
		if (configuration.getMapStartPosition() != null) {
			infoByte |= BITMAP_MAP_START_POSITION;
		}
		if (configuration.hasMapStartZoomLevel()) {
			infoByte |= BITMAP_MAP_START_ZOOM;
		}
		if (configuration.getPreferredLanguage() != null) {
			infoByte |= BITMAP_PREFERRED_LANGUAGE;
		}
		if (configuration.getComment() != null) {
			infoByte |= BITMAP_COMMENT;
		}

		infoByte |= BITMAP_CREATED_WITH;

		return infoByte;
	}

	static byte infoBytePOIFeatures(String name, int elevation, String housenumber) {
		byte infoByte = 0;

		if (name != null && !name.isEmpty()) {
			infoByte |= BITMAP_NAME;
		}
		if (housenumber != null && !housenumber.isEmpty()) {
			infoByte |= BITMAP_HOUSENUMBER;
		}
		if (elevation != 0) {
			infoByte |= BITMAP_ELEVATION;
		}
		return infoByte;
	}

	static byte infoBytePoiLayerAndTagAmount(TDNode node) {
		byte layer = node.getLayer();
		// make sure layer is in [0,10]
		layer = layer < 0 ? 0 : layer > 10 ? 10 : layer;
		short tagAmount = node.getTags() == null ? 0 : (short) node.getTags().length;

		return (byte) (layer << BYTES_INT | tagAmount);
	}

	static byte infoByteWayFeatures(TDWay way, WayPreprocessingResult wpr) {
		byte infoByte = 0;

		if (way.getName() != null && !way.getName().isEmpty()) {
			infoByte |= BITMAP_NAME;
		}
		if (way.getHouseNumber() != null && !way.getHouseNumber().isEmpty()) {
			infoByte |= BITMAP_HOUSENUMBER;
		}
		if (way.getRef() != null && !way.getRef().isEmpty()) {
			infoByte |= BITMAP_REF;
		}
		if (wpr.getLabelPosition() != null) {
			infoByte |= BITMAP_LABEL;
		}
		if (wpr.getWayDataBlocks().size() > 1) {
			infoByte |= BITMAP_MULTIPLE_WAY_BLOCKS;
		}

		if (!wpr.getWayDataBlocks().isEmpty()) {
			WayDataBlock wayDataBlock = wpr.getWayDataBlocks().get(0);
			if (wayDataBlock.getEncoding() == Encoding.DOUBLE_DELTA) {
				infoByte |= BITMAP_ENCODING;
			}
		}

		return infoByte;
	}

	static byte infoByteWayLayerAndTagAmount(TDWay way) {
		byte layer = way.getLayer();
		// make sure layer is in [0,10]
		layer = layer < 0 ? 0 : layer > 10 ? 10 : layer;
		short tagAmount = way.getTags() == null ? 0 : (short) way.getTags().length;

		return (byte) (layer << BYTES_INT | tagAmount);
	}

	static void processPOI(TDNode poi, int currentTileLat, int currentTileLon, boolean debugStrings,
			ByteBuffer poiBuffer) {
		if (debugStrings) {
			StringBuilder sb = new StringBuilder();
			sb.append(DEBUG_STRING_POI_HEAD).append(poi.getId()).append(DEBUG_STRING_POI_TAIL);
			poiBuffer.put(sb.toString().getBytes(UTF8_CHARSET));
			// append whitespaces so that block has 32 bytes
			appendWhitespace(DEBUG_BLOCK_SIZE - sb.toString().getBytes(UTF8_CHARSET).length, poiBuffer);
		}

		// write poi features to the file
		poiBuffer.put(Serializer.getVariableByteSigned(poi.getLatitude() - currentTileLat));
		poiBuffer.put(Serializer.getVariableByteSigned(poi.getLongitude() - currentTileLon));

		// write byte with layer and tag amount info
		poiBuffer.put(infoBytePoiLayerAndTagAmount(poi));

		// write tag ids to the file
		if (poi.getTags() != null) {
			for (short tagID : poi.getTags()) {
				poiBuffer.put(Serializer.getVariableByteUnsigned(OSMTagMapping.getInstance().getOptimizedPoiIds()
						.get(Short.valueOf(tagID)).intValue()));
			}
		}

		// write byte with bits set to 1 if the poi has a
		// name, an elevation
		// or a housenumber
		poiBuffer.put(infoBytePOIFeatures(poi.getName(), poi.getElevation(), poi.getHouseNumber()));

		if (poi.getName() != null && !poi.getName().isEmpty()) {
			writeUTF8(poi.getName(), poiBuffer);
		}

		if (poi.getHouseNumber() != null && !poi.getHouseNumber().isEmpty()) {
			writeUTF8(poi.getHouseNumber(), poiBuffer);
		}

		if (poi.getElevation() != 0) {
			poiBuffer.put(Serializer.getVariableByteSigned(poi.getElevation()));
		}
	}

	static void processWay(WayPreprocessingResult wpr, TDWay way, int currentTileLat, int currentTileLon,
			ByteBuffer wayBuffer) {
		// write subtile bitmask of way
		wayBuffer.putShort(wpr.getSubtileMask());

		// write byte with layer and tag amount
		wayBuffer.put(infoByteWayLayerAndTagAmount(way));

		// write tag ids
		if (way.getTags() != null) {
			for (short tagID : way.getTags()) {
				wayBuffer.put(Serializer.getVariableByteUnsigned(mappedWayTagID(tagID)));
			}
		}

		// write a byte with flags for existence of name,
		// ref, label position, and multiple blocks
		wayBuffer.put(infoByteWayFeatures(way, wpr));

		// if the way has a name, write it to the file
		if (way.getName() != null && !way.getName().isEmpty()) {
			writeUTF8(way.getName(), wayBuffer);
		}

		// if the way has a house number, write it to the file
		if (way.getHouseNumber() != null && !way.getHouseNumber().isEmpty()) {
			writeUTF8(way.getHouseNumber(), wayBuffer);
		}

		// if the way has a ref, write it to the file
		if (way.getRef() != null && !way.getRef().isEmpty()) {
			writeUTF8(way.getRef(), wayBuffer);
		}

		if (wpr.getLabelPosition() != null) {
			int firstWayStartLat = wpr.getWayDataBlocks().get(0).getOuterWay().get(0).intValue();
			int firstWayStartLon = wpr.getWayDataBlocks().get(0).getOuterWay().get(1).intValue();

			wayBuffer
					.put(Serializer.getVariableByteSigned(LatLongUtils.degreesToMicrodegrees(wpr.getLabelPosition().latitude)
							- firstWayStartLat));
			wayBuffer
					.put(Serializer.getVariableByteSigned(LatLongUtils.degreesToMicrodegrees(wpr.getLabelPosition().longitude)
							- firstWayStartLon));
		}

		if (wpr.getWayDataBlocks().size() > 1) {
			// write the amount of way data blocks
			wayBuffer.put(Serializer.getVariableByteUnsigned(wpr.getWayDataBlocks().size()));
		}

		// write the way data blocks

		// case 1: simple way or simple polygon --> the way
		// block consists of
		// exactly one way
		// case 2: multi polygon --> the way consists of
		// exactly one outer way and
		// one or more inner ways
		for (WayDataBlock wayDataBlock : wpr.getWayDataBlocks()) {
			// write the amount of coordinate blocks
			// we have at least one block (potentially
			// interpreted as outer way) and
			// possible blocks for inner ways
			if (wayDataBlock.getInnerWays() != null && !wayDataBlock.getInnerWays().isEmpty()) {
				// multi polygon: outer way + number of
				// inner ways
				wayBuffer.put(Serializer.getVariableByteUnsigned(1 + wayDataBlock.getInnerWays().size()));
			} else {
				// simply a single way (not a multi polygon)
				wayBuffer.put(Serializer.getVariableByteUnsigned(1));
			}

			// write block for (outer/simple) way
			writeWay(wayDataBlock.getOuterWay(), currentTileLat, currentTileLon, wayBuffer);

			// write blocks for inner ways
			if (wayDataBlock.getInnerWays() != null && !wayDataBlock.getInnerWays().isEmpty()) {
				for (List<Integer> innerWayCoordinates : wayDataBlock.getInnerWays()) {
					writeWay(innerWayCoordinates, currentTileLat, currentTileLon, wayBuffer);
				}
			}
		}
	}

	static int writeHeaderBuffer(final MapWriterConfiguration configuration,
			final TileBasedDataProcessor dataProcessor, final ByteBuffer containerHeaderBuffer) {
		LOGGER.fine("writing header");
		LOGGER.fine("Bounding box for file: " + dataProcessor.getBoundingBox().toString());

		// write file header
		// MAGIC BYTE
		byte[] magicBytes = MAGIC_BYTE.getBytes(UTF8_CHARSET);
		containerHeaderBuffer.put(magicBytes);

		// HEADER SIZE: Write dummy pattern as header size. It will be replaced
		// later in time
		int headerSizePosition = containerHeaderBuffer.position();
		containerHeaderBuffer.putInt(DUMMY_INT);

		// FILE VERSION
		containerHeaderBuffer.putInt(configuration.getFileSpecificationVersion());

		// FILE SIZE: Write dummy pattern as file size. It will be replaced
		// later in time
		containerHeaderBuffer.putLong(DUMMY_LONG);
		// DATE OF CREATION
		containerHeaderBuffer.putLong(System.currentTimeMillis());

		// BOUNDING BOX
		containerHeaderBuffer.putInt(LatLongUtils.degreesToMicrodegrees(dataProcessor.getBoundingBox().minLatitude));
		containerHeaderBuffer.putInt(LatLongUtils.degreesToMicrodegrees(dataProcessor.getBoundingBox().minLongitude));
		containerHeaderBuffer.putInt(LatLongUtils.degreesToMicrodegrees(dataProcessor.getBoundingBox().maxLatitude));
		containerHeaderBuffer.putInt(LatLongUtils.degreesToMicrodegrees(dataProcessor.getBoundingBox().maxLongitude));

		// TILE SIZE
		containerHeaderBuffer.putShort((short) Constants.DEFAULT_TILE_SIZE);

		// PROJECTION
		writeUTF8(PROJECTION, containerHeaderBuffer);

		// check whether zoom start is a valid zoom level

		// FLAGS
		containerHeaderBuffer.put(infoByteOptmizationParams(configuration));

		// MAP START POSITION
		LatLong mapStartPosition = configuration.getMapStartPosition();
		if (mapStartPosition != null) {
			containerHeaderBuffer.putInt(LatLongUtils.degreesToMicrodegrees(mapStartPosition.latitude));
			containerHeaderBuffer.putInt(LatLongUtils.degreesToMicrodegrees(mapStartPosition.longitude));
		}

		// MAP START ZOOM
		if (configuration.hasMapStartZoomLevel()) {
			containerHeaderBuffer.put((byte) configuration.getMapStartZoomLevel());
		}

		// PREFERRED LANGUAGE
		if (configuration.getPreferredLanguage() != null) {
			writeUTF8(configuration.getPreferredLanguage(), containerHeaderBuffer);
		}

		// COMMENT
		if (configuration.getComment() != null) {
			writeUTF8(configuration.getComment(), containerHeaderBuffer);
		}

		// CREATED WITH
		writeUTF8(configuration.getWriterVersion(), containerHeaderBuffer);

		// AMOUNT POI TAGS
		containerHeaderBuffer.putShort((short) configuration.getTagMapping().getOptimizedPoiIds().size());
		// POI TAGS
		// retrieves tag ids in order of frequency, most frequent come first
		for (short tagId : configuration.getTagMapping().getOptimizedPoiIds().keySet()) {
			OSMTag tag = configuration.getTagMapping().getPoiTag(tagId);
			writeUTF8(tag.tagKey(), containerHeaderBuffer);
		}

		// AMOUNT OF WAY TAGS
		containerHeaderBuffer.putShort((short) configuration.getTagMapping().getOptimizedWayIds().size());

		// WAY TAGS
		for (short tagId : configuration.getTagMapping().getOptimizedWayIds().keySet()) {
			OSMTag tag = configuration.getTagMapping().getWayTag(tagId);
			writeUTF8(tag.tagKey(), containerHeaderBuffer);
		}

		// AMOUNT OF ZOOM INTERVALS
		int numberOfZoomIntervals = dataProcessor.getZoomIntervalConfiguration().getNumberOfZoomIntervals();
		containerHeaderBuffer.put((byte) numberOfZoomIntervals);

		// SET MARK OF THIS BUFFER AT POSITION FOR WRITING ZOOM INTERVAL CONFIG
		containerHeaderBuffer.mark();
		// ZOOM INTERVAL CONFIGURATION: SKIP COMPUTED AMOUNT OF BYTES
		containerHeaderBuffer.position(containerHeaderBuffer.position() + SIZE_ZOOMINTERVAL_CONFIGURATION
				* numberOfZoomIntervals);

		// now write header size
		// -4 bytes of header size variable itself
		int headerSize = containerHeaderBuffer.position() - headerSizePosition - BYTES_INT;
		containerHeaderBuffer.putInt(headerSizePosition, headerSize);

		return containerHeaderBuffer.position();
	}

	static void writeWayNodes(List<Integer> waynodes, int currentTileLat, int currentTileLon, ByteBuffer buffer) {
		if (!waynodes.isEmpty() && waynodes.size() % 2 == 0) {
			Iterator<Integer> waynodeIterator = waynodes.iterator();
			buffer.put(Serializer.getVariableByteSigned(waynodeIterator.next().intValue() - currentTileLat));
			buffer.put(Serializer.getVariableByteSigned(waynodeIterator.next().intValue() - currentTileLon));

			while (waynodeIterator.hasNext()) {
				buffer.put(Serializer.getVariableByteSigned(waynodeIterator.next().intValue()));
			}
		}
	}

	static void writeZoomLevelTable(int[][] entitiesPerZoomLevel, ByteBuffer tileBuffer) {
		// write cumulated number of POIs and ways for this tile on
		// each zoom level
		for (int[] entityCount : entitiesPerZoomLevel) {
			tileBuffer.put(Serializer.getVariableByteUnsigned(entityCount[0]));
			tileBuffer.put(Serializer.getVariableByteUnsigned(entityCount[1]));
		}
	}

	private static void appendWhitespace(int amount, ByteBuffer buffer) {
		for (int i = 0; i < amount; i++) {
			buffer.put((byte) ' ');
		}
	}

	private static int mappedWayTagID(short original) {
		return OSMTagMapping.getInstance().getOptimizedWayIds().get(Short.valueOf(original)).intValue();
	}

	private static void processIndexEntry(TileCoordinate tileCoordinate, ByteBuffer indexBuffer,
			long currentSubfileOffset) {
		byte[] indexBytes = Serializer.getFiveBytes(currentSubfileOffset);
		if (TILE_INFO.isWaterTile(tileCoordinate)) {
			indexBytes[0] |= BITMAP_INDEX_ENTRY_WATER;
		}
		indexBuffer.put(indexBytes);
	}

	private static void processTile(MapWriterConfiguration configuration, TileCoordinate tileCoordinate,
			TileBasedDataProcessor dataProcessor, LoadingCache<TDWay, Geometry> jtsGeometryCache,
			int zoomIntervalIndex, ByteBuffer tileBuffer, ByteBuffer poiDataBuffer, ByteBuffer wayDataBuffer,
			ByteBuffer wayBuffer) {
		tileBuffer.clear();
		poiDataBuffer.clear();
		wayDataBuffer.clear();
		wayBuffer.clear();

		final TileData currentTile = dataProcessor.getTile(zoomIntervalIndex, tileCoordinate.getX(),
				tileCoordinate.getY());

		final int currentTileLat = LatLongUtils.degreesToMicrodegrees(MercatorProjection.tileYToLatitude(
				tileCoordinate.getY(), tileCoordinate.getZoomlevel()));
		final int currentTileLon = LatLongUtils.degreesToMicrodegrees(MercatorProjection.tileXToLongitude(
				tileCoordinate.getX(), tileCoordinate.getZoomlevel()));

		final byte minZoomCurrentInterval = dataProcessor.getZoomIntervalConfiguration().getMinZoom(zoomIntervalIndex);
		final byte maxZoomCurrentInterval = dataProcessor.getZoomIntervalConfiguration().getMaxZoom(zoomIntervalIndex);

		// write amount of POIs and ways for each zoom level
		Map<Byte, List<TDNode>> poisByZoomlevel = currentTile.poisByZoomlevel(minZoomCurrentInterval,
				maxZoomCurrentInterval);
		Map<Byte, List<TDWay>> waysByZoomlevel = currentTile.waysByZoomlevel(minZoomCurrentInterval,
				maxZoomCurrentInterval);

		if (!poisByZoomlevel.isEmpty() || !waysByZoomlevel.isEmpty()) {
			if (configuration.isDebugStrings()) {
				writeTileSignature(tileCoordinate, tileBuffer);
			}

			int amountZoomLevels = maxZoomCurrentInterval - minZoomCurrentInterval + 1;
			int[][] entitiesPerZoomLevel = new int[amountZoomLevels][2];

			// WRITE POIS
			for (byte zoomlevel = minZoomCurrentInterval; zoomlevel <= maxZoomCurrentInterval; zoomlevel++) {
				int indexEntitiesPerZoomLevelTable = zoomlevel - minZoomCurrentInterval;
				List<TDNode> pois = poisByZoomlevel.get(Byte.valueOf(zoomlevel));
				if (pois != null) {
					for (TDNode poi : pois) {
						processPOI(poi, currentTileLat, currentTileLon, configuration.isDebugStrings(), poiDataBuffer);
					}
					// increment count of POIs on this zoom level
					entitiesPerZoomLevel[indexEntitiesPerZoomLevelTable][0] += pois.size();
				}
			}

			// WRITE WAYS
			for (byte zoomlevel = minZoomCurrentInterval; zoomlevel <= maxZoomCurrentInterval; zoomlevel++) {
				int indexEntitiesPerZoomLevelTable = zoomlevel - minZoomCurrentInterval;

				List<TDWay> ways = waysByZoomlevel.get(Byte.valueOf(zoomlevel));
				if (ways != null) {
					List<WayPreprocessingCallable> callables = new ArrayList<>();
					for (TDWay way : ways) {
						if (!way.isInvalid()) {
							callables.add(new WayPreprocessingCallable(way, tileCoordinate, maxZoomCurrentInterval,
									jtsGeometryCache, configuration));
						}
					}
					try {
						List<Future<WayPreprocessingResult>> futures = EXECUTOR_SERVICE.invokeAll(callables);
						for (Future<WayPreprocessingResult> wprFuture : futures) {
							WayPreprocessingResult wpr;
							try {
								wpr = wprFuture.get();
							} catch (ExecutionException e) {
								LOGGER.log(Level.WARNING, "error in parallel preprocessing of ways", e);
								continue;
							}
							if (wpr != null) {
								wayBuffer.clear();
								// increment count of ways on this zoom level
								entitiesPerZoomLevel[indexEntitiesPerZoomLevelTable][1]++;
								if (configuration.isDebugStrings()) {
									writeWaySignature(wpr.getWay(), wayDataBuffer);
								}
								processWay(wpr, wpr.getWay(), currentTileLat, currentTileLon, wayBuffer);
								// write size of way to way data buffer
								wayDataBuffer.put(Serializer.getVariableByteUnsigned(wayBuffer.position()));
								// write way data to way data buffer
								wayDataBuffer.put(wayBuffer.array(), 0, wayBuffer.position());
							}
						}
					} catch (InterruptedException e) {
						LOGGER.log(Level.WARNING, "error in parallel preprocessing of ways", e);
					}
				}
			}

			// write zoom table
			writeZoomLevelTable(entitiesPerZoomLevel, tileBuffer);
			// write offset to first way in the tile header
			tileBuffer.put(Serializer.getVariableByteUnsigned(poiDataBuffer.position()));
			// write POI data to buffer
			tileBuffer.put(poiDataBuffer.array(), 0, poiDataBuffer.position());
			// write way data to buffer
			tileBuffer.put(wayDataBuffer.array(), 0, wayDataBuffer.position());
		}
	}

	private static void writeIndex(ByteBuffer indexBuffer, long startPositionSubfile, long subFileSize,
			RandomAccessFile randomAccessFile) throws IOException {
		randomAccessFile.seek(startPositionSubfile);
		randomAccessFile.write(indexBuffer.array());
		randomAccessFile.seek(subFileSize);
	}

	private static long writeSubfile(final long startPositionSubfile, final int zoomIntervalIndex,
			final TileBasedDataProcessor dataStore, final LoadingCache<TDWay, Geometry> jtsGeometryCache,
			final RandomAccessFile randomAccessFile, final MapWriterConfiguration configuration) throws IOException {
		LOGGER.fine("writing data for zoom interval " + zoomIntervalIndex + ", number of tiles: "
				+ dataStore.getTileGridLayout(zoomIntervalIndex).getAmountTilesHorizontal()
				* dataStore.getTileGridLayout(zoomIntervalIndex).getAmountTilesVertical());

		final TileCoordinate upperLeft = dataStore.getTileGridLayout(zoomIntervalIndex).getUpperLeft();
		final int lengthX = dataStore.getTileGridLayout(zoomIntervalIndex).getAmountTilesHorizontal();
		final int lengthY = dataStore.getTileGridLayout(zoomIntervalIndex).getAmountTilesVertical();
		final int amountTiles = lengthX * lengthY;

		// used to monitor progress
		double amountOfTilesInPercentStep = amountTiles;
		if (amountTiles > PROGRESS_PERCENT_STEP) {
			amountOfTilesInPercentStep = Math.ceil(amountTiles / PROGRESS_PERCENT_STEP);
		}

		int processedTiles = 0;

		final byte baseZoomCurrentInterval = dataStore.getZoomIntervalConfiguration().getBaseZoom(zoomIntervalIndex);

		final int tileAmountInBytes = lengthX * lengthY * BYTE_AMOUNT_SUBFILE_INDEX_PER_TILE;
		final int indexBufferSize = tileAmountInBytes
				+ (configuration.isDebugStrings() ? DEBUG_INDEX_START_STRING.getBytes(UTF8_CHARSET).length : 0);

		final ByteBuffer indexBuffer = ByteBuffer.allocate(indexBufferSize);
		final ByteBuffer tileBuffer = ByteBuffer.allocate(TILE_BUFFER_SIZE);
		final ByteBuffer wayDataBuffer = ByteBuffer.allocate(WAY_DATA_BUFFER_SIZE);
		final ByteBuffer wayBuffer = ByteBuffer.allocate(WAY_BUFFER_SIZE);
		final ByteBuffer poiDataBuffer = ByteBuffer.allocate(POI_DATA_BUFFER_SIZE);

		final ByteBuffer multipleTilesBuffer = ByteBuffer.allocate(TILES_BUFFER_SIZE);

		// write debug strings for tile index segment if necessary
		if (configuration.isDebugStrings()) {
			indexBuffer.put(DEBUG_INDEX_START_STRING.getBytes(UTF8_CHARSET));
		}

		long currentSubfileOffset = indexBufferSize;
		randomAccessFile.seek(startPositionSubfile + indexBufferSize);

		for (int tileY = upperLeft.getY(); tileY < upperLeft.getY() + lengthY; tileY++) {
			for (int tileX = upperLeft.getX(); tileX < upperLeft.getX() + lengthX; tileX++) {
				TileCoordinate tileCoordinate = new TileCoordinate(tileX, tileY, baseZoomCurrentInterval);

				processIndexEntry(tileCoordinate, indexBuffer, currentSubfileOffset);
				processTile(configuration, tileCoordinate, dataStore, jtsGeometryCache, zoomIntervalIndex, tileBuffer,
						poiDataBuffer, wayDataBuffer, wayBuffer);
				currentSubfileOffset += tileBuffer.position();

				writeTile(multipleTilesBuffer, tileBuffer, randomAccessFile);

				if (++processedTiles % amountOfTilesInPercentStep == 0) {
					if (processedTiles == amountTiles) {
						LOGGER.info("written 100% of sub file for zoom interval index " + zoomIntervalIndex);
					} else {
						LOGGER.info("written " + (processedTiles / amountOfTilesInPercentStep) * PROGRESS_PERCENT_STEP
								+ "% of sub file for zoom interval index " + zoomIntervalIndex);
					}
				}

				// TODO accounting for progress information
			} // end for loop over tile columns
		} // /end for loop over tile rows

		// write remaining tiles
		if (multipleTilesBuffer.position() > 0) {
			// byte buffer was not previously cleared
			randomAccessFile.write(multipleTilesBuffer.array(), 0, multipleTilesBuffer.position());
		}

		writeIndex(indexBuffer, startPositionSubfile, currentSubfileOffset, randomAccessFile);

		// return size of sub file in bytes
		return currentSubfileOffset;
	}

	private static void writeSubfileMetaDataToContainerHeader(ZoomIntervalConfiguration zoomIntervalConfiguration,
			int i, long startIndexOfSubfile, long subfileSize, ByteBuffer buffer) {
		// HEADER META DATA FOR SUB FILE
		// write zoom interval configuration to header
		byte minZoomCurrentInterval = zoomIntervalConfiguration.getMinZoom(i);
		byte maxZoomCurrentInterval = zoomIntervalConfiguration.getMaxZoom(i);
		byte baseZoomCurrentInterval = zoomIntervalConfiguration.getBaseZoom(i);

		buffer.put(baseZoomCurrentInterval);
		buffer.put(minZoomCurrentInterval);
		buffer.put(maxZoomCurrentInterval);
		buffer.putLong(startIndexOfSubfile);
		buffer.putLong(subfileSize);
	}

	private static void writeTile(ByteBuffer multipleTilesBuffer, ByteBuffer tileBuffer,
			RandomAccessFile randomAccessFile) throws IOException {
		// add tile to tiles buffer
		multipleTilesBuffer.put(tileBuffer.array(), 0, tileBuffer.position());

		// if necessary, allocate new buffer
		if (multipleTilesBuffer.remaining() < MIN_TILE_BUFFER_SIZE) {
			randomAccessFile.write(multipleTilesBuffer.array(), 0, multipleTilesBuffer.position());
			multipleTilesBuffer.clear();
		}
	}

	private static void writeTileSignature(TileCoordinate tileCoordinate, ByteBuffer tileBuffer) {
		StringBuilder sb = new StringBuilder();
		sb.append(DEBUG_STRING_TILE_HEAD).append(tileCoordinate.getX()).append(",").append(tileCoordinate.getY())
				.append(DEBUG_STRING_TILE_TAIL);
		tileBuffer.put(sb.toString().getBytes(UTF8_CHARSET));
		// append withespaces so that block has 32 bytes
		appendWhitespace(DEBUG_BLOCK_SIZE - sb.toString().getBytes(UTF8_CHARSET).length, tileBuffer);
	}

	private static void writeUTF8(String string, ByteBuffer buffer) {
		buffer.put(Serializer.getVariableByteUnsigned(string.getBytes(UTF8_CHARSET).length));
		buffer.put(string.getBytes(UTF8_CHARSET));
	}

	private static void writeWay(List<Integer> wayNodes, int currentTileLat, int currentTileLon, ByteBuffer buffer) {
		// write the amount of way nodes to the file
		// wayBuffer
		buffer.put(Serializer.getVariableByteUnsigned(wayNodes.size() / 2));

		// write the way nodes:
		// the first node is always stored with four bytes
		// the remaining way node differences are stored according to the
		// compression type
		writeWayNodes(wayNodes, currentTileLat, currentTileLon, buffer);
	}

	private static void writeWaySignature(TDWay way, ByteBuffer tileBuffer) {
		StringBuilder sb = new StringBuilder();
		sb.append(DEBUG_STRING_WAY_HEAD).append(way.getId()).append(DEBUG_STRING_WAY_TAIL);
		tileBuffer.put(sb.toString().getBytes(UTF8_CHARSET));
		// append withespaces so that block has 32 bytes
		appendWhitespace(DEBUG_BLOCK_SIZE - sb.toString().getBytes(UTF8_CHARSET).length, tileBuffer);
	}

	private MapFileWriter() {
		// do nothing
	}
}
