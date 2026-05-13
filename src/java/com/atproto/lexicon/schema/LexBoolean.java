package com.atproto.lexicon.schema;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record LexBoolean(
        String description,
        Boolean defaultValue,
        Boolean constValue
) implements LexUserType {}
