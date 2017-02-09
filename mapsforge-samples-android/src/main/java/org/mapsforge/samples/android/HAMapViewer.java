/*
 * Copyright 2017 devemux86
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
package org.mapsforge.samples.android;

import org.mapsforge.map.controller.FrameBufferController;

/**
 * Viewer using the hardware accelerated FrameBuffer.
 * <p>
 * Enable hardware acceleration in Settings!
 */
public class HAMapViewer extends DefaultTheme {

    @Override
    protected void createMapViews() {
        FrameBufferController.HA_FRAME_BUFFER = true;

        super.createMapViews();
    }
}
