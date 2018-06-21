/*
 * Copyright 2016 devemux86
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
package com.ikroshlab.ovtencoder;

import java.util.Locale;

public final class MapFileUtils {

    /**
     * Extracts substring of preferred language from multilingual string.<br/>
     * Example multilingual string: "Base\ren\bEnglish\rjp\bJapan\rzh_py\bPin-yin".
     * <p/>
     * Use '\r' delimiter among names and '\b' delimiter between each language and name.
     */
    public static String extract(String s, String language) {
        if (s == null || s.trim().isEmpty()) {
            return null;
        }

        String[] langNames = s.split("\r");
        if (language == null || language.trim().isEmpty()) {
            return langNames[0];
        }

        String fallback = null;
        for (int i = 1; i < langNames.length; i++) {
            String[] langName = langNames[i].split("\b");
            if (langName.length != 2) {
                continue;
            }

            // Perfect match
            if (langName[0].equalsIgnoreCase(language)) {
                return langName[1];
            }

            // Fall back to base, e.g. zh-min-lan -> zh
            if (fallback == null && !langName[0].contains("-") && (language.contains("-") || language.contains("_"))
                    && language.toLowerCase(Locale.ENGLISH).startsWith(langName[0].toLowerCase(Locale.ENGLISH))) {
                fallback = langName[1];
            }
        }
        return (fallback != null) ? fallback : langNames[0];
    }

    private MapFileUtils() {
    }
}
