/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright 2014 Ludwig M Brinckmann
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
package org.mapsforge.map.reader.header;

import java.io.IOException;

import org.mapsforge.map.reader.ReadBuffer;

/**
 * Reads and validates the header data from a binary map file.
 */
public class MapFileHeader {
	/**
	 * Maximum valid base zoom level of a sub-file.
	 */
	private static final int BASE_ZOOM_LEVEL_MAX = 20;

	/**
	 * Minimum size of the file header in bytes.
	 */
	private static final int HEADER_SIZE_MIN = 70;

	/**
	 * Length of the debug signature at the beginning of the index.
	 */
	private static final byte SIGNATURE_LENGTH_INDEX = 16;

	/**
	 * A single whitespace character.
	 */
	private static final char SPACE = ' ';

	private MapFileInfo mapFileInfo;
	private SubFileParameter[] subFileParameters;
	private byte zoomLevelMaximum;
	private byte zoomLevelMinimum;

	/**
	 * @return a MapFileInfo containing the header data.
	 */
	public MapFileInfo getMapFileInfo() {
		return this.mapFileInfo;
	}

	/**
	 * @param zoomLevel
	 *            the originally requested zoom level.
	 * @return the closest possible zoom level which is covered by a sub-file.
	 */
	public byte getQueryZoomLevel(byte zoomLevel) {
		if (zoomLevel > this.zoomLevelMaximum) {
			return this.zoomLevelMaximum;
		} else if (zoomLevel < this.zoomLevelMinimum) {
			return this.zoomLevelMinimum;
		}
		return zoomLevel;
	}

	/**
	 * @param queryZoomLevel
	 *            the zoom level for which the sub-file parameters are needed.
	 * @return the sub-file parameters for the given zoom level.
	 */
	public SubFileParameter getSubFileParameter(int queryZoomLevel) {
		return this.subFileParameters[queryZoomLevel];
	}

	/**
	 * Reads and validates the header block from the map file.
	 * 
	 * @param readBuffer
	 *            the ReadBuffer for the file data.
	 * @param fileSize
	 *            the size of the map file in bytes.
	 * @throws IOException
	 *             if an error occurs while reading the file.
	 */
	public void readHeader(ReadBuffer readBuffer, long fileSize) throws IOException {
		RequiredFields.readMagicByte(readBuffer);
		RequiredFields.readRemainingHeader(readBuffer);

		MapFileInfoBuilder mapFileInfoBuilder = new MapFileInfoBuilder();

		RequiredFields.readFileVersion(readBuffer, mapFileInfoBuilder);

		RequiredFields.readFileSize(readBuffer, fileSize, mapFileInfoBuilder);

		RequiredFields.readMapDate(readBuffer, mapFileInfoBuilder);

		RequiredFields.readBoundingBox(readBuffer, mapFileInfoBuilder);

		RequiredFields.readTilePixelSize(readBuffer, mapFileInfoBuilder);

		RequiredFields.readProjectionName(readBuffer, mapFileInfoBuilder);

		OptionalFields.readOptionalFields(readBuffer, mapFileInfoBuilder);

		RequiredFields.readPoiTags(readBuffer, mapFileInfoBuilder);

		RequiredFields.readWayTags(readBuffer, mapFileInfoBuilder);

		readSubFileParameters(readBuffer, fileSize, mapFileInfoBuilder);

		this.mapFileInfo = mapFileInfoBuilder.build();
	}

