package io.atproto.lexicon.schema;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record LexToken(String description) implements LexUserType {}
