package io.atproto.lexicon.schema;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record LexRecord(
        String description,
        String key,
        LexObject record
) implements LexUserType {}
