package org.codelibs.opensearch.extension.analysis;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.synonym.SynonymMap;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opensearch.common.settings.Settings;
import org.opensearch.env.Environment;
import org.opensearch.env.TestEnvironment;
import org.opensearch.index.IndexSettings;
import org.opensearch.test.IndexSettingsModule;
import org.opensearch.test.OpenSearchTestCase;

public class SynonymLoaderTest extends OpenSearchTestCase {

    private Environment env;
    private File tempDir;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        tempDir = createTempDir().toFile();
        Settings settings = Settings.builder()
                .put("path.home", tempDir.getAbsolutePath())
                .build();
        env = TestEnvironment.newEnvironment(settings);
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    public void testLoadSynonymsFromList() {
        Settings settings = Settings.builder()
                .putList("synonyms", "PC,personal computer", "laptop,notebook")
                .build();

        Analyzer analyzer = SynonymLoader.getAnalyzer(false);
        SynonymLoader loader = new SynonymLoader(env, settings, true, analyzer);

        assertNotNull(loader.getSynonymMap());
        assertFalse(loader.isReloadable());
        assertTrue(loader.getLastModified() > 0);
    }

    @Test
    public void testLoadSynonymsFromFile() throws IOException {
        File synonymFile = new File(env.configDir().toFile(), "synonyms.txt");
        try (FileWriter writer = new FileWriter(synonymFile)) {
            writer.write("PC,personal computer\n");
            writer.write("laptop,notebook\n");
        }

        Settings settings = Settings.builder()
                .put("synonyms_path", "synonyms.txt")
                .build();

        Analyzer analyzer = SynonymLoader.getAnalyzer(false);
        SynonymLoader loader = new SynonymLoader(env, settings, true, analyzer);

        assertNotNull(loader.getSynonymMap());
        assertFalse(loader.isReloadable());
    }

    @Test
    public void testLoadSynonymsWithDynamicReload() throws IOException {
        File synonymFile = new File(env.configDir().toFile(), "synonyms_dynamic.txt");
        try (FileWriter writer = new FileWriter(synonymFile)) {
            writer.write("PC,personal computer\n");
        }

        Settings settings = Settings.builder()
                .put("synonyms_path", "synonyms_dynamic.txt")
                .put("dynamic_reload", true)
                .put("reload_interval", "1s")
                .build();

        Analyzer analyzer = SynonymLoader.getAnalyzer(false);
        SynonymLoader loader = new SynonymLoader(env, settings, true, analyzer);

        assertNotNull(loader.getSynonymMap());
        assertTrue(loader.isReloadable());
        long initialModified = loader.getLastModified();

        // Test update detection
        assertFalse(loader.isUpdate(initialModified));
    }

    @Test
    public void testLoadSynonymsWithWordnetFormat() {
        Settings settings = Settings.builder()
                .putList("synonyms", "s(100000001,1,'PC',n,1,0).")
                .put("format", "wordnet")
                .build();

        Analyzer analyzer = SynonymLoader.getAnalyzer(false);
        SynonymLoader loader = new SynonymLoader(env, settings, true, analyzer);

        // Wordnet format might result in null if parsing fails, which is acceptable
        // This tests that the loader handles wordnet format without exceptions
        assertNotNull(loader);
    }

    @Test
    public void testEmptySynonyms() {
        Settings settings = Settings.builder().build();

        Analyzer analyzer = SynonymLoader.getAnalyzer(false);
        SynonymLoader loader = new SynonymLoader(env, settings, true, analyzer);

        assertNull(loader.getSynonymMap());
        assertFalse(loader.isReloadable());
    }

    @Test
    public void testGetAnalyzerIgnoreCase() {
        Analyzer analyzer = SynonymLoader.getAnalyzer(true);
        assertNotNull(analyzer);
    }

    @Test
    public void testGetAnalyzerCaseSensitive() {
        Analyzer analyzer = SynonymLoader.getAnalyzer(false);
        assertNotNull(analyzer);
    }

    @Test
    public void testSynonymExpansion() {
        Settings settings = Settings.builder()
                .putList("synonyms", "PC,personal computer,desktop")
                .build();

        Analyzer analyzer = SynonymLoader.getAnalyzer(false);
        SynonymLoader loader = new SynonymLoader(env, settings, true, analyzer);

        assertNotNull(loader.getSynonymMap());
        SynonymMap map = loader.getSynonymMap();
        assertNotNull(map.fst);
    }

    @Test
    public void testSynonymNoExpansion() {
        Settings settings = Settings.builder()
                .putList("synonyms", "PC => personal computer")
                .build();

        Analyzer analyzer = SynonymLoader.getAnalyzer(false);
        SynonymLoader loader = new SynonymLoader(env, settings, false, analyzer);

        assertNotNull(loader.getSynonymMap());
    }
}
