package com.atproto.lexicon.validation;

import com.atproto.lex.data.LexArray;
import com.atproto.lex.data.LexBlobRef;
import com.atproto.lex.data.LexBoolean;
import com.atproto.lex.data.LexBytes;
import com.atproto.lex.data.LexCidLink;
import com.atproto.lex.data.LexInteger;
import com.atproto.lex.data.LexMap;
import com.atproto.lex.data.LexNull;
import com.atproto.lex.data.LexString;
import com.atproto.lex.data.LexValue;
import com.atproto.lex.data.TypedBlobRef;
// Explicit imports resolve ambiguity with same-named schema types:
import com.atproto.lexicon.Lexicons;
import com.atproto.lexicon.schema.*;
import java.util.*;

/**
 * Validates {@link LexValue} instances against Lexicon schema definitions. Uses lazy-clone:
 * modifies maps only when defaults are applied.
 */
public final class LexiconValidator {

  private final Lexicons lexicons;

  public LexiconValidator(Lexicons lexicons) {
    this.lexicons = lexicons;
  }

  // ---- Public entry points ----

  public ValidationResult validateRecord(LexiconDoc doc, LexValue value) {
    LexUserType def = doc.mainDef();
    if (!(def instanceof LexRecord rec)) {
      return ValidationResult.failure(doc.id(), "Main def is not a record type");
    }
    return validateObject(doc.id(), rec.record(), asMap(value, doc.id()), false);
  }

  public ValidationResult validateXrpcParams(
      String nsid, LexXrpcParameters params, LexValue value) {
    if (!(value instanceof LexMap map)) {
      return ValidationResult.failure(nsid, "XRPC params must be an object");
    }
    return validateParamsMap(nsid, params, map);
  }

  public ValidationResult validateXrpcInput(LexiconDoc doc, LexValue value) {
    LexUserType def = doc.mainDef();
    if (def instanceof LexProcedure proc && proc.input() != null) {
      return validateBody(doc.id(), proc.input().schema(), value);
    }
    return ValidationResult.success(value);
  }

  public ValidationResult validateXrpcOutput(LexiconDoc doc, LexValue value) {
    LexUserType def = doc.mainDef();
    LexXrpcBody output =
        switch (def) {
          case LexQuery q -> q.output();
          case LexProcedure p -> p.output();
          default -> null;
        };
    if (output == null || output.schema() == null) return ValidationResult.success(value);
    return validateBody(doc.id(), output.schema(), value);
  }

  // ---- Internal dispatch ----

  public ValidationResult validateDef(String lexUri, LexUserType def, LexValue value) {
    return switch (def) {
      case LexRecord rec -> validateObject(lexUri, rec.record(), asMap(value, lexUri), false);
      case LexObject obj -> validateObject(lexUri, obj, asMap(value, lexUri), false);
      case com.atproto.lexicon.schema.LexArray arr -> validateArray(lexUri, arr, value);
      case LexBlob blob -> validateBlob(lexUri, blob, value);
      case com.atproto.lexicon.schema.LexBoolean b -> validateBooleanDef(lexUri, b, value);
      case com.atproto.lexicon.schema.LexInteger i -> validateIntegerDef(lexUri, i, value);
      case com.atproto.lexicon.schema.LexString s -> validateStringDef(lexUri, s, value);
      case com.atproto.lexicon.schema.LexBytes by -> validateBytesDef(lexUri, by, value);
      case com.atproto.lexicon.schema.LexCidLink ignored -> validateCidLink(lexUri, value);
      case LexRef ref -> validateRef(lexUri, ref, value);
      case LexRefUnion union -> validateUnion(lexUri, union, value);
      case LexToken ignored -> ValidationResult.success(value);
      case LexUnknown ignored -> ValidationResult.success(value);
      default -> ValidationResult.success(value);
    };
  }

  private ValidationResult validateObject(
      String lexUri, LexObject def, LexMap map, boolean allowExtra) {
    Map<String, LexValue> modified = null;

    // Check required fields
    for (String req : def.required()) {
      if (!map.fields().containsKey(req)) {
        return ValidationResult.failure(lexUri, "Missing required field: " + req);
      }
    }

    // Validate each defined property
    for (Map.Entry<String, LexUserType> entry : def.properties().entrySet()) {
      String key = entry.getKey();
      LexUserType propDef = entry.getValue();
      LexValue original = map.fields().get(key);

      if (original == null || original instanceof LexNull) {
        // Apply default if defined
        LexValue withDefault = applyDefault(propDef);
        if (withDefault != null) {
          if (modified == null) modified = new LinkedHashMap<>(map.fields());
          modified.put(key, withDefault);
        }
        continue;
      }

      // Nullable check
      boolean isNullable = def.nullable().contains(key);
      if (original instanceof LexNull && !isNullable) {
        return ValidationResult.failure(lexUri, "Field '" + key + "' cannot be null");
      }
      if (original instanceof LexNull) continue;

      ValidationResult result = validateDef(lexUri + "#" + key, propDef, original);
      if (!result.isSuccess()) return result;

      if (result instanceof ValidationSuccess s && s.value() != original) {
        if (modified == null) modified = new LinkedHashMap<>(map.fields());
        modified.put(key, s.value());
      }
    }

    LexMap result = modified == null ? map : new LexMap(modified);
    return ValidationResult.success(result);
  }

