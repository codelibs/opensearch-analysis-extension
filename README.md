# OpenSearch Analysis Extension

[![Java CI with Maven](https://github.com/codelibs/opensearch-analysis-extension/actions/workflows/maven.yml/badge.svg)](https://github.com/codelibs/opensearch-analysis-extension/actions/workflows/maven.yml)
[![Maven Central](https://img.shields.io/maven-central/v/org.codelibs.opensearch/opensearch-analysis-extension)](https://repo1.maven.org/maven2/org/codelibs/opensearch/opensearch-analysis-extension/)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)

A comprehensive text analysis plugin for OpenSearch that provides advanced tokenizers, character filters, and token filters with specialized support for Japanese text processing using Kuromoji analyzer.

## Features

- **Japanese Text Analysis**: Comprehensive Kuromoji-based tokenization and morphological analysis
- **Character Normalization**: Iteration mark and prolonged sound mark character filters
- **Advanced Token Filtering**: Number processing, character type filtering, concatenation filters
- **Reloadable Components**: Dynamic dictionary reloading capabilities
- **Extensible Architecture**: Easy integration of custom analysis components

## Tech Stack

- **Java**: 21
- **OpenSearch**: 3.2.0
- **Lucene**: 10.2.2
- **Build System**: Maven 3.x
- **Testing**: JUnit 4.13.2

## Quick Start

### Prerequisites

- OpenSearch 3.2.0 or later
- Java 21 or later (for building from source)

### Installation

#### From Maven Repository
```bash
$OPENSEARCH_HOME/bin/opensearch-plugin install org.codelibs:opensearch-analysis-extension:3.2.0
```

#### From Local Build
```bash
# Clone and build
git clone https://github.com/codelibs/opensearch-analysis-extension.git
cd opensearch-analysis-extension
mvn package

# Install plugin
$OPENSEARCH_HOME/bin/opensearch-plugin install file:target/releases/opensearch-analysis-extension-3.2.0.zip
```

### Basic Usage Example

Create an index with Japanese text analysis:

```bash
curl -XPUT 'http://localhost:9200/sample/' -H 'Content-Type: application/json' -d'
{
    "settings": {
        "index": {
            "analysis": {
                "analyzer": {
                    "japanese_analyzer": {
                        "type": "custom",
                        "tokenizer": "japanese_tokenizer",
                        "char_filter": ["iteration_mark", "prolonged_sound_mark"],
                        "filter": ["japanese_baseform", "kanji_number", "char_type"]
                    }
                }
            }
        }
    }
}'
```

Test the analyzer:

```bash
curl -XGET 'http://localhost:9200/sample/_analyze' -H 'Content-Type: application/json' -d'
{
    "analyzer": "japanese_analyzer",
    "text": "東京都港区にある会社"
}'
```

## Development

### Building from Source

```bash
# Clone the repository
git clone https://github.com/codelibs/opensearch-analysis-extension.git
cd opensearch-analysis-extension

# Build the plugin
mvn clean package

# Run tests
mvn test

# Skip tests during build
mvn package -DskipTests=true
```

### Project Structure

```
src/
├── main/
│   ├── java/org/codelibs/opensearch/extension/
│   │   ├── ExtensionPlugin.java              # Main plugin class
│   │   ├── analysis/                         # General analysis components
│   │   │   ├── CharTypeFilterFactory.java
│   │   │   ├── KanjiNumberFilterFactory.java
│   │   │   └── ...
│   │   └── kuromoji/                        # Japanese-specific components
│   │       └── index/analysis/
│   │           ├── KuromojiTokenizerFactory.java
│   │           └── ...
│   ├── assemblies/plugin.xml                 # Plugin assembly configuration
│   └── plugin-metadata/
│       ├── plugin-descriptor.properties      # Plugin metadata
│       └── plugin-security.policy           # Security policy
└── test/                                     # Unit and integration tests
```

### Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Make your changes following the existing code style
4. Add tests for new functionality
5. Run the test suite (`mvn test`)
6. Commit your changes (`git commit -m 'Add amazing feature'`)
7. Push to the branch (`git push origin feature/amazing-feature`)
8. Open a Pull Request

## Analysis Components

### Character Filters

#### IterationMarkCharFilter (`iteration_mark`)
Normalizes iteration mark characters. For example, converts "学問のすゝめ" to "学問のすすめ".

```json
{
  "char_filter": ["iteration_mark"]
}
```

#### ProlongedSoundMarkCharFilter (`prolonged_sound_mark`)
Replaces various prolonged sound mark characters with `\u30fc` (KATAKANA-HIRAGANA SOUND MARK).

**Supported Unicode Characters:**
| Unicode | Name |
|:-----:|:-----|
| U002D | HYPHEN-MINUS |
| UFF0D | FULLWIDTH HYPHEN-MINUS |
| U2010 | HYPHEN |
| U2011 | NON-BREAKING HYPHEN |
| U2012 | FIGURE DASH |
| U2013 | EN DASH |
| U2014 | EM DASH |
| U2015 | HORIZONTAL BAR |
| U207B | SUPERSCRIPT MINUS |
| U208B | SUBSCRIPT MINUS |
| U30FC | KATAKANA-HIRAGANA SOUND MARK |

```json
{
  "char_filter": ["prolonged_sound_mark"]
}
```

#### JapaneseIterationMarkCharFilter (`japanese_iteration_mark`)
Kuromoji-specific iteration mark character filter for Japanese text processing.

### Token Filters

#### KanjiNumberFilter (`kanji_number`)
Converts Kanji number characters (e.g., "一") to Arabic numerals (e.g., "1").

```json
{
  "filter": ["kanji_number"]
}
```

#### CharTypeFilter (`char_type`)
Keeps tokens based on character type: alphabetic, digit, or letter.

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

**Character Type Behavior:**
| Token | Default | digit:false | letter:false |
|:------|:-------:|:-----------:|:------------:|
| abc | keep | keep | keep |
| ab1 | keep | keep | keep |
| abあ | keep | keep | keep |
| 123 | keep | remove | keep |
| 12あ | keep | keep | keep |
| あいう | keep | keep | remove |
| #-= | remove | remove | remove |

#### NumberConcatenationFilter (`number_concat`)
Concatenates tokens with following numbers. Example: "10" + "years" → "10years".

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

#### PatternConcatenationFilter (`pattern_concat`)
Concatenates tokens matching specified patterns.

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

#### Additional Token Filters

- **japanese_baseform**: Converts to base forms
- **japanese_part_of_speech**: Part-of-speech filtering
- **japanese_readingform**: Reading form conversion
- **japanese_stemmer**: Japanese stemming
- **japanese_stop**: Japanese stop word removal
- **japanese_number**: Japanese number processing
- **japanese_completion**: Completion suggestions
- **stop_prefix/stop_suffix**: Prefix/suffix stop word filters
- **reloadable_keyword_marker**: Dynamic keyword marking
- **reloadable_stop**: Dynamic stop word filtering
- **flexible_porter_stem**: Flexible Porter stemming
- **alphanum_word**: Alphanumeric word processing

### Tokenizers

#### JapaneseTokenizer (`japanese_tokenizer`)
Advanced Japanese tokenization using Kuromoji morphological analyzer.

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

#### ReloadableKuromojiTokenizer (`reloadable_kuromoji`)
Dynamically reloads user dictionary files when updated.

```json
{
  "tokenizer": {
    "reloadable_tokenizer": {
      "type": "reloadable_kuromoji",
      "mode": "extended",
      "discard_punctuation": false,
      "user_dictionary": "userdict_ja.txt"
    }
  }
}
```

**Note**: Dictionary updates may affect search results due to term changes.

#### NGramSynonymTokenizer (`ngram_synonym`)
N-gram tokenization with synonym support.

## Configuration Examples

### Complete Japanese Analysis Setup

```json
{
  "settings": {
    "index": {
      "analysis": {
        "char_filter": {
          "japanese_normalize": {
            "type": "mapping",
            "mappings": ["iteration_mark", "prolonged_sound_mark"]
          }
        },
        "tokenizer": {
          "japanese_custom": {
            "type": "japanese_tokenizer",
            "mode": "extended",
            "user_dictionary": "custom_dict.txt"
          }
        },
        "filter": {
          "japanese_filters": {
            "type": "japanese_baseform"
          },
          "number_normalize": {
            "type": "kanji_number"
          },
          "char_cleanup": {
            "type": "char_type",
            "digit": true,
            "alphabetic": true,
            "letter": true
          }
        },
        "analyzer": {
          "japanese_full": {
            "type": "custom",
            "char_filter": ["iteration_mark", "prolonged_sound_mark"],
            "tokenizer": "japanese_custom",
            "filter": [
              "japanese_baseform",
              "japanese_part_of_speech",
              "kanji_number",
              "char_type",
              "lowercase"
            ]
          }
        }
      }
    }
  }
}
```

### Multi-Language Analysis

```json
{
  "settings": {
    "analysis": {
      "analyzer": {
        "multilang": {
          "type": "custom",
          "tokenizer": "standard",
          "char_filter": ["prolonged_sound_mark"],
          "filter": [
            "lowercase",
            "char_type",
            "flexible_porter_stem"
          ]
        }
      }
    }
  }
}
```

## Testing

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=ExtensionPluginTest

# Run tests with verbose output
mvn test -X

# Generate test coverage report
mvn jacoco:report
```

## Version Compatibility

| Plugin Version | OpenSearch Version | Lucene Version | Java Version |
|:---------------|:-------------------|:---------------|:-------------|
| 3.2.x | 3.2.0+ | 10.2.2+ | 21+ |
| 3.1.x | 3.1.0+ | 10.1.x+ | 21+ |

## Performance Considerations

- **Dictionary Size**: Larger user dictionaries impact tokenization performance
- **Filter Chain**: Minimize filter chain length for better performance  
- **Reloadable Components**: Use sparingly in high-throughput environments
- **Memory Usage**: Monitor heap usage with large dictionary files

## Troubleshooting

### Common Issues

**Plugin Installation Fails**
```bash
# Check OpenSearch version compatibility
$OPENSEARCH_HOME/bin/opensearch --version

# Verify plugin version matches OpenSearch version
$OPENSEARCH_HOME/bin/opensearch-plugin list
```

**Dictionary Not Loading**
```bash
# Check dictionary file permissions and encoding (UTF-8)
# Verify path in OpenSearch configuration directory
# Check OpenSearch logs for error messages
```

**Analysis Not Working**
```bash
# Test analyzer configuration
curl -XGET 'localhost:9200/_analyze' -d '{"analyzer":"your_analyzer","text":"test text"}'

# Check plugin registration
curl -XGET 'localhost:9200/_nodes/plugins'
```

### Debug Mode

Enable debug logging in `opensearch.yml`:

```yaml
logger.org.codelibs.opensearch.extension: DEBUG
```

## License

Licensed under the Apache License, Version 2.0. See [LICENSE](LICENSE) for details.

