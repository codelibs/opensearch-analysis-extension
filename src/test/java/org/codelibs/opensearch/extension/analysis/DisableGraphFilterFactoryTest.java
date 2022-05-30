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
import org.opensearch.action.search.SearchResponse;
import org.opensearch.action.support.WriteRequest.RefreshPolicy;
import org.opensearch.common.settings.Settings;
import org.opensearch.common.settings.Settings.Builder;
import org.opensearch.common.xcontent.XContentType;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.node.Node;

public class DisableGraphFilterFactoryTest {

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
    public void test_disableGraph() throws Exception {
        runner.ensureYellow();
        Node node = runner.node();

        final String index = "dataset";

        final String indexSettings = "{\"index\":{\"analysis\":{"//
                + "\"analyzer\":{"
                + "\"ja_analyzer\":{\"type\":\"custom\",\"tokenizer\":\"reloadable_kuromoji_tokenizer\",\"filter\":[\"disable_graph\"]}"
                + "}"//
                + "}}}";
        runner.createIndex(index, Settings.builder().loadFromSource(indexSettings, XContentType.JSON).build());
        runner.ensureYellow();
        runner.createMapping(index,
                "{\"properties\":{\"content\" : {\"type\" : \"text\",\"analyzer\":\"ja_analyzer\"}}}");
        try (CurlResponse response = OpenSearchCurl.post(node, "/" + index + "/_analyze").header("Content-Type", "application/json")
                .body("{\"analyzer\":\"ja_analyzer\",\"text\":\"レッドハウスフーズ\"}").execute()) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> tokens = (List<Map<String, Object>>) response.getContent(OpenSearchCurl.jsonParser()).get("tokens");
            assertEquals(2, tokens.size());
            assertEquals("レッド", tokens.get(0).get("token").toString());
            assertEquals("ハウスフーズ", tokens.get(1).get("token").toString());
        }

        runner.insert(index, "1",
                builder -> builder.setSource("{\"content\":\"レッド\"}", XContentType.JSON).setRefreshPolicy(RefreshPolicy.WAIT_UNTIL));

        SearchResponse response = runner.search(index, builder -> builder.setQuery(QueryBuilders.matchQuery("content", "レッドハウスフーズ")));
        assertEquals(1L, response.getHits().getTotalHits().value);
    }
}
