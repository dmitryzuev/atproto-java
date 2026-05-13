package com.atproto.xrpc;

import com.atproto.lex.data.*;
import com.atproto.lexicon.Lexicons;
import com.atproto.xrpc.error.*;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.*;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;

class XrpcClientTest {

    private MockWebServer server;
    private XrpcClient client;

    @BeforeEach
    void setup() throws Exception {
        server = new MockWebServer();
        server.start();
        client = new XrpcClient(server.url("").toString(), new Lexicons());
    }

    @AfterEach
    void teardown() throws Exception {
        server.shutdown();
    }

    @Test
    void getQueryReturnsData() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"feed\":[{\"post\":{\"text\":\"Hello\"}}]}"));

        XrpcResponse resp = client.call("app.bsky.feed.getTimeline", null, null, null);

        assertThat(resp.status()).isEqualTo(200);
        assertThat(resp.isSuccess()).isTrue();
        assertThat(resp.data()).isInstanceOf(LexMap.class);

        RecordedRequest req = server.takeRequest();
        assertThat(req.getMethod()).isEqualTo("GET");
        assertThat(req.getPath()).contains("/xrpc/app.bsky.feed.getTimeline");
    }

    @Test
    void postProcedureWithBody() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"uri\":\"at://did:example/post/1\"}"));

        LexMap body = new LexMap(Map.of("text", new LexString("Hello World")));
        XrpcResponse resp = client.call("app.bsky.feed.post", null, body, null);

        assertThat(resp.status()).isEqualTo(200);
        RecordedRequest req = server.takeRequest();
        assertThat(req.getMethod()).isEqualTo("POST");
        assertThat(req.getHeader("Content-Type")).contains("application/json");
        assertThat(req.getBody().readUtf8()).contains("Hello World");
    }

    @Test
    void authenticationErrorThrows401() {
        server.enqueue(new MockResponse()
                .setResponseCode(401)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"error\":\"AuthenticationRequired\",\"message\":\"Token expired\"}"));

        assertThatThrownBy(() -> client.call("com.atproto.server.getSession"))
                .isInstanceOf(XrpcAuthenticationException.class)
                .hasMessageContaining("Token expired");
    }

    @Test
    void rateLimit429Throws() {
        server.enqueue(new MockResponse()
                .setResponseCode(429)
                .setBody("{\"error\":\"RateLimitExceeded\",\"message\":\"Too many requests\"}"));

        assertThatThrownBy(() -> client.call("app.bsky.feed.getTimeline"))
                .isInstanceOf(XrpcResponseException.class)
                .extracting(e -> ((XrpcResponseException) e).statusCode())
                .isEqualTo(429);
    }

    @Test
    void headerInjection() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("{}"));

        client.setHeader("Authorization", "Bearer test-token");
        client.call("app.bsky.actor.getProfile");

        RecordedRequest req = server.takeRequest();
        assertThat(req.getHeader("Authorization")).isEqualTo("Bearer test-token");
    }

    @Test
    void dynamicHeaderSupplier() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("{}"));

        String[] token = {"token-v1"};
        client.setHeader("Authorization", () -> "Bearer " + token[0]);
        token[0] = "token-v2"; // mutate before call
        client.call("app.bsky.actor.getProfile");

        RecordedRequest req = server.takeRequest();
        assertThat(req.getHeader("Authorization")).isEqualTo("Bearer token-v2");
    }

    @Test
    void queryParamsAppendedToUrl() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("{}"));

        QueryParams params = new QueryParams().put("limit", 10L).put("cursor", "abc");
        client.call("app.bsky.feed.getTimeline", params, null, null);

        RecordedRequest req = server.takeRequest();
        String path = req.getPath();
        assertThat(path).contains("limit=10");
        assertThat(path).contains("cursor=abc");
    }

    @Test
    void emptyResponseBodyHandledGracefully() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(""));

        XrpcResponse resp = client.call("com.atproto.server.deleteSession");
        assertThat(resp.data()).isEqualTo(LexNull.INSTANCE);
    }

    @Test
    void asyncCallReturnsFuture() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"did\":\"did:example:alice\"}"));

        XrpcResponse resp = client.callAsync("com.atproto.identity.resolveHandle", null, null, null)
                .join();
        assertThat(resp.status()).isEqualTo(200);
    }
}
