package com.atproto.lexicon.schema;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record LexCidLink(String description) implements LexUserType {}
