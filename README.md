# Query Tarantino — Stage 1: Data Layer (Java)

Java implementation of the data layer of a search engine over [Project Gutenberg](https://www.gutenberg.org/) books:
a **datalake** with the raw texts, **datamarts** with metadata and an inverted index, and a minimal **control layer**
that coordinates downloading and indexing.

The behavior shared with the Python and C# implementations (split rules, datalake layouts, tokenizer,
datamart formats, control algorithm and benchmark format) is defined in [SPEC.md](SPEC.md).

## Data structure comparisons

The project implements several interchangeable structures for each part of the data layer and benchmarks
them against each other with the same books, the same rules and in three languages (Java, Python, C#):

**1. Datalake: how downloaded books are organized** (PDF §3.1)

| Structure | Layout                                            | Trade-off                                                   |
|-----------|---------------------------------------------------|-------------------------------------------------------------|
| `time`    | `datalake/YYYYMMDD/HH/<id>.header.txt`, `.body.txt` | New books are found by date directory; lookup by id scans   |
| `book`    | `datalake/<id>/header.txt`, `body.txt`            | Direct lookup by id; one directory per book                 |
| `batch`   | `datalake/<id div 1000>/<id>.header.txt`, `.body.txt` | Direct lookup by id; few directories with many files        |

Compared on write throughput, lookup time, new books detection, recovery after an interruption, and number
of files, directories and bytes.

**2. Inverted index: how terms map to books** (PDF §4.2)

| Structure | Storage                                           | Trade-off                                                   |
|-----------|---------------------------------------------------|-------------------------------------------------------------|
| `json`    | one `datamarts/inverted_index.json`               | Simple and fast to query; rewritten whole on every update   |
| `folders` | one `datamarts/inverted_index/<c>/<term>.txt` per term | Fine-grained updates; very many small files             |
| `mongo`   | MongoDB collection, one document per term         | Indexed random access and concurrency; needs a server       |

Compared on full build time, incremental update time, query time, allocated memory and disk usage.

**3. Metadata: where title, author, language and path are stored** (PDF §4.1)

| Structure | Storage                                           | Trade-off                                                   |
|-----------|---------------------------------------------------|-------------------------------------------------------------|
| `sqlite`  | `datamarts/metadata.db`, table `books`            | Embedded, no server, SQL queries                            |
| `mongo`   | MongoDB collection `books`                        | Server-based NoSQL, flexible schema                         |

Compared on bulk insertion time, lookup by id and search by author.

Every comparison runs at 100, 500, 1 000 and 2 000 books to show how each structure scales. See
[Benchmarks](#benchmarks) to run them and generate the comparison report.

## Repository structure

```
services/
  crawler/    downloads books, splits header/body and stores them in the datalake
  indexer/    reads the datalake and builds the inverted index and the metadata datamart
  query/      searches the datamarts
  control/    orchestrates crawler -> indexer and tracks progress in control files
scripts/
  fill_cache.sh        downloads the benchmark dataset from the official Gutenberg mirror
  compare_results.py   builds the data structure comparison report from the benchmark results
  comparison/          its code: model/, ports/, adapters/ (CSV, charts), report/ (Markdown), commands/
  tests/               unit tests of comparison/, in the same package layout
workload/     experiment definition shared by every language implementation
  book_ids.txt     candidate book ids for the benchmark dataset, in order
  sample_ids.txt   small sample dataset to test the pipeline quickly
  stopwords.txt    stopwords removed by the tokenizer
  queries.txt      fixed query workload for the search benchmark
```

Each service follows the same layout:

```
src/main/java/org/ulpgc/tarantino/<service>/
  model/      records and pure domain logic, grouped by concept (model/book, model/terms)
  ports/      interfaces the service depends on
  adapters/   implementations of the ports, one subpackage per functionality:
                datalake/   time/, book/, batch/   one subpackage per compared layout
                index/      json/, folders/, mongo/ one subpackage per compared structure
                metadata/   SQLite and MongoDB backends
                mongo/, stopwords/, gutenberg/
  commands/   use cases
  Main, <Service>Config, <Service>Factory
src/test/java/org/ulpgc/tarantino/<service>/
  ...         unit tests, in the package of the class they test
  benchmarking/   JMH benchmarks by comparison, and support/ code shared through the test-jars
```

No package holds more than three classes. Helpers used by a single structure stay package-private
inside its package (e.g. `index/folders/TermFiles`); only helpers shared by several structures are public
(e.g. `datalake/BookFiles`, `index/PendingPostings`).

The following directories are **created at runtime** in the project root and are not versioned:

| Directory     | Written by | Content                                                              |
|---------------|------------|----------------------------------------------------------------------|
| `datalake/`   | crawler    | `<id>.header.txt` / `<id>.body.txt` in the selected layout           |
| `datamarts/`  | indexer    | `inverted_index.json`, `inverted_index/`, `metadata.db`              |
| `control/`    | control    | `downloaded_books.txt`, `indexed_books.txt`                          |
| `benchmarks/` | benchmarks | `cache/` dataset, `results/` CSVs, `report/` comparison, raw JMH output |

## Requirements

- Java 25
- Maven 3.9+
- MongoDB 7.0, only for the `mongo` index or metadata backends: installed natively for the benchmarks,
  or in Docker for development
- Docker, only for the tests of the MongoDB adapters
- Python 3.10+ for the comparison report (matplotlib is optional and only adds charts)

MongoDB 7.0 is the version shared by every implementation. **The benchmarks require it installed natively**
(SPEC §11); on macOS:

```bash
brew tap mongodb/brew && brew install mongodb-community@7.0
```

Fix its cache at 1 GB, as the SPEC requires, by adding this to `/opt/homebrew/etc/mongod.conf` (by default
MongoDB takes up to half of the RAM minus 1 GB, about 11.5 GB on a 24 GB machine):

```yaml
storage:
  wiredTiger:
    engineConfig:
      cacheSizeGB: 1
```

```bash
brew services start mongodb-community@7.0   # start it on localhost:27017
brew services stop mongodb-community@7.0    # stop it
```

On Linux and Windows, install MongoDB 7.0 Community from https://www.mongodb.com/try/download/community
and set the same `cacheSizeGB` in its `mongod.conf`.

For development only, `docker compose up -d` starts the same version in a container (`docker compose down`
stops it, `-v` also deletes the data). It is not valid for benchmarking: on macOS and Windows Docker runs a
virtual machine, which uses more memory (the VM reserves its own, 2 GB by default with Colima, on top of
MongoDB) and adds a network round trip to every operation, so `mongo` would come out slower than it is.

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

Every service is a plain Java program run through Maven from a terminal; no IDE is needed.
Always run from the **project root**, so `datalake/`, `datamarts/` and `control/` are created there.
Books are downloaded from the official Project Gutenberg mirror `mirror.cs.odu.edu`.

**1. Build once.** The control service uses the crawler and indexer jars, so install them all:

```bash
mvn -q install -DskipTests
```

Run it again after changing the code of any service.

**2a. Run the whole pipeline** with the control service. It downloads and indexes every book of a
workload file (default `workload/sample_ids.txt`) and can be interrupted and run again at any time:

```bash
mvn -q -pl services/control exec:java
mvn -q -pl services/control exec:java -Dexec.args="book_ids.txt"
```

**2b. Or run each service by hand**, in pipeline order. Arguments go in `-Dexec.args`:

```bash
mvn -q -pl services/crawler exec:java -Dexec.args="1342 84"          # download and store books 1342 and 84
mvn -q -pl services/indexer exec:java -Dexec.args="1342 84"          # index them
mvn -q -pl services/query   exec:java -Dexec.args="adventure island" # search
```

Run by hand, the crawler and indexer do not update `control/`; only the control service does.

**3. Choose the structures** with the variables of [Configuration](#configuration), for example:

```bash
docker compose up -d
export TARANTINO_DATALAKE_LAYOUT=batch TARANTINO_INDEX=mongo TARANTINO_METADATA=mongo
mvn -q -pl services/control exec:java
```

## Tests

```bash
mvn test                                              # Java services
python3 -m unittest discover -s scripts -t scripts    # comparison report
```

MongoDB adapters are tested with [Testcontainers](https://testcontainers.com/), which starts a disposable
`mongo:7.0` container for the test run; no MongoDB needs to be running. Without Docker those tests are
skipped, not failed. With [Colima](https://github.com/abiosoft/colima) instead of Docker Desktop, export first:

```bash
export DOCKER_HOST="unix://$HOME/.colima/default/docker.sock"
export TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock
```

## Benchmarks

### Dataset

Benchmarks read the books from a local cache so the network is never measured. Fill it once, from the
project root; it can be interrupted and resumes where it stopped:

```bash
scripts/fill_cache.sh
```

It downloads from the official mirror `mirror.cs.odu.edu`, following `workload/book_ids.txt`, until
`benchmarks/cache/` holds 2 800 books with Gutenberg markers (ids without them are listed in
`benchmarks/cache/skipped.txt`). `TARANTINO_CACHE_BOOKS` changes the target and
`TARANTINO_CACHE_PARALLEL` the number of simultaneous downloads (default 16).

The dataset is about 1.3 GB (2 800 books of 470 KB on average) and downloads in **about 4 minutes** with the
default 16 parallel downloads (measured: about 12 books per second).

### Running

Native MongoDB must be running for the `mongo` structures (`brew services start mongodb-community@7.0`,
see [Requirements](#requirements)); stop the Docker one first (`docker compose down`), since both use
port 27017. From the project root:

```bash
mvn -q install -DskipTests                               # build the services and the benchmark support jars
mvn verify -Pbenchmark -DskipTests                       # every service
mvn verify -Pbenchmark -DskipTests -pl services/indexer  # a single service
```

A full run with 100, 500, 1 000 and 2 000 books takes **about 6 hours**: each benchmark runs in 3 processes
(SPEC §11), and a run with a single process took about 3 hours, most of it building the `folders` and
`mongo` indexes with 2 000 books. These variables shorten it:

| Variable                         | Effect                                                        |
|----------------------------------|---------------------------------------------------------------|
| `TARANTINO_BENCHMARK_BOOKS`      | Sizes to run, e.g. `100,500` (default `100,500,1000,2000`)     |
| `TARANTINO_BENCHMARK_QUICK`      | `true`: 1 process, 1 warm-up and 1 measured iteration, to check the setup |
| `TARANTINO_BENCHMARK_SKIP_MONGO` | `true`: skip the `mongo` index and metadata structures        |

```bash
TARANTINO_BENCHMARK_QUICK=true TARANTINO_BENCHMARK_BOOKS=20 mvn verify -Pbenchmark -DskipTests
```

### Results

`benchmarks/results/java-<service>.csv` holds the results shared with the other languages, in the format
of [SPEC.md](SPEC.md#11-benchmarks); `benchmarks/<service>/jmh-results.csv` keeps the raw JMH output.

| Comparison               | Structures                 | Metrics                                                                  | Benchmarks                                                          |
|--------------------------|----------------------------|--------------------------------------------------------------------------|---------------------------------------------------------------------|
| Datalake (PDF 3.1)       | `time`, `book`, `batch`    | write throughput, lookup, new books detection, recovery, files, disk     | crawler `DatalakeWriteBenchmark`, `DatalakeLookupBenchmark`, `NewBooksDetectionBenchmark`, `RecoveryScenario` |
| Inverted index (PDF 4.2) | `json`, `folders`, `mongo` | full build, incremental update, query, memory, disk                      | indexer `FullIndexBuildBenchmark`, `IncrementalUpdateBenchmark`, query `QueryTimeBenchmark` |
| Metadata (PDF 4.1)       | `sqlite`, `mongo`          | bulk insertion, book by id, books by author                              | indexer `MetadataInsertionBenchmark`, query `MetadataQueryBenchmark` |

How every metric is measured, and the rules that keep results comparable across languages, are defined
in [SPEC.md](SPEC.md#11-benchmarks).

### Comparing the structures

`scripts/compare_results.py` reads every CSV in `benchmarks/results/` and writes
`benchmarks/report/comparison.md`: for each comparison, the best structure per metric at the largest size,
one table per metric (structures against sizes, best value in bold) and one chart per metric if
matplotlib is installed.

```bash
python3 scripts/compare_results.py
```

Like `scripts/fill_cache.sh`, it works from any directory: both resolve paths from the project root.

To compare languages as well, copy the `python-*.csv` and `csharp-*.csv` results of the other
implementations, run on the same machine, into `benchmarks/results/` before running it.
