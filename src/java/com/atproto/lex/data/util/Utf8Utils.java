package com.atproto.lex.data.util;

import java.nio.charset.StandardCharsets;
import java.text.BreakIterator;

public final class Utf8Utils {

    private Utf8Utils() {}

    public static int byteLength(String s) {
        return s.getBytes(StandardCharsets.UTF_8).length;
    }

    /**
     * Count Unicode grapheme clusters (user-visible characters).
     * Uses java.text.BreakIterator for locale-independent grapheme segmentation.
     */
    public static int graphemeLength(String s) {
        if (s.isEmpty()) return 0;
        BreakIterator it = BreakIterator.getCharacterInstance();
        it.setText(s);
        int count = 0;
        while (it.next() != BreakIterator.DONE) {
            count++;
        }
        return count;
    }

    public static String fromBytes(byte[] bytes) {
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public static byte[] toBytes(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }
}
