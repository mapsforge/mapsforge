package org.mapsforge.map.writer.util;

import java.util.HashMap;

/**
 * Colors from http://www.w3.org/TR/css3-color
 */
public class ColorsCSS {

    static HashMap<String, Integer> sColors;

    public static Integer get(String name) {
        if (sColors == null)
            init();

        return sColors.get(name);
    }

    static void init() {
        sColors = new HashMap<>();

        sColors.put("aliceblue", Integer.valueOf(0xFFF0F8FF));
        sColors.put("antiquewhite", Integer.valueOf(0xFFFAEBD7));
        sColors.put("aqua", Integer.valueOf(0xFF00FFFF));
        sColors.put("aquamarine", Integer.valueOf(0xFF7FFFD4));
        sColors.put("azure", Integer.valueOf(0xFFF0FFFF));
        sColors.put("beige", Integer.valueOf(0xFFF5F5DC));
        sColors.put("bisque", Integer.valueOf(0xFFFFE4C4));
        sColors.put("black", Integer.valueOf(0xFF000000));
        sColors.put("blanchedalmond", Integer.valueOf(0xFFFFEBCD));
        sColors.put("blue", Integer.valueOf(0xFF0000FF));
        sColors.put("blueviolet", Integer.valueOf(0xFF8A2BE2));
        sColors.put("brown", Integer.valueOf(0xFFA52A2A));
        sColors.put("burlywood", Integer.valueOf(0xFFDEB887));
        sColors.put("cadetblue", Integer.valueOf(0xFF5F9EA0));
        sColors.put("chartreuse", Integer.valueOf(0xFF7FFF00));
        sColors.put("chocolate", Integer.valueOf(0xFFD2691E));
        sColors.put("coral", Integer.valueOf(0xFFFF7F50));
        sColors.put("cornflowerblue", Integer.valueOf(0xFF6495ED));
        sColors.put("cornsilk", Integer.valueOf(0xFFFFF8DC));
        sColors.put("crimson", Integer.valueOf(0xFFDC143C));
        sColors.put("cyan", Integer.valueOf(0xFF00FFFF));
        sColors.put("darkblue", Integer.valueOf(0xFF00008B));
        sColors.put("darkcyan", Integer.valueOf(0xFF008B8B));
        sColors.put("darkgoldenrod", Integer.valueOf(0xFFB8860B));
        sColors.put("darkgray", Integer.valueOf(0xFFA9A9A9));
        sColors.put("darkgreen", Integer.valueOf(0xFF006400));
        sColors.put("darkgrey", Integer.valueOf(0xFFA9A9A9));
        sColors.put("darkkhaki", Integer.valueOf(0xFFBDB76B));
        sColors.put("darkmagenta", Integer.valueOf(0xFF8B008B));
        sColors.put("darkolivegreen", Integer.valueOf(0xFF556B2F));
        sColors.put("darkorange", Integer.valueOf(0xFFFF8C00));
        sColors.put("darkorchid", Integer.valueOf(0xFF9932CC));
        sColors.put("darkred", Integer.valueOf(0xFF8B0000));
        sColors.put("darksalmon", Integer.valueOf(0xFFE9967A));
        sColors.put("darkseagreen", Integer.valueOf(0xFF8FBC8F));
        sColors.put("darkslateblue", Integer.valueOf(0xFF483D8B));
        sColors.put("darkslategray", Integer.valueOf(0xFF2F4F4F));
        sColors.put("darkslategrey", Integer.valueOf(0xFF2F4F4F));
        sColors.put("darkturquoise", Integer.valueOf(0xFF00CED1));
        sColors.put("darkviolet", Integer.valueOf(0xFF9400D3));
        sColors.put("deeppink", Integer.valueOf(0xFFFF1493));
        sColors.put("deepskyblue", Integer.valueOf(0xFF00BFFF));
        sColors.put("dimgray", Integer.valueOf(0xFF696969));
        sColors.put("dimgrey", Integer.valueOf(0xFF696969));
        sColors.put("dodgerblue", Integer.valueOf(0xFF1E90FF));
        sColors.put("firebrick", Integer.valueOf(0xFFB22222));
        sColors.put("floralwhite", Integer.valueOf(0xFFFFFAF0));
        sColors.put("forestgreen", Integer.valueOf(0xFF228B22));
        sColors.put("fuchsia", Integer.valueOf(0xFFFF00FF));
        sColors.put("gainsboro", Integer.valueOf(0xFFDCDCDC));
        sColors.put("ghostwhite", Integer.valueOf(0xFFF8F8FF));
        sColors.put("gold", Integer.valueOf(0xFFFFD700));
        sColors.put("goldenrod", Integer.valueOf(0xFFDAA520));
        sColors.put("gray", Integer.valueOf(0xFF808080));
        sColors.put("green", Integer.valueOf(0xFF008000));
        sColors.put("greenyellow", Integer.valueOf(0xFFADFF2F));
        sColors.put("grey", Integer.valueOf(0xFF808080));
        sColors.put("honeydew", Integer.valueOf(0xFFF0FFF0));
        sColors.put("hotpink", Integer.valueOf(0xFFFF69B4));
        sColors.put("indianred", Integer.valueOf(0xFFCD5C5C));
        sColors.put("indigo", Integer.valueOf(0xFF4B0082));
        sColors.put("ivory", Integer.valueOf(0xFFFFFFF0));
        sColors.put("khaki", Integer.valueOf(0xFFF0E68C));
        sColors.put("lavender", Integer.valueOf(0xFFE6E6FA));
        sColors.put("lavenderblush", Integer.valueOf(0xFFFFF0F5));
        sColors.put("lawngreen", Integer.valueOf(0xFF7CFC00));
        sColors.put("lemonchiffon", Integer.valueOf(0xFFFFFACD));
        sColors.put("lightblue", Integer.valueOf(0xFFADD8E6));
        sColors.put("lightcoral", Integer.valueOf(0xFFF08080));
        sColors.put("lightcyan", Integer.valueOf(0xFFE0FFFF));
        sColors.put("lightgoldenrodyellow", Integer.valueOf(0xFFFAFAD2));
        sColors.put("lightgray", Integer.valueOf(0xFFD3D3D3));
        sColors.put("lightgreen", Integer.valueOf(0xFF90EE90));
        sColors.put("lightgrey", Integer.valueOf(0xFFD3D3D3));
        sColors.put("lightpink", Integer.valueOf(0xFFFFB6C1));
        sColors.put("lightsalmon", Integer.valueOf(0xFFFFA07A));
        sColors.put("lightseagreen", Integer.valueOf(0xFF20B2AA));
        sColors.put("lightskyblue", Integer.valueOf(0xFF87CEFA));
        sColors.put("lightslategray", Integer.valueOf(0xFF778899));
        sColors.put("lightslategrey", Integer.valueOf(0xFF778899));
        sColors.put("lightsteelblue", Integer.valueOf(0xFFB0C4DE));
        sColors.put("lightyellow", Integer.valueOf(0xFFFFFFE0));
        sColors.put("lime", Integer.valueOf(0xFF00FF00));
        sColors.put("limegreen", Integer.valueOf(0xFF32CD32));
        sColors.put("linen", Integer.valueOf(0xFFFAF0E6));
        sColors.put("magenta", Integer.valueOf(0xFFFF00FF));
        sColors.put("maroon", Integer.valueOf(0xFF800000));
        sColors.put("mediumaquamarine", Integer.valueOf(0xFF66CDAA));
        sColors.put("mediumblue", Integer.valueOf(0xFF0000CD));
        sColors.put("mediumorchid", Integer.valueOf(0xFFBA55D3));
        sColors.put("mediumpurple", Integer.valueOf(0xFF9370DB));
        sColors.put("mediumseagreen", Integer.valueOf(0xFF3CB371));
        sColors.put("mediumslateblue", Integer.valueOf(0xFF7B68EE));
        sColors.put("mediumspringgreen", Integer.valueOf(0xFF00FA9A));
        sColors.put("mediumturquoise", Integer.valueOf(0xFF48D1CC));
        sColors.put("mediumvioletred", Integer.valueOf(0xFFC71585));
        sColors.put("midnightblue", Integer.valueOf(0xFF191970));
        sColors.put("mintcream", Integer.valueOf(0xFFF5FFFA));
        sColors.put("mistyrose", Integer.valueOf(0xFFFFE4E1));
        sColors.put("moccasin", Integer.valueOf(0xFFFFE4B5));
        sColors.put("navajowhite", Integer.valueOf(0xFFFFDEAD));
        sColors.put("navy", Integer.valueOf(0xFF000080));
        sColors.put("oldlace", Integer.valueOf(0xFFFDF5E6));
        sColors.put("olive", Integer.valueOf(0xFF808000));
        sColors.put("olivedrab", Integer.valueOf(0xFF6B8E23));
        sColors.put("orange", Integer.valueOf(0xFFFFA500));
        sColors.put("orangered", Integer.valueOf(0xFFFF4500));
        sColors.put("orchid", Integer.valueOf(0xFFDA70D6));
        sColors.put("palegoldenrod", Integer.valueOf(0xFFEEE8AA));
        sColors.put("palegreen", Integer.valueOf(0xFF98FB98));
        sColors.put("paleturquoise", Integer.valueOf(0xFFAFEEEE));
        sColors.put("palevioletred", Integer.valueOf(0xFFDB7093));
        sColors.put("papayawhip", Integer.valueOf(0xFFFFEFD5));
        sColors.put("peachpuff", Integer.valueOf(0xFFFFDAB9));
        sColors.put("peru", Integer.valueOf(0xFFCD853F));
        sColors.put("pink", Integer.valueOf(0xFFFFC0CB));
        sColors.put("plum", Integer.valueOf(0xFFDDA0DD));
        sColors.put("powderblue", Integer.valueOf(0xFFB0E0E6));
        sColors.put("purple", Integer.valueOf(0xFF800080));
        sColors.put("red", Integer.valueOf(0xFFFF0000));
        sColors.put("rosybrown", Integer.valueOf(0xFFBC8F8F));
        sColors.put("royalblue", Integer.valueOf(0xFF4169E1));
        sColors.put("saddlebrown", Integer.valueOf(0xFF8B4513));
        sColors.put("salmon", Integer.valueOf(0xFFFA8072));
        sColors.put("sandybrown", Integer.valueOf(0xFFF4A460));
        sColors.put("seagreen", Integer.valueOf(0xFF2E8B57));
        sColors.put("seashell", Integer.valueOf(0xFFFFF5EE));
        sColors.put("sienna", Integer.valueOf(0xFFA0522D));
        sColors.put("silver", Integer.valueOf(0xFFC0C0C0));
        sColors.put("skyblue", Integer.valueOf(0xFF87CEEB));
        sColors.put("slateblue", Integer.valueOf(0xFF6A5ACD));
        sColors.put("slategray", Integer.valueOf(0xFF708090));
        sColors.put("slategrey", Integer.valueOf(0xFF708090));
        sColors.put("snow", Integer.valueOf(0xFFFFFAFA));
        sColors.put("springgreen", Integer.valueOf(0xFF00FF7F));
        sColors.put("steelblue", Integer.valueOf(0xFF4682B4));
        sColors.put("tan", Integer.valueOf(0xFFD2B48C));
        sColors.put("teal", Integer.valueOf(0xFF008080));
        sColors.put("thistle", Integer.valueOf(0xFFD8BFD8));
        sColors.put("tomato", Integer.valueOf(0xFFFF6347));
        sColors.put("turquoise", Integer.valueOf(0xFF40E0D0));
        sColors.put("violet", Integer.valueOf(0xFFEE82EE));
        sColors.put("wheat", Integer.valueOf(0xFFF5DEB3));
        sColors.put("white", Integer.valueOf(0xFFFFFFFF));
        sColors.put("whitesmoke", Integer.valueOf(0xFFF5F5F5));
        sColors.put("yellow", Integer.valueOf(0xFFFFFF00));
        sColors.put("yellowgreen", Integer.valueOf(0xFF9ACD32));
    }
}
