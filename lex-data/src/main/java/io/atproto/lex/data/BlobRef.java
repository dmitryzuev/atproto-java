package io.atproto.lex.data;

/**
 * A reference to a blob stored in the AT Protocol network.
 * Two variants: modern TypedBlobRef (with $type:"blob") and legacy LegacyBlobRef.
 */
public sealed interface BlobRef permits TypedBlobRef, LegacyBlobRef {
    String mimeType();
}
