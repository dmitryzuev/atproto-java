> [!IMPORTANT]
>
> This library is currently in **preview**. The API is subject to change before the stable release.

Type-safe AT Protocol Lexicon tooling for Java 21.

- Core data model with sealed types (`LexValue` hierarchy, `Cid`, `BlobRef`)
- JSON and DAG-CBOR encoding for AT Protocol data structures
- Runtime schema validation with Lexicon documents
- Fully typed XRPC client with authentication support

```java
// Validate data against a loaded Lexicon schema

LexMap post = new LexMap(Map.of(
    "$type",     new LexString("app.bsky.feed.post"),
    "text",      new LexString("Hello, world!"),
    "createdAt", new LexString("2024-01-15T12:30:00.000Z")
));

lex.assertValidRecord("app.bsky.feed.post", post);
// throws ValidationError if the record violates schema constraints
```

```java
// Make type-aware XRPC requests

XrpcClient client = new XrpcClient("https://public.api.bsky.app", lex);

QueryParams params = new QueryParams().put("actor", "pfrazee.com");
XrpcResponse response = client.call("app.bsky.actor.getProfile", params, null, null);
LexMap profile = (LexMap) response.data();
```

```java
// Work with an authenticated session

XrpcClient client = new XrpcClient("https://bsky.social", lex);
client.setHeader("Authorization", () -> "Bearer " + tokenStore.getAccessJwt());

client.call("com.atproto.repo.createRecord", null, new LexMap(Map.of(
    "repo",       new LexString(myDid),
    "collection", new LexString("app.bsky.feed.post"),
    "record",     post
)), null);
```

