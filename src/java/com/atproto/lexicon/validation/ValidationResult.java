package com.atproto.lexicon.validation;

import com.atproto.lex.data.LexValue;

/**
 * Result of a lexicon validation operation.
 */
public sealed interface ValidationResult permits ValidationSuccess, ValidationFailure {

    boolean isSuccess();

    static ValidationResult success(LexValue value) {
        return new ValidationSuccess(value);
    }

    static ValidationResult failure(String lexUri, String message) {
        return new ValidationFailure(new ValidationError(lexUri, message));
    }
}
