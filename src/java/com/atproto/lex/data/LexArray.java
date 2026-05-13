package com.atproto.lex.data;

import java.util.Collections;
import java.util.List;

public record LexArray(List<LexValue> items) implements LexValue {

  public LexArray {
    if (items == null) throw new LexError("LexArray.items must not be null");
    items = Collections.unmodifiableList(items);
  }

  public static LexArray of(List<LexValue> items) {
    return new LexArray(List.copyOf(items));
  }
}
