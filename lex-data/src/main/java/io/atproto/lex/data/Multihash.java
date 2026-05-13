package io.atproto.lex.data;

import java.util.Arrays;

public record Multihash(int code, byte[] digest) {

    public Multihash {
        digest = digest.clone();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Multihash other)) return false;
        return code == other.code && Arrays.equals(digest, other.digest);
    }

    @Override
    public int hashCode() {
        return 31 * code + Arrays.hashCode(digest);
    }

    @Override
    public String toString() {
        return "Multihash{code=" + code + ", digest=" + Arrays.toString(digest) + "}";
    }
}
