package io.atproto.lexicon.schema;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record LexObject(
        String description,
        Map<String, LexUserType> properties,
        List<String> required,
        List<String> nullable
) implements LexUserType {
    public LexObject {
        properties = properties != null ? properties : Map.of();
        required   = required   != null ? required   : List.of();
        nullable   = nullable   != null ? nullable   : List.of();
    }
}
