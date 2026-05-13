# atproto-sdk (Java)

Java 21 SDK for the [AT Protocol](https://atproto.com) — the open, federated protocol powering [Bluesky](https://bsky.app).

This is a port of the TypeScript [`@atproto/lex`](https://github.com/bluesky-social/atproto/tree/main/packages/lex/lex) package.

---

## Overview

The SDK provides:

- **Core data types** — `LexValue` sealed type hierarchy, `Cid` (content identifiers), `BlobRef`
- **JSON serialization** — convert between `LexValue` and JSON, including `$link` / `$bytes` / blob encoding
- **CBOR serialization** — DAG-CBOR encode/decode and CID generation via SHA-256
- **Lexicon schema validation** — load lexicon documents, validate records and XRPC payloads at runtime
- **XRPC client** — type-aware HTTP client for AT Protocol queries and procedures

---

## Requirements

- Java 21+
- Bazel 9+ (with bzlmod)

---

## Build

Build all library targets:

```bash
bazel build //src/java/...
```

Build a specific module:

```bash
bazel build //src/java/com/atproto/lex/data:lex_data
bazel build //src/java/com/atproto/lex/json:lex_json
bazel build //src/java/com/atproto/lex/cbor:lex_cbor
bazel build //src/java/com/atproto/lexicon:lexicon
bazel build //src/java/com/atproto/xrpc:xrpc
```

Run all tests:

```bash
bazel test //src/test/...
```

Run tests for a specific module:

```bash
bazel test //src/test/java/com/atproto/lex/data/...
bazel test //src/test/java/com/atproto/lex/cbor/...
bazel test //src/test/java/com/atproto/lexicon/...
bazel test //src/test/java/com/atproto/xrpc/...
```

---

## Quick Start

### 1. Create a client

```java
import com.atproto.lexicon.Lexicons;
import com.atproto.xrpc.XrpcClient;
import com.atproto.xrpc.XrpcResponse;

Lexicons lex = new Lexicons();
// optionally add lexicon docs for schema validation:
// lex.add(new LexiconDocDeserializer().deserialize(postJson));

XrpcClient client = new XrpcClient("https://bsky.social", lex);
client.setHeader("Authorization", () -> "Bearer " + getAccessToken());
```

### 2. Make a query (GET)

```java
import com.atproto.xrpc.QueryParams;

QueryParams params = new QueryParams()
    .put("actor", "alice.bsky.social");

XrpcResponse response = client.call("app.bsky.actor.getProfile", params, null, null);

LexMap profile = (LexMap) response.data();
String did = ((LexString) profile.fields().get("did")).value();
```

### 3. Make a procedure (POST)

```java
import com.atproto.lex.data.*;

LexMap post = new LexMap(Map.of(
    "$type",     new LexString("app.bsky.feed.post"),
    "text",      new LexString("Hello from Java!"),
    "createdAt", new LexString("2024-01-01T12:00:00.000Z")
));

XrpcResponse response = client.call("com.atproto.repo.createRecord", null, post, null);
```

### 4. Async calls

```java
client.callAsync("app.bsky.feed.getTimeline", params, null, null)
    .thenAccept(resp -> System.out.println(resp.data()));
```

---

## Core Types

### LexValue

`LexValue` is a sealed interface representing any valid AT Protocol data value:

```
LexValue
├── LexNull
├── LexBoolean(boolean value)
├── LexInteger(long value)
├── LexString(String value)
├── LexBytes(byte[] value)       — binary data
├── LexCidLink(Cid cid)          — content-addressed link
├── LexArray(List<LexValue>)
├── LexMap(Map<String, LexValue>) — object; $type field used as discriminator
└── LexBlobRef(BlobRef)          — typed or legacy blob reference
```

Pattern-matching switch works across the full hierarchy:

```java
String describe(LexValue v) {
    return switch (v) {
        case LexNull ignored    -> "null";
        case LexBoolean b       -> "boolean: " + b.value();
        case LexInteger i       -> "integer: " + i.value();
        case LexString s        -> "string: " + s.value();
        case LexBytes b         -> "bytes[" + b.value().length + "]";
        case LexCidLink c       -> "cid: " + c.cid().toBase32();
        case LexBlobRef br      -> "blob: " + br.ref().mimeType();
        case LexArray arr       -> "array[" + arr.items().size() + "]";
        case LexMap map         -> "map{" + map.fields().size() + "}";
    };
}
```

### Cid

Content identifiers (CIDv1, dag-cbor, sha2-256):

```java
Cid cid = Cid.parse("bafyreib2rxk3rybk3aobmv5cjuql3bm2twh4jo5uf23kphtx3zs4oxvdvq");
byte[] bytes = cid.toBytes();
String base32 = cid.toBase32(); // "b..." multibase prefix
```

---

## JSON Serialization

Convert between `LexValue` and JSON using `LexJsonConverter`:

```java
import com.atproto.lex.json.LexJsonConverter;

LexJsonConverter converter = new LexJsonConverter();

// LexValue → JSON string
String json = converter.lexToJsonString(myValue);

// JSON string → LexValue
LexValue value = converter.jsonStringToLex(json);
```

Special encodings handled automatically:

| JSON shape | LexValue |
|---|---|
| `{"$link":"bafy..."}` | `LexCidLink` |
| `{"$bytes":"base64..."}` | `LexBytes` |
| `{"$type":"blob","ref":{...},...}` | `LexBlobRef(TypedBlobRef)` |
| `{"cid":"...","mimeType":"..."}` | `LexBlobRef(LegacyBlobRef)` |

---

## CBOR Serialization

Encode and decode [DAG-CBOR](https://ipld.io/specs/codecs/dag-cbor/spec/):

```java
import com.atproto.lex.cbor.CborCodec;
import com.atproto.lex.cbor.CidUtils;

CborCodec codec = new CborCodec();
byte[] encoded = codec.encode(myValue);
LexValue decoded = codec.decode(encoded);

// Generate a CID from any LexValue
CidUtils utils = new CidUtils();
Cid cid = utils.cidForLex(myValue);
```

---

## Lexicon Schema Validation

Load lexicon documents and validate records or XRPC payloads at runtime:

```java
import com.atproto.lexicon.Lexicons;
import com.atproto.lexicon.LexiconDocDeserializer;
import com.atproto.lexicon.schema.LexiconDoc;

LexiconDocDeserializer deserializer = new LexiconDocDeserializer();
LexiconDoc doc = deserializer.deserialize(Files.readString(Path.of("app.bsky.feed.post.json")));

Lexicons lex = new Lexicons();
lex.add(doc);

// Throws ValidationError if invalid
lex.assertValidRecord("app.bsky.feed.post", record);
lex.assertValidXrpcParams("app.bsky.feed.getTimeline", params);
lex.assertValidXrpcOutput("app.bsky.feed.getTimeline", output);
```

### Supported string formats

The validator enforces AT Protocol string format constraints:

| Format | Example |
|---|---|
| `did` | `did:plc:ewvi7nxzyoun6zhhandbv25z` |
| `handle` | `alice.bsky.social` |
| `nsid` | `app.bsky.feed.post` |
| `at-uri` | `at://did:plc:.../app.bsky.feed.post/3k4...` |
| `cid` | `bafyreih7a2...` |
| `datetime` | `2024-01-01T12:00:00.000Z` |
| `language` | `en`, `zh-Hans` |
| `tid` | `3k4duaz5drk2m` |
| `record-key` | `self`, `3k4duaz5drk2m` |

---

## XRPC Client

### Custom HTTP transport

Implement `FetchHandler` to plug in your own HTTP stack:

```java
import com.atproto.xrpc.FetchHandler;

FetchHandler myHandler = (urlPath, requestBuilder) -> {
    // add tracing, retry logic, etc.
    return myOkHttpClient.newCall(requestBuilder.url(urlPath).build()).execute();
};

XrpcClient client = new XrpcClient("https://bsky.social", myHandler, lex);
```

### Error handling

```java
import com.atproto.xrpc.error.*;

try {
    XrpcResponse resp = client.call("app.bsky.actor.getProfile", params, null, null);
} catch (XrpcAuthenticationException e) {
    // HTTP 401 — refresh token and retry
} catch (XrpcResponseException e) {
    System.err.println("Error " + e.statusCode() + ": " + e.errorCode());
} catch (XrpcFetchException e) {
    // Network failure — no HTTP response received
} catch (XrpcException e) {
    // Any other XRPC error
}
```

---

## Package Structure

```
src/java/com/atproto/
├── lex/data/           Core types: LexValue, Cid, BlobRef, LexError
│   └── util/           Base64Utils, Utf8Utils, LexValueDeepEquals, VarintUtils
├── lex/json/           JSON ↔ LexValue (Jackson)
├── lex/cbor/           DAG-CBOR encode/decode + CID generation
├── lexicon/            Lexicon document loading and registry (Lexicons)
│   ├── schema/         LexiconDoc, LexUserType sealed hierarchy
│   └── validation/     LexiconValidator, StringFormatValidator, ValidationResult
└── xrpc/               XRPC HTTP client (OkHttp3)
    ├── error/          XrpcException hierarchy
    └── internal/       RequestBuilder, ResponseParser
```

---

## Dependencies

| Library | Purpose |
|---|---|
| `jackson-databind` | JSON serialization |
| `okhttp3` | HTTP client |
| `com.upokecenter:cbor` | DAG-CBOR encoding |

---

## License

MIT — see [LICENSE](LICENSE).
