package org.mapsforge.core.graphics;

public enum Upright {

    /**
     * Automatic map rotation.
     */
    AUTO,

    /**
     * Rotate the text up when the path segment (where the text is to be placed) is facing east. / right.
     */
    RIGHT,

    /**
     * Rotate text up when path segment the text goes to the west/left.
     */
    LEFT;

    public static Upright fromString(String value) {
        if (value.equals("right")) {
            return RIGHT;
        } else if (value.equals("left")) {
            return LEFT;
        } else {
            return AUTO;
        }
    }
}
