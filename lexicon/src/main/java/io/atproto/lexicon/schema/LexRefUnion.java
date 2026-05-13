package io.atproto.lexicon.schema;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record LexRefUnion(
        String description,
        List<String> refs,
        boolean closed
) implements LexUserType {
    public LexRefUnion {
        refs = refs != null ? refs : List.of();
    }
}
