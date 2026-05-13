package com.atproto.lex.data;

public record LexString(String value) implements LexValue {
    public LexString {
        if (value == null) throw new LexError("LexString value must not be null");
    }
}
