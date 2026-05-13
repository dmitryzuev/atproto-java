package com.atproto.lexicon.validation;

import java.util.regex.Pattern;

/**
 * Validates AT Protocol string format constraints.
 * See: https://atproto.com/specs/lexicon#string-formats
 */
public final class StringFormatValidator {

    private StringFormatValidator() {}

    // did:method:identifier — method is alpha, identifier is unrestricted printable ASCII
    private static final Pattern DID =
            Pattern.compile("^did:[a-z]+:[a-zA-Z0-9._:%-]+$");

    // handle: TLD-valid domain, no leading/trailing hyphens per label
    private static final Pattern HANDLE =
            Pattern.compile("^([a-zA-Z0-9]([a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?\\.)+[a-zA-Z]{2,63}$");

    // NSID: segments separated by dots, last segment is method name
    private static final Pattern NSID =
            Pattern.compile("^[a-zA-Z]([a-zA-Z0-9-]{0,62}[a-zA-Z0-9])?(\\.[a-zA-Z]([a-zA-Z0-9-]{0,62}[a-zA-Z0-9])?)+$");

    // AT-URI: at://authority/collection/rkey
    private static final Pattern AT_URI =
            Pattern.compile("^at://[a-zA-Z0-9._:%-]+(/[a-zA-Z0-9._~:@!$&'()*+,;=%-]*)?(\\?[a-zA-Z0-9._~:@!$&'()*+,;=/?%-]*)?(#[a-zA-Z0-9._~:@!$&'()*+,;=/?%-]*)?$");

    // ISO 8601 / RFC 3339 datetime with timezone (T separator, Z or ±HH:MM)
    private static final Pattern DATETIME =
            Pattern.compile("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?(Z|[+-]\\d{2}:\\d{2})$");

    // TID: base32-sortable timestamp identifier (13 chars)
    private static final Pattern TID =
            Pattern.compile("^[2-7a-z]{13}$");

    // Record key: printable ASCII excluding whitespace, at-signs, and slashes
    private static final Pattern RECORD_KEY =
            Pattern.compile("^[a-zA-Z0-9_~.:-]{1,512}$");

    // Language tag (BCP 47 simplified)
    private static final Pattern LANGUAGE =
            Pattern.compile("^[a-zA-Z]{1,8}(-[a-zA-Z0-9]{1,8})*$");

    public static boolean isValidDid(String v) {
        return v != null && DID.matcher(v).matches();
    }

    public static boolean isValidHandle(String v) {
        return v != null && HANDLE.matcher(v).matches() && v.length() <= 253;
    }

    public static boolean isValidNsid(String v) {
        return v != null && NSID.matcher(v).matches();
    }

    public static boolean isValidAtUri(String v) {
        return v != null && AT_URI.matcher(v).matches();
    }

    public static boolean isValidCid(String v) {
        if (v == null || v.isEmpty()) return false;
        try {
            com.atproto.lex.data.Cid.parse(v);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isValidDatetime(String v) {
        return v != null && DATETIME.matcher(v).matches();
    }

    public static boolean isValidUri(String v) {
        if (v == null || v.isBlank()) return false;
        try {
            new java.net.URI(v);
            return true;
        } catch (java.net.URISyntaxException e) {
            return false;
        }
    }

    public static boolean isValidLanguage(String v) {
        return v != null && LANGUAGE.matcher(v).matches();
    }

    public static boolean isValidTid(String v) {
        return v != null && TID.matcher(v).matches();
    }

    public static boolean isValidRecordKey(String v) {
        if (v == null) return false;
        // Disallow "." and ".."
        if (v.equals(".") || v.equals("..")) return false;
        return RECORD_KEY.matcher(v).matches();
    }

    /**
     * Validates a value against a named format string.
     * Returns true if valid or if the format is unknown (forward-compat).
     */
    public static boolean isValid(String format, String value) {
        if (format == null) return true;
        return switch (format) {
            case "did"        -> isValidDid(value);
            case "handle"     -> isValidHandle(value);
            case "nsid"       -> isValidNsid(value);
            case "at-uri"     -> isValidAtUri(value);
            case "cid"        -> isValidCid(value);
            case "datetime"   -> isValidDatetime(value);
            case "uri"        -> isValidUri(value);
            case "language"   -> isValidLanguage(value);
            case "tid"        -> isValidTid(value);
            case "record-key" -> isValidRecordKey(value);
            default           -> true; // forward-compatible: unknown formats pass
        };
    }
}
