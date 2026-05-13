package com.atproto.lex.data;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public record LexMap(Map<String, LexValue> fields) implements LexValue {

    public LexMap {
        if (fields == null) throw new LexError("LexMap.fields must not be null");
        fields = Collections.unmodifiableMap(new LinkedHashMap<>(fields));
    }

    public static LexMap of(Map<String, LexValue> fields) {
        return new LexMap(fields);
    }

    public Optional<LexValue> get(String key) {
        return Optional.ofNullable(fields.get(key));
    }

    /** Returns the $type discriminator value, if present. */
    public Optional<String> typeDiscriminator() {
        LexValue v = fields.get("$type");
        if (v instanceof LexString s) return Optional.of(s.value());
        return Optional.empty();
    }
}
