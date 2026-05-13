package io.atproto.xrpc.internal;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.atproto.lex.data.LexNull;
import io.atproto.lex.data.LexValue;
import io.atproto.lex.json.LexJsonConverter;
import io.atproto.xrpc.XrpcResponse;
import io.atproto.xrpc.error.*;
import okhttp3.Response;
import okhttp3.ResponseBody;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Parses raw OkHttp responses into {@link XrpcResponse} or throws the appropriate exception.
 */
public final class ResponseParser {

    private final LexJsonConverter converter;
    private final ObjectMapper mapper;

    public ResponseParser(LexJsonConverter converter) {
        this.converter = converter;
        this.mapper = new ObjectMapper();
    }

    public XrpcResponse parse(String nsid, Response response) throws XrpcException {
        int status = response.code();
        Map<String, String> headers = collectHeaders(response);

        // Handle authentication error
        if (status == 401) {
            throw new XrpcAuthenticationException(extractError(response, "Authentication required"));
        }

        // Handle other error responses
        if (status < 200 || status >= 300) {
            ErrorPayload err = parseErrorPayload(response);
            throw new XrpcResponseException(status, err.error(), err.message());
        }

        // Success — parse body
        LexValue body = parseSuccessBody(nsid, response);
        return new XrpcResponse(body, headers, status);
    }

    private LexValue parseSuccessBody(String nsid, Response response) throws XrpcException {
        ResponseBody body = response.body();
        if (body == null) return LexNull.INSTANCE;

        String contentType = response.header("Content-Type", "");
        try {
            if (contentType.startsWith("application/json") || contentType.startsWith("application/ld+json")) {
                String json = body.string();
                if (json.isBlank()) return LexNull.INSTANCE;
                return converter.jsonStringToLex(json);
            } else if (contentType.startsWith("application/cbor")) {
                // Return raw bytes for CBOR — caller can decode with CborCodec
                return new io.atproto.lex.data.LexBytes(body.bytes());
            } else {
                // Binary or unknown — return raw bytes
                return new io.atproto.lex.data.LexBytes(body.bytes());
            }
        } catch (IOException e) {
            throw new XrpcInvalidResponseException(nsid, "Failed to read response body", e);
        }
    }

    private ErrorPayload parseErrorPayload(Response response) {
        try {
            ResponseBody body = response.body();
            if (body == null) return new ErrorPayload("Error", null);
            String json = body.string();
            if (json.isBlank()) return new ErrorPayload("Error", null);
            JsonNode node = mapper.readTree(json);
            String error = node.path("error").textValue();
            String message = node.path("message").textValue();
            return new ErrorPayload(error != null ? error : "Error", message);
        } catch (IOException e) {
            return new ErrorPayload("Error", e.getMessage());
        }
    }

    private String extractError(Response response, String fallback) {
        ErrorPayload p = parseErrorPayload(response);
        return p.message() != null ? p.message() : fallback;
    }

    private Map<String, String> collectHeaders(Response response) {
        Map<String, String> headers = new HashMap<>();
        response.headers().forEach(pair -> headers.put(pair.getFirst(), pair.getSecond()));
        return headers;
    }

    private record ErrorPayload(String error, String message) {}
}
