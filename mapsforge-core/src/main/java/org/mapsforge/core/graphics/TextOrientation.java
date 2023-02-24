package org.mapsforge.core.graphics;

public enum TextOrientation {

    /**
     * Automatic rotation of texts along the line with requirement to be always readable.
     */
    AUTO,

    /**
     * Same as "AUTO" just everything is upside-down.
     */
    AUTO_DOWN,

    /**
     * Rotate the texts up when the path segment (where the text is to be placed) is facing east/right.
     */
    RIGHT,

    /**
     * Rotate text up when path segment the text goes to the west/left.
     */
    LEFT;

    public static TextOrientation fromString(String value) {
        switch (value) {
            case "auto_down":
                return AUTO_DOWN;
            case "right":
                return RIGHT;
            case "left":
                return LEFT;
            default: // "auto"
                return AUTO;
        }
    }
}
