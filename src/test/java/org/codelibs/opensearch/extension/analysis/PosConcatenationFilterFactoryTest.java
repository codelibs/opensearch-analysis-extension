package org.codelibs.opensearch.extension.analysis;

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
import org.opensearch.common.settings.Settings;
import org.opensearch.common.settings.Settings.Builder;
import org.opensearch.common.xcontent.XContentType;
import org.opensearch.node.Node;

public class PosConcatenationFilterFactoryTest {

    private OpenSearchRunner runner;

    private int numOfNode = 1;

    private File[] numberSuffixFiles;

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

        numberSuffixFiles = null;
    }

    @After
    public void cleanUp() throws Exception {
        runner.close();
        runner.clean();
        if (numberSuffixFiles != null) {
            for (File file : numberSuffixFiles) {
                file.deleteOnExit();
            }
        }
    }

    @Test
    public void test_basic() throws Exception {
        numberSuffixFiles = new File[numOfNode];
        for (int i = 0; i < numOfNode; i++) {
            String homePath = runner.getNode(i).settings().get("path.home");
            numberSuffixFiles[i] = new File(new File(homePath, "config"), "tags.txt");
            updateDictionary(numberSuffixFiles[i], "名詞-形容動詞語幹\n名詞-サ変接続");
        }

        runner.ensureYellow();
        Node node = runner.node();

        final String index = "dataset";

        final String indexSettings = "{\"index\":{\"analysis\":{"
                + "\"filter\":{"
                + "\"tag_concat_filter\":{\"type\":\"kuromoji_pos_concat\",\"tags_path\":\"tags.txt\"}"
                + "},"//
                + "\"analyzer\":{"
                + "\"ja_analyzer\":{\"type\":\"custom\",\"tokenizer\":\"reloadable_kuromoji_tokenizer\"},"
                + "\"ja_concat_analyzer\":{\"type\":\"custom\",\"tokenizer\":\"reloadable_kuromoji_tokenizer\",\"filter\":[\"tag_concat_filter\"]}"
                + "}"//
                + "}}}";
        runner.createIndex(index, Settings.builder().loadFromSource(indexSettings, XContentType.JSON).build());
        runner.ensureYellow();

        {
            String text = "詳細設計";
            try (CurlResponse response = OpenSearchCurl.post(node, "/" + index + "/_analyze").header("Content-Type", "application/json")
                    .body("{\"analyzer\":\"ja_concat_analyzer\",\"text\":\"" + text + "\"}").execute()) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> tokens = (List<Map<String, Object>>) response
                        .getContent(OpenSearchCurl.jsonParser()).get("tokens");
                assertEquals(1, tokens.size());
                assertEquals("詳細設計", tokens.get(0).get("token").toString());
            }
        }
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
}
