package org.codelibs.opensearch.extension.analysis;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.lucene.analysis.Tokenizer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opensearch.common.settings.Settings;
import org.opensearch.env.Environment;
import org.opensearch.index.IndexSettings;

import java.nio.file.Files;
import java.nio.file.Path;

public class NGramSynonymTokenizerFactoryTest {

    private Environment env;
    private File tempDir;
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
        indexSettings = new IndexSettings(
                org.opensearch.index.Index.create("test", "_na_"),
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
    public void testDefaultSettings() {
        Settings settings = Settings.builder().build();

        NGramSynonymTokenizerFactory factory = new NGramSynonymTokenizerFactory(
                indexSettings, env, "test", settings);

        Tokenizer tokenizer = factory.create();
        assertNotNull(tokenizer);
        assertTrue(tokenizer instanceof NGramSynonymTokenizer);
    }

    @Test
    public void testCustomNGramSize() {
        Settings settings = Settings.builder()
                .put("n", 3)
                .build();

        NGramSynonymTokenizerFactory factory = new NGramSynonymTokenizerFactory(
                indexSettings, env, "test", settings);

        Tokenizer tokenizer = factory.create();
        assertNotNull(tokenizer);
    }

    @Test
    public void testIgnoreCaseTrue() {
        Settings settings = Settings.builder()
                .put("ignore_case", true)
                .build();

        NGramSynonymTokenizerFactory factory = new NGramSynonymTokenizerFactory(
                indexSettings, env, "test", settings);

        Tokenizer tokenizer = factory.create();
        assertNotNull(tokenizer);
    }

    @Test
    public void testIgnoreCaseFalse() {
        Settings settings = Settings.builder()
                .put("ignore_case", false)
                .build();

        NGramSynonymTokenizerFactory factory = new NGramSynonymTokenizerFactory(
                indexSettings, env, "test", settings);

        Tokenizer tokenizer = factory.create();
        assertNotNull(tokenizer);
    }

    @Test
    public void testCustomDelimiters() {
        Settings settings = Settings.builder()
                .put("delimiters", " \t\n")
                .build();

        NGramSynonymTokenizerFactory factory = new NGramSynonymTokenizerFactory(
                indexSettings, env, "test", settings);

        Tokenizer tokenizer = factory.create();
        assertNotNull(tokenizer);
    }

    @Test
    public void testExpandTrue() {
        Settings settings = Settings.builder()
                .put("expand", true)
                .putList("synonyms", "PC,personal computer")
                .build();

        NGramSynonymTokenizerFactory factory = new NGramSynonymTokenizerFactory(
                indexSettings, env, "test", settings);

        Tokenizer tokenizer = factory.create();
        assertNotNull(tokenizer);
    }

    @Test
    public void testExpandFalse() {
        Settings settings = Settings.builder()
                .put("expand", false)
                .putList("synonyms", "PC => personal computer")
                .build();

        NGramSynonymTokenizerFactory factory = new NGramSynonymTokenizerFactory(
                indexSettings, env, "test", settings);

        Tokenizer tokenizer = factory.create();
        assertNotNull(tokenizer);
    }

    @Test
    public void testWithSynonymsFromList() {
        Settings settings = Settings.builder()
                .put("n", 2)
                .put("ignore_case", true)
                .putList("synonyms", "PC,personal computer", "laptop,notebook")
                .build();

        NGramSynonymTokenizerFactory factory = new NGramSynonymTokenizerFactory(
                indexSettings, env, "test", settings);

        Tokenizer tokenizer = factory.create();
        assertNotNull(tokenizer);
    }

    @Test
    public void testWithSynonymsFromFile() throws IOException {
        File synonymFile = new File(env.configDir().toFile(), "synonyms_ngram.txt");
        try (FileWriter writer = new FileWriter(synonymFile)) {
            writer.write("PC,personal computer\n");
            writer.write("laptop,notebook\n");
        }

        Settings settings = Settings.builder()
                .put("synonyms_path", "synonyms_ngram.txt")
                .put("n", 2)
                .build();

        NGramSynonymTokenizerFactory factory = new NGramSynonymTokenizerFactory(
                indexSettings, env, "test", settings);

        Tokenizer tokenizer = factory.create();
        assertNotNull(tokenizer);
    }

    @Test
    public void testEmptySynonyms() {
        Settings settings = Settings.builder()
                .put("n", 2)
                .build();

        NGramSynonymTokenizerFactory factory = new NGramSynonymTokenizerFactory(
                indexSettings, env, "test", settings);

        Tokenizer tokenizer = factory.create();
        assertNotNull(tokenizer);
    }

    @Test
    public void testAllSettings() {
        Settings settings = Settings.builder()
                .put("n", 3)
                .put("ignore_case", true)
                .put("delimiters", " \t")
                .put("expand", true)
                .putList("synonyms", "test,exam")
                .build();

        NGramSynonymTokenizerFactory factory = new NGramSynonymTokenizerFactory(
                indexSettings, env, "test", settings);

        Tokenizer tokenizer = factory.create();
        assertNotNull(tokenizer);
    }
}
