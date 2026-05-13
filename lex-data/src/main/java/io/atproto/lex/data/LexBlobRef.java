package io.atproto.lex.data;

public record LexBlobRef(BlobRef ref) implements LexValue {
    public LexBlobRef {
        if (ref == null) throw new LexError("LexBlobRef.ref must not be null");
    }
}
