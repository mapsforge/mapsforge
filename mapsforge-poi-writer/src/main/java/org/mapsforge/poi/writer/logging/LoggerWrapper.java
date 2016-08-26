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

import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

/**
 * This extension of Java's default logger provides {@link ProgressManager} integration that allows
 * forwarding log messages to a GUI. It allows simultaneous logging on an output stream as provided
 * in the base Logger class and to a GUI.
 */
public class LoggerWrapper extends Logger {
    private static ProgressManager defaultProgressManager;

    private ProgressManager pm;

    private LoggerWrapper(String name) {
        super(name, null);
    }

    /**
     * Returns or creates a logger that forwards messages to a {@link ProgressManager}. To create a
     * logger object with a progress manager use {@link #getLogger(String, ProgressManager)}
     * instead.
     *
     * @param name The logger's unique name. By default the calling class' name is used.
     * @return A logger that forwards messages to a {@link ProgressManager}.
     */
    public synchronized static LoggerWrapper getLogger(String name) {
        //System.out.println("getLogger: " + name);
        LogManager lm = LogManager.getLogManager();
        Logger l = lm.getLogger(name);

        if (l == null) {
            lm.addLogger(new LoggerWrapper(name));
            ((LoggerWrapper) lm.getLogger(name)).setProgressManager(
                    LoggerWrapper.defaultProgressManager);
        }

        return (LoggerWrapper) lm.getLogger(name);
    }

    /**
     * Returns or creates a logger that forwards messages to a {@link ProgressManager}. If the
     * logger does not yet have any progress manager assigned, the given one will be used.
     *
     * @param name The logger's unique name. By default the calling class' name is used.
     * @param pm   The logger's progress manager. This value is only used if a logger object has to
     *             be created or a logger with a given name does not yet have any progress manager
     *             assigned or if the default progress manager is used.
     * @return A logger that forwards messages to a {@link ProgressManager}.
     */
    public static LoggerWrapper getLogger(String name, ProgressManager pm) {
        LoggerWrapper ret = getLogger(name);

        if (ret.getProgressManager() == null
                || ret.getProgressManager() == LoggerWrapper.defaultProgressManager) {
            ret.setProgressManager(pm);
        }

        return ret;
    }

    private synchronized ProgressManager getProgressManager() {
        return this.pm;
    }

    @Override
    public void log(Level level, String msg) {
        super.log(level, msg);
        this.pm.appendLogMessage(msg, false);
    }

    /**
     * Sets the {@link ProgressManager} object that will be used for each <code>LoggerWrapper</code>
     * instance.
     *
     * @param pm The progress manager to be used whenever an instance of this class is created.
     */
    public static void setDefaultProgressManager(ProgressManager pm) {
        LoggerWrapper.defaultProgressManager = pm;
    }

    /**
     * This method sets the logger's progress manager that receives certain log messages and
     * forwards them to a GUI. Call this method to override the current {@link ProgressManager}.
     *
     * @param pm The progress manager to be used.
     */
    public synchronized void setProgressManager(ProgressManager pm) {
        this.pm = pm;
    }
}
