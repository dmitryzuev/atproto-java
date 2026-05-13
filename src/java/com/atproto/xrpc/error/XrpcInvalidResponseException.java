package com.atproto.xrpc.error;

import com.atproto.xrpc.ResponseType;

/** Malformed or non-compliant server response. */
public class XrpcInvalidResponseException extends XrpcException {

    private final String lexiconNsid;

    public XrpcInvalidResponseException(String lexiconNsid, String message) {
        super(ResponseType.InvalidResponse, message, "InvalidResponse");
        this.lexiconNsid = lexiconNsid;
    }

    public XrpcInvalidResponseException(String lexiconNsid, String message, Throwable cause) {
        super(ResponseType.InvalidResponse, message, "InvalidResponse", cause);
        this.lexiconNsid = lexiconNsid;
    }

    public String lexiconNsid() { return lexiconNsid; }
}
