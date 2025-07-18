package org.codelibs.opensearch.extension;

import static org.codelibs.opensearch.runner.OpenSearchRunner.newConfigs;
import static org.junit.Assert.assertEquals;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;

import org.codelibs.curl.CurlResponse;
import org.codelibs.opensearch.runner.OpenSearchRunner;
import org.codelibs.opensearch.runner.net.OpenSearchCurl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opensearch.action.index.IndexResponse;
import org.opensearch.action.search.SearchResponse;
import org.opensearch.common.settings.Settings;
import org.opensearch.common.settings.Settings.Builder;
import org.opensearch.common.xcontent.XContentFactory;
import org.opensearch.common.xcontent.XContentType;
import org.opensearch.core.rest.RestStatus;
import org.opensearch.core.xcontent.XContentBuilder;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.node.Node;

public class ExtensionPluginTest {

    private OpenSearchRunner runner;

    private File[] userDictFiles;

    private int numOfNode = 1;

    private int numOfDocs = 1000;

    private String clusterName;

    @Before
    public void setUp() throws Exception {
        clusterName = "es-analysisja-" + System.currentTimeMillis();
        runner = new OpenSearchRunner();
        runner.onBuild(new OpenSearchRunner.Builder() {
            @Override
            public void build(final int number, final Builder settingsBuilder) {
                settingsBuilder.put("http.cors.enabled", true);
                settingsBuilder.put("http.cors.allow-origin", "*");
                settingsBuilder.put("discovery.type", "single-node");
                // settingsBuilder.putList("discovery.seed_hosts", "127.0.0.1:9301");
                // settingsBuilder.putList("cluster.initial_master_nodes", "127.0.0.1:9301");
            }
        }).build(newConfigs().clusterName(clusterName).numOfNode(numOfNode)
                .pluginTypes("org.codelibs.opensearch.extension.ExtensionPlugin,"
                        + "org.codelibs.opensearch.extension.kuromoji.plugin.analysis.kuromoji.AnalysisKuromojiPlugin"));

        userDictFiles = null;
    }

