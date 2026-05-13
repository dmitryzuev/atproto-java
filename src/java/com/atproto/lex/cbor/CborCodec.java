package com.atproto.lex.cbor;

import com.atproto.lex.data.*;
import com.upokecenter.cbor.CBORObject;
import java.util.*;

/**
 * DAG-CBOR encoder/decoder for {@link LexValue}.
 *
 * <p>DAG-CBOR rules followed: - Map keys are sorted in canonical CBOR order (length then
 * lexicographic). - CID links are encoded as CBOR tag 42 wrapping bytes [0x00 || cid.toBytes()]. -
 * No floating-point numbers. - Byte strings use major type 2.
 */
public final class CborCodec {

  /** CBOR tag for CID links (IPLD spec: tag 42) */
  private static final int TAG_CID = 42;

  public CborCodec() {}

  // ---- Encode ----

  public byte[] encode(LexValue value) {
    return toCbor(value).EncodeToBytes();
  }

  private CBORObject toCbor(LexValue value) {
    return switch (value) {
      case LexNull ignored -> CBORObject.Null;
      case LexBoolean b -> b.value() ? CBORObject.True : CBORObject.False;
      case LexInteger i -> CBORObject.FromObject(i.value());
      case LexString s -> CBORObject.FromObject(s.value());
      case LexBytes b -> CBORObject.FromObject(b.value());
      case LexCidLink c -> encodeCidLink(c.cid());
      case LexBlobRef br -> encodeBlobRef(br);
      case LexArray arr -> encodeArray(arr);
      case LexMap map -> encodeMap(map);
    };
  }

  private CBORObject encodeCidLink(Cid cid) {
    byte[] cidBytes = cid.toBytes();
    // DAG-CBOR: tag 42 + multibase identity prefix 0x00 + CID bytes
    byte[] payload = new byte[cidBytes.length + 1];
    payload[0] = 0x00; // multibase identity prefix
    System.arraycopy(cidBytes, 0, payload, 1, cidBytes.length);
    return CBORObject.FromObjectAndTag(CBORObject.FromObject(payload), TAG_CID);
  }

  private CBORObject encodeBlobRef(LexBlobRef blobRef) {
    CBORObject map = CBORObject.NewOrderedMap();
    switch (blobRef.ref()) {
      case TypedBlobRef t -> {
        map.set("$type", CBORObject.FromObject("blob"));
        map.set("ref", encodeCidLink(t.ref()));
        map.set("mimeType", CBORObject.FromObject(t.mimeType()));
        map.set("size", CBORObject.FromObject(t.size()));
      }
      case LegacyBlobRef l -> {
        map.set("cid", CBORObject.FromObject(l.cidString()));
        map.set("mimeType", CBORObject.FromObject(l.mimeType()));
      }
    }
    return map;
  }

  private CBORObject encodeArray(LexArray arr) {
    CBORObject array = CBORObject.NewArray();
    for (LexValue item : arr.items()) {
      array.Add(toCbor(item));
    }
    return array;
  }

  private CBORObject encodeMap(LexMap map) {
    // Use ordered map — keys sorted in DAG-CBOR canonical order (by CBOR-encoded key length, then
    // lexicographic)
    CBORObject cborMap = CBORObject.NewOrderedMap();
    map.fields().entrySet().stream()
        .sorted(Map.Entry.comparingByKey(CborCodec::dagCborKeyComparator))
        .forEach(e -> cborMap.set(e.getKey(), toCbor(e.getValue())));
    return cborMap;
  }

  // ---- Decode ----

  public LexValue decode(byte[] bytes) {
    CBORObject obj = CBORObject.DecodeFromBytes(bytes);
    return fromCbor(obj);
  }

  private LexValue fromCbor(CBORObject obj) {
    if (obj.isNull()) return LexNull.INSTANCE;

    // Tag 42 = CID link
    if (obj.isTagged() && obj.getMostOuterTag().ToInt32Unchecked() == TAG_CID) {
      return decodeCidLink(obj);
    }

    return switch (obj.getType()) {
      case Boolean -> LexBoolean.of(obj.AsBoolean());
      case Integer -> new LexInteger(obj.AsInt64Value());
      case TextString -> new LexString(obj.AsString());
      case ByteString -> new LexBytes(obj.GetByteString());
      case Array -> {
        List<LexValue> items = new ArrayList<>(obj.size());
        for (CBORObject item : obj.getValues()) {
          items.add(fromCbor(item));
        }
        yield LexArray.of(items);
      }
      case Map -> decodeMap(obj);
      default -> throw new LexError("Unsupported CBOR type: " + obj.getType());
    };
  }

  private LexCidLink decodeCidLink(CBORObject taggedObj) {
    CBORObject inner = taggedObj.UntagOne();
    byte[] payload = inner.GetByteString();
    if (payload.length < 2 || payload[0] != 0x00) {
      throw new LexError("Invalid CID tag-42 payload: missing identity multibase prefix");
    }
    byte[] cidBytes = Arrays.copyOfRange(payload, 1, payload.length);
    return new LexCidLink(Cid.fromBytes(cidBytes));
  }

  private LexValue decodeMap(CBORObject obj) {
    // Check for blob ref pattern
    CBORObject typeVal = obj.get("$type");
    if (typeVal != null && "blob".equals(typeVal.AsString())) {
      return decodeBlobRef(obj);
    }
    // Check for legacy blob
    if (obj.ContainsKey("cid") && obj.ContainsKey("mimeType") && !obj.ContainsKey("$type")) {
      String cidStr = obj.get("cid").AsString();
      String mime = obj.get("mimeType").AsString();
      if (cidStr != null && mime != null) {
        return new LexBlobRef(new LegacyBlobRef(cidStr, mime));
      }
    }

    Map<String, LexValue> fields = new LinkedHashMap<>();
    for (CBORObject key : obj.getKeys()) {
      fields.put(key.AsString(), fromCbor(obj.get(key)));
    }
    return new LexMap(fields);
  }

  private LexBlobRef decodeBlobRef(CBORObject obj) {
    CBORObject refObj = obj.get("ref");
    if (refObj == null) throw new LexError("TypedBlobRef missing 'ref'");
    LexCidLink cidLink = decodeCidLink(refObj);
    String mimeType = obj.get("mimeType").AsString();
    long size = obj.ContainsKey("size") ? obj.get("size").AsInt64Value() : 0L;
    return new LexBlobRef(new TypedBlobRef(cidLink.cid(), mimeType, size));
  }

  // ---- Utilities ----

  /**
   * DAG-CBOR canonical map key order: sort by CBOR-encoded key length first, then
   * lexicographically.
   */
  static int dagCborKeyComparator(String a, String b) {
    byte[] ba = CBORObject.FromObject(a).EncodeToBytes();
    byte[] bb = CBORObject.FromObject(b).EncodeToBytes();
    if (ba.length != bb.length) return Integer.compare(ba.length, bb.length);
    for (int i = 0; i < ba.length; i++) {
      int diff = Byte.toUnsignedInt(ba[i]) - Byte.toUnsignedInt(bb[i]);
      if (diff != 0) return diff;
    }
    return 0;
  }

  static class LexError extends com.atproto.lex.data.LexError {
    LexError(String msg) {
      super(msg);
    }
  }
}
