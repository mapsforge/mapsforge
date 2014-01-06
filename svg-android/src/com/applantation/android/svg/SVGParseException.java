package com.applantation.android.svg;

import java.io.IOException;

/**
 * Runtime exception thrown when there is a problem parsing an SVG.
 *
 * @author Larva Labs, LLC
 */
public class SVGParseException extends IOException {

    public SVGParseException(String s) {
        super(s);
    }

    public SVGParseException(String s, Throwable throwable) {
        super(s, throwable);
    }

    public SVGParseException(Throwable throwable) {
        super(throwable);
    }
}
