package com.atproto.lexicon.schema;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record LexInteger(
    String description,
    Long minimum,
    Long maximum,
    Long defaultValue,
    Long constValue,
    List<Long> enumValues)
    implements LexUserType {
  public LexInteger {
    enumValues = enumValues != null ? enumValues : List.of();
  }
}
