package org.codelibs.opensearch.extension.analysis;

import java.io.Reader;

import org.codelibs.analysis.ja.IterationMarkCharFilter;
import org.opensearch.common.settings.Settings;
import org.opensearch.env.Environment;
import org.opensearch.index.IndexSettings;
import org.opensearch.index.analysis.AbstractCharFilterFactory;

public class IterationMarkCharFilterFactory extends AbstractCharFilterFactory {

    public IterationMarkCharFilterFactory(final IndexSettings indexSettings, final Environment env, final String name,
            final Settings settings) {
        super(indexSettings, name);
    }

    @Override
    public Reader create(final Reader tokenStream) {
        return new IterationMarkCharFilter(tokenStream);
    }

}
