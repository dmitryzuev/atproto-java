package com.atproto.lexicon.schema;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record LexBlob(
        String description,
        List<String> accept,
        Long maxSize
) implements LexUserType {
    public LexBlob {
        accept = accept != null ? accept : List.of();
    }
}
