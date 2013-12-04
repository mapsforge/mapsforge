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
package org.mapsforge.map;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.mapsforge.core.util.IOUtils;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.DefaultServlet;
import org.mortbay.jetty.servlet.ServletHolder;
import org.mortbay.resource.FileResource;

public class HttpServerTest {
	private static final String TMP_DIR = System.getProperty("java.io.tmpdir");

	private final File httpRoot = new File(TMP_DIR, getClass().getSimpleName() + System.currentTimeMillis());
	private final Server server = new Server(0);

	protected HttpServerTest() {
		// do nothing
	}

	@After
    @SuppressWarnings("PMD.SignatureDeclareThrowsException")
	public final void afterTest() throws Exception {
		try {
			this.server.stop();
			this.server.join();
		} finally {
			FileUtils.deleteDirectory(this.httpRoot);
		}
	}

	@Before
    @SuppressWarnings("PMD.SignatureDeclareThrowsException")
	public final void beforeTest() throws Exception {
		Assert.assertFalse(this.httpRoot.exists());
		Assert.assertTrue(this.httpRoot.mkdirs());

		Context context = new Context(this.server, "/", Context.SESSIONS);
		context.setBaseResource(new FileResource(new URL("file://" + this.httpRoot.getAbsolutePath())));
		context.setContextPath("/");
		context.addServlet(new ServletHolder(new DefaultServlet()), "/*");

		this.server.setHandler(context);
		this.server.start();
	}

	protected final void addFile(String path, File file) throws IOException {
		File fileCopy = new File(this.httpRoot, path);
		Assert.assertFalse(fileCopy.exists());
		if (!fileCopy.getParentFile().exists()) {
			Assert.assertTrue(fileCopy.getParentFile().mkdirs());
		}

		OutputStream outputStream = new FileOutputStream(fileCopy);
		try {
			outputStream.write(FileUtils.readFileToByteArray(file));
		} finally {
			IOUtils.closeQuietly(outputStream);
		}
	}

	protected final int getPort() {
		return this.server.getConnectors()[0].getLocalPort();
	}
}
