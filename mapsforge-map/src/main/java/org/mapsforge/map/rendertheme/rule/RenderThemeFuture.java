/*
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
package org.mapsforge.map.rendertheme.rule;

import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.map.model.DisplayModel;
import org.mapsforge.map.rendertheme.XmlRenderTheme;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A RenderThemeFuture implements a asynchronous parsing of an XmlRenderTheme in order to
 * move the delay caused by parsing the XML file off the user interface thread in mapsforge
 * application.
 * The RenderThemeFuture is reference counted to make it shareable between threads. Each thread
 * that uses the RenderThemeFuture to retrieve a rendertheme should first call incrementRefCount to
 * ensure that the RenderTheme does not get destroyed while the thread is waiting for execution.
 */
public class RenderThemeFuture extends FutureTask<RenderTheme> {

	private final AtomicInteger refCount = new AtomicInteger(1);

	/**
	 * Callable that performs the actual parsing of the render theme (via the RenderThemeHandler
	 * as before).
	 */
	private static class RenderThemeCallable implements Callable<RenderTheme> {
		private final GraphicFactory graphicFactory;
		private final XmlRenderTheme xmlRenderTheme;
		private final DisplayModel displayModel;

		public RenderThemeCallable(GraphicFactory graphicFactory, XmlRenderTheme xmlRenderTheme, DisplayModel displayModel) {
			this.graphicFactory = graphicFactory;
			this.xmlRenderTheme = xmlRenderTheme;
			this.displayModel = displayModel;
		}

		@Override
		public RenderTheme call() {
			if (xmlRenderTheme == null || this.displayModel == null) {
				return null;
			}
			try {
				return RenderThemeHandler.getRenderTheme(this.graphicFactory, displayModel, this.xmlRenderTheme);
			} catch (XmlPullParserException e) {
				throw new IllegalArgumentException("Parse error for XML rendertheme", e);
			} catch (IOException e) {
				throw new IllegalArgumentException("File error for XML rendertheme", e);
			}
		}
	}

	public RenderThemeFuture(GraphicFactory graphicFactory, XmlRenderTheme xmlRenderTheme, DisplayModel displayModel) {
		super(new RenderThemeCallable(graphicFactory, xmlRenderTheme, displayModel));
	}

	public void decrementRefCount() {
		int c = this.refCount.decrementAndGet();
		if (c <= 0) {
			try {
				if (this.isDone()) {
					get().destroy();
				} else {
					cancel(true);
				}
			} catch (Exception e) {
				// just cleaning up
			}
		}
	}

	public void incrementRefCount() {
		this.refCount.incrementAndGet();
	}
}
