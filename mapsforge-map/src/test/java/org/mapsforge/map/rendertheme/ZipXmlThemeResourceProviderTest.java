package org.mapsforge.map.rendertheme;

import org.junit.Assert;
import org.junit.Test;
import org.mapsforge.core.util.IOUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.zip.ZipInputStream;

public class ZipXmlThemeResourceProviderTest {

    @Test
    public void openZip() throws IOException {
        final ZipInputStream zis = new ZipInputStream(ZipXmlThemeResourceProviderTest.class.getResourceAsStream("/xmlthemetest.zip"));
        Assert.assertNotNull(zis);

        final ZipXmlThemeResourceProvider zts = new ZipXmlThemeResourceProvider(zis);

        //all files contained
        Assert.assertNotNull(zts.createInputStream("file:one.xml"));
        Assert.assertNotNull(zts.createInputStream("file:two.xml"));
        Assert.assertNotNull(zts.createInputStream("file:res/three.xml"));
        Assert.assertNotNull(zts.createInputStream("file:res/blue_star_1.svg"));
        Assert.assertNotNull(zts.createInputStream("file:res/test.txt"));
        Assert.assertNotNull(zts.createInputStream("file:res/sub/four.xml"));
        Assert.assertNotNull(zts.createInputStream("file:res/sub/blue_star_sub_1.svg"));
        Assert.assertNotNull(zts.createInputStream("file:res/sub/blue_star_sub_2.svg"));

        //can get same files using various other formats
        Assert.assertNotNull(zts.createInputStream("res/sub/blue_star_sub_2.svg"));
        Assert.assertNotNull(zts.createInputStream("/res/sub/blue_star_sub_2.svg"));
        Assert.assertNotNull(zts.createInputStream("file:/res/sub/blue_star_sub_2.svg"));


        //dirs NOT contained!
        Assert.assertNull(zts.createInputStream("file:res/"));

        Assert.assertTrue(zts.getSize() == 8);


        final List<String> xmlThemes = zts.getXmlThemes();
        Assert.assertTrue(xmlThemes.size()==4);
        Assert.assertTrue(xmlThemes.contains("one.xml"));
        Assert.assertTrue(xmlThemes.contains("two.xml"));
        Assert.assertTrue(xmlThemes.contains("res/three.xml"));
        Assert.assertTrue(xmlThemes.contains("res/sub/four.xml"));

        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(zts.createInputStream("file:res/test.txt")));
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
}
