package com.atproto.lex.cbor;

import com.atproto.lex.data.*;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;

class CidUtilsTest {

    @Test
    void cidForLexProducesCidv1() {
        CidUtils utils = new CidUtils();
        LexMap record = new LexMap(Map.of(
                "$type", new LexString("app.bsky.feed.post"),
                "text", new LexString("Hello World"),
                "createdAt", new LexString("2024-01-01T00:00:00.000Z")
        ));
        Cid cid = utils.cidForLex(record);
        assertThat(cid.version()).isEqualTo(1);
        assertThat(cid.codec()).isEqualTo(Cid.CODEC_DAG_CBOR);
        assertThat(cid.multihash().code()).isEqualTo(Cid.MH_SHA2_256);
        assertThat(cid.multihash().digest()).hasSize(32);
        // CID string starts with "b" (multibase base32-lower)
        assertThat(cid.toBase32()).startsWith("b");
    }

    @Test
    void cidIsDeterministic() {
        CidUtils utils = new CidUtils();
        LexMap record = new LexMap(Map.of("x", new LexInteger(1)));
        Cid cid1 = utils.cidForLex(record);
        Cid cid2 = utils.cidForLex(record);
        assertThat(cid1).isEqualTo(cid2);
    }

    @Test
    void differentDataDifferentCid() {
        CidUtils utils = new CidUtils();
        Cid a = utils.cidForLex(new LexString("hello"));
        Cid b = utils.cidForLex(new LexString("world"));
        assertThat(a).isNotEqualTo(b);
    }
}
