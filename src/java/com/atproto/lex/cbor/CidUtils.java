package com.atproto.lex.cbor;

import com.atproto.lex.data.Cid;
import com.atproto.lex.data.LexError;
import com.atproto.lex.data.Multihash;
import com.atproto.lex.data.LexValue;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Generates CIDs from LexValue using DAG-CBOR encoding + SHA-256 hashing.
 */
public final class CidUtils {

    private final CborCodec codec;

    public CidUtils() {
        this.codec = new CborCodec();
    }

    /**
     * Compute the CIDv1 (dag-cbor + sha2-256) for the given LexValue.
     */
    public Cid cidForLex(LexValue value) {
        byte[] cborBytes = codec.encode(value);
        return cidForCborBytes(cborBytes);
    }

    /**
     * Compute a CIDv1 dag-cbor CID for already-encoded CBOR bytes.
     */
    public Cid cidForCborBytes(byte[] cborBytes) {
        byte[] digest = sha256(cborBytes);
        return cidForSha256Digest(digest);
    }

    /**
     * Build a CIDv1 (dag-cbor) from a raw SHA-256 digest.
     */
    public static Cid cidForSha256Digest(byte[] digest) {
        if (digest.length != 32) {
            throw new LexError("SHA-256 digest must be 32 bytes, got " + digest.length);
        }
        // multihash: [code=0x12][len=0x20][...32-byte-digest]
        Multihash mh = new Multihash(Cid.MH_SHA2_256, digest);
        return new Cid(1, Cid.CODEC_DAG_CBOR, mh);
    }

    private static byte[] sha256(byte[] input) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(input);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is mandated by the Java spec — this should never happen
            throw new LexError("SHA-256 not available", e);
        }
    }
}
