package com.atproto.lexicon.validation;

import com.atproto.lex.data.LexValue;

public record ValidationSuccess(LexValue value) implements ValidationResult {
  @Override
  public boolean isSuccess() {
    return true;
  }
}
