# OpenSearch Analysis Extension

[![Java CI with Maven](https://github.com/codelibs/opensearch-analysis-extension/actions/workflows/maven.yml/badge.svg)](https://github.com/codelibs/opensearch-analysis-extension/actions/workflows/maven.yml)
[![Maven Central](https://img.shields.io/maven-central/v/org.codelibs.opensearch/opensearch-analysis-extension)](https://central.sonatype.com/artifact/org.codelibs.opensearch/opensearch-analysis-extension)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue)](LICENSE)

OpenSearch Analysis Extension adds character filters, tokenizers and token filters
to OpenSearch, with a focus on Japanese text processing built on the Kuromoji
morphological analyzer. It also provides a few generally useful components such as
character-type filtering, token concatenation and dictionary files that can be
reloaded without restarting the cluster.

## Compatibility

| Plugin Version | OpenSearch Version | Lucene Version | Java Version |
|----------------|--------------------|----------------|--------------|
| 3.8.x          | 3.8.0+             | 10.5.0+        | 21+          |
| 3.7.x          | 3.7.0+             | 10.4.0+        | 21+          |
| 3.6.x          | 3.6.0+             | 10.4.0+        | 21+          |
| 3.2.x          | 3.2.0+             | 10.2.2+        | 21+          |
| 3.1.x          | 3.1.0+             | 10.1.x+        | 21+          |

Released versions are listed on
[Maven Central](https://central.sonatype.com/artifact/org.codelibs.opensearch/opensearch-analysis-extension/versions).

## Installation

```bash
$OPENSEARCH_HOME/bin/opensearch-plugin install org.codelibs.opensearch:opensearch-analysis-extension:3.8.0
```

Restart the node, then confirm that the plugin is loaded:

```bash
$OPENSEARCH_HOME/bin/opensearch-plugin list
# analysis-extension
```

To install a locally built package instead:

```bash
mvn clean package
$OPENSEARCH_HOME/bin/opensearch-plugin install file:target/releases/opensearch-analysis-extension-3.8.0-SNAPSHOT.zip
```

Use `opensearch-plugin remove analysis-extension` to uninstall.

## Getting Started

Create an index that uses the Japanese components:

```bash
curl -XPUT 'localhost:9200/sample' -H 'Content-Type: application/json' -d '{
  "settings": {
    "index": {
      "analysis": {
        "analyzer": {
          "japanese_analyzer": {
            "type": "custom",
            "char_filter": ["iteration_mark", "prolonged_sound_mark"],
            "tokenizer": "japanese_tokenizer",
            "filter": ["japanese_baseform", "kanji_number", "char_type"]
          }
        }
      }
    }
  }
}'
```

Check the result with the analyze API:

```bash
curl -XGET 'localhost:9200/sample/_analyze' -H 'Content-Type: application/json' -d '{
  "analyzer": "japanese_analyzer",
  "text": "東京都港区にある会社"
}'
```

## Character Filters

### `iteration_mark`

Normalizes Japanese iteration marks. For example, `学問のすゝめ` becomes
`学問のすすめ`.

```json
{ "char_filter": ["iteration_mark"] }
```

### `prolonged_sound_mark`

Replaces dash-like characters with `ー` (KATAKANA-HIRAGANA PROLONGED SOUND
MARK), so that variants of the same katakana word normalize to a single form.

| Code point | Name |
|:----------:|:-----|
| U+002D | HYPHEN-MINUS |
| U+FF0D | FULLWIDTH HYPHEN-MINUS |
| U+2010 | HYPHEN |
| U+2011 | NON-BREAKING HYPHEN |
| U+2012 | FIGURE DASH |
| U+2013 | EN DASH |
| U+2014 | EM DASH |
| U+2015 | HORIZONTAL BAR |
| U+207B | SUPERSCRIPT MINUS |
| U+208B | SUBSCRIPT MINUS |
| U+30FC | KATAKANA-HIRAGANA PROLONGED SOUND MARK |

```json
{ "char_filter": ["prolonged_sound_mark"] }
```

### `japanese_iteration_mark`

The Kuromoji iteration mark character filter, equivalent to the one shipped with
`analysis-kuromoji`.

## Tokenizers

### `japanese_tokenizer`

Kuromoji-based Japanese tokenizer.

| Parameter | Default | Description |
|-----------|---------|-------------|
| `mode` | `search` | Segmentation mode: `normal`, `search` or `extended`. |
| `discard_punctuation` | `true` | Drop punctuation tokens. |
| `discard_compound_token` | `false` | Drop the original compound token in `search` mode. |
| `user_dictionary` | none | Path to a user dictionary file, relative to the OpenSearch config directory. |
| `user_dictionary_rules` | none | Inline user dictionary rules. Cannot be combined with `user_dictionary`. |
| `nbest_cost` | `-1` | Additional cost allowed when generating n-best paths. |
| `nbest_examples` | none | Examples used to derive `nbest_cost`. |

```json
{
  "tokenizer": {
    "my_tokenizer": {
      "type": "japanese_tokenizer",
      "mode": "extended",
      "discard_punctuation": false,
      "user_dictionary": "userdict_ja.txt"
    }
  }
}
```

### `ngram_synonym`

N-gram tokenizer that expands synonyms while tokenizing, which avoids the
positional mismatches that occur when a synonym filter is applied after n-gram
tokenization.

| Parameter | Default | Description |
|-----------|---------|-------------|
| `n` | `2` | N-gram size. |
| `delimiters` | whitespace | Characters treated as token boundaries. |
| `synonyms` | none | Inline synonym rules. |
| `synonyms_path` | none | Path to a synonym file, relative to the OpenSearch config directory. |
| `expand` | `true` | Expand synonym rules in both directions. |
| `expand_ngram` | `false` | Also emit n-grams of the expanded synonyms. |
| `ignore_case` | `true` | Match synonyms case-insensitively. |

## Token Filters

### `kanji_number`

Converts Kanji numerals to Arabic numerals, for example `一` to `1`.

```json
{ "filter": ["kanji_number"] }
```

### `char_type`

Keeps or removes tokens according to the character types they contain. A token is
kept if every one of its enabled character classes is allowed.

| Parameter | Default | Description |
|-----------|---------|-------------|
| `alphabetic` | `true` | Keep tokens containing ASCII letters. |
| `digit` | `true` | Keep tokens containing digits. |
| `letter` | `true` | Keep tokens containing non-ASCII letters. |

```json
{
  "filter": {
    "my_char_type": {
      "type": "char_type",
      "digit": false,
      "alphabetic": true,
      "letter": true
    }
  }
}
```

| Token | Default | `digit: false` | `letter: false` |
|:------|:-------:|:--------------:|:---------------:|
| abc | keep | keep | keep |
| ab1 | keep | keep | keep |
| abあ | keep | keep | keep |
| 123 | keep | remove | keep |
| 12あ | keep | keep | keep |
| あいう | keep | keep | remove |
| #-= | remove | remove | remove |

### `number_concat`

Concatenates a number with the token that follows it when that token is listed in
the suffix dictionary, so that `10` and `years` become `10years`.

| Parameter | Default | Description |
|-----------|---------|-------------|
| `suffix_words_path` | none | Path to the suffix word list, relative to the OpenSearch config directory. |

```json
{
  "filter": {
    "numconcat_filter": {
      "type": "number_concat",
      "suffix_words_path": "suffix.txt"
    }
  }
}
```

### `pattern_concat`

Concatenates two adjacent tokens when they match the configured regular
expressions.

| Parameter | Default | Description |
|-----------|---------|-------------|
| `pattern1` | none | Pattern the first token must match. |
| `pattern2` | `.*` | Pattern the second token must match. |

```json
{
  "filter": {
    "pattern_filter": {
      "type": "pattern_concat",
      "pattern1": "[0-9]+",
      "pattern2": "year(s)?"
    }
  }
}
```

### `kuromoji_pos_concat`

Concatenates adjacent tokens that share one of the configured part-of-speech tags.
Requires a Kuromoji tokenizer upstream.

| Parameter | Default | Description |
|-----------|---------|-------------|
| `tags` | none | Part-of-speech tags to concatenate. |
| `tags_path` | none | Path to a file listing the tags, relative to the OpenSearch config directory. |

### `stop_prefix` and `stop_suffix`

Remove tokens that start (`stop_prefix`) or end (`stop_suffix`) with one of the
configured words.

| Parameter | Default | Description |
|-----------|---------|-------------|
| `stopwords` | none | Inline word list. |
| `stopwords_path` | none | Path to a word list, relative to the OpenSearch config directory. |
| `ignore_case` | `false` | Match case-insensitively. |

### `alphanum_word`

Splits alphanumeric tokens that exceed a maximum length.

| Parameter | Default | Description |
|-----------|---------|-------------|
| `max_token_length` | `255` | Maximum length before a token is split. |

### `flexible_porter_stem`

Porter stemmer whose individual steps can be enabled or disabled, which is useful
when the default stemmer is too aggressive for a particular corpus.

| Parameter | Default | Description |
|-----------|---------|-------------|
| `step1` … `step6` | `true` | Enable the corresponding step of the Porter algorithm. |

### Reloadable filters

These filters re-read their dictionary file at a fixed interval, so the word list
can be updated without restarting the cluster or reopening the index.

#### `reloadable_stop`

| Parameter | Default | Description |
|-----------|---------|-------------|
| `stopwords_path` | none | Path to the stop word file, relative to the OpenSearch config directory. |
| `reload_interval` | `1m` | How often the file is checked for changes. |
| `ignore_case` | `false` | Match case-insensitively. |

#### `reloadable_keyword_marker`

| Parameter | Default | Description |
|-----------|---------|-------------|
| `keywords_path` | none | Path to the keyword file, relative to the OpenSearch config directory. |
| `reload_interval` | `1m` | How often the file is checked for changes. |

Updating a dictionary changes the terms produced at index time. Documents indexed
before the change keep their old terms until they are reindexed.

### Kuromoji filters

The following filters wrap the corresponding Lucene Kuromoji components and behave
like their `analysis-kuromoji` counterparts:

| Name | Description |
|------|-------------|
| `japanese_baseform` | Replaces inflected forms with their base form. |
| `japanese_part_of_speech` | Removes tokens with the configured part-of-speech tags. |
| `japanese_readingform` | Replaces tokens with their reading, in katakana or romaji. |
| `japanese_stemmer` | Removes the trailing prolonged sound mark from katakana. |
| `japanese_stop` | Removes Japanese stop words. |
| `japanese_number` | Normalizes Japanese numerals to Arabic numerals. |
| `japanese_completion` | Emits romaji completion variants for suggestions. |

## Deprecated Names

The following names remain registered for backward compatibility and are scheduled
for removal. Each is an alias of the component listed next to it and adds no
behaviour of its own.

| Deprecated name | Use instead |
|-----------------|-------------|
| `reloadable_kuromoji` | `japanese_tokenizer` |
| `reloadable_kuromoji_tokenizer` | `japanese_tokenizer` |
| `reloadable_kuromoji_iteration_mark` | `japanese_iteration_mark` |
| `reloadable_kuromoji_baseform` | `japanese_baseform` |
| `reloadable_kuromoji_part_of_speech` | `japanese_part_of_speech` |
| `reloadable_kuromoji_readingform` | `japanese_readingform` |
| `reloadable_kuromoji_stemmer` | `japanese_stemmer` |
| `reloadable_kuromoji_number` | `japanese_number` |
| `reloadable_ja_stop` | `japanese_stop` |

## Troubleshooting

Enable debug logging in `opensearch.yml` to trace analyzer construction and
dictionary loading:

```yaml
logger.org.codelibs.opensearch.extension: DEBUG
```

Dictionary files are resolved relative to the OpenSearch config directory and must
be UTF-8 encoded and readable by the OpenSearch process.

## Building from Source

Java 21 and Maven 3.6 or later are required.

```bash
git clone https://github.com/codelibs/opensearch-analysis-extension.git
cd opensearch-analysis-extension
mvn clean package
```

The plugin package is written to `target/releases/`.

```bash
mvn test                              # run the test suite
mvn test -Dtest=ExtensionPluginTest   # run a single test class
mvn package -DskipTests=true          # build without running tests
```

## Contributing

Issues and pull requests are welcome at
[github.com/codelibs/opensearch-analysis-extension](https://github.com/codelibs/opensearch-analysis-extension).
Please add tests for behaviour changes and make sure `mvn test` passes before
opening a pull request.

## License

Licensed under the Apache License, Version 2.0. See [LICENSE](LICENSE) for details.
