package org.codelibs.opensearch.extension.analysis;

import java.util.regex.Pattern;

import org.apache.lucene.analysis.TokenStream;
import org.codelibs.analysis.ja.PatternConcatenationFilter;
import org.opensearch.common.settings.Settings;
import org.opensearch.env.Environment;
import org.opensearch.index.IndexSettings;
import org.opensearch.index.analysis.AbstractTokenFilterFactory;

public class PatternConcatenationFilterFactory extends AbstractTokenFilterFactory {

    private Pattern pattern1;

    private Pattern pattern2;

    public PatternConcatenationFilterFactory(final IndexSettings indexSettings, final Environment environment, final String name,
            final Settings settings) {
        super(indexSettings, name, settings);

        final String pattern1Str = settings.get("pattern1");
        final String pattern2Str = settings.get("pattern2", ".*");

        if (logger.isDebugEnabled()) {
            logger.debug("pattern1: {}, pattern2: {}", pattern1Str, pattern2Str);
        }
        if (pattern1Str != null) {
            pattern1 = Pattern.compile(pattern1Str);
            pattern2 = Pattern.compile(pattern2Str);
        }
    }

    @Override
    public TokenStream create(final TokenStream tokenStream) {
        return new PatternConcatenationFilter(tokenStream, pattern1, pattern2);
    }
}
