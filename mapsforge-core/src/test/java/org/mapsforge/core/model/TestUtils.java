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
package org.mapsforge.core.model;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.junit.Assert;
import org.mapsforge.core.util.IOUtils;

final class TestUtils {
	static void equalsTest(Object object1, Object object2) {
		Assert.assertEquals(object1, object1);
		Assert.assertEquals(object2, object2);

		Assert.assertEquals(object1.hashCode(), object2.hashCode());
		Assert.assertEquals(object1, object2);
		Assert.assertEquals(object2, object1);
	}

	static <T extends Comparable<T>> void notCompareToTest(T comparable1, T comparable2) {
		Assert.assertNotEquals(0, comparable1.compareTo(comparable2));
		Assert.assertNotEquals(0, comparable2.compareTo(comparable1));
	}

	static void notEqualsTest(Object object1, Object object2) {
		Assert.assertNotEquals(object1, object2);
		Assert.assertNotEquals(object2, object1);
	}

	static void serializeTest(Object objectToSerialize) throws IOException, ClassNotFoundException {
		File file = new File("object.ser");
        boolean fileNotDeleted = false; // to keep PMD happy no throw in finally
		try {
			Assert.assertTrue(file.createNewFile());
			Assert.assertEquals(0, file.length());

			serializeObject(objectToSerialize, file);
			Object deserializedObject = deserializeObject(file);
			TestUtils.equalsTest(objectToSerialize, deserializedObject);
		} finally {
			if (file.exists() && !file.delete()) {
				fileNotDeleted = true;
			}
		}
        if (fileNotDeleted) {
            throw new IOException("could not delete file: " + file);
        }
	}

	private static Object deserializeObject(File file) throws IOException, ClassNotFoundException {
		FileInputStream fileInputStream = null;
		ObjectInputStream objectInputStream = null;
		try {
			fileInputStream = new FileInputStream(file);
			objectInputStream = new ObjectInputStream(fileInputStream);
			return objectInputStream.readObject();
		} finally {
			IOUtils.closeQuietly(objectInputStream);
			IOUtils.closeQuietly(fileInputStream);
		}
	}

	private static void serializeObject(Object objectToSerialize, File file) throws IOException {
		FileOutputStream fileOutputStream = null;
		ObjectOutputStream objectOutputStream = null;
		try {
			fileOutputStream = new FileOutputStream(file);
			objectOutputStream = new ObjectOutputStream(fileOutputStream);
			objectOutputStream.writeObject(objectToSerialize);
		} finally {
			IOUtils.closeQuietly(objectOutputStream);
			IOUtils.closeQuietly(fileOutputStream);
		}
	}

	private TestUtils() {
		throw new IllegalStateException();
	}
}
