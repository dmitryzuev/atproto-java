package com.atproto.xrpc;

import java.util.Map;

public record CallOptions(String encoding, Map<String, String> headers) {
  public static final CallOptions EMPTY = new CallOptions(null, null);

  public static CallOptions withEncoding(String encoding) {
    return new CallOptions(encoding, null);
  }

  public static CallOptions withHeaders(Map<String, String> headers) {
    return new CallOptions(null, headers);
  }
}
