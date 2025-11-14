package org.codelibs.opensearch.extension.analysis;

import static org.junit.Assert.*;

import java.io.Reader;
import java.io.File;
import java.io.StringReader;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opensearch.common.settings.Settings;
import org.opensearch.env.Environment;
import org.opensearch.index.IndexSettings;

import java.nio.file.Files;
import java.nio.file.Path;

public class ProlongedSoundMarkCharFilterFactoryTest {

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
    public void testDefaultReplacement() {
        Settings settings = Settings.builder().build();

        ProlongedSoundMarkCharFilterFactory factory = new ProlongedSoundMarkCharFilterFactory(
                indexSettings, env, "test", settings);

        Reader input = new StringReader("test");
        Reader output = factory.create(input);

        assertNotNull(output);
    }

    @Test
    public void testCustomReplacement() {
        Settings settings = Settings.builder()
                .put("replacement", "ー")
                .build();

        ProlongedSoundMarkCharFilterFactory factory = new ProlongedSoundMarkCharFilterFactory(
                indexSettings, env, "test", settings);

        Reader input = new StringReader("test");
        Reader output = factory.create(input);

        assertNotNull(output);
    }

    @Test
    public void testSingleCharReplacement() {
        Settings settings = Settings.builder()
                .put("replacement", "X")
                .build();

        ProlongedSoundMarkCharFilterFactory factory = new ProlongedSoundMarkCharFilterFactory(
                indexSettings, env, "test", settings);

        assertNotNull(factory);
    }

    @Test
    public void testEmptyReplacementUsesDefault() {
        Settings settings = Settings.builder()
                .put("replacement", "")
                .build();

        ProlongedSoundMarkCharFilterFactory factory = new ProlongedSoundMarkCharFilterFactory(
                indexSettings, env, "test", settings);

        // Empty replacement should use default \u30fc
        assertNotNull(factory);
    }

    @Test
    public void testFactoryName() {
        Settings settings = Settings.builder().build();

        ProlongedSoundMarkCharFilterFactory factory = new ProlongedSoundMarkCharFilterFactory(
                indexSettings, env, "prolonged_sound_filter", settings);

        assertNotNull(factory);
        assertEquals("prolonged_sound_filter", factory.name());
    }

    @Test
    public void testMultipleCreations() {
        Settings settings = Settings.builder()
                .put("replacement", "ー")
                .build();

        ProlongedSoundMarkCharFilterFactory factory = new ProlongedSoundMarkCharFilterFactory(
                indexSettings, env, "test", settings);

        Reader input1 = new StringReader("test1");
        Reader output1 = factory.create(input1);
        assertNotNull(output1);

        Reader input2 = new StringReader("test2");
        Reader output2 = factory.create(input2);
        assertNotNull(output2);

        assertNotSame(output1, output2);
    }
}
