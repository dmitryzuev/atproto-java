package io.atproto.lex.data;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class CidTest {

    // Known CIDv1 base32 string from the AT Protocol test suite
    private static final String KNOWN_CID = "bafyreih7a2zjxm4vq4e6pmgbpnfhz7ywkf7hmqzz4z5ssvvoyv5d6yqiq";

    @Test
    void roundTripBase32() {
        Cid cid = Cid.parse(KNOWN_CID);
        assertThat(cid.toBase32()).isEqualTo(KNOWN_CID);
    }

    @Test
    void parseAndInspect() {
        Cid cid = Cid.parse(KNOWN_CID);
        assertThat(cid.version()).isEqualTo(1);
        assertThat(cid.codec()).isEqualTo(Cid.CODEC_DAG_CBOR);
        assertThat(cid.multihash().code()).isEqualTo(Cid.MH_SHA2_256);
        assertThat(cid.multihash().digest()).hasSize(32);
    }

    @Test
    void roundTripBytes() {
        Cid cid = Cid.parse(KNOWN_CID);
        byte[] bytes = cid.toBytes();
        Cid reparsed = Cid.fromBytes(bytes);
        assertThat(reparsed).isEqualTo(cid);
    }

    @Test
    void equalityAndHashCode() {
        Cid a = Cid.parse(KNOWN_CID);
        Cid b = Cid.parse(KNOWN_CID);
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
