package com.atproto.lexicon.schema;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Sealed hierarchy for all definition types in an AT Protocol Lexicon document. Dispatch is on the
 * "type" JSON field.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", visible = true)
@JsonSubTypes({
  @JsonSubTypes.Type(value = LexRecord.class, name = "record"),
  @JsonSubTypes.Type(value = LexQuery.class, name = "query"),
  @JsonSubTypes.Type(value = LexProcedure.class, name = "procedure"),
  @JsonSubTypes.Type(value = LexSubscription.class, name = "subscription"),
  @JsonSubTypes.Type(value = LexObject.class, name = "object"),
  @JsonSubTypes.Type(value = LexBlob.class, name = "blob"),
  @JsonSubTypes.Type(value = LexArray.class, name = "array"),
  @JsonSubTypes.Type(value = LexToken.class, name = "token"),
  @JsonSubTypes.Type(value = LexBoolean.class, name = "boolean"),
  @JsonSubTypes.Type(value = LexInteger.class, name = "integer"),
  @JsonSubTypes.Type(value = LexString.class, name = "string"),
  @JsonSubTypes.Type(value = LexBytes.class, name = "bytes"),
  @JsonSubTypes.Type(value = LexCidLink.class, name = "cid-link"),
  @JsonSubTypes.Type(value = LexRef.class, name = "ref"),
  @JsonSubTypes.Type(value = LexRefUnion.class, name = "union"),
  @JsonSubTypes.Type(value = LexUnknown.class, name = "unknown"),
})
public sealed interface LexUserType
    permits LexRecord,
        LexQuery,
        LexProcedure,
        LexSubscription,
        LexObject,
        LexBlob,
        LexArray,
        LexToken,
        LexBoolean,
        LexInteger,
        LexString,
        LexBytes,
        LexCidLink,
        LexRef,
        LexRefUnion,
        LexUnknown {

  String description();
}
