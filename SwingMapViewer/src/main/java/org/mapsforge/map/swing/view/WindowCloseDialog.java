/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright 2014 devemux86
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
package org.mapsforge.map.swing.view;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.WindowConstants;

import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.model.common.PreferencesFacade;

public class WindowCloseDialog extends WindowAdapter {
	private static final String MESSAGE = "Are you sure you want to exit the application?";
	private static final String TITLE = "Confirm close";

	private final JFrame jFrame;
	private final MapView mapView;
	private final PreferencesFacade preferencesFacade;
	private final TileCache tileCache;

	public WindowCloseDialog(JFrame jFrame, MapView mapView, PreferencesFacade preferencesFacade, TileCache tileCache) {
		super();

		this.jFrame = jFrame;
		this.mapView = mapView;
		this.preferencesFacade = preferencesFacade;
		this.tileCache = tileCache;

		jFrame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
	}

	@Override
	public void windowClosing(WindowEvent windowEvent) {
		int result = JOptionPane.showConfirmDialog(this.jFrame, MESSAGE, TITLE, JOptionPane.YES_NO_OPTION);

		if (result == JOptionPane.YES_OPTION) {
			this.mapView.getModel().save(this.preferencesFacade);
			this.tileCache.destroy();
			this.mapView.destroy();
			this.jFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		}
	}
}
