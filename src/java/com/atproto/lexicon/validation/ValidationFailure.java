package com.atproto.lexicon.validation;

public record ValidationFailure(ValidationError error) implements ValidationResult {
    @Override
    public boolean isSuccess() { return false; }
}
