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
package org.mapsforge.map.model.common;

import org.junit.Assert;
import org.junit.Test;

public class ObservableTest {
	private static void verifyAddObserverInvalid(Observable observable, Observer observer) {
		try {
			observable.addObserver(observer);
			Assert.fail("observer: " + observer);
		} catch (IllegalArgumentException e) {
			Assert.assertTrue(true);
		}
	}

	private static void verifyRemoveObserverInvalid(Observable observable, Observer observer) {
		try {
			observable.removeObserver(observer);
			Assert.fail("observer: " + observer);
		} catch (IllegalArgumentException e) {
			Assert.assertTrue(true);
		}
	}

	@Test
	public void addRemoveObserverTest() {
		DummyObserver dummyObserver = new DummyObserver();
		Observable observable = new Observable();

		observable.addObserver(dummyObserver);
		verifyAddObserverInvalid(observable, null);
		verifyAddObserverInvalid(observable, dummyObserver);

		observable.removeObserver(dummyObserver);
		verifyRemoveObserverInvalid(observable, null);
		verifyRemoveObserverInvalid(observable, dummyObserver);
	}
}
