package org.codelibs.opensearch.extension.analysis;

import java.nio.file.Path;

import org.apache.lucene.analysis.TokenStream;
import org.codelibs.analysis.en.ReloadableStopFilter;
import org.opensearch.common.settings.Settings;
import org.opensearch.common.unit.TimeValue;
import org.opensearch.env.Environment;
import org.opensearch.index.IndexSettings;
import org.opensearch.index.analysis.AbstractTokenFilterFactory;

public class ReloadableStopFilterFactory extends AbstractTokenFilterFactory {

    private final Path stopwordPath;

    private final long reloadInterval;

    private final boolean ignoreCase;

    public ReloadableStopFilterFactory(final IndexSettings indexSettings, final Environment environment, final String name,
            final Settings settings) {
        super(indexSettings, name, settings);

        final String path = settings.get("stopwords_path");
        if (path != null) {
            stopwordPath = environment.configDir().resolve(path);
        } else {
            stopwordPath = null;
        }

        ignoreCase = settings.getAsBoolean("ignore_case", false);
        reloadInterval = settings.getAsTime("reload_interval", TimeValue.timeValueMinutes(1)).getMillis();
    }

    @Override
    public TokenStream create(final TokenStream tokenStream) {
        if (stopwordPath == null) {
            return tokenStream;
        }
        return new ReloadableStopFilter(tokenStream, stopwordPath, ignoreCase, reloadInterval);
    }

}