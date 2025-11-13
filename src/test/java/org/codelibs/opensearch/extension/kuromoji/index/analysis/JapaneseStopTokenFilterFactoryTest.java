package org.codelibs.opensearch.extension.kuromoji.index.analysis;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.WhitespaceTokenizer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opensearch.common.settings.Settings;
import org.opensearch.env.Environment;
import org.opensearch.env.TestEnvironment;
import org.opensearch.index.IndexSettings;
import org.opensearch.test.IndexSettingsModule;
import org.opensearch.test.OpenSearchTestCase;

public class JapaneseStopTokenFilterFactoryTest extends OpenSearchTestCase {

    private Environment env;
    private IndexSettings indexSettings;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        Settings settings = Settings.builder()
                .put("path.home", createTempDir().toString())
                .put("index.version.created", org.opensearch.Version.CURRENT)
                .build();
        env = TestEnvironment.newEnvironment(settings);
        indexSettings = IndexSettingsModule.newIndexSettings("test", settings);
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    public void testDefaultStopWords() {
        Settings settings = Settings.builder().build();

        JapaneseStopTokenFilterFactory factory = new JapaneseStopTokenFilterFactory(
                indexSettings, env, "test", settings);

        WhitespaceTokenizer tokenizer = new WhitespaceTokenizer();
        TokenStream tokenStream = factory.create(tokenizer);

        assertNotNull(tokenStream);
        assertNotNull(factory.stopWords());
        assertFalse(factory.stopWords().isEmpty());
    }

    @Test
    public void testCustomStopWords() {
        Settings settings = Settings.builder()
                .putList("stopwords", "の", "に", "は", "を")
                .build();

        JapaneseStopTokenFilterFactory factory = new JapaneseStopTokenFilterFactory(
                indexSettings, env, "test", settings);

        WhitespaceTokenizer tokenizer = new WhitespaceTokenizer();
        TokenStream tokenStream = factory.create(tokenizer);

        assertNotNull(tokenStream);
        assertNotNull(factory.stopWords());
    }

    @Test
    public void testStopWordsFromFile() throws IOException {
        File stopWordsFile = new File(env.configDir().toFile(), "stopwords_ja.txt");
        try (FileWriter writer = new FileWriter(stopWordsFile)) {
            writer.write("の\n");
            writer.write("に\n");
            writer.write("は\n");
        }

        Settings settings = Settings.builder()
                .put("stopwords", "stopwords_ja.txt")
                .build();

        JapaneseStopTokenFilterFactory factory = new JapaneseStopTokenFilterFactory(
                indexSettings, env, "test", settings);

        WhitespaceTokenizer tokenizer = new WhitespaceTokenizer();
        TokenStream tokenStream = factory.create(tokenizer);

        assertNotNull(tokenStream);
    }

    @Test
    public void testIgnoreCaseTrue() {
        Settings settings = Settings.builder()
                .put("ignore_case", true)
                .putList("stopwords", "Test", "Example")
                .build();

        JapaneseStopTokenFilterFactory factory = new JapaneseStopTokenFilterFactory(
                indexSettings, env, "test", settings);

        assertTrue(factory.ignoreCase());
        assertNotNull(factory.stopWords());
    }

    @Test
    public void testIgnoreCaseFalse() {
        Settings settings = Settings.builder()
                .put("ignore_case", false)
                .build();

        JapaneseStopTokenFilterFactory factory = new JapaneseStopTokenFilterFactory(
                indexSettings, env, "test", settings);

        assertFalse(factory.ignoreCase());
    }

    @Test
    public void testRemoveTrailingTrue() {
        Settings settings = Settings.builder()
                .put("remove_trailing", true)
                .build();

        JapaneseStopTokenFilterFactory factory = new JapaneseStopTokenFilterFactory(
                indexSettings, env, "test", settings);

        WhitespaceTokenizer tokenizer = new WhitespaceTokenizer();
        TokenStream tokenStream = factory.create(tokenizer);

        assertNotNull(tokenStream);
    }

    @Test
    public void testRemoveTrailingFalse() {
        Settings settings = Settings.builder()
                .put("remove_trailing", false)
                .build();

        JapaneseStopTokenFilterFactory factory = new JapaneseStopTokenFilterFactory(
                indexSettings, env, "test", settings);

        WhitespaceTokenizer tokenizer = new WhitespaceTokenizer();
        TokenStream tokenStream = factory.create(tokenizer);

        assertNotNull(tokenStream);
    }

    @Test
    public void testNamedStopWords() {
        Settings settings = Settings.builder()
                .put("stopwords", "_japanese_")
                .build();

        JapaneseStopTokenFilterFactory factory = new JapaneseStopTokenFilterFactory(
                indexSettings, env, "test", settings);

        assertNotNull(factory.stopWords());
        assertFalse(factory.stopWords().isEmpty());
    }

    @Test
    public void testFactoryName() {
        Settings settings = Settings.builder().build();

        JapaneseStopTokenFilterFactory factory = new JapaneseStopTokenFilterFactory(
                indexSettings, env, "ja_stop", settings);

        assertNotNull(factory);
        assertEquals("ja_stop", factory.name());
    }

    @Test
    public void testAllSettings() {
        Settings settings = Settings.builder()
                .putList("stopwords", "の", "に")
                .put("ignore_case", true)
                .put("remove_trailing", false)
                .build();

        JapaneseStopTokenFilterFactory factory = new JapaneseStopTokenFilterFactory(
                indexSettings, env, "test", settings);

        assertTrue(factory.ignoreCase());
        assertNotNull(factory.stopWords());

        WhitespaceTokenizer tokenizer = new WhitespaceTokenizer();
        TokenStream tokenStream = factory.create(tokenizer);
        assertNotNull(tokenStream);
    }
}
