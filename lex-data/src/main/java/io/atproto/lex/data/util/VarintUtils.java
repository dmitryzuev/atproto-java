package io.atproto.lex.data.util;

import io.atproto.lex.data.LexError;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public final class VarintUtils {

    private VarintUtils() {}

    public static void writeUvarint(OutputStream out, long value) throws IOException {
        while ((value & ~0x7FL) != 0) {
            out.write((int) ((value & 0x7F) | 0x80));
            value >>>= 7;
        }
        out.write((int) value);
    }

    public static long readUvarint(InputStream in) throws IOException {
        long result = 0;
        int shift = 0;
        while (true) {
            int b = in.read();
            if (b == -1) throw new LexError("Unexpected end of stream reading varint");
            result |= (long) (b & 0x7F) << shift;
            if ((b & 0x80) == 0) return result;
            shift += 7;
            if (shift >= 64) throw new LexError("Varint too long");
        }
    }
}
