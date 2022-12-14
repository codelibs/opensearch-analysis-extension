package org.codelibs.opensearch.extension.analysis;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.ja.tokenattributes.PartOfSpeechAttribute;
import org.codelibs.analysis.ja.PosConcatenationFilter;
import org.opensearch.common.settings.Settings;
import org.opensearch.env.Environment;
import org.opensearch.index.IndexSettings;
import org.opensearch.index.analysis.AbstractTokenFilterFactory;
import org.opensearch.index.analysis.Analysis;

public class PosConcatenationFilterFactory extends AbstractTokenFilterFactory {

    private final Set<String> posTags = new HashSet<>();

    public PosConcatenationFilterFactory(final IndexSettings indexSettings, final Environment environment, final String name, final Settings settings) {
        super(indexSettings, name, settings);

        final List<String> tagList = Analysis.parseWordList(environment, settings, "tags", s -> s);
        if (tagList != null) {
            posTags.addAll(tagList);
        }
    }

    @Override
    public TokenStream create(final TokenStream tokenStream) {
        final PartOfSpeechAttribute posAtt = tokenStream.addAttribute(PartOfSpeechAttribute.class);
        return new PosConcatenationFilter(tokenStream, posTags, () -> posAtt.getPartOfSpeech());
    }
}
