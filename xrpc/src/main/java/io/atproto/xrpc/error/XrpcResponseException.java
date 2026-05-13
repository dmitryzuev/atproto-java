package io.atproto.xrpc.error;

import io.atproto.xrpc.ResponseType;

/** HTTP 4xx/5xx response with valid XRPC error payload. */
public class XrpcResponseException extends XrpcException {

    private final int statusCode;
    private final String errorMessage;

    public XrpcResponseException(int statusCode, String errorCode, String errorMessage) {
        super(ResponseType.fromHttpCode(statusCode),
              errorMessage != null ? errorMessage : errorCode,
              errorCode);
        this.statusCode = statusCode;
        this.errorMessage = errorMessage;
    }

    public int statusCode()    { return statusCode; }
    public String errorMessage() { return errorMessage; }
}
