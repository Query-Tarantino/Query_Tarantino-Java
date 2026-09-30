# Query Tarantino — Stage 1: Data Layer (Java)

Java implementation of the data layer of a search engine over [Project Gutenberg](https://www.gutenberg.org/) books:
a **datalake** with the raw texts, **datamarts** with metadata and an inverted index, and a minimal **control layer**
that coordinates downloading and indexing.

The behavior shared with the Python and C# implementations (split rules, datalake layouts, tokenizer,
datamart formats, control algorithm and benchmark format) is defined in [SPEC.md](SPEC.md).

## Repository structure

```
services/
  crawler/    downloads books, splits header/body and stores them in the datalake
  indexer/    reads the datalake and builds the inverted index and the metadata datamart
  query/      searches the datamarts
  control/    orchestrates crawler -> indexer and tracks progress in control files
workload/     experiment definition shared by every language implementation
  book_ids.txt     book ids used by the benchmarks (the first N are taken)
  sample_ids.txt   small sample dataset to test the pipeline quickly
  stopwords.txt    stopwords removed by the tokenizer
  queries.txt      fixed query workload for the search benchmark
```

Each service follows the same layout:

```
src/main/java/org/ulpgc/tarantino/<service>/
  model/      records and pure domain logic
  ports/      interfaces the service depends on
  adapters/   implementations of the ports (filesystem, HTTP, SQLite, MongoDB)
  commands/   use cases
  Main, <Service>Config, <Service>Factory
src/test/java/org/ulpgc/tarantino/<service>/benchmarking/
```

The following directories are **created at runtime** in the project root and are not versioned:

| Directory     | Written by | Content                                                              |
|---------------|------------|----------------------------------------------------------------------|
| `datalake/`   | crawler    | `<id>.header.txt` / `<id>.body.txt` in the selected layout           |
| `datamarts/`  | indexer    | `inverted_index.json`, `inverted_index/`, `metadata.db`              |
| `control/`    | control    | `downloaded_books.txt`, `indexed_books.txt`                          |
| `benchmarks/` | benchmarks | `<service>/jmh-results.csv`, download cache and temporary data       |

## Requirements

- Java 25
- Maven 3.9+
- MongoDB (only for the `mongo` index or metadata backends), e.g.
  `docker run -d -p 27017:27017 --name tarantino-mongo mongo:7`

## Configuration

Every setting has a default that works when running from the project root. Override them with environment variables:

| Variable                    | Default                     | Values                   |
|-----------------------------|-----------------------------|--------------------------|
| `TARANTINO_DATALAKE`        | `datalake`                  | path                     |
| `TARANTINO_DATAMARTS`       | `datamarts`                 | path                     |
| `TARANTINO_CONTROL`         | `control`                   | path                     |
| `TARANTINO_BENCHMARKS`      | `benchmarks`                | path                     |
| `TARANTINO_WORKLOAD`        | `workload`                  | path                     |
| `TARANTINO_DATALAKE_LAYOUT` | `time`                      | `time`, `book`, `batch`  |
| `TARANTINO_INDEX`           | `json`                      | `json`, `mongo`, `folders` |
| `TARANTINO_METADATA`        | `sqlite`                    | `sqlite`, `mongo`        |
| `TARANTINO_MONGO_URI`       | `mongodb://localhost:27017` | connection string        |

The crawler and the indexer must use the same `TARANTINO_DATALAKE_LAYOUT`; the indexer and the query service must use
the same `TARANTINO_INDEX` and `TARANTINO_METADATA`.

## Running

Always run from the project root so the runtime directories are created there.

```bash
mvn -q install -DskipTests                                  # build all services once

mvn -q -pl services/control exec:java                       # full pipeline over workload/sample_ids.txt
mvn -q -pl services/control exec:java -Dexec.args="book_ids.txt"

mvn -q -pl services/crawler exec:java -Dexec.args="1342 84"  # ingest specific books
mvn -q -pl services/indexer exec:java -Dexec.args="1342 84"  # index specific books
mvn -q -pl services/query   exec:java -Dexec.args="adventure island"
```

## Benchmarks

```bash
mvn test -Pbenchmark                          # every service
mvn -pl services/crawler test -Pbenchmark     # a single service
```

Raw JMH results are written to `benchmarks/<service>/jmh-results.csv`, and the results shared with
the other languages to `benchmarks/results/java-<service>.csv` (format in [SPEC.md](SPEC.md#11-benchmarks)).

| Comparison                  | Structures                     | Metrics                                                                 | Benchmark                         |
|-----------------------------|--------------------------------|-------------------------------------------------------------------------|-----------------------------------|
| Datalake (PDF 3.1)          | `time`, `book`, `batch`        | write throughput, lookup, incremental detection, recovery, file overhead | crawler `DatalakeBenchmark`, `DatalakeRecoveryTest` |
| Inverted index (PDF 4.2)    | `json`, `mongo`, `folders`     | build time, update time, query time, memory, disk, scalability           | indexer `IndexingBenchmark`, query `QueryBenchmark` |
| Metadata (PDF 4.1)          | `sqlite`, `mongo`              | insertion, query by author, path by id, scalability                      | indexer `MetadataBenchmark`, query `QueryBenchmark` |

The rules that keep results comparable across languages (dataset, sizes, iterations, metrics and
units) are defined in [SPEC.md](SPEC.md#11-benchmarks).
