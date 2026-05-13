package io.atproto.lexicon.schema;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record LexXrpcBody(
        String encoding,
        LexUserType schema,
        String description
) {}
