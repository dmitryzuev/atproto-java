package com.atproto.xrpc.error;

import com.atproto.xrpc.ResponseType;

/** Network-level failure (no HTTP response received). */
public class XrpcFetchException extends XrpcException {
  public XrpcFetchException(Throwable cause) {
    super(ResponseType.Unknown, "Network error: " + cause.getMessage(), "FetchError", cause);
  }
}
