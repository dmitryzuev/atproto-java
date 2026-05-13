package io.atproto.lex.data;

/**
 * Modern blob reference: {"$type":"blob","ref":{...CID...},"mimeType":"...","size":N}
 */
public record TypedBlobRef(Cid ref, String mimeType, long size) implements BlobRef {

    public TypedBlobRef {
        if (ref == null) throw new LexError("TypedBlobRef.ref must not be null");
        if (mimeType == null || !mimeType.contains("/")) {
            throw new LexError("TypedBlobRef.mimeType must be a valid MIME type (contain '/')");
        }
        if (size < 0) throw new LexError("TypedBlobRef.size must be non-negative");
    }
}
