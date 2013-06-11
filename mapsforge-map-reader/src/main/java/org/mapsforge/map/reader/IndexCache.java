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
package org.mapsforge.map.reader;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Map;

import org.mapsforge.core.util.LRUCache;
import org.mapsforge.map.reader.header.SubFileParameter;

/**
 * A cache for database index blocks with a fixed size and LRU policy.
 */
class IndexCache {
	/**
	 * Number of index entries that one index block consists of.
	 */
	private static final int INDEX_ENTRIES_PER_BLOCK = 128;

	/**
	 * Maximum size in bytes of one index block.
	 */
	private static final int SIZE_OF_INDEX_BLOCK = INDEX_ENTRIES_PER_BLOCK * SubFileParameter.BYTES_PER_INDEX_ENTRY;

	private final Map<IndexCacheEntryKey, byte[]> map;
	private final RandomAccessFile randomAccessFile;

	/**
	 * @param randomAccessFile
	 *            the map file from which the index should be read and cached.
	 * @param capacity
	 *            the maximum number of entries in the cache.
	 * @throws IllegalArgumentException
	 *             if the capacity is negative.
	 */
	IndexCache(RandomAccessFile randomAccessFile, int capacity) {
		this.randomAccessFile = randomAccessFile;
		this.map = new LRUCache<IndexCacheEntryKey, byte[]>(capacity);
	}

	/**
	 * Destroy the cache at the end of its lifetime.
	 */
	void destroy() {
		this.map.clear();
	}

	/**
	 * Returns the index entry of a block in the given map file. If the required index entry is not cached, it will be
	 * read from the map file index and put in the cache.
	 * 
	 * @param subFileParameter
	 *            the parameters of the map file for which the index entry is needed.
	 * @param blockNumber
	 *            the number of the block in the map file.
	 * @return the index entry.
	 * @throws IOException
	 *             if an I/O error occurs during reading.
	 */
	long getIndexEntry(SubFileParameter subFileParameter, long blockNumber) throws IOException {
		// check if the block number is out of bounds
		if (blockNumber >= subFileParameter.numberOfBlocks) {
			throw new IOException("invalid block number: " + blockNumber);
		}

		// calculate the index block number
		long indexBlockNumber = blockNumber / INDEX_ENTRIES_PER_BLOCK;

		// create the cache entry key for this request
		IndexCacheEntryKey indexCacheEntryKey = new IndexCacheEntryKey(subFileParameter, indexBlockNumber);

		// check for cached index block
		byte[] indexBlock = this.map.get(indexCacheEntryKey);
		if (indexBlock == null) {
			// cache miss, seek to the correct index block in the file and read it
			long indexBlockPosition = subFileParameter.indexStartAddress + indexBlockNumber * SIZE_OF_INDEX_BLOCK;

			int remainingIndexSize = (int) (subFileParameter.indexEndAddress - indexBlockPosition);
			int indexBlockSize = Math.min(SIZE_OF_INDEX_BLOCK, remainingIndexSize);
			indexBlock = new byte[indexBlockSize];

			this.randomAccessFile.seek(indexBlockPosition);
			if (this.randomAccessFile.read(indexBlock, 0, indexBlockSize) != indexBlockSize) {
				throw new IOException("could not read index block with size: " + indexBlockSize);
			}

			// put the index block in the map
			this.map.put(indexCacheEntryKey, indexBlock);
		}

		// calculate the address of the index entry inside the index block
		long indexEntryInBlock = blockNumber % INDEX_ENTRIES_PER_BLOCK;
		int addressInIndexBlock = (int) (indexEntryInBlock * SubFileParameter.BYTES_PER_INDEX_ENTRY);

		// return the real index entry
		return Deserializer.getFiveBytesLong(indexBlock, addressInIndexBlock);
	}
}
