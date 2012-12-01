/*
 * Copyright 2010, 2011, 2012 mapsforge.org
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
package org.mapsforge.core.util;

import java.io.Closeable;
import java.io.IOException;

import junit.framework.Assert;

import org.junit.Test;

public class IOUtilsTest {
	static class DummyCloseable implements Closeable {
		boolean closed;

		@Override
		public void close() throws IOException {
			if (this.closed) {
				throw new IOException();
			}
			this.closed = true;
		}
	}

	@Test
	public void closeQuietlyTest() {
		IOUtils.closeQuietly(null);

		DummyCloseable dummyCloseable = new DummyCloseable();
		IOUtils.closeQuietly(dummyCloseable);
		Assert.assertTrue(dummyCloseable.closed);

		IOUtils.closeQuietly(dummyCloseable);
	}
}
