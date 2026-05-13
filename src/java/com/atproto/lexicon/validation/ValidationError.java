package com.atproto.lexicon.validation;

import com.atproto.lex.data.LexError;

public class ValidationError extends LexError {
    private final String lexUri;

    public ValidationError(String lexUri, String message) {
        super(message);
        this.lexUri = lexUri;
    }

    public String lexUri() {
        return lexUri;
    }
}
