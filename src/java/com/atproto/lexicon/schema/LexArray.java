package com.atproto.lexicon.schema;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record LexArray(String description, LexUserType items, Integer minLength, Integer maxLength)
    implements LexUserType {}
