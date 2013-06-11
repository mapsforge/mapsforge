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

import java.nio.ByteBuffer;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mapsforge.map.writer.model.MapWriterConfiguration;
import org.mapsforge.map.writer.model.TileBasedDataProcessor;

public class MapFileWriterTest {
	private MapWriterConfiguration configuration;
	private TileBasedDataProcessor dataProcessor;

	@Before
	public void setUp() {
		this.configuration = new MapWriterConfiguration();
		// this.configuration.addOutputFile(getStringArgument(taskConfig, PARAM_OUTFILE,
		// Constants.DEFAULT_PARAM_OUTFILE));
		this.configuration.setWriterVersion("test");
		this.configuration.loadTagMappingFile("src/test/resources/tag-mapping.xml");
		this.configuration.addMapStartPosition("52.455882,13.297244");
		this.configuration.addMapStartZoom("14");
		this.configuration.addBboxConfiguration("52,13,53,14");
		this.configuration.addZoomIntervalConfiguration("5,0,7,10,8,11,14,12,18");
		this.configuration.setComment("i love mapsforge");
		this.configuration.setDebugStrings(false);
		this.configuration.setPolygonClipping(true);
		this.configuration.setWayClipping(true);
		this.configuration.setSimplification(0.00001);
		this.configuration.setDataProcessorType("ram");
		this.configuration.setBboxEnlargement(10);
		this.configuration.setPreferredLanguage("de");
		this.configuration.addEncodingChoice("auto");
		this.configuration.validate();

		this.dataProcessor = RAMTileBasedDataProcessor.newInstance(this.configuration);
	}

	@Test
	public void testWriteHeaderBuffer() {
		ByteBuffer headerBuffer = ByteBuffer.allocate(MapFileWriter.HEADER_BUFFER_SIZE);
		int headerLength = MapFileWriter.writeHeaderBuffer(this.configuration, this.dataProcessor, headerBuffer);

		// expected header length
		// 20 + 4 + 4 + 8 + 8 + 16 + 2
		// + 9 ("Mercator")
		// + 1 + 8 + 1
		// + 3 ("de")
		// + 17 ("i love mapsforge")
		// + 5("test")
		// + 2 + 19 ("amenity=university")
		// + 2 + 14 + 18 ("natural=beach", natural=coastline")
		// + 1
		// + 3 * (3 + 8 + 8)
		// == 219
		Assert.assertEquals(219, headerLength);
	}
	// @Test
	// public void testProcessPOI() {
	// fail("Not yet implemented");
	// }
	//
	// @Test
	// public void testProcessWay() {
	// fail("Not yet implemented");
	// }
	//
	// @Test
	// public void testWriteWayNodes() {
	// fail("Not yet implemented");
	// }
	//
	// @Test
	// public void testInfoBytePoiLayerAndTagAmount() {
	// fail("Not yet implemented");
	// }
	//
	// @Test
	// public void testInfoByteWayLayerAndTagAmount() {
	// fail("Not yet implemented");
	// }
	//
	// @Test
	// public void testInfoByteOptmizationParams() {
	// fail("Not yet implemented");
	// }
	//
	// @Test
	// public void testInfoBytePOIFeatures() {
	// fail("Not yet implemented");
	// }
	//
	// @Test
	// public void testInfoByteWayFeatures() {
	// fail("Not yet implemented");
	// }
}
