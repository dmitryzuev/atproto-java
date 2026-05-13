package io.atproto.lex.data.util;

import io.atproto.lex.data.*;

import java.util.*;

/**
 * Stack-based structural deep-equality for LexValue — avoids StackOverflowError on deeply nested data.
 * Cycle detection via IdentityHashMap prevents infinite loops.
 */
public final class LexValueDeepEquals {

    private LexValueDeepEquals() {}

    public static boolean deepEquals(LexValue a, LexValue b) {
        if (a == b) return true;
        if (a == null || b == null) return false;

        // Stack holds pairs to compare
        Deque<LexValue[]> stack = new ArrayDeque<>();
        // Tracks pairs we've started comparing (identity, not equality) to detect cycles
        Set<Long> visiting = new HashSet<>();

        stack.push(new LexValue[]{a, b});

        while (!stack.isEmpty()) {
            LexValue[] pair = stack.pop();
            LexValue x = pair[0];
            LexValue y = pair[1];

            if (x == y) continue;
            if (x == null || y == null) return false;
            if (x.getClass() != y.getClass()) return false;

            // Use identity pair key to detect cycles in compound types
            long pairKey = ((long) System.identityHashCode(x) << 32) | (System.identityHashCode(y) & 0xFFFFFFFFL);
            if (!visiting.add(pairKey)) continue; // already being compared — assume equal (cycle)

            switch (x) {
                case LexNull ignored -> {}  // same class already checked above
                case LexBoolean bx -> { if (bx.value() != ((LexBoolean) y).value()) return false; }
                case LexInteger ix -> { if (ix.value() != ((LexInteger) y).value()) return false; }
                case LexString sx -> { if (!sx.value().equals(((LexString) y).value())) return false; }
                case LexBytes bx -> { if (!Arrays.equals(bx.value(), ((LexBytes) y).value())) return false; }
                case LexCidLink lx -> { if (!lx.cid().equals(((LexCidLink) y).cid())) return false; }
                case LexBlobRef brx -> { if (!brx.ref().equals(((LexBlobRef) y).ref())) return false; }
                case LexArray ax -> {
                    LexArray ay = (LexArray) y;
                    if (ax.items().size() != ay.items().size()) return false;
                    for (int i = 0; i < ax.items().size(); i++) {
                        stack.push(new LexValue[]{ax.items().get(i), ay.items().get(i)});
                    }
                }
                case LexMap mx -> {
                    LexMap my = (LexMap) y;
                    if (mx.fields().size() != my.fields().size()) return false;
                    for (Map.Entry<String, LexValue> entry : mx.fields().entrySet()) {
                        LexValue yVal = my.fields().get(entry.getKey());
                        if (yVal == null && !my.fields().containsKey(entry.getKey())) return false;
                        stack.push(new LexValue[]{entry.getValue(), yVal});
                    }
                }
            }
        }
        return true;
    }
}
