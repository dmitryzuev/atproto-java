package com.atproto.xrpc.error;

import com.atproto.lex.data.LexError;
import com.atproto.xrpc.ResponseType;

/**
 * Base exception for all XRPC errors.
 */
public class XrpcException extends LexError {

    private final ResponseType responseType;
    private final String errorCode;

    public XrpcException(ResponseType responseType, String message, String errorCode) {
        super(message);
        this.responseType = responseType;
        this.errorCode = errorCode;
    }

    public XrpcException(ResponseType responseType, String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.responseType = responseType;
        this.errorCode = errorCode;
    }

    public ResponseType responseType() { return responseType; }
    public String errorCode()          { return errorCode; }

    public static XrpcException from(Throwable cause, int statusCode) {
        ResponseType rt = ResponseType.fromHttpCode(statusCode);
        return new XrpcException(rt, cause.getMessage(), rt.name(), cause);
    }
}
