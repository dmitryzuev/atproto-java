package com.atproto.xrpc.internal;

import com.atproto.lex.data.LexValue;
import com.atproto.lex.json.LexJsonConverter;
import com.atproto.lexicon.schema.*;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import okhttp3.*;

/** Assembles OkHttp requests from XRPC method metadata. */
public final class RequestBuilder {

  private RequestBuilder() {}

  /** Returns "GET" for queries, "POST" for procedures/subscriptions. */
  public static String httpMethod(LexUserType def) {
    return (def instanceof LexQuery) ? "GET" : "POST";
  }

  /** Build a full URL including encoded query parameters. */
  public static String buildUrl(String baseUrl, String nsid, com.atproto.xrpc.QueryParams params) {
    String base = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    StringBuilder url = new StringBuilder(base).append("/xrpc/").append(nsid);
    if (params != null && !params.isEmpty()) {
      url.append('?');
      boolean first = true;
      for (Map.Entry<String, List<String>> entry : params.asMap().entrySet()) {
        for (String val : entry.getValue()) {
          if (!first) url.append('&');
          url.append(urlEncode(entry.getKey())).append('=').append(urlEncode(val));
          first = false;
        }
      }
    }
    return url.toString();
  }

  /** Encode a LexValue body as a JSON RequestBody, or return null for GET. */
  public static RequestBody encodeBody(
      LexValue data, String contentType, LexJsonConverter converter) throws IOException {
    if (data == null) return null;
    String json = converter.lexToJsonString(data);
    return RequestBody.create(
        json, MediaType.parse(contentType != null ? contentType : "application/json"));
  }

  /** Encode a raw byte array body. */
  public static RequestBody encodeRawBody(byte[] data, String contentType) {
    return RequestBody.create(data, MediaType.parse(contentType));
  }

  private static String urlEncode(String s) {
    try {
      return java.net.URLEncoder.encode(s, "UTF-8").replace("+", "%20");
    } catch (java.io.UnsupportedEncodingException e) {
      return s; // UTF-8 is always available
    }
  }
}
