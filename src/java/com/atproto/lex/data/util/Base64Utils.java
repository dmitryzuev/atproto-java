package com.atproto.lex.data.util;

import java.util.Base64;

public final class Base64Utils {

  private Base64Utils() {}

  public static byte[] decode(String base64) {
    // Accept both standard and URL-safe, with or without padding.
    String clean = base64.replace('-', '+').replace('_', '/');
    // Add padding if needed.
    int mod = clean.length() % 4;
    if (mod == 2) clean += "==";
    else if (mod == 3) clean += "=";
    return Base64.getDecoder().decode(clean);
  }

  public static String encode(byte[] bytes) {
    return Base64.getEncoder().encodeToString(bytes);
  }

  public static String encodeUrl(byte[] bytes) {
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }
}
