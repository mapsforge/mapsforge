package org.mapsforge.map.rendertheme;

import java.io.IOException;
import java.io.InputStream;

/**
 * Interface for a provider of resources referenced inside XML themes
 */
public interface XmlThemeResourceProvider {

    /**
     * @param source a source string parsed out of an XML render theme "src" attribute.
     * @return an InputStream to read the resource data from.
     * @throws IOException if the resource cannot be found or an access error occurred.
     */
    InputStream createInputStream(final String source) throws IOException;
}