  private ValidationResult validateParamsMap(String nsid, LexXrpcParameters params, LexMap map) {
    for (String req : params.required()) {
      if (!map.fields().containsKey(req)) {
        return ValidationResult.failure(nsid, "Missing required XRPC param: " + req);
      }
    }
    for (Map.Entry<String, LexUserType> entry : params.properties().entrySet()) {
      LexValue val = map.fields().get(entry.getKey());
      if (val == null) continue;
      ValidationResult res = validateDef(nsid, entry.getValue(), val);
      if (!res.isSuccess()) return res;
    }
    return ValidationResult.success(map);
  }

  private ValidationResult validateBody(String lexUri, LexUserType schema, LexValue value) {
    if (schema == null) return ValidationResult.success(value);
    return validateDef(lexUri, schema, value);
  }

  private ValidationResult validateArray(
      String lexUri, com.atproto.lexicon.schema.LexArray def, LexValue value) {
    if (!(value instanceof LexArray arr)) {
      return ValidationResult.failure(lexUri, "Expected array");
    }
    if (def.minLength() != null && arr.items().size() < def.minLength()) {
      return ValidationResult.failure(lexUri, "Array too short: minimum " + def.minLength());
    }
    if (def.maxLength() != null && arr.items().size() > def.maxLength()) {
      return ValidationResult.failure(lexUri, "Array too long: maximum " + def.maxLength());
    }
    if (def.items() != null) {
      List<LexValue> modified = null;
      for (int i = 0; i < arr.items().size(); i++) {
        LexValue item = arr.items().get(i);
        ValidationResult res = validateDef(lexUri + "[" + i + "]", def.items(), item);
        if (!res.isSuccess()) return res;
        if (res instanceof ValidationSuccess s && s.value() != item) {
          if (modified == null) modified = new ArrayList<>(arr.items());
          modified.set(i, s.value());
        }
      }
      if (modified != null) return ValidationResult.success(LexArray.of(modified));
    }
    return ValidationResult.success(arr);
  }

  private ValidationResult validateBlob(String lexUri, LexBlob def, LexValue value) {
    if (!(value instanceof LexBlobRef br)) {
      return ValidationResult.failure(lexUri, "Expected blob reference");
    }
    if (!def.accept().isEmpty()) {
      String mime = br.ref().mimeType();
      boolean accepted = def.accept().stream().anyMatch(pattern -> mimeMatches(pattern, mime));
      if (!accepted) {
        return ValidationResult.failure(
            lexUri, "Blob MIME type '" + mime + "' not in accepted list");
      }
    }
    if (def.maxSize() != null && br.ref() instanceof TypedBlobRef t && t.size() > def.maxSize()) {
      return ValidationResult.failure(lexUri, "Blob too large: max " + def.maxSize() + " bytes");
    }
    return ValidationResult.success(value);
  }

  private ValidationResult validateBooleanDef(
      String lexUri, com.atproto.lexicon.schema.LexBoolean def, LexValue value) {
    if (!(value instanceof com.atproto.lex.data.LexBoolean b)) {
      return ValidationResult.failure(lexUri, "Expected boolean");
    }
    if (def.constValue() != null && b.value() != def.constValue()) {
      return ValidationResult.failure(lexUri, "Boolean must be " + def.constValue());
    }
    return ValidationResult.success(value);
  }

  private ValidationResult validateIntegerDef(
      String lexUri, com.atproto.lexicon.schema.LexInteger def, LexValue value) {
    if (!(value instanceof LexInteger i)) {
      return ValidationResult.failure(lexUri, "Expected integer");
    }
    long v = i.value();
    if (def.minimum() != null && v < def.minimum()) {
      return ValidationResult.failure(lexUri, "Integer below minimum " + def.minimum());
    }
    if (def.maximum() != null && v > def.maximum()) {
      return ValidationResult.failure(lexUri, "Integer above maximum " + def.maximum());
    }
    if (!def.enumValues().isEmpty() && !def.enumValues().contains(v)) {
      return ValidationResult.failure(lexUri, "Integer " + v + " not in enum");
    }
    if (def.constValue() != null && v != def.constValue()) {
      return ValidationResult.failure(lexUri, "Integer must be " + def.constValue());
    }
    return ValidationResult.success(value);
  }

