package org.codelibs.opensearch.extension.analysis;

import static org.codelibs.opensearch.runner.OpenSearchRunner.newConfigs;
import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.Map;

import org.codelibs.curl.CurlResponse;
import org.codelibs.opensearch.runner.OpenSearchRunner;
import org.codelibs.opensearch.runner.net.OpenSearchCurl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opensearch.common.settings.Settings;
import org.opensearch.common.settings.Settings.Builder;
import org.opensearch.common.xcontent.XContentType;
import org.opensearch.node.Node;

public class CharTypeFilterFactoryTest {

    private OpenSearchRunner runner;

    private int numOfNode = 1;

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
        }).build(newConfigs().clusterName(clusterName).numOfNode(numOfNode).pluginTypes("org.codelibs.opensearch.extension.ExtensionPlugin"));

    }

    @After
    public void cleanUp() throws Exception {
        runner.close();
        runner.clean();
    }

    @Test
    public void test_alphabetic() throws Exception {
        runner.ensureYellow();
        Node node = runner.node();

        final String index = "dataset";

        final String indexSettings = "{\"index\":{\"analysis\":{"
                + "\"filter\":{"
                + "\"alphabetic_filter\":{\"type\":\"char_type\",\"alphabetic\":true,\"digit\":false,\"letter\":false}"
                + "},"//
                + "\"analyzer\":{"
                + "\"ja_analyzer\":{\"type\":\"custom\",\"tokenizer\":\"whitespace\"},"
                + "\"ja_alphabetic_analyzer\":{\"type\":\"custom\",\"tokenizer\":\"whitespace\",\"filter\":[\"alphabetic_filter\"]}"
                + "}"//
                + "}}}";
        runner.createIndex(index, Settings.builder().loadFromSource(indexSettings, XContentType.JSON).build());
        runner.ensureYellow();
        {
            String text = "aaa aa1 aaあ aa! 111 11あ 11- あああ ああ- ---";
            try (CurlResponse response = OpenSearchCurl.post(node, "/" + index + "/_analyze").header("Content-Type", "application/json")
                    .body("{\"analyzer\":\"ja_alphabetic_analyzer\",\"text\":\"" + text + "\"}").execute()) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> tokens = (List<Map<String, Object>>) response
                        .getContent(OpenSearchCurl.jsonParser()).get("tokens");
                assertEquals(4, tokens.size());
                assertEquals("aaa", tokens.get(0).get("token").toString());
                assertEquals("aa1", tokens.get(1).get("token").toString());
                assertEquals("aaあ", tokens.get(2).get("token").toString());
                assertEquals("aa!", tokens.get(3).get("token").toString());
            }
        }
    }

    @Test
    public void test_digit() throws Exception {
        runner.ensureYellow();
        Node node = runner.node();

        final String index = "dataset";

        final String indexSettings = "{\"index\":{\"analysis\":{"
                + "\"filter\":{"
                + "\"alphabetic_filter\":{\"type\":\"char_type\",\"alphabetic\":false,\"digit\":true,\"letter\":false}"
                + "},"//
                + "\"analyzer\":{"
                + "\"ja_analyzer\":{\"type\":\"custom\",\"tokenizer\":\"whitespace\"},"
                + "\"ja_alphabetic_analyzer\":{\"type\":\"custom\",\"tokenizer\":\"whitespace\",\"filter\":[\"alphabetic_filter\"]}"
                + "}"//
                + "}}}";
        runner.createIndex(index, Settings.builder().loadFromSource(indexSettings, XContentType.JSON).build());
        runner.ensureYellow();

        {
            String text = "aaa aa1 aaあ aa! 111 11あ 11- あああ ああ- ---";
            try (CurlResponse response = OpenSearchCurl.post(node, "/" + index + "/_analyze").header("Content-Type", "application/json")
                    .body("{\"analyzer\":\"ja_alphabetic_analyzer\",\"text\":\"" + text + "\"}").execute()) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> tokens = (List<Map<String, Object>>) response
                        .getContent(OpenSearchCurl.jsonParser()).get("tokens");
                assertEquals(4, tokens.size());
                assertEquals("aa1", tokens.get(0).get("token").toString());
                assertEquals("111", tokens.get(1).get("token").toString());
                assertEquals("11あ", tokens.get(2).get("token").toString());
                assertEquals("11-", tokens.get(3).get("token").toString());
            }
        }
    }

    @Test
    public void test_letter() throws Exception {
        runner.ensureYellow();
        Node node = runner.node();

        final String index = "dataset";

        final String indexSettings = "{\"index\":{\"analysis\":{"
                + "\"filter\":{"
                + "\"alphabetic_filter\":{\"type\":\"char_type\",\"alphabetic\":false,\"digit\":false,\"letter\":true}"
                + "},"//
                + "\"analyzer\":{"
                + "\"ja_analyzer\":{\"type\":\"custom\",\"tokenizer\":\"whitespace\"},"
                + "\"ja_alphabetic_analyzer\":{\"type\":\"custom\",\"tokenizer\":\"whitespace\",\"filter\":[\"alphabetic_filter\"]}"
                + "}"//
                + "}}}";
        runner.createIndex(index, Settings.builder().loadFromSource(indexSettings, XContentType.JSON).build());
        runner.ensureYellow();

        {
            String text = "aaa aa1 aaあ aa! 111 11あ 11- あああ ああ- ---";
            try (CurlResponse response = OpenSearchCurl.post(node, "/" + index + "/_analyze").header("Content-Type", "application/json")
                    .body("{\"analyzer\":\"ja_alphabetic_analyzer\",\"text\":\"" + text + "\"}").execute()) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> tokens = (List<Map<String, Object>>) response
                        .getContent(OpenSearchCurl.jsonParser()).get("tokens");
                assertEquals(7, tokens.size());
                assertEquals("aaa", tokens.get(0).get("token").toString());
                assertEquals("aa1", tokens.get(1).get("token").toString());
                assertEquals("aaあ", tokens.get(2).get("token").toString());
                assertEquals("aa!", tokens.get(3).get("token").toString());
                assertEquals("11あ", tokens.get(4).get("token").toString());
                assertEquals("あああ", tokens.get(5).get("token").toString());
                assertEquals("ああ-", tokens.get(6).get("token").toString());
            }
        }
    }

    @Test
    public void test_digitOrLetter() throws Exception {
        runner.ensureYellow();
        Node node = runner.node();

        final String index = "dataset";

        final String indexSettings = "{\"index\":{\"analysis\":{"
                + "\"filter\":{"
                + "\"alphabetic_filter\":{\"type\":\"char_type\",\"alphabetic\":false,\"digit\":true,\"letter\":true}"
                + "},"//
                + "\"analyzer\":{"
                + "\"ja_analyzer\":{\"type\":\"custom\",\"tokenizer\":\"whitespace\"},"
                + "\"ja_alphabetic_analyzer\":{\"type\":\"custom\",\"tokenizer\":\"whitespace\",\"filter\":[\"alphabetic_filter\"]}"
                + "}"//
                + "}}}";
        runner.createIndex(index, Settings.builder().loadFromSource(indexSettings, XContentType.JSON).build());
        runner.ensureYellow();

        {
            String text = "aaa aa1 aaあ aa! 111 11あ 11- あああ ああ- ---";
            try (CurlResponse response = OpenSearchCurl.post(node, "/" + index + "/_analyze").header("Content-Type", "application/json")
                    .body("{\"analyzer\":\"ja_alphabetic_analyzer\",\"text\":\"" + text + "\"}").execute()) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> tokens = (List<Map<String, Object>>) response
                        .getContent(OpenSearchCurl.jsonParser()).get("tokens");
                assertEquals(9, tokens.size());
                assertEquals("aaa", tokens.get(0).get("token").toString());
                assertEquals("aa1", tokens.get(1).get("token").toString());
                assertEquals("aaあ", tokens.get(2).get("token").toString());
                assertEquals("aa!", tokens.get(3).get("token").toString());
                assertEquals("111", tokens.get(4).get("token").toString());
                assertEquals("11あ", tokens.get(5).get("token").toString());
                assertEquals("11-", tokens.get(6).get("token").toString());
                assertEquals("あああ", tokens.get(7).get("token").toString());
                assertEquals("ああ-", tokens.get(8).get("token").toString());
            }
        }
    }
}
