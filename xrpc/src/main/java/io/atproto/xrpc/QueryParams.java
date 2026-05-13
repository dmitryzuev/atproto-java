package io.atproto.xrpc;

import java.util.*;

/**
 * Typed query parameter map for XRPC GET requests.
 */
public final class QueryParams {

    private final Map<String, List<String>> params = new LinkedHashMap<>();

    public QueryParams put(String key, String value) {
        params.computeIfAbsent(key, k -> new ArrayList<>()).add(value);
        return this;
    }

    public QueryParams put(String key, long value) {
        return put(key, Long.toString(value));
    }

    public QueryParams put(String key, boolean value) {
        return put(key, Boolean.toString(value));
    }

    public QueryParams put(String key, List<String> values) {
        params.computeIfAbsent(key, k -> new ArrayList<>()).addAll(values);
        return this;
    }

    public Map<String, List<String>> asMap() {
        return Collections.unmodifiableMap(params);
    }

    public boolean isEmpty() {
        return params.isEmpty();
    }

    public static QueryParams empty() {
        return new QueryParams();
    }
}
