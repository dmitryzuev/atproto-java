#!/usr/bin/env bash
set -euo pipefail

MAVEN_REPO="${1:?Usage: publish_all.sh <maven_repo_url>}"

MODULES=(
  "//src/java/com/atproto/lex/data:lex_data_export.publish"
  "//src/java/com/atproto/lex/json:lex_json_export.publish"
  "//src/java/com/atproto/lex/cbor:lex_cbor_export.publish"
  "//src/java/com/atproto/lexicon:lexicon_export.publish"
  "//src/java/com/atproto/xrpc:xrpc_export.publish"
)

for target in "${MODULES[@]}"; do
  echo "Publishing $target..."
  bazel run --define "maven_repo=$MAVEN_REPO" "$target"
done

echo "All modules published."
