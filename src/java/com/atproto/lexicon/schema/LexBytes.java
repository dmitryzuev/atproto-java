package com.atproto.lexicon.schema;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record LexBytes(String description, Integer minLength, Integer maxLength)
    implements LexUserType {}
