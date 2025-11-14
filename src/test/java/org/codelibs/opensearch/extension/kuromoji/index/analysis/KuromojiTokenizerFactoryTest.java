package org.codelibs.opensearch.extension.kuromoji.index.analysis;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.ja.JapaneseTokenizer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opensearch.cluster.metadata.IndexMetadata;
import org.opensearch.common.settings.Settings;
import org.opensearch.env.Environment;
import org.opensearch.index.IndexSettings;

import java.nio.file.Files;
import java.nio.file.Path;

public class KuromojiTokenizerFactoryTest {

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
    public void testDefaultMode() {
        Settings settings = Settings.builder().build();

        KuromojiTokenizerFactory factory = new KuromojiTokenizerFactory(
                indexSettings, env, "test", settings);

        Tokenizer tokenizer = factory.create();
        assertNotNull(tokenizer);
        assertTrue(tokenizer instanceof JapaneseTokenizer);
    }

    @Test
    public void testSearchMode() {
        Settings settings = Settings.builder()
                .put("mode", "search")
                .build();

        KuromojiTokenizerFactory factory = new KuromojiTokenizerFactory(
                indexSettings, env, "test", settings);

        Tokenizer tokenizer = factory.create();
        assertNotNull(tokenizer);
    }

    @Test
    public void testNormalMode() {
        Settings settings = Settings.builder()
                .put("mode", "normal")
                .build();

        KuromojiTokenizerFactory factory = new KuromojiTokenizerFactory(
                indexSettings, env, "test", settings);

        Tokenizer tokenizer = factory.create();
        assertNotNull(tokenizer);
    }

    @Test
    public void testExtendedMode() {
        Settings settings = Settings.builder()
                .put("mode", "extended")
                .build();

        KuromojiTokenizerFactory factory = new KuromojiTokenizerFactory(
                indexSettings, env, "test", settings);

        Tokenizer tokenizer = factory.create();
        assertNotNull(tokenizer);
    }

    @Test
    public void testDiscardPunctuationTrue() {
        Settings settings = Settings.builder()
                .put("discard_punctuation", true)
                .build();

        KuromojiTokenizerFactory factory = new KuromojiTokenizerFactory(
                indexSettings, env, "test", settings);

        Tokenizer tokenizer = factory.create();
        assertNotNull(tokenizer);
    }

    @Test
    public void testDiscardPunctuationFalse() {
        Settings settings = Settings.builder()
                .put("discard_punctuation", false)
                .build();

        KuromojiTokenizerFactory factory = new KuromojiTokenizerFactory(
                indexSettings, env, "test", settings);

        Tokenizer tokenizer = factory.create();
        assertNotNull(tokenizer);
    }

    @Test
    public void testWithUserDictionaryRules() {
        Settings settings = Settings.builder()
                .putList("user_dictionary_rules",
                        "東京スカイツリー,東京 スカイツリー,トウキョウ スカイツリー,カスタム名詞")
                .build();

        KuromojiTokenizerFactory factory = new KuromojiTokenizerFactory(
                indexSettings, env, "test", settings);

        Tokenizer tokenizer = factory.create();
        assertNotNull(tokenizer);
    }

    @Test
    public void testWithUserDictionaryPath() throws IOException {
        File dictFile = new File(env.configDir().toFile(), "userdict.txt");
        try (FileWriter writer = new FileWriter(dictFile)) {
            writer.write("東京スカイツリー,東京 スカイツリー,トウキョウ スカイツリー,カスタム名詞\n");
        }

        Settings settings = Settings.builder()
                .put("user_dictionary", "userdict.txt")
                .build();

        KuromojiTokenizerFactory factory = new KuromojiTokenizerFactory(
                indexSettings, env, "test", settings);

        Tokenizer tokenizer = factory.create();
        assertNotNull(tokenizer);
    }

    @Test
    public void testNBestCost() {
        Settings settings = Settings.builder()
                .put("nbest_cost", 1000)
                .build();

        KuromojiTokenizerFactory factory = new KuromojiTokenizerFactory(
                indexSettings, env, "test", settings);

        Tokenizer tokenizer = factory.create();
        assertNotNull(tokenizer);
    }

    @Test
    public void testNBestExamples() {
        Settings settings = Settings.builder()
                .put("nbest_examples", "食べ--た")
                .build();

        KuromojiTokenizerFactory factory = new KuromojiTokenizerFactory(
                indexSettings, env, "test", settings);

        Tokenizer tokenizer = factory.create();
        assertNotNull(tokenizer);
    }

    @Test
    public void testDiscardCompoundToken() {
        Settings settings = Settings.builder()
                .put("discard_compound_token", true)
                .build();

        KuromojiTokenizerFactory factory = new KuromojiTokenizerFactory(
                indexSettings, env, "test", settings);

        Tokenizer tokenizer = factory.create();
        assertNotNull(tokenizer);
    }

    @Test
    public void testAllSettings() {
        Settings settings = Settings.builder()
                .put("mode", "search")
                .put("discard_punctuation", true)
                .put("nbest_cost", 1000)
                .put("discard_compound_token", false)
                .build();

        KuromojiTokenizerFactory factory = new KuromojiTokenizerFactory(
                indexSettings, env, "test", settings);

        Tokenizer tokenizer = factory.create();
        assertNotNull(tokenizer);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConflictingUserDictionary() throws IOException {
        File dictFile = new File(env.configDir().toFile(), "conflict_dict.txt");
        try (FileWriter writer = new FileWriter(dictFile)) {
            writer.write("テスト,テスト,テスト,名詞\n");
        }

        Settings settings = Settings.builder()
                .put("user_dictionary", "conflict_dict.txt")
                .putList("user_dictionary_rules", "テスト2,テスト2,テスト2,名詞")
                .build();

        // Should throw IllegalArgumentException
        new KuromojiTokenizerFactory(indexSettings, env, "test", settings);
    }

    @Test(expected = RuntimeException.class)
    public void testDuplicateUserDictionaryEntries() {
        Settings settings = Settings.builder()
                .putList("user_dictionary_rules",
                        "東京,トウキョウ,トウキョウ,名詞",
                        "東京,トウキョウ,トウキョウ,名詞")
                .build();

        // Should throw RuntimeException due to duplicate entries
        new KuromojiTokenizerFactory(indexSettings, env, "test", settings);
    }
}
