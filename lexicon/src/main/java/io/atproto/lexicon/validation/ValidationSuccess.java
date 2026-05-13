package io.atproto.lexicon.validation;

import io.atproto.lex.data.LexValue;

public record ValidationSuccess(LexValue value) implements ValidationResult {
    @Override
    public boolean isSuccess() { return true; }
}