- [Quick Start](#quick-start)
- [Data Model](#data-model)
  - [Types](#types)
  - [JSON Encoding](#json-encoding)
  - [CBOR Encoding](#cbor-encoding)
- [Schema Validation](#schema-validation)
  - [Loading Schemas](#loading-schemas)
  - [Validating Records and Payloads](#validating-records-and-payloads)
  - [String Format Validators](#string-format-validators)
- [Making XRPC Requests](#making-xrpc-requests)
- [Client API](#client-api)
  - [Creating a Client](#creating-a-client)
  - [Dynamic Headers](#dynamic-headers)
  - [Core Methods](#core-methods)
  - [Async Calls](#async-calls)
  - [Error Handling](#error-handling)
  - [Custom Transport](#custom-transport)
- [Utilities](#utilities)
  - [CID Utilities](#cid-utilities)
  - [Blob References](#blob-references)
  - [Deep Equality](#deep-equality)
- [Build](#build)
- [License](#license)

## Quick Start

**1. Build the SDK**

```bash
bazel build //src/java/...
```

Requires Bazel 9+ with bzlmod. Java 21 is configured via `.bazelrc`.

Or publish to a local Maven repository and add the artifact to your build tool:

```bash
bazel run //src/java/com/atproto/xrpc:xrpc_export.publish -- \
  --maven_repo=file://${HOME}/.m2/repository
```

Then declare `com.atproto:xrpc:0.1.0` as a dependency — it transitively includes `lex-data`, `lex-json`, `lex-cbor`, and `lexicon`.

**2. Load Lexicon schemas (optional)**

You can use the client without schemas for schema-unaware calls. To enable runtime validation, load Lexicon JSON documents:

```java
import com.atproto.lexicon.Lexicons;
import com.atproto.lexicon.LexiconDocDeserializer;
import com.atproto.lexicon.schema.LexiconDoc;

LexiconDocDeserializer deserializer = new LexiconDocDeserializer();

Lexicons lex = new Lexicons();
lex.add(deserializer.deserialize(
    Files.readString(Path.of("lexicons/app.bsky.feed.post.json"))
));
lex.add(deserializer.deserialize(
    Files.readString(Path.of("lexicons/app.bsky.actor.profile.json"))
));
```

**3. Create a client and make calls**

```java
import com.atproto.xrpc.XrpcClient;
import com.atproto.xrpc.QueryParams;
import com.atproto.xrpc.XrpcResponse;
import com.atproto.lex.data.*;

XrpcClient client = new XrpcClient("https://bsky.social", lex);
client.setHeader("Authorization", () -> "Bearer " + getAccessJwt());

QueryParams params = new QueryParams().put("actor", "alice.bsky.social");
XrpcResponse response = client.call("app.bsky.actor.getProfile", params, null, null);

LexMap profile = (LexMap) response.data();
String displayName = ((LexString) profile.fields().get("displayName")).value();
```

**4. Validate and create records**

```java
LexMap post = new LexMap(Map.of(
    "$type",     new LexString("app.bsky.feed.post"),
    "text",      new LexString("Hello from Java!"),
    "createdAt", new LexString("2024-01-15T12:30:00.000Z")
));

// Validate locally before submitting
lex.assertValidRecord("app.bsky.feed.post", post);

// Then create via XRPC
client.call("com.atproto.repo.createRecord", null, new LexMap(Map.of(
    "repo",       new LexString(myDid),
    "collection", new LexString("app.bsky.feed.post"),
    "record",     post
)), null);
```

## Data Model

The AT Protocol uses a [data model](https://atproto.com/specs/data-model) that extends JSON with two additional data structures: **CIDs** (content-addressed links) and **bytes** (for raw binary data). This data model can be encoded either as JSON for XRPC (HTTP API) or as [DAG-CBOR](https://ipld.io/specs/codecs/dag-cbor/spec/) for storage and authentication.

### Types

`LexValue` is the root sealed interface representing any valid AT Protocol data value:

```
LexValue
├── LexNull                            — JSON null
├── LexBoolean(boolean value)          — JSON boolean
├── LexInteger(long value)             — JSON integer (no floats in AT Protocol)
├── LexString(String value)            — JSON string
├── LexBytes(byte[] value)             — raw binary data
├── LexCidLink(Cid cid)                — content-addressed link
├── LexArray(List<LexValue> items)     — JSON array
├── LexMap(Map<String, LexValue>)      — JSON object; $type field used as discriminator
└── LexBlobRef(BlobRef ref)            — typed or legacy blob reference
```

All subtypes are Java records. Java 21 pattern-matching switch works across the full hierarchy:

```java
import com.atproto.lex.data.*;

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

`LexBytes` defensively copies its array on construction and in its accessor. `LexArray` and `LexMap` expose unmodifiable views.

#### `Cid`

Content identifiers (CIDv1, dag-cbor, sha2-256):

```java
import com.atproto.lex.data.Cid;
import com.atproto.lex.data.Multihash;

// Parse from a multibase base32 string (the "b" prefix is mandatory)
Cid cid = Cid.parse("bafyreiabc...");
assert cid.version() == 1;
assert cid.codec()   == Cid.CODEC_DAG_CBOR;

// Serialise to/from bytes (36 bytes: version + codec + multihash)
byte[] bytes      = cid.toBytes();
Cid    roundTrip  = Cid.fromBytes(bytes);
String base32     = cid.toBase32();  // "b..." multibase prefix

// Build a CID from a raw SHA-256 digest
byte[] digest = MessageDigest.getInstance("SHA-256").digest(data);
Cid manual = new Cid(1, Cid.CODEC_DAG_CBOR, new Multihash(Cid.MH_SHA2_256, digest));
```

### JSON Encoding

In JSON, CIDs are encoded as `{"$link": "bafyrei..."}` and bytes as `{"$bytes": "base64..."}`. `LexJsonConverter` translates between `LexValue` and JSON:

```java
import com.atproto.lex.json.LexJsonConverter;
import com.atproto.lex.data.*;

LexJsonConverter converter = new LexJsonConverter();

// LexValue → JSON string
String json = converter.lexToJsonString(myValue);

// JSON string → LexValue
LexValue value = converter.jsonStringToLex(json);

// Also works with Jackson JsonNode directly
JsonNode node = converter.lexToJson(myValue);
LexValue fromNode = converter.jsonToLex(node);
```

Special encodings are handled automatically:

| JSON shape | LexValue |
|---|---|
| `{"$link": "bafy..."}` | `LexCidLink` |
| `{"$bytes": "base64..."}` | `LexBytes` |
| `{"$type": "blob", "ref": {...}, ...}` | `LexBlobRef(TypedBlobRef)` |
| `{"cid": "...", "mimeType": "..."}` | `LexBlobRef(LegacyBlobRef)` |

Any object with a `"__proto__"` key is rejected to prevent prototype-pollution-style attacks.

### CBOR Encoding

Use `CborCodec` to encode/decode the data model to/from DAG-CBOR format for storage and authentication:

```java
import com.atproto.lex.cbor.CborCodec;
import com.atproto.lex.cbor.CidUtils;

CborCodec codec = new CborCodec();

// Encode to DAG-CBOR bytes (map keys sorted deterministically)
byte[] cborBytes = codec.encode(myValue);

// Decode from DAG-CBOR bytes
LexValue decoded = codec.decode(cborBytes);

// Generate a CIDv1 (dag-cbor, sha2-256) for any LexValue
CidUtils cidUtils = new CidUtils();
Cid cid = cidUtils.cidForLex(myValue);
```

CID links are encoded as CBOR tag 42 with the `0x00` multibase identity prefix, as required by the IPLD spec.

## Schema Validation

The `Lexicons` class is a registry for Lexicon schema documents. Once loaded, it validates records and XRPC payloads against their schemas at runtime.

### Loading Schemas

```java
import com.atproto.lexicon.Lexicons;
import com.atproto.lexicon.LexiconDocDeserializer;

LexiconDocDeserializer deserializer = new LexiconDocDeserializer();
Lexicons lex = new Lexicons();

// Add schemas — can be loaded from files, classpath resources, or network responses
lex.add(deserializer.deserialize(Files.readString(Path.of("app.bsky.feed.post.json"))));
lex.add(deserializer.deserialize(Files.readString(Path.of("app.bsky.actor.profile.json"))));

// Remove a schema by NSID
lex.remove("app.bsky.feed.post");

// Look up a specific definition by URI (bare NSID resolves to #main)
LexUserType def = lex.getDef("app.bsky.feed.post");         // → app.bsky.feed.post#main
LexUserType def2 = lex.getDef("app.bsky.feed.defs#postView");
```

### Validating Records and Payloads

```java
// Validate a record against its schema (throws ValidationError on failure)
lex.assertValidRecord("app.bsky.feed.post", record);

// Validate XRPC query params
lex.assertValidXrpcParams("app.bsky.feed.getTimeline", params);

// Validate XRPC response output
lex.assertValidXrpcOutput("app.bsky.feed.getTimeline", output);

// Validate XRPC request input
lex.assertValidXrpcInput("com.atproto.repo.createRecord", input);
```

Validation enforces all schema constraints: required fields, nullable fields, default values, string format checks, array length limits, integer bounds, and `$type` discriminator matching.

When validation succeeds, the returned `LexValue` may differ from the input if defaults were applied. The validator uses a lazy-clone strategy: the original object is returned unchanged when no modifications are needed, so identity checks (`==`) can be used to detect whether defaults were applied.

### String Format Validators

The validator enforces AT Protocol string format constraints for all fields declared with a `format`:

| Format | Example |
|---|---|
| `did` | `did:plc:ewvi7nxzyoun6zhhandbv25z` |
| `handle` | `alice.bsky.social` |
| `nsid` | `app.bsky.feed.post` |
| `at-uri` | `at://did:plc:.../app.bsky.feed.post/3k4...` |
| `cid` | `bafyreiabc...` |
| `datetime` | `2024-01-15T12:30:00.000Z` |
| `language` | `en`, `zh-Hans` |
| `tid` | `3k4duaz5drk2m` |
| `record-key` | `self`, `3k4duaz5drk2m` |

## Making XRPC Requests

[XRPC](https://atproto.com/specs/xrpc) is the HTTP convention used by AT Protocol. Endpoints follow the pattern `/xrpc/<nsid>`. **Queries** (HTTP GET) are for read operations; **procedures** (HTTP POST) are for mutations.

```java
import com.atproto.xrpc.XrpcClient;
import com.atproto.xrpc.QueryParams;
import com.atproto.xrpc.XrpcResponse;

XrpcClient client = new XrpcClient("https://public.api.bsky.app", new Lexicons());

// Query (GET) — params in the URL, no request body
QueryParams params = new QueryParams().put("handle", "atproto.com");
XrpcResponse response = client.call(
    "com.atproto.identity.resolveHandle",
    params,
    null,   // no request body
    null    // no call options
);

LexMap result = (LexMap) response.data();
String did = ((LexString) result.fields().get("did")).value();
```

When a `Lexicons` registry is provided and a schema is loaded for the NSID, the client determines GET vs POST from the schema and validates the response output after parsing. Pass an empty `new Lexicons()` to make schema-unaware calls.

## Client API

`XrpcClient` is the main entry point. It wraps a `FetchHandler` (backed by OkHttp3 by default) and an optional `Lexicons` registry.

### Creating a Client

#### Unauthenticated

```java
import com.atproto.xrpc.XrpcClient;
import com.atproto.lexicon.Lexicons;

XrpcClient client = new XrpcClient("https://public.api.bsky.app", new Lexicons());
```

#### Authenticated

```java
XrpcClient client = new XrpcClient("https://bsky.social", lex);
client.setHeader("Authorization", () -> "Bearer " + tokenStore.getAccessJwt());
```

### Dynamic Headers

Header values can be static strings or dynamic suppliers — the supplier is called on every request:

```java
// Static value
client.setHeader("X-App-Version", "1.0.0");

// Dynamic value (e.g. rotating access token)
client.setHeader("Authorization", () -> "Bearer " + tokenStore.getAccessJwt());

// Remove a header
client.unsetHeader("Authorization");
```

### Core Methods

#### `client.call()`

Make a synchronous XRPC call. The HTTP method (GET vs POST) is determined from the loaded schema, or defaults to POST if no schema is loaded:

```java
// Query (GET)
XrpcResponse profileResp = client.call(
    "app.bsky.actor.getProfile",
    new QueryParams().put("actor", "alice.bsky.social"),
    null, null
);
LexMap profile = (LexMap) profileResp.data();

// Procedure (POST)
XrpcResponse createResp = client.call(
    "com.atproto.repo.createRecord",
    null,
    new LexMap(Map.of(
        "repo",       new LexString(myDid),
        "collection", new LexString("app.bsky.feed.post"),
        "record",     post
    )),
    null
);
LexMap created = (LexMap) createResp.data();
String uri = ((LexString) created.fields().get("uri")).value();
String cid = ((LexString) created.fields().get("cid")).value();
```

### Async Calls

For non-blocking execution use `callAsync()`, which returns a `CompletableFuture<XrpcResponse>`:

```java
client.callAsync("app.bsky.feed.getTimeline", params, null, null)
    .thenApply(resp -> (LexMap) resp.data())
    .thenAccept(timeline -> render(timeline))
    .exceptionally(ex -> { handleError(ex); return null; });
```

### Error Handling

All client methods throw `XrpcException` (unchecked) on failure. Use the hierarchy for fine-grained handling:

```java
import com.atproto.xrpc.error.*;

try {
    XrpcResponse resp = client.call("app.bsky.actor.getProfile", params, null, null);
} catch (XrpcAuthenticationException e) {
    // HTTP 401 — refresh the access token and retry
    refreshToken();
    // retry...
} catch (XrpcResponseException e) {
    // HTTP 4xx/5xx from the server
    System.err.println("Error " + e.statusCode() + " [" + e.errorCode() + "]: " + e.getMessage());
} catch (XrpcResponseValidationException e) {
    // 2xx response that failed schema validation
} catch (XrpcInvalidResponseException e) {
    // 2xx response that is not valid XRPC (malformed JSON, unexpected content type, etc.)
} catch (XrpcFetchException e) {
    // Network failure — no HTTP response received (timeout, connection refused, etc.)
} catch (XrpcException e) {
    // Any other XRPC error
}
```

Exception hierarchy:

```
XrpcException  (extends LexError / RuntimeException)
├── XrpcResponseException               HTTP 4xx/5xx response
│   └── XrpcAuthenticationException     HTTP 401
├── XrpcInvalidResponseException        2xx response that is not valid XRPC
│   └── XrpcResponseValidationException 2xx response that failed schema validation
└── XrpcFetchException                  network/IO failure (no response received)
```

### Custom Transport

Implement `FetchHandler` to plug in your own HTTP stack, add tracing, configure retries, or mock calls in tests:

```java
import com.atproto.xrpc.FetchHandler;
import com.atproto.xrpc.XrpcClient;

OkHttpClient tracingHttpClient = new OkHttpClient.Builder()
    .addInterceptor(new TracingInterceptor())
    .connectTimeout(10, TimeUnit.SECONDS)
    .readTimeout(30, TimeUnit.SECONDS)
    .build();

FetchHandler handler = (urlPath, requestBuilder) ->
    tracingHttpClient.newCall(requestBuilder.url(urlPath).build()).execute();

XrpcClient client = new XrpcClient(handler, lex);
```

## Utilities

### CID Utilities

```java
import com.atproto.lex.data.Cid;
import com.atproto.lex.data.Multihash;
import com.atproto.lex.cbor.CidUtils;

// Parse from multibase base32 string
Cid cid = Cid.parse("bafyreiabc...");

// Byte layout: [0x01 version][0x71 dag-cbor][0x12 sha2-256][0x20 len=32][32-byte digest]
byte[] bytes = cid.toBytes();   // always 36 bytes for SHA-256 CIDv1
String base32 = cid.toBase32(); // always 59 chars including "b" prefix

// Parse from raw bytes (e.g. from the CBOR tag-42 payload after stripping 0x00 prefix)
Cid fromBytes = Cid.fromBytes(bytes);

// Compute the CID for any LexValue (dag-cbor encoded, sha2-256 hashed)
Cid contentCid = new CidUtils().cidForLex(myLexValue);
```

### Blob References

In AT Protocol, binary data (blobs) are referenced with metadata (MIME type, size). Two formats exist in the network:

#### TypedBlobRef — current standard

```java
import com.atproto.lex.data.BlobRef.TypedBlobRef;

TypedBlobRef blob = new TypedBlobRef(cid, "image/png", 12345);
blob.ref()      // Cid
blob.mimeType() // "image/png"
blob.size()     // 12345
```

Always use `TypedBlobRef` when creating new blobs. This is the format returned by `com.atproto.repo.uploadBlob` and expected by PDS endpoints.

#### LegacyBlobRef — historical format

```java
import com.atproto.lex.data.BlobRef.LegacyBlobRef;

// Older records on the network use this format (no $type, no size)
LegacyBlobRef legacy = new LegacyBlobRef("bafyrei...", "image/jpeg");
legacy.cid()      // String (not a Cid object)
legacy.mimeType() // "image/jpeg"
```

> [!IMPORTANT]
>
> Legacy blob references still exist in older AT Protocol records. In strict validation mode they are rejected; load schemas and validate data from unknown sources with `assertValidRecord()`. When reading data from the network, always handle both formats:

```java
import com.atproto.lex.data.BlobRef;
import com.atproto.lex.data.LexBlobRef;

if (value instanceof LexBlobRef blobRef) {
    BlobRef ref = blobRef.ref();
    if (ref instanceof BlobRef.TypedBlobRef typed) {
        System.out.println("modern: " + typed.mimeType() + " size=" + typed.size());
    } else if (ref instanceof BlobRef.LegacyBlobRef legacy) {
        System.out.println("legacy: " + legacy.mimeType());
    }
}
```

### Deep Equality

`LexValueDeepEquals` compares two `LexValue` trees for structural equality, with correct handling of `LexBytes` (array content, not reference) and `LexCidLink` (CID value equality):

```java
import com.atproto.lex.data.util.LexValueDeepEquals;

boolean equal = LexValueDeepEquals.equals(value1, value2);
```

The comparison uses iterative traversal with cycle detection via `IdentityHashMap`, safe for arbitrarily deep structures.

## Build

### Bazel targets

| What | Target |
|---|---|
| Build everything | `bazel build //src/java/...` |
| Test everything | `bazel test //src/test/...` |
| `lex-data` library | `//src/java/com/atproto/lex/data:lex_data` |
| `lex-json` library | `//src/java/com/atproto/lex/json:lex_json` |
| `lex-cbor` library | `//src/java/com/atproto/lex/cbor:lex_cbor` |
| `lexicon` library | `//src/java/com/atproto/lexicon:lexicon` |
| `xrpc` library | `//src/java/com/atproto/xrpc:xrpc` |

Requires Bazel 9+ with bzlmod. Java 21 is configured in `.bazelrc`; no flags needed.

### Publishing to Maven

Each module has a `java_export` target that generates a POM and a publish task:

```bash
bazel run //src/java/com/atproto/lex/data:lex_data_export.publish -- \
  --maven_repo=file://${HOME}/.m2/repository

bazel run //src/java/com/atproto/lex/json:lex_json_export.publish -- \
  --maven_repo=file://${HOME}/.m2/repository

bazel run //src/java/com/atproto/lex/cbor:lex_cbor_export.publish -- \
  --maven_repo=file://${HOME}/.m2/repository

bazel run //src/java/com/atproto/lexicon:lexicon_export.publish -- \
  --maven_repo=file://${HOME}/.m2/repository

bazel run //src/java/com/atproto/xrpc:xrpc_export.publish -- \
  --maven_repo=file://${HOME}/.m2/repository
```

Maven coordinates:

| Module | Coordinate |
|---|---|
| Core data model | `com.atproto:lex-data:0.1.0` |
| JSON encoding | `com.atproto:lex-json:0.1.0` |
| CBOR encoding | `com.atproto:lex-cbor:0.1.0` |
| Lexicon validation | `com.atproto:lexicon:0.1.0` |
| XRPC client | `com.atproto:xrpc:0.1.0` |

Most users only need `com.atproto:xrpc:0.1.0` — it transitively pulls in all other modules and their POM dependencies.

### Package Structure

```
src/java/com/atproto/
├── lex/data/           LexValue sealed hierarchy, Cid, BlobRef, LexError
│   └── util/           LexValueDeepEquals, Base64Utils, Utf8Utils
├── lex/json/           JSON ↔ LexValue (Jackson)
├── lex/cbor/           DAG-CBOR encode/decode, CID generation
├── lexicon/            Lexicons registry, LexiconDocDeserializer
│   ├── schema/         LexiconDoc, LexUserType sealed hierarchy
│   └── validation/     LexiconValidator, StringFormatValidator, ValidationResult
└── xrpc/               XrpcClient, FetchHandler, OkHttpFetchHandler
    ├── error/          XrpcException hierarchy
    └── internal/       RequestBuilder, ResponseParser
```

## License

MIT — see [LICENSE](LICENSE).
