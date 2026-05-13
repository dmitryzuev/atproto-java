package com.atproto.lexicon.schema;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record LexXrpcParameters(
    Map<String, LexUserType> properties, List<String> required, String description) {
  public LexXrpcParameters {
    properties = properties != null ? properties : Map.of();
    required = required != null ? required : List.of();
  }
}
