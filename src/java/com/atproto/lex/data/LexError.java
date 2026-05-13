package com.atproto.lex.data;

public class LexError extends RuntimeException {
    public LexError(String message) {
        super(message);
    }

    public LexError(String message, Throwable cause) {
        super(message, cause);
    }
}
