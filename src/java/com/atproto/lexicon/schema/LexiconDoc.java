package com.atproto.lexicon.schema;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Map;

/**
 * Top-level AT Protocol Lexicon document. Example file:
 * {"lexicon":1,"id":"app.bsky.feed.post","defs":{...}}
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record LexiconDoc(
    int lexicon, String id, String revision, String description, Map<String, LexUserType> defs) {
  public LexiconDoc {
    if (lexicon != 1) throw new IllegalArgumentException("Unsupported lexicon version: " + lexicon);
    if (id == null || id.isBlank())
      throw new IllegalArgumentException("LexiconDoc id must not be blank");
    defs = defs != null ? defs : Map.of();
  }

  /** Returns the "main" definition, which is the primary def for this NSID. */
  public LexUserType mainDef() {
    return defs.get("main");
  }
}
