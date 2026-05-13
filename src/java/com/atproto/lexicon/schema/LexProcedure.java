package com.atproto.lexicon.schema;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record LexProcedure(
    String description,
    LexXrpcParameters parameters,
    LexXrpcBody input,
    LexXrpcBody output,
    List<LexXrpcError> errors)
    implements LexUserType {
  public LexProcedure {
    errors = errors != null ? errors : List.of();
  }
}
