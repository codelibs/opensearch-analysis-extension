package org.codelibs.opensearch.extension.analysis;

import org.apache.lucene.analysis.Tokenizer;
import org.opensearch.common.settings.Settings;
import org.opensearch.env.Environment;
import org.opensearch.index.IndexSettings;
import org.opensearch.index.analysis.AbstractTokenizerFactory;

/**
 * Factory for {@link NGramSynonymTokenizer}.
 */
public final class NGramSynonymTokenizerFactory extends AbstractTokenizerFactory {

    private final boolean ignoreCase;

    private final int n;

    private final String delimiters;

    private final boolean expand;

    private SynonymLoader synonymLoader = null;

    public NGramSynonymTokenizerFactory(final IndexSettings indexSettings, final Environment env, final String name,
            final Settings settings) {
        super(indexSettings, settings, name);
        ignoreCase = settings.getAsBoolean("ignore_case", true);
        n = settings.getAsInt("n", NGramSynonymTokenizer.DEFAULT_N_SIZE);
        delimiters = settings.get("delimiters", NGramSynonymTokenizer.DEFAULT_DELIMITERS);
        expand = settings.getAsBoolean("expand", true);

        settings.getAsBoolean("expand_ngram", false); // TODO remove

        synonymLoader = new SynonymLoader(env, settings, expand, SynonymLoader.getAnalyzer(ignoreCase));
        if (synonymLoader.getSynonymMap() == null) {
            if (settings.getAsList("synonyms", null) != null) {
                logger.warn("synonyms values are empty.");
            } else if (settings.get("synonyms_path") != null) {
                logger.warn("synonyms_path[{}] is empty.", settings.get("synonyms_path"));
            } else {
                logger.debug("No synonym data.");
            }
        }
    }

    @Override
    public Tokenizer create() {
        return new NGramSynonymTokenizer(n, delimiters, expand, ignoreCase, synonymLoader);
    }
}
