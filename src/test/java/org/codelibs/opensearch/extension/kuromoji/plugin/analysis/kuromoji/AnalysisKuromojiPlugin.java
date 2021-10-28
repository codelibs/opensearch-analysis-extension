/*
 * Licensed to OpenSearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. OpenSearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.codelibs.opensearch.extension.kuromoji.plugin.analysis.kuromoji;

import static java.util.Collections.singletonMap;

import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.analysis.Analyzer;
import org.codelibs.opensearch.extension.kuromoji.index.analysis.JapaneseStopTokenFilterFactory;
import org.codelibs.opensearch.extension.kuromoji.index.analysis.KuromojiAnalyzerProvider;
import org.codelibs.opensearch.extension.kuromoji.index.analysis.KuromojiBaseFormFilterFactory;
import org.codelibs.opensearch.extension.kuromoji.index.analysis.KuromojiIterationMarkCharFilterFactory;
import org.codelibs.opensearch.extension.kuromoji.index.analysis.KuromojiKatakanaStemmerFactory;
import org.codelibs.opensearch.extension.kuromoji.index.analysis.KuromojiNumberFilterFactory;
import org.codelibs.opensearch.extension.kuromoji.index.analysis.KuromojiPartOfSpeechFilterFactory;
import org.codelibs.opensearch.extension.kuromoji.index.analysis.KuromojiReadingFormFilterFactory;
import org.codelibs.opensearch.extension.kuromoji.index.analysis.KuromojiTokenizerFactory;
import org.opensearch.index.analysis.AnalyzerProvider;
import org.opensearch.index.analysis.CharFilterFactory;
import org.opensearch.index.analysis.TokenFilterFactory;
import org.opensearch.index.analysis.TokenizerFactory;
import org.opensearch.indices.analysis.AnalysisModule.AnalysisProvider;
import org.opensearch.plugins.AnalysisPlugin;
import org.opensearch.plugins.Plugin;

public class AnalysisKuromojiPlugin extends Plugin implements AnalysisPlugin {
    @Override
    public Map<String, AnalysisProvider<CharFilterFactory>> getCharFilters() {
        return singletonMap("kuromoji_iteration_mark", KuromojiIterationMarkCharFilterFactory::new);
    }

    @Override
    public Map<String, AnalysisProvider<TokenFilterFactory>> getTokenFilters() {
        final Map<String, AnalysisProvider<TokenFilterFactory>> extra = new HashMap<>();
        extra.put("kuromoji_baseform", KuromojiBaseFormFilterFactory::new);
        extra.put("kuromoji_part_of_speech", KuromojiPartOfSpeechFilterFactory::new);
        extra.put("kuromoji_readingform", KuromojiReadingFormFilterFactory::new);
        extra.put("kuromoji_stemmer", KuromojiKatakanaStemmerFactory::new);
        extra.put("ja_stop", JapaneseStopTokenFilterFactory::new);
        extra.put("kuromoji_number", KuromojiNumberFilterFactory::new);
        return extra;
    }

    @Override
    public Map<String, AnalysisProvider<TokenizerFactory>> getTokenizers() {
        return singletonMap("kuromoji_tokenizer", KuromojiTokenizerFactory::new);
    }

    @Override
    public Map<String, AnalysisProvider<AnalyzerProvider<? extends Analyzer>>> getAnalyzers() {
        return singletonMap("kuromoji", KuromojiAnalyzerProvider::new);
    }
}
