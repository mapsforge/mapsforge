/*
 * Copyright 2025 mapsforge
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
package org.mapsforge.poi.writer.util;

public final class ArabicNormalizer {

    private static final String ARABIC_DIGITS = "٠١٢٣٤٥٦٧٨٩";
    private static final String DIGITS_REPLACEMENT = "0123456789";

    public static boolean isSpecialArabic(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        char first = text.charAt(0);
        return Character.UnicodeBlock.of(first) == Character.UnicodeBlock.ARABIC;
    }

    public static String normalize(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        String result = replaceCharacters(text);
        return replaceDigits(result);
    }

    private static String replaceCharacters(String text) {
        if (text == null || text.isEmpty()) {
            return null;
        }
        char first = text.charAt(0);
        if (Character.UnicodeBlock.of(first) != Character.UnicodeBlock.ARABIC) {
            return text;
        }

        //Remove honorific sign
        text = text.replaceAll("\u0610", "");//ARABIC SIGN SALLALLAHOU ALAYHE WA SALLAM
        text = text.replaceAll("\u0611", "");//ARABIC SIGN ALAYHE ASSALLAM
        text = text.replaceAll("\u0612", "");//ARABIC SIGN RAHMATULLAH ALAYHE
        text = text.replaceAll("\u0613", "");//ARABIC SIGN RADI ALLAHOU ANHU
        text = text.replaceAll("\u0614", "");//ARABIC SIGN TAKHALLUS

        //Remove koranic anotation
        text = text.replaceAll("\u0615", "");//ARABIC SMALL HIGH TAH
        text = text.replaceAll("\u0616", "");//ARABIC SMALL HIGH LIGATURE ALEF WITH LAM WITH YEH
        text = text.replaceAll("\u0617", "");//ARABIC SMALL HIGH ZAIN
        text = text.replaceAll("\u0618", "");//ARABIC SMALL FATHA
        text = text.replaceAll("\u0619", "");//ARABIC SMALL DAMMA
        text = text.replaceAll("\u061A", "");//ARABIC SMALL KASRA
        text = text.replaceAll("\u06D6", "");//ARABIC SMALL HIGH LIGATURE SAD WITH LAM WITH ALEF MAKSURA
        text = text.replaceAll("\u06D7", "");//ARABIC SMALL HIGH LIGATURE QAF WITH LAM WITH ALEF MAKSURA
        text = text.replaceAll("\u06D8", "");//ARABIC SMALL HIGH MEEM INITIAL FORM
        text = text.replaceAll("\u06D9", "");//ARABIC SMALL HIGH LAM ALEF
        text = text.replaceAll("\u06DA", "");//ARABIC SMALL HIGH JEEM
        text = text.replaceAll("\u06DB", "");//ARABIC SMALL HIGH THREE DOTS
        text = text.replaceAll("\u06DC", "");//ARABIC SMALL HIGH SEEN
        text = text.replaceAll("\u06DD", "");//ARABIC END OF AYAH
        text = text.replaceAll("\u06DE", "");//ARABIC START OF RUB EL HIZB
        text = text.replaceAll("\u06DF", "");//ARABIC SMALL HIGH ROUNDED ZERO
        text = text.replaceAll("\u06E0", "");//ARABIC SMALL HIGH UPRIGHT RECTANGULAR ZERO
        text = text.replaceAll("\u06E1", "");//ARABIC SMALL HIGH DOTLESS HEAD OF KHAH
        text = text.replaceAll("\u06E2", "");//ARABIC SMALL HIGH MEEM ISOLATED FORM
        text = text.replaceAll("\u06E3", "");//ARABIC SMALL LOW SEEN
        text = text.replaceAll("\u06E4", "");//ARABIC SMALL HIGH MADDA
        text = text.replaceAll("\u06E5", "");//ARABIC SMALL WAW
        text = text.replaceAll("\u06E6", "");//ARABIC SMALL YEH
        text = text.replaceAll("\u06E7", "");//ARABIC SMALL HIGH YEH
        text = text.replaceAll("\u06E8", "");//ARABIC SMALL HIGH NOON
        text = text.replaceAll("\u06E9", "");//ARABIC PLACE OF SAJDAH
        text = text.replaceAll("\u06EA", "");//ARABIC EMPTY CENTRE LOW STOP
        text = text.replaceAll("\u06EB", "");//ARABIC EMPTY CENTRE HIGH STOP
        text = text.replaceAll("\u06EC", "");//ARABIC ROUNDED HIGH STOP WITH FILLED CENTRE
        text = text.replaceAll("\u06ED", "");//ARABIC SMALL LOW MEEM

        //Remove tatweel
        text = text.replaceAll("\u0640", "");

        //Remove tashkeel
        text = text.replaceAll("\u064B", "");//ARABIC FATHATAN
        text = text.replaceAll("\u064C", "");//ARABIC DAMMATAN
        text = text.replaceAll("\u064D", "");//ARABIC KASRATAN
        text = text.replaceAll("\u064E", "");//ARABIC FATHA
        text = text.replaceAll("\u064F", "");//ARABIC DAMMA
        text = text.replaceAll("\u0650", "");//ARABIC KASRA
        text = text.replaceAll("\u0651", "");//ARABIC SHADDA
        text = text.replaceAll("\u0652", "");//ARABIC SUKUN
        text = text.replaceAll("\u0653", "");//ARABIC MADDAH ABOVE
        text = text.replaceAll("\u0654", "");//ARABIC HAMZA ABOVE
        text = text.replaceAll("\u0655", "");//ARABIC HAMZA BELOW
        text = text.replaceAll("\u0656", "");//ARABIC SUBSCRIPT ALEF
        text = text.replaceAll("\u0657", "");//ARABIC INVERTED DAMMA
        text = text.replaceAll("\u0658", "");//ARABIC MARK NOON GHUNNA
        text = text.replaceAll("\u0659", "");//ARABIC ZWARAKAY
        text = text.replaceAll("\u065A", "");//ARABIC VOWEL SIGN SMALL V ABOVE
        text = text.replaceAll("\u065B", "");//ARABIC VOWEL SIGN INVERTED SMALL V ABOVE
        text = text.replaceAll("\u065C", "");//ARABIC VOWEL SIGN DOT BELOW
        text = text.replaceAll("\u065D", "");//ARABIC REVERSED DAMMA
        text = text.replaceAll("\u065E", "");//ARABIC FATHA WITH TWO DOTS
        text = text.replaceAll("\u065F", "");//ARABIC WAVY HAMZA BELOW
        text = text.replaceAll("\u0670", "");//ARABIC LETTER SUPERSCRIPT ALEF

        //Replace Waw Hamza Above by Waw
        text = text.replaceAll("\u0624", "\u0648");

        //Replace Ta Marbuta by Ha
        text = text.replaceAll("\u0629", "\u0647");

        //Replace Ya
        // and Ya Hamza Above by Alif Maksura
        text = text.replaceAll("\u064A", "\u0649");
        text = text.replaceAll("\u0626", "\u0649");

        // Replace Alifs with Hamza Above/Below
        // and with Madda Above by Alif
        text = text.replaceAll("\u0622", "\u0627");
        text = text.replaceAll("\u0623", "\u0627");
        text = text.replaceAll("\u0625", "\u0627");

        return text;
    }

    private static String replaceDigits(String text) {
        if (text == null || text.isEmpty()) {
            return null;
        }
        char first = text.charAt(0);
        if (Character.UnicodeBlock.of(first) != Character.UnicodeBlock.ARABIC) {
            return text;
        }

        char[] textChars = text.toCharArray();
        for (int i = 0; i < ARABIC_DIGITS.length(); i++) {
            char c = ARABIC_DIGITS.charAt(i);
            char replacement = DIGITS_REPLACEMENT.charAt(i);
            int index = text.indexOf(c);
            while (index >= 0) {
                textChars[index] = replacement;
                index = text.indexOf(c, index + 1);
            }
        }
        return String.valueOf(textChars);
    }

    private ArabicNormalizer() {
    }
}
