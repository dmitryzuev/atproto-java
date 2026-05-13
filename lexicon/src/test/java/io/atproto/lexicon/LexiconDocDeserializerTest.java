package io.atproto.lexicon;

import io.atproto.lexicon.schema.*;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.assertj.core.api.Assertions.*;

class LexiconDocDeserializerTest {

    private final LexiconDocDeserializer deserializer = new LexiconDocDeserializer();

    @Test
    void deserializesBlueskyFeedPost() {
        InputStream stream = getClass().getResourceAsStream("/testdata/app.bsky.feed.post.json");
        assertThat(stream).isNotNull();

        LexiconDoc doc = deserializer.deserialize(stream);
        assertThat(doc.lexicon()).isEqualTo(1);
        assertThat(doc.id()).isEqualTo("app.bsky.feed.post");
        assertThat(doc.defs()).containsKey("main");
        assertThat(doc.defs()).containsKey("replyRef");

        LexUserType main = doc.defs().get("main");
        assertThat(main).isInstanceOf(LexRecord.class);
        LexRecord record = (LexRecord) main;
        assertThat(record.record()).isNotNull();
        assertThat(record.record().required()).contains("text", "createdAt");
        assertThat(record.record().properties()).containsKey("text");
        assertThat(record.record().properties()).containsKey("createdAt");
    }

    @Test
    void deserializesTextProperty() {
        InputStream stream = getClass().getResourceAsStream("/testdata/app.bsky.feed.post.json");
        LexiconDoc doc = deserializer.deserialize(stream);
        LexUserType textDef = ((LexRecord) doc.mainDef()).record().properties().get("text");
        assertThat(textDef).isInstanceOf(LexString.class);
        LexString text = (LexString) textDef;
        assertThat(text.maxLength()).isEqualTo(3000);
        assertThat(text.maxGraphemes()).isEqualTo(300);
    }

    @Test
    void rejectsUnknownLexiconVersion() {
        assertThatThrownBy(() -> deserializer.deserialize("{\"lexicon\":2,\"id\":\"foo.bar\",\"defs\":{}}"))
                .isInstanceOf(Exception.class);
    }

    @Test
    void deserializesFromJsonString() {
        String json = """
                {
                  "lexicon": 1,
                  "id": "com.example.test",
                  "defs": {
                    "main": {
                      "type": "query",
                      "description": "A test query",
                      "output": {"encoding": "application/json"}
                    }
                  }
                }
                """;
        LexiconDoc doc = deserializer.deserialize(json);
        assertThat(doc.id()).isEqualTo("com.example.test");
        assertThat(doc.mainDef()).isInstanceOf(LexQuery.class);
    }
}
