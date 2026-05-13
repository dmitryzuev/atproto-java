package com.atproto.lexicon.schema;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record LexUnknown(String description) implements LexUserType {}
