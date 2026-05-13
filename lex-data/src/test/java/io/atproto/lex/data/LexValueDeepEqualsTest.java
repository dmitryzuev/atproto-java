package io.atproto.lex.data;

import io.atproto.lex.data.util.LexValueDeepEquals;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

class LexValueDeepEqualsTest {

    @Test
    void scalarsEqual() {
        assertThat(LexValueDeepEquals.deepEquals(new LexString("hello"), new LexString("hello"))).isTrue();
        assertThat(LexValueDeepEquals.deepEquals(new LexInteger(42), new LexInteger(42))).isTrue();
        assertThat(LexValueDeepEquals.deepEquals(LexBoolean.TRUE, LexBoolean.TRUE)).isTrue();
        assertThat(LexValueDeepEquals.deepEquals(LexNull.INSTANCE, LexNull.INSTANCE)).isTrue();
    }

    @Test
    void scalarsNotEqual() {
        assertThat(LexValueDeepEquals.deepEquals(new LexString("a"), new LexString("b"))).isFalse();
        assertThat(LexValueDeepEquals.deepEquals(new LexInteger(1), new LexInteger(2))).isFalse();
        assertThat(LexValueDeepEquals.deepEquals(LexBoolean.TRUE, LexBoolean.FALSE)).isFalse();
    }

    @Test
    void differentTypesNotEqual() {
        assertThat(LexValueDeepEquals.deepEquals(new LexString("1"), new LexInteger(1))).isFalse();
    }

    @Test
    void nestedMapsEqual() {
        Map<String, LexValue> inner = Map.of("x", new LexInteger(1));
        LexMap a = new LexMap(Map.of("nested", new LexMap(inner)));
        LexMap b = new LexMap(Map.of("nested", new LexMap(Map.of("x", new LexInteger(1)))));
        assertThat(LexValueDeepEquals.deepEquals(a, b)).isTrue();
    }

    @Test
    void arraysEqual() {
        LexArray a = LexArray.of(List.of(new LexString("x"), new LexInteger(2)));
        LexArray b = LexArray.of(List.of(new LexString("x"), new LexInteger(2)));
        assertThat(LexValueDeepEquals.deepEquals(a, b)).isTrue();
    }

    @Test
    void arraysDifferentLength() {
        LexArray a = LexArray.of(List.of(new LexString("x")));
        LexArray b = LexArray.of(List.of(new LexString("x"), new LexString("y")));
        assertThat(LexValueDeepEquals.deepEquals(a, b)).isFalse();
    }

    @Test
    void bytesEqual() {
        assertThat(LexValueDeepEquals.deepEquals(new LexBytes(new byte[]{1, 2}), new LexBytes(new byte[]{1, 2}))).isTrue();
        assertThat(LexValueDeepEquals.deepEquals(new LexBytes(new byte[]{1}), new LexBytes(new byte[]{2}))).isFalse();
    }
}
