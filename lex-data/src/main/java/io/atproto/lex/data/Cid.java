package io.atproto.lex.data;

import io.atproto.lex.data.util.Base64Utils;
import io.atproto.lex.data.util.VarintUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

/**
 * CIDv1 content identifier as used in the AT Protocol.
 * Wire format: [version-varint][codec-varint][mh-code-varint][mh-len-varint][digest]
 * String format: multibase 'b' + base32-lower (RFC 4648, no padding).
 */
public record Cid(int version, int codec, Multihash multihash) {

    /** dag-cbor codec code */
    public static final int CODEC_DAG_CBOR = 0x71;
    /** raw codec code */
    public static final int CODEC_RAW = 0x55;
    /** SHA-256 multihash code */
    public static final int MH_SHA2_256 = 0x12;

    /**
     * Parse a base32-multibase CID string (starts with 'b').
     */
    public static Cid parse(String cidString) {
        if (cidString == null || cidString.isEmpty()) {
            throw new LexError("CID string is empty");
        }
        char prefix = cidString.charAt(0);
        if (prefix != 'b' && prefix != 'B') {
            throw new LexError("Unsupported CID multibase prefix: '" + prefix + "' (only base32-lower 'b' is supported)");
        }
        byte[] bytes = base32Decode(cidString.substring(1));
        return fromBytes(bytes);
    }

    /**
     * Parse CIDv1 from its raw byte representation.
     */
    public static Cid fromBytes(byte[] bytes) {
        try {
            ByteArrayInputStream in = new ByteArrayInputStream(bytes);
            long version = VarintUtils.readUvarint(in);
            if (version != 1) {
                throw new LexError("Only CIDv1 is supported, got version: " + version);
            }
            long codec = VarintUtils.readUvarint(in);
            long mhCode = VarintUtils.readUvarint(in);
            long mhLen = VarintUtils.readUvarint(in);
            if (mhLen > 128) {
                throw new LexError("Multihash digest too large: " + mhLen);
            }
            byte[] digest = in.readNBytes((int) mhLen);
            if (digest.length != (int) mhLen) {
                throw new LexError("Truncated CID bytes");
            }
            return new Cid((int) version, (int) codec, new Multihash((int) mhCode, digest));
        } catch (IOException e) {
            throw new LexError("Failed to parse CID bytes", e);
        }
    }

    /**
     * Serialize this CID to its raw byte representation.
     */
    public byte[] toBytes() {
        ByteArrayOutputStream out = new ByteArrayOutputStream(36);
        try {
            VarintUtils.writeUvarint(out, version);
            VarintUtils.writeUvarint(out, codec);
            VarintUtils.writeUvarint(out, multihash.code());
            VarintUtils.writeUvarint(out, multihash.digest().length);
            out.write(multihash.digest());
        } catch (IOException e) {
            throw new LexError("Failed to serialize CID", e);
        }
        return out.toByteArray();
    }

    /**
     * Encode to multibase base32-lower string with 'b' prefix.
     */
    public String toBase32() {
        return "b" + base32Encode(toBytes());
    }

    @Override
    public String toString() {
        return toBase32();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Cid other)) return false;
        return version == other.version
                && codec == other.codec
                && multihash.equals(other.multihash);
    }

    @Override
    public int hashCode() {
        return 31 * (31 * version + codec) + multihash.hashCode();
    }

    // ---- base32 (RFC 4648, lowercase, no padding) ----

    private static final String BASE32_ALPHABET = "abcdefghijklmnopqrstuvwxyz234567";
    private static final int[] BASE32_DECODE_TABLE = new int[128];

    static {
        Arrays.fill(BASE32_DECODE_TABLE, -1);
        for (int i = 0; i < BASE32_ALPHABET.length(); i++) {
            BASE32_DECODE_TABLE[BASE32_ALPHABET.charAt(i)] = i;
            // also accept uppercase
            BASE32_DECODE_TABLE[Character.toUpperCase(BASE32_ALPHABET.charAt(i))] = i;
        }
    }

    static String base32Encode(byte[] data) {
        if (data.length == 0) return "";
        StringBuilder sb = new StringBuilder((data.length * 8 + 4) / 5);
        int buffer = 0;
        int bitsLeft = 0;
        for (byte b : data) {
            buffer = (buffer << 8) | (b & 0xFF);
            bitsLeft += 8;
            while (bitsLeft >= 5) {
                bitsLeft -= 5;
                sb.append(BASE32_ALPHABET.charAt((buffer >> bitsLeft) & 0x1F));
            }
        }
        if (bitsLeft > 0) {
            sb.append(BASE32_ALPHABET.charAt((buffer << (5 - bitsLeft)) & 0x1F));
        }
        return sb.toString();
    }

    static byte[] base32Decode(String encoded) {
        if (encoded.isEmpty()) return new byte[0];
        int outputLen = encoded.length() * 5 / 8;
        byte[] out = new byte[outputLen];
        int buffer = 0;
        int bitsLeft = 0;
        int idx = 0;
        for (char c : encoded.toCharArray()) {
            if (c >= BASE32_DECODE_TABLE.length || BASE32_DECODE_TABLE[c] == -1) {
                throw new LexError("Invalid base32 character: '" + c + "'");
            }
            buffer = (buffer << 5) | BASE32_DECODE_TABLE[c];
            bitsLeft += 5;
            if (bitsLeft >= 8) {
                bitsLeft -= 8;
                out[idx++] = (byte) ((buffer >> bitsLeft) & 0xFF);
            }
        }
        return idx == outputLen ? out : Arrays.copyOf(out, idx);
    }
}