    private void updateDictionary(File file, String content)
            throws IOException, UnsupportedEncodingException,
            FileNotFoundException {
        try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(file), "UTF-8"))) {
            bw.write(content);
            bw.flush();
        }
    }

    @After
    public void cleanUp() throws Exception {
        runner.close();
        runner.clean();
        if (userDictFiles != null) {
            for (File file : userDictFiles) {
                file.deleteOnExit();
            }
        }
    }

    @Test
    public void test_kuromoji() throws Exception {
        userDictFiles = new File[numOfNode];
        for (int i = 0; i < numOfNode; i++) {
            String homePath = runner.getNode(i).settings().get("path.home");
            userDictFiles[i] = new File(new File(homePath, "config"), "userdict_ja.txt");
            updateDictionary(userDictFiles[i],
                    "東京スカイツリー,東京 スカイツリー,トウキョウ スカイツリー,カスタム名詞");
        }

        runner.ensureYellow();
        Node node = runner.node();

        final String index = "dataset";

        final String indexSettings = "{\"index\":{\"analysis\":{"
                + "\"tokenizer\":{"//
                + "\"kuromoji_user_dict\":{\"type\":\"kuromoji_tokenizer\",\"mode\":\"extended\",\"user_dictionary\":\"userdict_ja.txt\"},"
                + "\"kuromoji_user_dict_reload\":{\"type\":\"reloadable_kuromoji\",\"mode\":\"extended\",\"user_dictionary\":\"userdict_ja.txt\",\"reload_interval\":\"1s\"}"
                + "},"//
                + "\"analyzer\":{"
                + "\"ja_analyzer\":{\"type\":\"custom\",\"tokenizer\":\"kuromoji_user_dict\",\"filter\":[\"reloadable_kuromoji_stemmer\"]},"
                + "\"ja_reload_analyzer\":{\"type\":\"custom\",\"tokenizer\":\"kuromoji_user_dict_reload\",\"filter\":[\"reloadable_kuromoji_stemmer\"]}"
                + "}"//
                + "}}}";
        runner.createIndex(index, Settings.builder().loadFromSource(indexSettings, XContentType.JSON).build());
        runner.ensureYellow();

        // create a mapping
        final XContentBuilder mappingBuilder = XContentFactory.jsonBuilder()//
                .startObject()//
                .startObject("properties")//

                // id
                .startObject("id")//
                .field("type", "keyword")//
                .endObject()//

                // msg1
                .startObject("msg1")//
                .field("type", "text")//
                .field("analyzer", "ja_reload_analyzer")//
                .endObject()//

                // msg2
                .startObject("msg2")//
                .field("type", "text")//
                .field("analyzer", "ja_analyzer")//
                .endObject()//

                .endObject()//
                .endObject();
        runner.createMapping(index, mappingBuilder);

        final IndexResponse indexResponse1 = runner.insert(index, "1",
                "{\"msg1\":\"東京スカイツリー\", \"msg2\":\"東京スカイツリー\", \"id\":\"1\"}");
        assertEquals(RestStatus.CREATED, indexResponse1.status());
        runner.refresh();

        String text;
        for (int i = 0; i < 1000; i++) {
            text = "東京スカイツリー";
            assertDocCount(1, index, "msg1", text);
            assertDocCount(1, index, "msg2", text);

            try (CurlResponse response = OpenSearchCurl.post(node, "/" + index + "/_analyze").header("Content-Type", "application/json")
                    .body("{\"analyzer\":\"ja_reload_analyzer\",\"text\":\"" + text + "\"}").execute()) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> tokens = (List<Map<String, Object>>) response
                        .getContent(OpenSearchCurl.jsonParser()).get("tokens");
                assertEquals("東京", tokens.get(0).get("token").toString());
                assertEquals("スカイツリ", tokens.get(1).get("token").toString());
            }

            text = "朝青龍";
            try (CurlResponse response = OpenSearchCurl.post(node, "/" + index + "/_analyze").header("Content-Type", "application/json")
                    .body("{\"analyzer\":\"ja_reload_analyzer\",\"text\":\"" + text + "\"}").execute()) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> tokens = (List<Map<String, Object>>) response
                        .getContent(OpenSearchCurl.jsonParser()).get("tokens");
                assertEquals("朝", tokens.get(0).get("token").toString());
                assertEquals("青龍", tokens.get(1).get("token").toString());
            }
        }

        // changing a file timestamp
        Thread.sleep(2000);

        for (int i = 0; i < numOfNode; i++) {
            updateDictionary(userDictFiles[i],
                    "東京スカイツリー,東京 スカイ ツリー,トウキョウ スカイ ツリー,カスタム名詞\n"
                            + "朝青龍,朝青龍,アサショウリュウ,人名");
        }

        final IndexResponse indexResponse2 = runner.insert(index, "2",
                "{\"msg1\":\"東京スカイツリー\", \"msg2\":\"東京スカイツリー\", \"id\":\"2\"}");
        assertEquals(RestStatus.CREATED, indexResponse2.status());
        runner.refresh();

        for (int i = 0; i < 1000; i++) {
            text = "東京スカイツリー";
            assertDocCount(2, index, "msg1", text);
            assertDocCount(2, index, "msg2", text);

            try (CurlResponse response = OpenSearchCurl.post(node, "/" + index + "/_analyze").header("Content-Type", "application/json")
                    .body("{\"analyzer\":\"ja_reload_analyzer\",\"text\":\"" + text + "\"}").execute()) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> tokens = (List<Map<String, Object>>) response
                        .getContent(OpenSearchCurl.jsonParser()).get("tokens");
                assertEquals("東京", tokens.get(0).get("token").toString());
                assertEquals("スカイツリ", tokens.get(1).get("token").toString());
            }

            text = "朝青龍";
            try (CurlResponse response = OpenSearchCurl.post(node, "/" + index + "/_analyze").header("Content-Type", "application/json")
                    .body("{\"analyzer\":\"ja_reload_analyzer\",\"text\":\"" + text + "\"}").execute()) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> tokens = (List<Map<String, Object>>) response
                        .getContent(OpenSearchCurl.jsonParser()).get("tokens");
                assertEquals("朝", tokens.get(0).get("token").toString());
                assertEquals("青龍", tokens.get(1).get("token").toString());
            }
        }
    }

    @Test
    public void test_iteration_mark() throws Exception {
        runner.ensureYellow();
        Node node = runner.node();

        final String index = "dataset";

        final String indexSettings = "{\"index\":{\"analysis\":{"
                + "\"analyzer\":{"
                + "\"ja_analyzer\":{\"type\":\"custom\",\"tokenizer\":\"whitespace\"},"
                + "\"ja_imark_analyzer\":{\"type\":\"custom\",\"tokenizer\":\"whitespace\",\"char_filter\":[\"iteration_mark\"]}"
                + "}"//
                + "}}}";
        runner.createIndex(index, Settings.builder().loadFromSource(indexSettings, XContentType.JSON).build());
        runner.ensureYellow();

        // create a mapping
        final XContentBuilder mappingBuilder = XContentFactory.jsonBuilder()//
                .startObject()//
                .startObject("properties")//

                // id
                .startObject("id")//
                .field("type", "keyword")//
                .endObject()//

                // msg1
                .startObject("msg1")//
                .field("type", "text")//
                .field("analyzer", "ja_imark_analyzer")//
                .endObject()//

                // msg2
                .startObject("msg2")//
                .field("type", "text")//
                .field("analyzer", "ja_analyzer")//
                .endObject()//

                .endObject()//
                .endObject();
        runner.createMapping(index, mappingBuilder);

        final IndexResponse indexResponse1 = runner.insert(index, "1",
                "{\"msg1\":\"時々\", \"msg2\":\"時々\", \"id\":\"1\"}");
        assertEquals(RestStatus.CREATED, indexResponse1.status());
        runner.refresh();

        String[] inputs = new String[] { "こゝ ここ", "バナヽ バナナ", "学問のすゝめ 学問のすすめ",
                "いすゞ いすず", "づゝ づつ", "ぶゞ漬け ぶぶ漬け", "各〻 各各" };

        assertDocCount(1, index, "msg1", "時々");
        assertDocCount(1, index, "msg1", "時時");
        assertDocCount(1, index, "msg2", "時々");

        for (int i = 0; i < inputs.length; i++) {
            String[] values = inputs[i].split(" ");
            String text = values[0];
            try (CurlResponse response = OpenSearchCurl.post(node, "/" + index + "/_analyze").header("Content-Type", "application/json")
                    .body("{\"analyzer\":\"ja_imark_analyzer\",\"text\":\"" + text + "\"}").execute()) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> tokens = (List<Map<String, Object>>) response
                        .getContent(OpenSearchCurl.jsonParser()).get("tokens");
                assertEquals(values[1], tokens.get(0).get("token").toString());
            }
        }

    }

    @Test
    public void test_prolonged_sound_mark() throws Exception {
        runner.ensureYellow();
        Node node = runner.node();

        final String index = "dataset";

        final String indexSettings = "{\"index\":{\"analysis\":{"
                + "\"analyzer\":{"
                + "\"ja_analyzer\":{\"type\":\"custom\",\"tokenizer\":\"whitespace\"},"
                + "\"ja_psmark_analyzer\":{\"type\":\"custom\",\"tokenizer\":\"whitespace\",\"char_filter\":[\"prolonged_sound_mark\"]}"
                + "}"//
                + "}}}";
        runner.createIndex(index, Settings.builder().loadFromSource(indexSettings, XContentType.JSON).build());
        runner.ensureYellow();

        // create a mapping
        final XContentBuilder mappingBuilder = XContentFactory.jsonBuilder()//
                .startObject()//
                .startObject("properties")//

                // id
                .startObject("id")//
                .field("type", "keyword")//
                .endObject()//

                // msg1
                .startObject("msg1")//
                .field("type", "text")//
                .field("analyzer", "ja_psmark_analyzer")//
                .endObject()//

                // msg2
                .startObject("msg2")//
                .field("type", "text")//
                .field("analyzer", "ja_analyzer")//
                .endObject()//

                .endObject()//
                .endObject();
        runner.createMapping(index, mappingBuilder);

        final IndexResponse indexResponse1 = runner.insert(index, "1",
                "{\"msg1\":\"あ‐\", \"msg2\":\"あ‐\", \"id\":\"1\"}");
        assertEquals(RestStatus.CREATED, indexResponse1.status());
        runner.refresh();

        String[] psms = new String[] { "\u002d", "\uff0d", "\u2010", "\u2011",
                "\u2012", "\u2013", "\u2014", "\u2015", "\u207b", "\u208b",
                "\u30fc" };

        assertDocCount(1, index, "msg1", "あ‐");
        assertDocCount(1, index, "msg2", "あ‐");

        for (String psm : psms) {
            String text = "あ" + psm;
            assertDocCount(1, index, "msg1", text);
            try (CurlResponse response = OpenSearchCurl.post(node, "/" + index + "/_analyze").header("Content-Type", "application/json")
                    .body("{\"analyzer\":\"ja_psmark_analyzer\",\"text\":\"" + text + "\"}").execute()) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> tokens = (List<Map<String, Object>>) response
                        .getContent(OpenSearchCurl.jsonParser()).get("tokens");
                assertEquals("あー", tokens.get(0).get("token").toString());
            }
        }

    }

    @Test
    public void test_kanji_number() throws Exception {
        runner.ensureYellow();
        Node node = runner.node();

        final String index = "dataset";

        final String indexSettings = "{\"index\":{\"analysis\":{"
                + "\"tokenizer\":{"//
                + "\"kuromoji_user_dict\":{\"type\":\"kuromoji_tokenizer\",\"mode\":\"extended\"}"
                + "},"//
                + "\"analyzer\":{"
                + "\"ja_analyzer\":{\"type\":\"custom\",\"tokenizer\":\"kuromoji_user_dict\"},"
                + "\"ja_knum_analyzer\":{\"type\":\"custom\",\"tokenizer\":\"kuromoji_user_dict\",\"filter\":[\"kanji_number\"]}"
                + "}"//
                + "}}}";
        runner.createIndex(index, Settings.builder().loadFromSource(indexSettings, XContentType.JSON).build());
        runner.ensureYellow();

        // create a mapping
        final XContentBuilder mappingBuilder = XContentFactory.jsonBuilder()//
                .startObject()//
                .startObject("properties")//

                // id
                .startObject("id")//
                .field("type", "keyword")//
                .endObject()//

                // msg1
                .startObject("msg1")//
                .field("type", "text")//
                .field("analyzer", "ja_knum_analyzer")//
                .endObject()//

                // msg2
                .startObject("msg2")//
                .field("type", "text")//
                .field("analyzer", "ja_analyzer")//
                .endObject()//

                .endObject()//
                .endObject();
        runner.createMapping(index, mappingBuilder);

        final IndexResponse indexResponse1 = runner.insert(index, "1",
                "{\"msg1\":\"十二時間\", \"msg2\":\"十二時間\", \"id\":\"1\"}");
        assertEquals(RestStatus.CREATED, indexResponse1.status());
        runner.refresh();

        assertDocCount(1, index, "msg1", "十二時間");
        assertDocCount(1, index, "msg1", "12時間");
        assertDocCount(1, index, "msg2", "十二時間");
        assertDocCount(0, index, "msg2", "12時間");

        String text = "一億九千万円";
        try (CurlResponse response = OpenSearchCurl.post(node, "/" + index + "/_analyze").header("Content-Type", "application/json")
                .body("{\"analyzer\":\"ja_knum_analyzer\",\"text\":\"" + text + "\"}").execute()) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> tokens = (List<Map<String, Object>>) response
                    .getContent(OpenSearchCurl.jsonParser()).get("tokens");
            assertEquals("190000000", tokens.get(0).get("token").toString());
            assertEquals("円", tokens.get(1).get("token").toString());
        }

    }

    private void assertDocCount(int expected, final String index,
            final String field, final String value) {
        final SearchResponse searchResponse = runner.search(index,
                QueryBuilders.matchPhraseQuery(field, value), null,
                0, numOfDocs);
        assertEquals(expected, searchResponse.getHits().getTotalHits().value());
    }
}
