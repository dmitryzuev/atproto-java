package io.atproto.lexicon;

import io.atproto.lex.data.LexError;
import io.atproto.lex.data.LexValue;
import io.atproto.lexicon.schema.*;
import io.atproto.lexicon.validation.*;

import java.util.*;

/**
 * Registry of AT Protocol Lexicon documents.
 * Provides definition lookup and runtime validation.
 *
 * Not thread-safe — external synchronization required for concurrent add/remove.
 */
public final class Lexicons implements Iterable<LexiconDoc> {

    private final Map<String, LexiconDoc> docs = new LinkedHashMap<>();
    // Flat index: "lex:nsid#name" -> LexUserType
    private final Map<String, LexUserType> defs = new HashMap<>();

    private final LexiconValidator validator;

    public Lexicons() {
        this.validator = new LexiconValidator(this);
    }

    public Lexicons(Iterable<LexiconDoc> initial) {
        this();
        initial.forEach(this::add);
    }

    // ---- Document management ----

    public void add(LexiconDoc doc) {
        docs.put(doc.id(), doc);
        for (Map.Entry<String, LexUserType> entry : doc.defs().entrySet()) {
            String uri = "lex:" + doc.id() + "#" + entry.getKey();
            defs.put(uri, entry.getValue());
        }
    }

    public void remove(String nsid) {
        LexiconDoc doc = docs.remove(nsid);
        if (doc != null) {
            doc.defs().keySet().forEach(name -> defs.remove("lex:" + nsid + "#" + name));
        }
    }

    public Optional<LexiconDoc> get(String nsid) {
        return Optional.ofNullable(docs.get(nsid));
    }

    public Optional<LexUserType> getDef(String uri) {
        return Optional.ofNullable(defs.get(normalizeUri(uri)));
    }

    public LexUserType getDefOrThrow(String uri) {
        LexUserType def = defs.get(normalizeUri(uri));
        if (def == null) throw new LexError("Lexicon def not found: " + uri);
        return def;
    }

    // ---- URI resolution ----

    /**
     * Resolves a lexicon URI reference against a base URI.
     * Rules:
     *   "#fragment"       → baseUri (without any existing fragment) + "#fragment"
     *   "nsid#name"       → "lex:nsid#name"
     *   "nsid" (no #)     → "lex:nsid#main"
     *   "lex:..."         → unchanged
     */
    public String resolveLexUri(String uri, String baseUri) {
        if (uri == null) throw new LexError("Cannot resolve null URI");
        if (uri.startsWith("lex:")) return uri;
        if (uri.startsWith("#")) {
            // Relative fragment: append to base NSID
            String base = stripFragment(baseUri);
            return base + uri;
        }
        if (uri.contains("#")) {
            return "lex:" + uri;
        }
        return "lex:" + uri + "#main";
    }

    private String stripFragment(String uri) {
        if (uri.startsWith("lex:")) {
            int hash = uri.indexOf('#');
            return hash >= 0 ? uri.substring(0, hash) : uri;
        }
        return "lex:" + uri;
    }

    private String normalizeUri(String uri) {
        if (uri.startsWith("lex:")) return uri;
        if (uri.contains("#"))     return "lex:" + uri;
        return "lex:" + uri + "#main";
    }

    // ---- Validation ----

    public ValidationResult validate(String lexUri, LexValue value) {
        LexUserType def = getDefOrThrow(lexUri);
        return validator.validateDef(lexUri, def, value);
    }

    public LexValue assertValidRecord(String nsid, LexValue value) {
        LexiconDoc doc = get(nsid).orElseThrow(() -> new LexError("Lexicon not found: " + nsid));
        ValidationResult result = validator.validateRecord(doc, value);
        return requireSuccess(result);
    }

    public LexValue assertValidXrpcParams(String nsid, LexValue value) {
        LexUserType def = getDefOrThrow("lex:" + nsid + "#main");
        LexXrpcParameters params = switch (def) {
            case LexQuery q      -> q.parameters();
            case LexProcedure p  -> p.parameters();
            case LexSubscription s -> s.parameters();
            default -> null;
        };
        if (params == null) return value;
        ValidationResult result = validator.validateXrpcParams(nsid, params, value);
        return requireSuccess(result);
    }

    public LexValue assertValidXrpcInput(String nsid, LexValue value) {
        LexiconDoc doc = get(nsid).orElseThrow(() -> new LexError("Lexicon not found: " + nsid));
        ValidationResult result = validator.validateXrpcInput(doc, value);
        return requireSuccess(result);
    }

    public LexValue assertValidXrpcOutput(String nsid, LexValue value) {
        LexiconDoc doc = get(nsid).orElseThrow(() -> new LexError("Lexicon not found: " + nsid));
        ValidationResult result = validator.validateXrpcOutput(doc, value);
        return requireSuccess(result);
    }

    @Override
    public Iterator<LexiconDoc> iterator() {
        return docs.values().iterator();
    }

    private LexValue requireSuccess(ValidationResult result) {
        return switch (result) {
            case ValidationSuccess s -> s.value();
            case ValidationFailure f -> throw f.error();
        };
    }
}
