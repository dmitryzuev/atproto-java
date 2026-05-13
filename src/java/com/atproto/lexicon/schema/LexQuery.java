package com.atproto.lexicon.schema;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record LexQuery(
        String description,
        LexXrpcParameters parameters,
        LexXrpcBody output,
        List<LexXrpcError> errors
) implements LexUserType {
    public LexQuery {
        errors = errors != null ? errors : List.of();
    }
}
