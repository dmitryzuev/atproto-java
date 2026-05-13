package io.atproto.lexicon;

import io.atproto.lex.data.*;
import io.atproto.lexicon.schema.*;
import io.atproto.lexicon.validation.ValidationError;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

class LexiconsTest {

    private Lexicons lexicons;
    private LexiconDoc feedPost;

    @BeforeEach
    void setup() {
        lexicons = new Lexicons();
        InputStream stream = getClass().getResourceAsStream("/testdata/app.bsky.feed.post.json");
        feedPost = new LexiconDocDeserializer().deserialize(stream);
        lexicons.add(feedPost);
    }

    @Test
    void getDoc() {
        assertThat(lexicons.get("app.bsky.feed.post")).isPresent();
        assertThat(lexicons.get("com.unknown")).isEmpty();
    }

    @Test
    void getDef() {
        assertThat(lexicons.getDef("lex:app.bsky.feed.post#main")).isPresent();
        assertThat(lexicons.getDef("app.bsky.feed.post")).isPresent(); // auto-normalizes to #main
        assertThat(lexicons.getDef("lex:app.bsky.feed.post#replyRef")).isPresent();
    }

    @Test
    void resolveLexUri_bareName() {
        assertThat(lexicons.resolveLexUri("app.bsky.feed.post", ""))
                .isEqualTo("lex:app.bsky.feed.post#main");
    }

    @Test
    void resolveLexUri_withFragment() {
        assertThat(lexicons.resolveLexUri("app.bsky.feed.post#replyRef", ""))
                .isEqualTo("lex:app.bsky.feed.post#replyRef");
    }

    @Test
    void resolveLexUri_relativeFragment() {
        assertThat(lexicons.resolveLexUri("#replyRef", "lex:app.bsky.feed.post"))
                .isEqualTo("lex:app.bsky.feed.post#replyRef");
    }

    @Test
    void assertValidRecord_valid() {
        LexMap record = new LexMap(Map.of(
                "text", new LexString("Hello World"),
                "createdAt", new LexString("2024-01-01T00:00:00.000Z")
        ));
        assertThatNoException().isThrownBy(() -> lexicons.assertValidRecord("app.bsky.feed.post", record));
    }

    @Test
    void assertValidRecord_missingRequired() {
        LexMap record = new LexMap(Map.of("text", new LexString("Hello")));
        assertThatThrownBy(() -> lexicons.assertValidRecord("app.bsky.feed.post", record))
                .isInstanceOf(ValidationError.class)
                .hasMessageContaining("createdAt");
    }

    @Test
    void removeDoc() {
        lexicons.remove("app.bsky.feed.post");
        assertThat(lexicons.get("app.bsky.feed.post")).isEmpty();
        assertThat(lexicons.getDef("lex:app.bsky.feed.post#main")).isEmpty();
    }

    @Test
    void iterates() {
        long count = 0;
        for (LexiconDoc doc : lexicons) {
            count++;
        }
        assertThat(count).isEqualTo(1);
    }

    @Test
    void stringFormatValidation_invalidDatetime() {
        LexMap record = new LexMap(Map.of(
                "text", new LexString("Hi"),
                "createdAt", new LexString("not-a-date")
        ));
        assertThatThrownBy(() -> lexicons.assertValidRecord("app.bsky.feed.post", record))
                .isInstanceOf(ValidationError.class)
                .hasMessageContaining("datetime");
    }
}
