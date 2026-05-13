package com.atproto.xrpc.error;

/** HTTP 401 — authentication required. */
public class XrpcAuthenticationException extends XrpcResponseException {
  public XrpcAuthenticationException(String message) {
    super(401, "AuthenticationRequired", message);
  }
}
