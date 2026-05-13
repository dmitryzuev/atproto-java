package io.atproto.lex.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.*;
import io.atproto.lex.data.*;
import io.atproto.lex.data.util.Base64Utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * Converts between {@link LexValue} and Jackson {@link JsonNode} / JSON strings.
 *
 * Special JSON encoding rules (mirroring the TypeScript @atproto/lex-json):
 *   LexCidLink  -> {"$link": "<base32-cid>"}
 *   LexBytes    -> {"$bytes": "<base64>"}
 *   TypedBlobRef -> {"$type":"blob","ref":{"$link":"..."},"mimeType":"...","size":N}
 *   LegacyBlobRef -> {"cid":"...","mimeType":"..."}
 *
 * Special JSON decoding rules:
 *   {"$link":"..."} -> LexCidLink
 *   {"$bytes":"..."} -> LexBytes
 *   {"$type":"blob",...} -> LexBlobRef(TypedBlobRef)
 *   {"cid":"...","mimeType":"..."} (no $type) -> LexBlobRef(LegacyBlobRef)
 *   Objects with "__proto__" key -> rejected (security)
 */
public final class LexJsonConverter {

    private final ObjectMapper mapper;

    public LexJsonConverter() {
        this.mapper = new ObjectMapper();
    }

    public LexJsonConverter(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    // ---- LexValue → JSON ----

    public JsonNode lexToJson(LexValue value) {
        return switch (value) {
            case LexNull ignored       -> NullNode.instance;
            case LexBoolean b          -> BooleanNode.valueOf(b.value());
            case LexInteger i          -> LongNode.valueOf(i.value());
            case LexString s           -> TextNode.valueOf(s.value());
            case LexBytes b            -> encodeLexBytes(b.value());
            case LexCidLink c          -> encodeLexLink(c.cid());
            case LexBlobRef br         -> encodeBlobRef(br.ref());
            case LexArray arr          -> encodeArray(arr);
            case LexMap map            -> encodeMap(map);
        };
    }

    public String lexToJsonString(LexValue value) {
        try {
            return mapper.writeValueAsString(lexToJson(value));
        } catch (JsonProcessingException e) {
            throw new LexError("Failed to serialize LexValue to JSON", e);
        }
    }

    // ---- JSON → LexValue ----

    public LexValue jsonToLex(JsonNode node) {
        if (node == null || node.isNull()) return LexNull.INSTANCE;
        if (node.isBoolean())             return LexBoolean.of(node.booleanValue());
        if (node.isIntegralNumber())      return new LexInteger(node.longValue());
        if (node.isFloatingPointNumber()) throw new LexError("Floating-point numbers are not valid LexValues");
        if (node.isTextual())             return new LexString(node.textValue());
        if (node.isArray())               return decodeArray(node);
        if (node.isObject())              return decodeObject((ObjectNode) node);
        throw new LexError("Unsupported JSON node type: " + node.getNodeType());
    }

    public LexValue jsonStringToLex(String json) {
        try {
            return jsonToLex(mapper.readTree(json));
        } catch (JsonProcessingException e) {
            throw new LexError("Failed to parse JSON string", e);
        }
    }

    public LexValue jsonBytesToLex(byte[] bytes) {
        try {
            return jsonToLex(mapper.readTree(bytes));
        } catch (IOException e) {
            throw new LexError("Failed to parse JSON bytes", e);
        }
    }

    public LexValue jsonStreamToLex(InputStream stream) {
        try {
            return jsonToLex(mapper.readTree(stream));
        } catch (IOException e) {
            throw new LexError("Failed to parse JSON stream", e);
        }
    }

    // ---- Encoding helpers ----

    private ObjectNode encodeLexBytes(byte[] bytes) {
        ObjectNode node = mapper.createObjectNode();
        node.put("$bytes", Base64Utils.encode(bytes));
        return node;
    }

    private ObjectNode encodeLexLink(Cid cid) {
        ObjectNode node = mapper.createObjectNode();
        node.put("$link", cid.toBase32());
        return node;
    }

    private ObjectNode encodeBlobRef(BlobRef ref) {
        ObjectNode node = mapper.createObjectNode();
        switch (ref) {
            case TypedBlobRef t -> {
                node.put("$type", "blob");
                node.set("ref", encodeLexLink(t.ref()));
                node.put("mimeType", t.mimeType());
                node.put("size", t.size());
            }
            case LegacyBlobRef l -> {
                node.put("cid", l.cidString());
                node.put("mimeType", l.mimeType());
            }
        }
        return node;
    }

    private ArrayNode encodeArray(LexArray arr) {
        ArrayNode node = mapper.createArrayNode();
        for (LexValue item : arr.items()) {
            node.add(lexToJson(item));
        }
        return node;
    }

    private ObjectNode encodeMap(LexMap map) {
        ObjectNode node = mapper.createObjectNode();
        for (Map.Entry<String, LexValue> entry : map.fields().entrySet()) {
            node.set(entry.getKey(), lexToJson(entry.getValue()));
        }
        return node;
    }

    // ---- Decoding helpers ----

    private LexArray decodeArray(JsonNode node) {
        List<LexValue> items = new ArrayList<>(node.size());
        for (JsonNode element : node) {
            items.add(jsonToLex(element));
        }
        return LexArray.of(items);
    }

    private LexValue decodeObject(ObjectNode node) {
        // Security: reject __proto__ key injection
        if (node.has("__proto__")) {
            throw new LexError("Object contains forbidden key '__proto__'");
        }

        // {"$link": "..."} -> CID link
        if (node.size() == 1 && node.has("$link")) {
            String cidStr = node.get("$link").textValue();
            if (cidStr == null) throw new LexError("$link value must be a string");
            return new LexCidLink(Cid.parse(cidStr));
        }

        // {"$bytes": "..."} -> binary data
        if (node.size() == 1 && node.has("$bytes")) {
            String b64 = node.get("$bytes").textValue();
            if (b64 == null) throw new LexError("$bytes value must be a string");
            return new LexBytes(Base64Utils.decode(b64));
        }

        // {"$type":"blob",...} -> typed blob ref
        JsonNode typeNode = node.get("$type");
        if (typeNode != null && "blob".equals(typeNode.textValue())) {
            return decodeTypedBlobRef(node);
        }

        // {"cid":"...","mimeType":"..."} (no $type) -> legacy blob ref
        if (node.has("cid") && node.has("mimeType") && !node.has("$type")) {
            String cid = node.get("cid").textValue();
            String mime = node.get("mimeType").textValue();
            if (cid != null && mime != null) {
                return new LexBlobRef(new LegacyBlobRef(cid, mime));
            }
        }

        // Generic object -> LexMap
        Map<String, LexValue> fields = new LinkedHashMap<>();
        node.fields().forEachRemaining(entry -> {
            JsonNode v = entry.getValue();
            // Skip null/undefined-equivalent entries
            if (!v.isNull()) {
                fields.put(entry.getKey(), jsonToLex(v));
            } else {
                fields.put(entry.getKey(), LexNull.INSTANCE);
            }
        });
        return new LexMap(fields);
    }

    private LexBlobRef decodeTypedBlobRef(ObjectNode node) {
        JsonNode refNode = node.get("ref");
        if (refNode == null || !refNode.isObject()) {
            throw new LexError("TypedBlobRef missing or invalid 'ref' field");
        }
        JsonNode linkNode = refNode.get("$link");
        if (linkNode == null) {
            throw new LexError("TypedBlobRef.ref missing '$link' field");
        }
        Cid cid = Cid.parse(linkNode.textValue());

        String mimeType = node.path("mimeType").textValue();
        if (mimeType == null) throw new LexError("TypedBlobRef missing 'mimeType'");

        long size = node.path("size").longValue(); // defaults to 0 if missing
        return new LexBlobRef(new TypedBlobRef(cid, mimeType, size));
    }

    static class LexError extends io.atproto.lex.data.LexError {
        LexError(String message) { super(message); }
        LexError(String message, Throwable cause) { super(message, cause); }
    }
}
