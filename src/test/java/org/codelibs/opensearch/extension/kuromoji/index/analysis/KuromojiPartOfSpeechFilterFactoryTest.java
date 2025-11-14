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
import org.opensearch.index.IndexSettings;

import java.nio.file.Files;
import java.nio.file.Path;

public class KuromojiPartOfSpeechFilterFactoryTest {

    private Environment env;
    private IndexSettings indexSettings;
    private Path tempDir;


    // Simple Index implementation for testing
    private static class TestIndex extends org.opensearch.index.Index {
        TestIndex(String name, String uuid) {
            super(name, uuid);
        }
    }
    @Before
    public void setUp() throws Exception {
        tempDir = Files.createTempDirectory("test");

        Settings settings = Settings.builder()

                .put("path.home", tempDir.toString())
                .put("index.version.created", org.opensearch.Version.CURRENT)
                .build();
        env = new Environment(settings, tempDir.resolve("config"));
        Files.createDirectories(env.configDir());
        indexSettings = new IndexSettings(
                new TestIndex("test", "_na_"),
                Settings.builder()
                        .put(settings)
                        .put("index.version.created", org.opensearch.Version.CURRENT)
                        .build());
    }

    @After
    public void tearDown() throws Exception {
        if (tempDir != null && Files.exists(tempDir)) {
            deleteDirectory(tempDir.toFile());
        }
    }
    private void deleteDirectory(File dir) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    file.delete();
                }
            }
        }
        dir.delete();
    }

    @Test
    public void testDefaultStopTags() {
        Settings settings = Settings.builder().build();

        KuromojiPartOfSpeechFilterFactory factory = new KuromojiPartOfSpeechFilterFactory(
                indexSettings, env, "test", settings);

        WhitespaceTokenizer tokenizer = new WhitespaceTokenizer();
        TokenStream tokenStream = factory.create(tokenizer);

        assertNotNull(tokenStream);
    }

    @Test
    public void testCustomStopTags() {
        Settings settings = Settings.builder()
                .putList("stoptags", "助詞", "助動詞")
                .build();

        KuromojiPartOfSpeechFilterFactory factory = new KuromojiPartOfSpeechFilterFactory(
                indexSettings, env, "test", settings);

        WhitespaceTokenizer tokenizer = new WhitespaceTokenizer();
        TokenStream tokenStream = factory.create(tokenizer);

        assertNotNull(tokenStream);
    }

    @Test
    public void testStopTagsFromFile() throws IOException {
        File stopTagsFile = new File(env.configDir().toFile(), "stoptags.txt");
        try (FileWriter writer = new FileWriter(stopTagsFile)) {
            writer.write("助詞\n");
            writer.write("助動詞\n");
            writer.write("接続詞\n");
        }

        Settings settings = Settings.builder()
                .put("stoptags", "stoptags.txt")
                .build();

        KuromojiPartOfSpeechFilterFactory factory = new KuromojiPartOfSpeechFilterFactory(
                indexSettings, env, "test", settings);

        WhitespaceTokenizer tokenizer = new WhitespaceTokenizer();
        TokenStream tokenStream = factory.create(tokenizer);

        assertNotNull(tokenStream);
    }

    @Test
    public void testFactoryName() {
        Settings settings = Settings.builder().build();

        KuromojiPartOfSpeechFilterFactory factory = new KuromojiPartOfSpeechFilterFactory(
                indexSettings, env, "kuromoji_pos", settings);

        assertNotNull(factory);
        assertEquals("kuromoji_pos", factory.name());
    }

    @Test
    public void testMultipleCreations() {
        Settings settings = Settings.builder()
                .putList("stoptags", "助詞")
                .build();

        KuromojiPartOfSpeechFilterFactory factory = new KuromojiPartOfSpeechFilterFactory(
                indexSettings, env, "test", settings);

        WhitespaceTokenizer tokenizer1 = new WhitespaceTokenizer();
        TokenStream output1 = factory.create(tokenizer1);
        assertNotNull(output1);

        WhitespaceTokenizer tokenizer2 = new WhitespaceTokenizer();
        TokenStream output2 = factory.create(tokenizer2);
        assertNotNull(output2);

        assertNotSame(output1, output2);
    }

    @Test
    public void testEmptyStopTags() {
        Settings settings = Settings.builder()
                .putList("stoptags")
                .build();

        KuromojiPartOfSpeechFilterFactory factory = new KuromojiPartOfSpeechFilterFactory(
                indexSettings, env, "test", settings);

        assertNotNull(factory);
    }
}
