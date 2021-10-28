package org.codelibs.opensearch.extension.analysis;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.miscellaneous.DisableGraphAttribute;
import org.opensearch.common.settings.Settings;
import org.opensearch.env.Environment;
import org.opensearch.index.IndexSettings;
import org.opensearch.index.analysis.AbstractTokenFilterFactory;

public class DisableGraphFilterFactory extends AbstractTokenFilterFactory {

    public DisableGraphFilterFactory(final IndexSettings indexSettings, final Environment environment, final String name,
            final Settings settings) {
        super(indexSettings, name, settings);
    }

    @Override
    public TokenStream create(final TokenStream tokenStream) {
        tokenStream.addAttribute(DisableGraphAttribute.class);
        return tokenStream;
    }
}