	private void readSubFileParameters(ReadBuffer readBuffer, long fileSize,
			MapFileInfoBuilder mapFileInfoBuilder) {
		// get and check the number of sub-files (1 byte)
		byte numberOfSubFiles = readBuffer.readByte();
		if (numberOfSubFiles < 1) {
			throw new MapFileException("invalid number of sub-files: " + numberOfSubFiles);
		}
		mapFileInfoBuilder.numberOfSubFiles = numberOfSubFiles;

		SubFileParameter[] tempSubFileParameters = new SubFileParameter[numberOfSubFiles];
		this.zoomLevelMinimum = Byte.MAX_VALUE;
		this.zoomLevelMaximum = Byte.MIN_VALUE;

		// get and check the information for each sub-file
		for (byte currentSubFile = 0; currentSubFile < numberOfSubFiles; ++currentSubFile) {
			SubFileParameterBuilder subFileParameterBuilder = new SubFileParameterBuilder();

			// get and check the base zoom level (1 byte)
			byte baseZoomLevel = readBuffer.readByte();
			if (baseZoomLevel < 0 || baseZoomLevel > BASE_ZOOM_LEVEL_MAX) {
				throw new MapFileException("invalid base zoom level: " + baseZoomLevel);
			}
			subFileParameterBuilder.baseZoomLevel = baseZoomLevel;

			// get and check the minimum zoom level (1 byte)
			byte zoomLevelMin = readBuffer.readByte();
			if (zoomLevelMin < 0 || zoomLevelMin > 22) {
				throw new MapFileException("invalid minimum zoom level: " + zoomLevelMin);
			}
			subFileParameterBuilder.zoomLevelMin = zoomLevelMin;

			// get and check the maximum zoom level (1 byte)
			byte zoomLevelMax = readBuffer.readByte();
			if (zoomLevelMax < 0 || zoomLevelMax > 22) {
				throw new MapFileException("invalid maximum zoom level: " + zoomLevelMax);
			}
			subFileParameterBuilder.zoomLevelMax = zoomLevelMax;

			// check for valid zoom level range
			if (zoomLevelMin > zoomLevelMax) {
				throw new MapFileException("invalid zoom level range: " + zoomLevelMin + SPACE + zoomLevelMax);
			}

			// get and check the start address of the sub-file (8 bytes)
			long startAddress = readBuffer.readLong();
			if (startAddress < HEADER_SIZE_MIN || startAddress >= fileSize) {
				throw new MapFileException("invalid start address: " + startAddress);
			}
			subFileParameterBuilder.startAddress = startAddress;

			long indexStartAddress = startAddress;
			if (mapFileInfoBuilder.optionalFields.isDebugFile) {
				// the sub-file has an index signature before the index
				indexStartAddress += SIGNATURE_LENGTH_INDEX;
			}
			subFileParameterBuilder.indexStartAddress = indexStartAddress;

			// get and check the size of the sub-file (8 bytes)
			long subFileSize = readBuffer.readLong();
			if (subFileSize < 1) {
				throw new MapFileException("invalid sub-file size: " + subFileSize);
			}
			subFileParameterBuilder.subFileSize = subFileSize;

			subFileParameterBuilder.boundingBox = mapFileInfoBuilder.boundingBox;

			// add the current sub-file to the list of sub-files
			tempSubFileParameters[currentSubFile] = subFileParameterBuilder.build();


			// update the global minimum and maximum zoom level information
			if (this.zoomLevelMinimum > tempSubFileParameters[currentSubFile].zoomLevelMin) {
				this.zoomLevelMinimum = tempSubFileParameters[currentSubFile].zoomLevelMin;
				mapFileInfoBuilder.zoomLevelMin = this.zoomLevelMinimum;
			}
			if (this.zoomLevelMaximum < tempSubFileParameters[currentSubFile].zoomLevelMax) {
				this.zoomLevelMaximum = tempSubFileParameters[currentSubFile].zoomLevelMax;
				mapFileInfoBuilder.zoomLevelMax = this.zoomLevelMaximum;
			}

		}

		// create and fill the lookup table for the sub-files
		this.subFileParameters = new SubFileParameter[this.zoomLevelMaximum + 1];
		for (int currentMapFile = 0; currentMapFile < numberOfSubFiles; ++currentMapFile) {
			SubFileParameter subFileParameter = tempSubFileParameters[currentMapFile];
			for (byte zoomLevel = subFileParameter.zoomLevelMin; zoomLevel <= subFileParameter.zoomLevelMax; ++zoomLevel) {
				this.subFileParameters[zoomLevel] = subFileParameter;
			}
		}
	}

}
