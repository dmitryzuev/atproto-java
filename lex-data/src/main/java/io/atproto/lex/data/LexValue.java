package io.atproto.lex.data;

/**
 * The fundamental recursive value type for AT Protocol Lexicon data.
 * Maps to TypeScript: type LexValue = LexScalar | LexValue[] | { [_:string]?: LexValue }
 */
public sealed interface LexValue
        permits LexNull, LexBoolean, LexInteger, LexString,
                LexBytes, LexCidLink, LexArray, LexMap, LexBlobRef {
}
