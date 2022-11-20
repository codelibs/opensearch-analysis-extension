package org.codelibs.opensearch.extension.analysis;

import java.io.Reader;

import org.codelibs.analysis.ja.ProlongedSoundMarkCharFilter;
import org.opensearch.common.settings.Settings;
import org.opensearch.env.Environment;
import org.opensearch.index.IndexSettings;
import org.opensearch.index.analysis.AbstractCharFilterFactory;

public class ProlongedSoundMarkCharFilterFactory extends AbstractCharFilterFactory {
    private char replacement;

    public ProlongedSoundMarkCharFilterFactory(final IndexSettings indexSettings, final Environment env, final String name,
            final Settings settings) {
        super(indexSettings, name);
        final String value = settings.get("replacement");
        if (value == null || value.length() == 0) {
            replacement = '\u30fc';
        } else {
            replacement = value.charAt(0);
        }
    }

    @Override
    public Reader create(final Reader tokenStream) {
        return new ProlongedSoundMarkCharFilter(tokenStream, replacement);
    }

}
