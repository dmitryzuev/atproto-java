package com.atproto.lex.json;

import com.atproto.lex.data.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

class LexJsonConverterTest {

    private LexJsonConverter converter;

    @BeforeEach
    void setup() {
        converter = new LexJsonConverter();
    }

    @Test
    void roundTripNull() {
        assertThat(converter.jsonToLex(converter.lexToJson(LexNull.INSTANCE))).isEqualTo(LexNull.INSTANCE);
    }

    @Test
    void roundTripBoolean() {
        assertThat(converter.jsonToLex(converter.lexToJson(LexBoolean.TRUE))).isEqualTo(LexBoolean.TRUE);
        assertThat(converter.jsonToLex(converter.lexToJson(LexBoolean.FALSE))).isEqualTo(LexBoolean.FALSE);
    }

    @Test
    void roundTripInteger() {
        LexInteger v = new LexInteger(42);
        assertThat(converter.jsonToLex(converter.lexToJson(v))).isEqualTo(v);
    }

    @Test
    void roundTripString() {
        LexString v = new LexString("hello world");
        assertThat(converter.jsonToLex(converter.lexToJson(v))).isEqualTo(v);
    }

    @Test
    void encodesLexBytesAs$bytes() {
        LexBytes v = new LexBytes(new byte[]{1, 2, 3});
        String json = converter.lexToJsonString(v);
        assertThat(json).contains("$bytes");
        LexValue decoded = converter.jsonStringToLex(json);
        assertThat(decoded).isInstanceOf(LexBytes.class);
        assertThat(((LexBytes) decoded).value()).isEqualTo(new byte[]{1, 2, 3});
    }

    @Test
    void encodesLexCidLinkAs$link() {
        Cid cid = Cid.parse("bafyreih7a2zjxm4vq4e6pmgbpnfhz7ywkf7hmqzz4z5ssvvoyv5d6yqiq");
        LexCidLink v = new LexCidLink(cid);
        String json = converter.lexToJsonString(v);
        assertThat(json).contains("$link");
        LexValue decoded = converter.jsonStringToLex(json);
        assertThat(decoded).isInstanceOf(LexCidLink.class);
        assertThat(((LexCidLink) decoded).cid()).isEqualTo(cid);
    }

    @Test
    void encodesTypedBlobRef() {
        Cid cid = Cid.parse("bafyreih7a2zjxm4vq4e6pmgbpnfhz7ywkf7hmqzz4z5ssvvoyv5d6yqiq");
        LexBlobRef v = new LexBlobRef(new TypedBlobRef(cid, "image/png", 1024L));
        String json = converter.lexToJsonString(v);
        assertThat(json).contains("\"$type\":\"blob\"");
        LexValue decoded = converter.jsonStringToLex(json);
        assertThat(decoded).isInstanceOf(LexBlobRef.class);
        BlobRef ref = ((LexBlobRef) decoded).ref();
        assertThat(ref).isInstanceOf(TypedBlobRef.class);
        assertThat(((TypedBlobRef) ref).mimeType()).isEqualTo("image/png");
    }

    @Test
    void encodesLegacyBlobRef() {
        LexBlobRef v = new LexBlobRef(new LegacyBlobRef("bafyabc123", "image/jpeg"));
        String json = converter.lexToJsonString(v);
        assertThat(json).contains("\"cid\"");
        LexValue decoded = converter.jsonStringToLex(json);
        assertThat(((LexBlobRef) decoded).ref()).isInstanceOf(LegacyBlobRef.class);
    }

    @Test
    void roundTripArray() {
        LexArray v = LexArray.of(List.of(new LexString("a"), new LexInteger(1)));
        LexValue decoded = converter.jsonToLex(converter.lexToJson(v));
        assertThat(decoded).isInstanceOf(LexArray.class);
        assertThat(((LexArray) decoded).items()).hasSize(2);
    }

    @Test
    void roundTripMap() {
        LexMap v = new LexMap(Map.of("key", new LexString("value")));
        LexValue decoded = converter.jsonToLex(converter.lexToJson(v));
        assertThat(decoded).isInstanceOf(LexMap.class);
        assertThat(((LexMap) decoded).fields()).containsKey("key");
    }

    @Test
    void rejectsProtoKeyInjection() {
        assertThatThrownBy(() -> converter.jsonStringToLex("{\"__proto__\":{\"polluted\":true}}"))
                .isInstanceOf(LexError.class)
                .hasMessageContaining("__proto__");
    }

    @Test
    void rejectsFloatingPointNumbers() {
        assertThatThrownBy(() -> converter.jsonStringToLex("3.14"))
                .isInstanceOf(LexError.class);
    }
}
