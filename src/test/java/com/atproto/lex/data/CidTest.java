package com.atproto.lex.data;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class CidTest {

    private static Cid sampleCid() {
        byte[] digest = new byte[32];
        for (int i = 0; i < digest.length; i++) digest[i] = (byte) (i + 1);
        return new Cid(1, Cid.CODEC_DAG_CBOR, new Multihash(Cid.MH_SHA2_256, digest));
    }

    @Test
    void roundTripBase32() {
        Cid original = sampleCid();
        Cid reparsed = Cid.parse(original.toBase32());
        assertThat(reparsed).isEqualTo(original);
    }

    @Test
    void parseAndInspect() {
        Cid cid = sampleCid();
        assertThat(cid.version()).isEqualTo(1);
        assertThat(cid.codec()).isEqualTo(Cid.CODEC_DAG_CBOR);
        assertThat(cid.multihash().code()).isEqualTo(Cid.MH_SHA2_256);
        assertThat(cid.multihash().digest()).hasSize(32);
        // Verify round-trip through base32 string
        assertThat(Cid.parse(cid.toBase32())).isEqualTo(cid);
    }

    @Test
    void roundTripBytes() {
        Cid cid = sampleCid();
        byte[] bytes = cid.toBytes();
        Cid reparsed = Cid.fromBytes(bytes);
        assertThat(reparsed).isEqualTo(cid);
    }

    @Test
    void equalityAndHashCode() {
        Cid a = sampleCid();
        Cid b = sampleCid();
        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    void rejectsBadMultibasePrefix() {
        assertThatThrownBy(() -> Cid.parse("zQmabc"))
                .isInstanceOf(LexError.class)
                .hasMessageContaining("Unsupported CID multibase prefix");
    }
}
