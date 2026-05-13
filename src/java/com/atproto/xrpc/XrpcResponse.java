package com.atproto.xrpc;

import com.atproto.lex.data.LexValue;

import java.util.Map;

/**
 * A successful XRPC response.
 */
public record XrpcResponse(
        LexValue data,
        Map<String, String> headers,
        int status
) {
    public boolean isSuccess() {
        return status >= 200 && status < 300;
    }
}
