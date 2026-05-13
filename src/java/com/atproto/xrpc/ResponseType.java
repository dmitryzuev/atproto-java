package com.atproto.xrpc;

/**
 * Canonical XRPC response type codes, mirroring the TypeScript ResponseType enum.
 */
public enum ResponseType {
    Unknown(1),
    InvalidResponse(2),
    Success(200),
    InvalidRequest(400),
    AuthenticationRequired(401),
    Forbidden(403),
    XRPCNotSupported(404),
    NotAcceptable(406),
    PayloadTooLarge(413),
    UnsupportedMediaType(415),
    RateLimitExceeded(429),
    InternalServerError(500),
    NotImplemented(501),
    UpstreamFailure(502),
    NotEnoughResources(503),
    UpstreamTimeout(504);

    public final int code;

    ResponseType(int code) {
        this.code = code;
    }

    public static ResponseType fromHttpCode(int httpCode) {
        for (ResponseType t : values()) {
            if (t.code == httpCode) return t;
        }
        return httpCode >= 200 && httpCode < 300 ? Success : Unknown;
    }
}
