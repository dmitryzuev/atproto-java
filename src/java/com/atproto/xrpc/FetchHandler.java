package com.atproto.xrpc;

import java.io.IOException;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Low-level HTTP transport abstraction. Implementations handle the actual HTTP send/receive;
 * XrpcClient handles XRPC semantics.
 */
@FunctionalInterface
public interface FetchHandler {
  /**
   * @param urlPath Full URL (including query string, no trailing slash).
   * @param builder Pre-configured request builder (method and body already set by XrpcClient).
   * @return The raw OkHttp response (caller is responsible for closing).
   */
  Response fetch(String urlPath, Request.Builder builder) throws IOException;
}
