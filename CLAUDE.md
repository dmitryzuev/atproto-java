# CLAUDE.md — atproto-sdk

Project-local knowledge for Claude Code. Captures decisions, pitfalls, and conventions that aren't obvious from the code.

---

## What this project is

Java 21 port of the TypeScript `@atproto/lex` SDK (AT Protocol — the open protocol behind Bluesky). Five modules in a flat source tree under `src/java/com/atproto/` and `src/test/java/com/atproto/`.

---

## Build system: Bazel 9 with bzlmod

- **Bazel version**: 9.x. Rules that worked in Bazel 7/8 may not work here.
- **`MODULE.bazel`** declares all deps. `use_repo(maven, "maven")` is the only entry — individual repo aliases are not needed because all BUILD files reference `@maven//:artifact_name`.
- **`.bazelrc`** sets `--java_language_version=21` and `--java_runtime_version=remotejdk_21` globally. Without this, tests compile as Java 11 and text blocks / sealed types fail.
- **`rules_java` load required**: `java_library` and `java_test` are NOT in global scope in Bazel 9. Every BUILD file must `load("@rules_java//java:defs.bzl", "java_library")` or `load("@rules_java//java:defs.bzl", "java_test")`.
- **`contrib_rules_jvm` is NOT used** — it defines `junit5_test` but its `pmd.bzl` uses `JavaInfo` as a global, which was removed in Bazel 9. The project replaces it with a local macro.

## JUnit 5 test runner

JUnit 5 tests use a local macro defined in `tools/junit5.bzl`. All test BUILD files load it:

```python
load("//tools:junit5.bzl", "junit5_test")
```

The macro runs tests via the JUnit Platform `ConsoleLauncher` (`use_testrunner = False`). It automatically adds `junit-jupiter-engine`, `junit-platform-launcher`, and `junit-platform-console` as runtime deps — test BUILD files do not need to list them.

Do **not** switch back to `contrib_rules_jvm` without first checking whether the version in use has fixed the Bazel 9 `JavaInfo` global removal.

---

## Bazel targets

| What | Target |
|---|---|
| Build everything | `bazel build //src/java/...` |
| Test everything | `bazel test //src/test/...` |
| lex/data library | `//src/java/com/atproto/lex/data:lex_data` |
| lex/json library | `//src/java/com/atproto/lex/json:lex_json` |
| lex/cbor library | `//src/java/com/atproto/lex/cbor:lex_cbor` |
| lexicon library | `//src/java/com/atproto/lexicon:lexicon` |
| xrpc library | `//src/java/com/atproto/xrpc:xrpc` |
| Test resources filegroup | `//src/test/resources:testdata` |

---

## Package structure and circular-dependency avoidance

Each library's `BUILD.bazel` uses `glob(["**/*.java"])` to include subdirectories:

- `lexicon` includes `schema/` and `validation/` in the same Bazel package because `Lexicons` ↔ `LexiconValidator` is circular if split.
- `xrpc` includes `error/` and `internal/` in the same Bazel package for the same reason.

Do not add separate `BUILD.bazel` files inside `lexicon/schema/`, `lexicon/validation/`, `xrpc/error/`, or `xrpc/internal/` — that would create circular dependencies.

---

## Type-name collision: data vs schema package

Both `com.atproto.lex.data` and `com.atproto.lexicon.schema` define types with the same simple names:

| Conflicting name | Data type | Schema type |
|---|---|---|
| `LexArray` | `LexValue` subtype | Schema def for array fields |
| `LexBoolean` | `LexValue` subtype | Schema def for boolean fields |
| `LexBytes` | `LexValue` subtype | Schema def for bytes fields |
| `LexCidLink` | `LexValue` subtype | Schema def for CID link fields |
| `LexInteger` | `LexValue` subtype | Schema def for integer fields |
| `LexString` | `LexValue` subtype | Schema def for string fields |

**Rule**: any file that imports both packages with wildcards will get an ambiguity error. The fix used throughout the project: keep `import com.atproto.lexicon.schema.*` as the wildcard, and add **explicit** imports for the conflicting data types (`import com.atproto.lex.data.LexArray;` etc.). Explicit imports beat wildcard imports, so `LexArray` resolves to the data type while schema types use their fully-qualified form in switch expressions.

In `LexiconValidator.java`, switch cases over `LexUserType` that match conflicting schema names must use the fully-qualified form:
```java
case com.atproto.lexicon.schema.LexArray arr -> ...
case com.atproto.lexicon.schema.LexCidLink ignored -> ...
```

---

## CBORObject API (com.upokecenter:cbor:4.5.2)

The cbor4j 4.5.x API does **not** have `FromInt64`, `FromString`, `FromByteArray`, or `FromCBORObjectAndTag`. Use:

