package com.atproto.lexicon.schema;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record LexSubscription(
    String description,
    LexXrpcParameters parameters,
    LexRefUnion message,
    List<LexXrpcError> errors)
    implements LexUserType {
  public LexSubscription {
    errors = errors != null ? errors : List.of();
  }
}
