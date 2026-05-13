package com.atproto.lex.data;

public record LexNull() implements LexValue {
    public static final LexNull INSTANCE = new LexNull();
}
