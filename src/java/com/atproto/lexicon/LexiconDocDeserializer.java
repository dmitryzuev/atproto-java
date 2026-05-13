package com.atproto.lexicon;

import com.atproto.lex.data.LexError;
import com.atproto.lexicon.schema.LexiconDoc;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;

/** Deserializes AT Protocol Lexicon JSON documents into {@link LexiconDoc} objects. */
public final class LexiconDocDeserializer {

  private final ObjectMapper mapper;

  public LexiconDocDeserializer() {
    this.mapper =
        new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
  }

  public LexiconDocDeserializer(ObjectMapper mapper) {
    this.mapper = mapper;
  }

  public LexiconDoc deserialize(String json) {
    try {
      return mapper.readValue(json, LexiconDoc.class);
    } catch (IOException e) {
      throw new LexError("Failed to deserialize lexicon document", e);
    }
  }

  public LexiconDoc deserialize(byte[] bytes) {
    try {
      return mapper.readValue(bytes, LexiconDoc.class);
    } catch (IOException e) {
      throw new LexError("Failed to deserialize lexicon document", e);
    }
  }

  public LexiconDoc deserialize(InputStream stream) {
    try {
      return mapper.readValue(stream, LexiconDoc.class);
    } catch (IOException e) {
      throw new LexError("Failed to deserialize lexicon document", e);
    }
  }
}
