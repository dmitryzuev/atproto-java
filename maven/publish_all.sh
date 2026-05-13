#!/usr/bin/env bash
set -euo pipefail

MODULES=(
  "//src/java/com/atproto/lex/data:lex_data.publish"
  "//src/java/com/atproto/lex/json:lex_json.publish"
  "//src/java/com/atproto/lex/cbor:lex_cbor.publish"
  "//src/java/com/atproto/lexicon:lexicon.publish"
  "//src/java/com/atproto/xrpc:xrpc.publish"
)

for target in "${MODULES[@]}"; do
  echo "Publishing $target..."
  bazel run "$target" -- "$@"
done

echo "All modules published."
