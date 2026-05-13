package com.atproto.xrpc;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Default OkHttp3-backed {@link FetchHandler}.
 */
public final class OkHttpFetchHandler implements FetchHandler {

    private final OkHttpClient client;

    public OkHttpFetchHandler() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    public OkHttpFetchHandler(OkHttpClient client) {
        this.client = client;
    }

    @Override
    public Response fetch(String urlPath, Request.Builder builder) throws IOException {
        return client.newCall(builder.url(urlPath).build()).execute();
    }
}
