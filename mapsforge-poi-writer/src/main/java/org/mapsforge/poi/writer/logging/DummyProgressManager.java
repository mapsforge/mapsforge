/*
 * Copyright 2010, 2011, 2012 mapsforge.org
 * Copyright 2010, 2011, 2012 Karsten Groll
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
package org.mapsforge.poi.writer.logging;

/**
 * A progress manager doing nothing when the application calls a progress update.
 */
public class DummyProgressManager implements ProgressManager {
    @Override
    public void appendLogMessage(String message, boolean isErrorMessage) {
        // empty by purpose
    }

    @Override
    public void finish() {
        // empty by purpose
    }

    @Override
    public void initProgressBar(int minVal, int maxVal) {
        // empty by purpose
    }

    @Override
    public void setMessage(String message) {
        // empty by purpose
    }

    @Override
    public void start() {
        // empty by purpose
    }

    @Override
    public void tick() {
        // empty by purpose
    }

    @Override
    public void updateProgressBar(int newVal) {
        // empty by purpose
    }
}
