package org.mapsforge.core.graphics;

public enum SymbolOrientation {

    /**
     * Automatic rotation of symbols along the line with requirement to be always readable.
     */
    AUTO,

    /**
     * Same as "AUTO" just everything is upside-down.
     */
    AUTO_DOWN,

    /**
     * Rotate the symbols up when the path segment (where the text is to be placed) is facing east/right.
     */
    RIGHT,

    /**
     * Rotate symbol up when path segment the text goes to the west/left.
     */
    LEFT,

    /**
     * All symbols are rotated always "up".
     */
    UP,

    /**
     * All symbols are rotated always "down".
     */
    DOWN;

    public static SymbolOrientation fromString(String value) {
        switch (value) {
            case "auto_down":
                return AUTO_DOWN;
            case "right":
                return RIGHT;
            case "left":
                return LEFT;
            case "up":
                return UP;
            case "down":
                return DOWN;
            default: // "auto"
                return AUTO;
        }
    }
}
