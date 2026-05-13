#!/usr/bin/env bash
set -euo pipefail

MODULES=(
  "//src/java/com/atproto/lex/data:lex_data_export"
  "//src/java/com/atproto/lex/json:lex_json_export"
  "//src/java/com/atproto/lex/cbor:lex_cbor_export"
  "//src/java/com/atproto/lexicon:lexicon_export"
  "//src/java/com/atproto/xrpc:xrpc_export"
)

for target in "${MODULES[@]}"; do
  echo "Publishing $target..."
  bazel run "$target" -- "$@"
done

echo "All modules published."
