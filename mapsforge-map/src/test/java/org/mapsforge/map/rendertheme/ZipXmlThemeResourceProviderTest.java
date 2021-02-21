/*
 * Copyright 2021 eddiemuc
 * Copyright 2021 devemux86
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
package org.mapsforge.map.rendertheme;

import org.junit.Assert;
import org.junit.Test;
import org.mapsforge.core.util.IOUtils;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.zip.ZipInputStream;

public class ZipXmlThemeResourceProviderTest {

    @Test
    public void openZip() throws IOException {
        ZipInputStream zis = new ZipInputStream(new BufferedInputStream(ZipXmlThemeResourceProviderTest.class.getResourceAsStream("/xmlthemetest.zip")));
        Assert.assertNotNull(zis);

        ZipXmlThemeResourceProvider zts = new ZipXmlThemeResourceProvider(zis);

        // All files contained
        Assert.assertNotNull(zts.createInputStream(null, "file:one.xml"));
        Assert.assertNotNull(zts.createInputStream(null, "file:two.xml"));
        Assert.assertNotNull(zts.createInputStream(null, "file:res/three.xml"));
        Assert.assertNotNull(zts.createInputStream(null, "file:res/blue_star_1.svg"));
        Assert.assertNotNull(zts.createInputStream(null, "file:res/test.txt"));
        Assert.assertNotNull(zts.createInputStream(null, "file:res/sub/four.xml"));
        Assert.assertNotNull(zts.createInputStream(null, "file:res/sub/blue_star_sub_1.svg"));
        Assert.assertNotNull(zts.createInputStream(null, "file:res/sub/blue_star_sub_2.svg"));

        //Relative Reference ok
        Assert.assertNotNull(zts.createInputStream("", "file:res/sub/blue_star_sub_2.svg"));
        Assert.assertNotNull(zts.createInputStream("res", "file:sub/blue_star_sub_2.svg"));
        Assert.assertNotNull(zts.createInputStream("/", "file:res/sub/blue_star_sub_2.svg"));
        Assert.assertNotNull(zts.createInputStream("/res", "file:sub/blue_star_sub_2.svg"));
        Assert.assertNotNull(zts.createInputStream("res/", "file:/sub/blue_star_sub_2.svg"));

        // Can get same files using various other formats
        Assert.assertNotNull(zts.createInputStream(null, "res/sub/blue_star_sub_2.svg"));
        Assert.assertNotNull(zts.createInputStream(null, "/res/sub/blue_star_sub_2.svg"));
        Assert.assertNotNull(zts.createInputStream(null, "file:/res/sub/blue_star_sub_2.svg"));

        // Dirs NOT contained!
        Assert.assertNull(zts.createInputStream(null, "file:res/"));

        Assert.assertEquals(8, zts.getCount());

        List<String> xmlThemes = zts.getXmlThemes();
        Assert.assertEquals(4, xmlThemes.size());
        Assert.assertTrue(xmlThemes.contains("one.xml"));
        Assert.assertTrue(xmlThemes.contains("two.xml"));
        Assert.assertTrue(xmlThemes.contains("res/three.xml"));
        Assert.assertTrue(xmlThemes.contains("res/sub/four.xml"));

        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(zts.createInputStream(null, "file:res/test.txt")));
            String line = reader.readLine();
            Assert.assertEquals(line, "This is a test");
        } finally {
            IOUtils.closeQuietly(reader);
        }
    }

    @Test
    public void openEmpty() throws IOException {
        Assert.assertTrue(new ZipXmlThemeResourceProvider(null).getXmlThemes().isEmpty());
    }

    @Test
    public void scanZipForXmlThemes() throws IOException {
        ZipInputStream zis = new ZipInputStream(new BufferedInputStream(ZipXmlThemeResourceProviderTest.class.getResourceAsStream("/xmlthemetest.zip")));
        Assert.assertNotNull(zis);

        List<String> xmlThemes = ZipXmlThemeResourceProvider.scanXmlThemes(zis);

        Assert.assertEquals(4, xmlThemes.size());
        Assert.assertTrue(xmlThemes.contains("one.xml"));
        Assert.assertTrue(xmlThemes.contains("two.xml"));
        Assert.assertTrue(xmlThemes.contains("res/three.xml"));
        Assert.assertTrue(xmlThemes.contains("res/sub/four.xml"));
    }
}
