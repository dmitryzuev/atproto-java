package com.atproto.lex.cbor;

import static org.assertj.core.api.Assertions.*;

import com.atproto.lex.data.*;
import com.atproto.lex.data.util.LexValueDeepEquals;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CborCodecTest {

  private CborCodec codec;

  @BeforeEach
  void setup() {
    codec = new CborCodec();
  }

  private static Cid sampleCid() {
    byte[] digest = new byte[32];
    for (int i = 0; i < digest.length; i++) digest[i] = (byte) (i + 1);
    return new Cid(1, Cid.CODEC_DAG_CBOR, new Multihash(Cid.MH_SHA2_256, digest));
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
    LexBytes v = new LexBytes(new byte[] {0x01, 0x02, 0x03});
    LexValue decoded = codec.decode(codec.encode(v));
    assertThat(decoded).isInstanceOf(LexBytes.class);
    assertThat(((LexBytes) decoded).value()).isEqualTo(new byte[] {0x01, 0x02, 0x03});
  }

  @Test
  void roundTripCidLink() {
    Cid cid = sampleCid();
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
    Cid cid = sampleCid();
    LexBlobRef v = new LexBlobRef(new TypedBlobRef(cid, "image/png", 512L));
    LexValue decoded = codec.decode(codec.encode(v));
    assertThat(decoded).isInstanceOf(LexBlobRef.class);
    assertThat(((LexBlobRef) decoded).ref()).isInstanceOf(TypedBlobRef.class);
    assertThat(((TypedBlobRef) ((LexBlobRef) decoded).ref()).size()).isEqualTo(512L);
  }
}