  private ValidationResult validateStringDef(
      String lexUri, com.atproto.lexicon.schema.LexString def, LexValue value) {
    if (!(value instanceof LexString s)) {
      return ValidationResult.failure(lexUri, "Expected string");
    }
    String str = s.value();
    if (def.minLength() != null && str.length() < def.minLength()) {
      return ValidationResult.failure(lexUri, "String too short (bytes): min " + def.minLength());
    }
    if (def.maxLength() != null && str.length() > def.maxLength()) {
      return ValidationResult.failure(lexUri, "String too long (bytes): max " + def.maxLength());
    }
    if (def.minGraphemes() != null) {
      int g = com.atproto.lex.data.util.Utf8Utils.graphemeLength(str);
      if (g < def.minGraphemes()) {
        return ValidationResult.failure(
            lexUri, "String too short (graphemes): min " + def.minGraphemes());
      }
    }
    if (def.maxGraphemes() != null) {
      int g = com.atproto.lex.data.util.Utf8Utils.graphemeLength(str);
      if (g > def.maxGraphemes()) {
        return ValidationResult.failure(
            lexUri, "String too long (graphemes): max " + def.maxGraphemes());
      }
    }
    if (!def.enumValues().isEmpty() && !def.enumValues().contains(str)) {
      return ValidationResult.failure(lexUri, "String not in enum: " + str);
    }
    if (def.constValue() != null && !def.constValue().equals(str)) {
      return ValidationResult.failure(lexUri, "String must be '" + def.constValue() + "'");
    }
    if (def.format() != null && !StringFormatValidator.isValid(def.format(), str)) {
      return ValidationResult.failure(
          lexUri, "String does not match format '" + def.format() + "': " + str);
    }
    return ValidationResult.success(value);
  }

  private ValidationResult validateBytesDef(
      String lexUri, com.atproto.lexicon.schema.LexBytes def, LexValue value) {
    if (!(value instanceof LexBytes b)) {
      return ValidationResult.failure(lexUri, "Expected bytes");
    }
    if (def.minLength() != null && b.value().length < def.minLength()) {
      return ValidationResult.failure(lexUri, "Bytes too short: min " + def.minLength());
    }
    if (def.maxLength() != null && b.value().length > def.maxLength()) {
      return ValidationResult.failure(lexUri, "Bytes too long: max " + def.maxLength());
    }
    return ValidationResult.success(value);
  }

  private ValidationResult validateCidLink(String lexUri, LexValue value) {
    if (!(value instanceof LexCidLink)) {
      return ValidationResult.failure(lexUri, "Expected CID link");
    }
    return ValidationResult.success(value);
  }

  private ValidationResult validateRef(String lexUri, LexRef ref, LexValue value) {
    String resolvedUri = lexicons.resolveLexUri(ref.ref(), lexUri);
    java.util.Optional<LexUserType> def = lexicons.getDef(resolvedUri);
    if (def.isEmpty()) {
      return ValidationResult.failure(lexUri, "Ref not found: " + resolvedUri);
    }
    return validateDef(resolvedUri, def.get(), value);
  }

  private ValidationResult validateUnion(String lexUri, LexRefUnion union, LexValue value) {
    if (!(value instanceof LexMap map)) {
      return ValidationResult.failure(lexUri, "Union value must be an object with $type");
    }
    java.util.Optional<String> typeOpt = map.typeDiscriminator();
    if (typeOpt.isEmpty()) {
      return ValidationResult.failure(lexUri, "Union value missing $type field");
    }
    String typeName = typeOpt.get();
    if (!union.refs().isEmpty()) {
      boolean matched =
          union.refs().stream()
              .anyMatch(
                  r -> lexicons.resolveLexUri(r, lexUri).equals(typeName) || r.equals(typeName));
      if (!matched && union.closed()) {
        return ValidationResult.failure(lexUri, "$type '" + typeName + "' not in union refs");
      }
    }
    java.util.Optional<LexUserType> defOpt = lexicons.getDef(typeName);
    if (defOpt.isPresent()) {
      return validateDef(typeName, defOpt.get(), value);
    }
    return ValidationResult.success(value);
  }

  // ---- Helpers ----

  private LexMap asMap(LexValue value, String lexUri) {
    if (!(value instanceof LexMap map)) {
      throw new ValidationError(lexUri, "Expected object, got " + value.getClass().getSimpleName());
    }
    return map;
  }

  private LexValue applyDefault(LexUserType def) {
    return switch (def) {
      case com.atproto.lexicon.schema.LexBoolean b when b.defaultValue() != null ->
          LexBoolean.of(b.defaultValue());
      case com.atproto.lexicon.schema.LexInteger i when i.defaultValue() != null ->
          new LexInteger(i.defaultValue());
      case com.atproto.lexicon.schema.LexString s when s.defaultValue() != null ->
          new LexString(s.defaultValue());
      default -> null;
    };
  }

  private boolean mimeMatches(String pattern, String mime) {
    if (pattern.endsWith("/*")) {
      return mime.startsWith(pattern.substring(0, pattern.length() - 2));
    }
    return pattern.equals(mime) || pattern.equals("*/*");
  }
}
