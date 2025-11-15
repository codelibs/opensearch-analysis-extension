package org.codelibs.opensearch.extension.kuromoji.index.analysis;

import static org.junit.Assert.*;

import java.io.File;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.WhitespaceTokenizer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opensearch.cluster.metadata.IndexMetadata;
import org.opensearch.common.settings.Settings;
import org.opensearch.env.Environment;
import org.opensearch.index.IndexSettings;

import java.nio.file.Files;
import java.nio.file.Path;

public class KuromojiKatakanaStemmerFactoryTest {

    private Environment env;
    private IndexSettings indexSettings;
    private Path tempDir;

    @Before
    public void setUp() throws Exception {
        tempDir = Files.createTempDirectory("test");

        Settings settings = Settings.builder()
                .put("path.home", tempDir.toString())
                .put("index.version.created", org.opensearch.Version.CURRENT)
                .build();
        env = new Environment(settings, tempDir.resolve("config"));
        Files.createDirectories(env.configDir());

        IndexMetadata indexMetadata = IndexMetadata.builder("test")
                .settings(Settings.builder()
                        .put(settings)
                        .put("index.version.created", org.opensearch.Version.CURRENT)
                        .build())
                .numberOfShards(1)
                .numberOfReplicas(0)
                .build();
        indexSettings = new IndexSettings(indexMetadata, settings);
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
    public void testDefaultMinimumLength() {
        Settings settings = Settings.builder().build();

        KuromojiKatakanaStemmerFactory factory = new KuromojiKatakanaStemmerFactory(
                indexSettings, env, "test", settings);

        WhitespaceTokenizer tokenizer = new WhitespaceTokenizer();
        TokenStream tokenStream = factory.create(tokenizer);

        assertNotNull(tokenStream);
    }

    @Test
    public void testCustomMinimumLength() {
        Settings settings = Settings.builder()
                .put("minimum_length", 5)
                .build();

        KuromojiKatakanaStemmerFactory factory = new KuromojiKatakanaStemmerFactory(
                indexSettings, env, "test", settings);

        WhitespaceTokenizer tokenizer = new WhitespaceTokenizer();
        TokenStream tokenStream = factory.create(tokenizer);

        assertNotNull(tokenStream);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMinimumLengthZero() {
        Settings settings = Settings.builder()
                .put("minimum_length", 0)
                .build();

        KuromojiKatakanaStemmerFactory factory = new KuromojiKatakanaStemmerFactory(
                indexSettings, env, "test", settings);

        // Exception is thrown when creating the filter, not during factory instantiation
        WhitespaceTokenizer tokenizer = new WhitespaceTokenizer();
        factory.create(tokenizer);
    }

    @Test
    public void testMinimumLengthOne() {
        Settings settings = Settings.builder()
                .put("minimum_length", 1)
                .build();

        KuromojiKatakanaStemmerFactory factory = new KuromojiKatakanaStemmerFactory(
                indexSettings, env, "test", settings);

        WhitespaceTokenizer tokenizer = new WhitespaceTokenizer();
        TokenStream tokenStream = factory.create(tokenizer);

        assertNotNull(tokenStream);
    }

    @Test
    public void testMinimumLengthLarge() {
        Settings settings = Settings.builder()
                .put("minimum_length", 100)
                .build();

        KuromojiKatakanaStemmerFactory factory = new KuromojiKatakanaStemmerFactory(
                indexSettings, env, "test", settings);

        WhitespaceTokenizer tokenizer = new WhitespaceTokenizer();
        TokenStream tokenStream = factory.create(tokenizer);

        assertNotNull(tokenStream);
    }

    @Test
    public void testFactoryName() {
        Settings settings = Settings.builder().build();

        KuromojiKatakanaStemmerFactory factory = new KuromojiKatakanaStemmerFactory(
                indexSettings, env, "katakana_stemmer", settings);

        assertNotNull(factory);
        assertEquals("katakana_stemmer", factory.name());
    }

    @Test
    public void testMultipleCreations() {
        Settings settings = Settings.builder()
                .put("minimum_length", 4)
                .build();

        KuromojiKatakanaStemmerFactory factory = new KuromojiKatakanaStemmerFactory(
                indexSettings, env, "test", settings);

        WhitespaceTokenizer tokenizer1 = new WhitespaceTokenizer();
        TokenStream output1 = factory.create(tokenizer1);
        assertNotNull(output1);

        WhitespaceTokenizer tokenizer2 = new WhitespaceTokenizer();
        TokenStream output2 = factory.create(tokenizer2);
        assertNotNull(output2);

        assertNotSame(output1, output2);
    }
}