| Old (wrong) | Correct |
|---|---|
| `CBORObject.FromInt64(x)` | `CBORObject.FromObject(x)` |
| `CBORObject.FromString(x)` | `CBORObject.FromObject(x)` |
| `CBORObject.FromByteArray(x)` | `CBORObject.FromObject(x)` |
| `CBORObject.FromCBORObjectAndTag(obj, tag)` | `CBORObject.FromObjectAndTag(obj, tag)` |

---

## CID byte format

A CIDv1 with SHA-256 is always **36 bytes** and encodes to exactly **59 base32 characters** (including the `b` multibase prefix). If you see a 58-char CID in test fixtures, it's missing one character and will fail `Cid.fromBytes()` with "Truncated CID bytes".

Wire layout: `[0x01][0x71][0x12][0x20][32-byte digest]`
- `0x01` = version 1
- `0x71` = dag-cbor codec
- `0x12` = SHA-256 multihash code
- `0x20` = digest length (32)

In tests, construct CIDs programmatically rather than using hardcoded strings:
```java
byte[] digest = new byte[32];
for (int i = 0; i < digest.length; i++) digest[i] = (byte) (i + 1);
Cid cid = new Cid(1, Cid.CODEC_DAG_CBOR, new Multihash(Cid.MH_SHA2_256, digest));
```

`Multihash` is a **top-level class** in `com.atproto.lex.data` — not a nested class inside `Cid`.

---

## DAG-CBOR CID link encoding

CID links in DAG-CBOR are encoded as **CBOR tag 42** wrapping a byte string of `[0x00 || cid.toBytes()]`. The leading `0x00` is the multibase identity prefix required by the IPLD spec.

---

## Maven artifact names in BUILD files

`rules_jvm_external` converts Maven coordinates to Bazel labels by replacing `.` and `:` and `-` with `_`. Examples:

| Maven coordinate | Bazel label |
|---|---|
| `com.fasterxml.jackson.core:jackson-databind` | `@maven//:com_fasterxml_jackson_core_jackson_databind` |
| `com.squareup.okhttp3:mockwebserver` | `@maven//:com_squareup_okhttp3_mockwebserver` |
| `org.junit.platform:junit-platform-launcher` | `@maven//:org_junit_platform_junit_platform_launcher` |

---

## Maven publishing with `java_export`

Each library package has **two targets**: a `java_library` for internal deps/tests, and a `java_export` for publishing. They live side by side in the same `BUILD.bazel`.

```python
load("@rules_java//java:defs.bzl", "java_library")
load("@rules_jvm_external//:defs.bzl", "java_export")

java_library(
    name = "lex_data",
    srcs = glob(["**/*.java"]),
    javacopts = ["--release", "21"],
    visibility = ["//visibility:public"],
)

java_export(
    name = "lex_data_export",
    tags = ["no-javadocs"],
    maven_coordinates = "com.atproto:lex-data:0.1.0",
    pom_template = "//maven:pom_template.xml",
    exports = [":lex_data"],
    visibility = ["//visibility:public"],
)
```

Key rules:
- **`exports`, not `deps`**: `java_export` without `srcs` must use `exports` to reference the compiled library. Using `deps` instead causes "deps not allowed without srcs".
- **`tags = ["no-javadocs"]`**: Required on all `java_export` targets. Java 21 sealed types cause the standard javadoc tool to fail; this tag skips javadoc JAR generation.
- **`pom_template`**: Declared in `maven/BUILD.bazel` via `exports_files(["pom_template.xml"])`. The `{dependencies}` placeholder is auto-populated by traversing the dep graph for targets tagged with `maven_coordinates`.
- **Inter-module exports**: For modules that depend on other modules, include the peer `_export` targets (not the `java_library` targets) in `exports` so the POM lists the correct Maven coordinates as dependencies:

```python
java_export(
    name = "xrpc_export",
    tags = ["no-javadocs"],
    maven_coordinates = "com.atproto:xrpc:0.1.0",
    pom_template = "//maven:pom_template.xml",
    exports = [
        ":xrpc",
        "//src/java/com/atproto/lex/data:lex_data_export",
        "//src/java/com/atproto/lex/json:lex_json_export",
        "//src/java/com/atproto/lexicon:lexicon_export",
    ],
    visibility = ["//visibility:public"],
)
```

Publishing command:
```bash
bazel run --define "maven_repo=file://$HOME/.m2/repository" \
  //src/java/com/atproto/xrpc:xrpc_export.publish
```

---

## README structure

The `README.md` mirrors the structure and depth of the upstream TypeScript `@atproto/lex` README. When updating or extending the README, keep the same section order:

1. Preview callout → tagline → feature bullets → three code examples
2. Linked TOC
3. Quick Start (numbered steps)
4. Data Model (Types, JSON Encoding, CBOR Encoding)
5. Schema Validation (Loading Schemas, Validating Records, String Format Validators)
6. Making XRPC Requests
7. Client API (Creating, Dynamic Headers, Core Methods, Async, Error Handling, Custom Transport)
8. Utilities (CID Utilities, Blob References, Deep Equality)
9. Build (Bazel targets, Publishing, Package Structure)
10. License
