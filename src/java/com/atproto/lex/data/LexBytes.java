package com.atproto.lex.data;

import java.util.Arrays;

public record LexBytes(byte[] value) implements LexValue {

  public LexBytes {
    if (value == null) throw new LexError("LexBytes value must not be null");
    value = value.clone();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof LexBytes other)) return false;
    return Arrays.equals(value, other.value);
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(value);
  }

  @Override
  public String toString() {
    return "LexBytes[length=" + value.length + "]";
  }
}
