package io.atproto.lex.data;

public record LexCidLink(Cid cid) implements LexValue {
    public LexCidLink {
        if (cid == null) throw new LexError("LexCidLink.cid must not be null");
    }
}
