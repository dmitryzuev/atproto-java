package com.atproto.lexicon.schema;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record LexString(
    String description,
    String format,
    Integer minLength,
    Integer maxLength,
    Integer minGraphemes,
    Integer maxGraphemes,
    String defaultValue,
    String constValue,
    @JsonProperty("enum") List<String> enumValues,
    List<String> knownValues)
    implements LexUserType {
  public LexString {
    enumValues = enumValues != null ? enumValues : List.of();
    knownValues = knownValues != null ? knownValues : List.of();
  }
}
