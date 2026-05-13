package com.atproto.lex.cbor;

import com.atproto.lex.data.*;
import com.atproto.lex.data.util.LexValueDeepEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

class CborCodecTest {

    private CborCodec codec;

    @BeforeEach
    void setup() {
        codec = new CborCodec();
    }

    @Test
    void roundTripNull() {
        assertThat(codec.decode(codec.encode(LexNull.INSTANCE))).isEqualTo(LexNull.INSTANCE);
    }

    @Test
    void roundTripBoolean() {
        assertThat(codec.decode(codec.encode(LexBoolean.TRUE))).isEqualTo(LexBoolean.TRUE);
        assertThat(codec.decode(codec.encode(LexBoolean.FALSE))).isEqualTo(LexBoolean.FALSE);
    }

    @Test
    void roundTripInteger() {
        LexInteger v = new LexInteger(12345L);
        assertThat(codec.decode(codec.encode(v))).isEqualTo(v);
    }

    @Test
    void roundTripString() {
        LexString v = new LexString("hello");
        assertThat(codec.decode(codec.encode(v))).isEqualTo(v);
    }

    @Test
    void roundTripBytes() {
        LexBytes v = new LexBytes(new byte[]{0x01, 0x02, 0x03});
        LexValue decoded = codec.decode(codec.encode(v));
        assertThat(decoded).isInstanceOf(LexBytes.class);
        assertThat(((LexBytes) decoded).value()).isEqualTo(new byte[]{0x01, 0x02, 0x03});
    }

    @Test
    void roundTripCidLink() {
        Cid cid = Cid.parse("bafyreih7a2zjxm4vq4e6pmgbpnfhz7ywkf7hmqzz4z5ssvvoyv5d6yqiq");
        LexCidLink v = new LexCidLink(cid);
        LexValue decoded = codec.decode(codec.encode(v));
        assertThat(decoded).isInstanceOf(LexCidLink.class);
        assertThat(((LexCidLink) decoded).cid()).isEqualTo(cid);
    }

    @Test
    void roundTripArray() {
        LexArray v = LexArray.of(List.of(new LexString("a"), new LexInteger(1)));
        LexValue decoded = codec.decode(codec.encode(v));
        assertThat(LexValueDeepEquals.deepEquals(v, decoded)).isTrue();
    }

    @Test
    void roundTripMap() {
        LexMap v = new LexMap(Map.of("foo", new LexString("bar"), "n", new LexInteger(7)));
        LexValue decoded = codec.decode(codec.encode(v));
        assertThat(decoded).isInstanceOf(LexMap.class);
        assertThat(((LexMap) decoded).fields()).containsKey("foo");
        assertThat(((LexMap) decoded).fields()).containsKey("n");
    }

    @Test
    void roundTripTypedBlobRef() {
        Cid cid = Cid.parse("bafyreih7a2zjxm4vq4e6pmgbpnfhz7ywkf7hmqzz4z5ssvvoyv5d6yqiq");
        LexBlobRef v = new LexBlobRef(new TypedBlobRef(cid, "image/png", 512L));
        LexValue decoded = codec.decode(codec.encode(v));
        assertThat(decoded).isInstanceOf(LexBlobRef.class);
        assertThat(((LexBlobRef) decoded).ref()).isInstanceOf(TypedBlobRef.class);
        assertThat(((TypedBlobRef) ((LexBlobRef) decoded).ref()).size()).isEqualTo(512L);
    }
}
