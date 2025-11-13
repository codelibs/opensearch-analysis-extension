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
import org.opensearch.env.TestEnvironment;
import org.opensearch.index.IndexSettings;
import org.opensearch.test.IndexSettingsModule;
import org.opensearch.test.OpenSearchTestCase;

public class NGramSynonymTokenizerFactoryTest extends OpenSearchTestCase {

    private Environment env;
    private File tempDir;
    private IndexSettings indexSettings;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        tempDir = createTempDir().toFile();
        Settings settings = Settings.builder()
                .put("path.home", tempDir.getAbsolutePath())
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
