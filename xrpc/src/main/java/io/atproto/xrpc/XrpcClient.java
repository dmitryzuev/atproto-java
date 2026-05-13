package io.atproto.xrpc;

import io.atproto.lex.data.LexError;
import io.atproto.lex.data.LexValue;
import io.atproto.lex.json.LexJsonConverter;
import io.atproto.lexicon.Lexicons;
import io.atproto.lexicon.schema.LexiconDoc;
import io.atproto.lexicon.schema.LexUserType;
import io.atproto.xrpc.error.*;
import io.atproto.xrpc.internal.RequestBuilder;
import io.atproto.xrpc.internal.ResponseParser;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Supplier;

/**
 * XRPC client for the AT Protocol.
 *
 * Usage:
 * <pre>
 *   Lexicons lex = new Lexicons();
 *   // add your lexicon docs...
 *   XrpcClient client = new XrpcClient("https://bsky.social", lex);
 *   client.setHeader("Authorization", () -> "Bearer " + getToken());
 *   XrpcResponse resp = client.call("app.bsky.feed.getTimeline", params, null, null);
 * </pre>
 *
 * Not thread-safe with respect to header mutations.
 */
public final class XrpcClient {

    private final String serviceBaseUrl;
    private final FetchHandler fetchHandler;
    private final Lexicons lexicons;
    private final LexJsonConverter jsonConverter;
    private final ResponseParser responseParser;

    // Ordered map of header name -> supplier (lazy evaluation per request)
    private final Map<String, Supplier<String>> headers = new LinkedHashMap<>();

    public XrpcClient(String serviceBaseUrl, Lexicons lexicons) {
        this(serviceBaseUrl, new OkHttpFetchHandler(), lexicons);
    }

    public XrpcClient(String serviceBaseUrl, FetchHandler fetchHandler, Lexicons lexicons) {
        this.serviceBaseUrl = serviceBaseUrl;
        this.fetchHandler = fetchHandler;
        this.lexicons = lexicons;
        this.jsonConverter = new LexJsonConverter();
        this.responseParser = new ResponseParser(jsonConverter);
    }

    // ---- Header management ----

    public void setHeader(String key, String value) {
        headers.put(key, () -> value);
    }

    public void setHeader(String key, Supplier<String> supplier) {
        headers.put(key, supplier);
    }

    public void unsetHeader(String key) {
        headers.remove(key);
    }

    public void clearHeaders() {
        headers.clear();
    }

    // ---- Synchronous call ----

    /**
     * Make an XRPC call synchronously.
     *
     * @param nsid   The lexicon method NSID (e.g. "app.bsky.feed.getTimeline").
     * @param params Query parameters for GET requests (ignored for POST if data is provided).
     * @param data   Request body for POST procedures. Null for queries.
     * @param opts   Optional call options (encoding override, extra headers).
     * @return The parsed XRPC response.
     * @throws XrpcException on any XRPC or network error.
     */
    public XrpcResponse call(String nsid,
                             QueryParams params,
                             LexValue data,
                             CallOptions opts) throws XrpcException {
        // Resolve method definition from lexicon (optional — proceed if not found)
        Optional<LexiconDoc> doc = lexicons.get(nsid);
        LexUserType def = doc.map(LexiconDoc::mainDef).orElse(null);

        // Determine HTTP method
        String httpMethod = (def != null) ? RequestBuilder.httpMethod(def) : (data != null ? "POST" : "GET");

        // Build URL
        String url = RequestBuilder.buildUrl(serviceBaseUrl, nsid, params);

        // Build request
        Request.Builder reqBuilder = new Request.Builder();

        // Add SDK-level headers
        headers.forEach((key, supplier) -> {
            String val = supplier.get();
            if (val != null) reqBuilder.header(key, val);
        });

        // Add call-option headers
        if (opts != null && opts.headers() != null) {
            opts.headers().forEach(reqBuilder::header);
        }

        // Encode body
        try {
            if ("GET".equals(httpMethod)) {
                reqBuilder.get();
            } else {
                String contentType = (opts != null && opts.encoding() != null)
                        ? opts.encoding()
                        : "application/json";
                okhttp3.RequestBody body = data != null
                        ? RequestBuilder.encodeBody(data, contentType, jsonConverter)
                        : okhttp3.RequestBody.create(new byte[0], okhttp3.MediaType.parse("application/json"));
                reqBuilder.method(httpMethod, body);
                if (body != null && body.contentType() != null) {
                    reqBuilder.header("Content-Type", contentType);
                }
            }
        } catch (IOException e) {
            throw new XrpcFetchException(e);
        }

        // Execute
        try (Response response = fetchHandler.fetch(url, reqBuilder)) {
            return responseParser.parse(nsid, response);
        } catch (XrpcException e) {
            throw e;
        } catch (IOException e) {
            throw new XrpcFetchException(e);
        }
    }

    /** Convenience overload with no params, data, or options. */
    public XrpcResponse call(String nsid) throws XrpcException {
        return call(nsid, null, null, null);
    }

    // ---- Asynchronous call ----

    /**
     * Make an XRPC call asynchronously using the common fork-join pool.
     */
    public CompletableFuture<XrpcResponse> callAsync(String nsid,
                                                      QueryParams params,
                                                      LexValue data,
                                                      CallOptions opts) {
        return callAsync(nsid, params, data, opts, ForkJoinPool.commonPool());
    }

    public CompletableFuture<XrpcResponse> callAsync(String nsid,
                                                      QueryParams params,
                                                      LexValue data,
                                                      CallOptions opts,
                                                      Executor executor) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return call(nsid, params, data, opts);
            } catch (XrpcException e) {
                throw new RuntimeException(e);
            }
        }, executor);
    }
}
