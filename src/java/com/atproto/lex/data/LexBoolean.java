package com.atproto.lex.data;

public record LexBoolean(boolean value) implements LexValue {
    public static final LexBoolean TRUE = new LexBoolean(true);
    public static final LexBoolean FALSE = new LexBoolean(false);

    public static LexBoolean of(boolean value) {
        return value ? TRUE : FALSE;
    }
}
