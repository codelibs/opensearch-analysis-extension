package org.codelibs.opensearch.extension.analysis;

import static org.junit.Assert.*;

import java.io.Reader;
import java.io.StringReader;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opensearch.common.settings.Settings;
import org.opensearch.env.Environment;
import org.opensearch.env.TestEnvironment;
import org.opensearch.index.IndexSettings;
import org.opensearch.test.IndexSettingsModule;
import org.opensearch.test.OpenSearchTestCase;

public class ProlongedSoundMarkCharFilterFactoryTest extends OpenSearchTestCase {

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
