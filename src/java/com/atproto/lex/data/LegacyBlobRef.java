package com.atproto.lex.data;

/** Deprecated blob reference format: {"cid":"...","mimeType":"..."} */
public record LegacyBlobRef(String cidString, String mimeType) implements BlobRef {

  public LegacyBlobRef {
    if (cidString == null || cidString.isBlank()) {
      throw new LexError("LegacyBlobRef.cidString must not be blank");
    }
    if (mimeType == null) {
      throw new LexError("LegacyBlobRef.mimeType must not be null");
    }
  }
}
