package org.codelibs.opensearch.extension.kuromoji.index.analysis;

import static org.junit.Assert.*;

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

public class KuromojiBaseFormFilterFactoryTest extends OpenSearchTestCase {

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
    public void testCreate() {
        Settings settings = Settings.builder().build();

        KuromojiBaseFormFilterFactory factory = new KuromojiBaseFormFilterFactory(
                indexSettings, env, "test", settings);

        WhitespaceTokenizer tokenizer = new WhitespaceTokenizer();
        TokenStream tokenStream = factory.create(tokenizer);

        assertNotNull(tokenStream);
    }

    @Test
    public void testFactoryName() {
        Settings settings = Settings.builder().build();

        KuromojiBaseFormFilterFactory factory = new KuromojiBaseFormFilterFactory(
                indexSettings, env, "kuromoji_baseform", settings);

        assertNotNull(factory);
        assertEquals("kuromoji_baseform", factory.name());
    }

    @Test
    public void testMultipleCreations() {
        Settings settings = Settings.builder().build();

        KuromojiBaseFormFilterFactory factory = new KuromojiBaseFormFilterFactory(
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
    public void testWithEmptySettings() {
        Settings settings = Settings.builder().build();

        KuromojiBaseFormFilterFactory factory = new KuromojiBaseFormFilterFactory(
                indexSettings, env, "test", settings);

        assertNotNull(factory);
    }
}
