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

Compared on write throughput, lookup time, new books detection, recovery after an interruption and the files
it leaves behind, and number of files, directories and bytes (logical and in whole disk blocks).

**2. Inverted index: how terms map to books** (PDF §4.2)

| Structure | Storage                                           | Trade-off                                                   |
|-----------|---------------------------------------------------|-------------------------------------------------------------|
| `json`    | one `datamarts/inverted_index.json`               | Simple and fast to query; rewritten whole on every update   |
| `folders` | one `datamarts/inverted_index/<c>/<name>.txt` per term (name encoded, SPEC §8.1) | Fine-grained updates; very many small files |
| `mongo`   | MongoDB collection, one document per term         | Indexed random access and concurrency; needs a server       |

Compared on full build time, incremental update time (book by book, as the control layer indexes, and in
batch), index open time, query time (mean, 99th percentile and per kind of query), memory retained while
building and while open, allocated memory, number of terms and disk usage (logical and in whole disk blocks).

**3. Metadata: where title, author, language and path are stored** (PDF §4.1)

| Structure | Storage                                           | Trade-off                                                   |
|-----------|---------------------------------------------------|-------------------------------------------------------------|
| `sqlite`  | `datamarts/metadata.db`, table `books`            | Embedded, no server, SQL queries                            |
| `mongo`   | MongoDB collection `books`                        | Server-based NoSQL, flexible schema                         |

Compared on bulk insertion time, lookup by id and search by author.

Every comparison runs at 100, 300 and 1 000 books to show how each structure scales. See
[Benchmarks](#benchmarks) to run them and generate the comparison report.

## Repository structure

```
services/
  crawler/    downloads books, splits header/body and stores them in the datalake
  indexer/    reads the datalake and builds the inverted index and the metadata datamart
  query/      searches the datamarts
  control/    orchestrates crawler -> indexer and tracks progress in control files
scripts/
  fill_cache.sh        copies the benchmark dataset from Gutenberg's official mirrors (rsync, or HTTP)
  compare_results.py   builds the data structure comparison report from the benchmark results
  comparison/          its code: model/, ports/, adapters/ (CSV, charts), report/ (Markdown), commands/
  tests/               unit tests of comparison/, in the same package layout
workload/     experiment definition shared by every language implementation
  book_ids.txt     candidate book ids for the benchmark dataset, in order
  sample_ids.txt   small sample dataset to test the pipeline quickly
  stopwords.txt    stopwords removed by the tokenizer
  queries.txt      search benchmark workload: 5 queries per category (frequent, rare, mixed, long, empty, non-ASCII)
  conformance/     cases of the SPEC rules, in JSON, that every implementation must pass (SPEC §13)
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
| `benchmarks/` | benchmarks | `cache/` dataset, `results/` CSVs, `report/` comparison, raw JMH output, `tmp.noindex/` scratch files |

## Requirements

- Java 25
- Maven 3.9+
- A Unix system: Linux is the reference platform; macOS works the same
- MongoDB 7.0, only for the `mongo` index or metadata backends
- Docker, only for the tests of the MongoDB adapters (and optionally to run MongoDB on Linux)
- Python 3.10+ for the comparison report (matplotlib is optional and only adds charts)

MongoDB 7.0 is the version shared by every implementation. For the benchmarks it must run on the machine
itself, not inside a virtual machine, with its cache fixed at 1 GB (SPEC §11); by default MongoDB takes up to
half of the RAM minus 1 GB.

**Linux.** Either install the official MongoDB 7.0 Community packages for your distribution
(https://www.mongodb.com/docs/v7.0/administration/install-on-linux/), set the cache in `/etc/mongod.conf`
and start it with `sudo systemctl start mongod`, or run the official image with host networking, which on
Linux is as fast as a native install because containers share the host kernel:

```bash
docker run -d --name tarantino-mongo --network host mongo:7.0 --wiredTigerCacheSizeGB 1
```

**macOS.** Docker runs containers inside a virtual machine there, which reserves its own memory (2 GB by
default with Colima) and adds a network round trip to every operation, so install MongoDB natively:

```bash
brew tap mongodb/brew && brew install mongodb-community@7.0
brew services start mongodb-community@7.0   # stop with: brew services stop mongodb-community@7.0
```

The cache setting, in `/etc/mongod.conf` on Linux or `/opt/homebrew/etc/mongod.conf` on macOS, goes inside the
existing `storage:` section:

```yaml
storage:
  wiredTiger:
    engineConfig:
      cacheSizeGB: 1
```

For development on any system, `docker compose up -d` starts the same version (`docker compose down` stops
it, `-v` also deletes the data).

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
| `TARANTINO_INDEX_BATCH`     | `100`                       | books indexed per index flush by the control service (1 = book by book) |
| `TARANTINO_MIRROR`          | (none)                      | local copy of Gutenberg's generated collection to read books from instead of downloading them |

The crawler and the indexer must use the same `TARANTINO_DATALAKE_LAYOUT`; the indexer and the query service must use
the same `TARANTINO_INDEX` and `TARANTINO_METADATA`.

## Running

Every service is a plain Java program run through Maven from a terminal; no IDE is needed.
Always run from the **project root**, so `datalake/`, `datamarts/` and `control/` are created there.
Books are downloaded from the official Project Gutenberg mirror `mirror.cs.odu.edu`, one request per book.
For many books, copy them first in bulk with rsync, Project Gutenberg's documented way to mirror its
collection, and point `TARANTINO_MIRROR` at the copy; the crawler then reads them from disk:

```bash
rsync -av --include='*/' --include='pg[0-9]*.txt' --exclude='*' rsync.ibiblio.org::gutenberg-epub/ ~/gutenberg-txt/
export TARANTINO_MIRROR=~/gutenberg-txt
```

Copying every plain text takes some tens of gigabytes. Networks that block the rsync port (873), as some
university networks do, need another connection.

**1. Build once.** The control service uses the crawler and indexer jars, so install them all:

```bash
mvn -q install -DskipTests
```

Run it again after changing the code of any service.

**2a. Run the whole pipeline** with the control service. It downloads and indexes every book of a
workload file (default `workload/sample_ids.txt`) and can be interrupted and run again at any time. It
indexes in batches of `TARANTINO_INDEX_BATCH` books (100 by default) with one index write per batch, so a
downloaded book becomes searchable when its batch is written; an interrupted batch is indexed again on the
next run:

```bash
mvn -q -pl services/control exec:java
mvn -q -pl services/control exec:java -Dexec.args="book_ids.txt"
```

**2b. Or run each service by hand**, in pipeline order. Arguments go in `-Dexec.args`:

```bash
mvn -q -pl services/crawler exec:java -Dexec.args="1342 84"          # download and store books 1342 and 84
mvn -q -pl services/indexer exec:java -Dexec.args="1342 84"          # index them, one index write for both
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

`mvn test` also runs the conformance cases of `workload/conformance/` (SPEC §13): the header and body split,
datalake paths, header fields, tokenizer, `folders` file names, query terms and search, each case reported
by name. The other language implementations must pass the same files.

MongoDB adapters are tested with [Testcontainers](https://testcontainers.com/), which starts a disposable
`mongo:7.0` container for the test run; no MongoDB needs to be running. Without Docker those tests are
skipped, not failed. On macOS with [Colima](https://github.com/abiosoft/colima) instead of Docker Desktop, export
first:

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

It follows `workload/book_ids.txt` until `benchmarks/cache/` holds 2 800 books with Gutenberg markers (ids
missing or without them are listed in `benchmarks/cache/skipped.txt`). By default it copies the candidates
from `rsync.ibiblio.org::gutenberg-epub` in one rsync transfer, into `benchmarks/mirror/`, and moves the valid
ones to the cache. Where the rsync port is blocked, download them over HTTP instead:

```bash
TARANTINO_CACHE_SOURCE=http scripts/fill_cache.sh
```

| Variable                   | Effect                                                              |
|----------------------------|---------------------------------------------------------------------|
| `TARANTINO_CACHE_BOOKS`    | Books to cache (default 2800)                                       |
| `TARANTINO_CACHE_SOURCE`   | `rsync` (default) or `http`                                         |
| `TARANTINO_RSYNC_SOURCE`   | rsync module (default `rsync.ibiblio.org::gutenberg-epub`)          |
| `TARANTINO_CACHE_PARALLEL` | Simultaneous HTTP downloads (default 16)                            |

The dataset is about 1.3 GB (2 800 books of 470 KB on average). Over HTTP it downloads in about 4 minutes
with 16 parallel downloads (measured: about 12 books per second); rsync copies it in one transfer, limited
only by the bandwidth.

### Running

MongoDB 7.0 must be running for the `mongo` structures, set up for benchmarking as described in
[Requirements](#requirements); stop the development container first (`docker compose down`) if it is not the
one you set up, since both use port 27017. From the project root:

```bash
mvn -q install -DskipTests                                 # build the services and the benchmark support jars
mvn verify -Pbenchmark -DskipTests                         # every service
mvn verify -Pbenchmark -DskipTests -pl services/indexer    # a single service
```

Keep the machine as SPEC §11 requires: plugged in, out of any power-saving mode, with nothing else running,
and awake for the whole run. Wrap the command so the system does not sleep until it ends:

```bash
systemd-inhibit --what=idle:sleep mvn verify -Pbenchmark -DskipTests   # Linux
caffeinate -i mvn verify -Pbenchmark -DskipTests                       # macOS
```

Laptops, above all fanless ones, lower their clock speed under sustained load, so a cool machine on a hard
surface gives steadier results. The scratch files go under `benchmarks/tmp.noindex/`, which Spotlight skips
on macOS; on a Linux desktop with KDE Baloo, which indexes the whole home directory, exclude `benchmarks/` in
its settings.

A full run with 100, 300 and 1 000 books takes **about 1 hour 10 minutes** on an Apple M4 laptop with an SSD:
13 minutes for 100 books alone (measured), the larger sizes estimated from how each step grows with N, so it
depends mostly on the disk. Every benchmark runs in 2 processes (SPEC §11), and two thirds of the time go to
building and updating the `folders` index, one file per term. These variables shorten it:

| Variable                         | Effect                                                        |
|----------------------------------|---------------------------------------------------------------|
| `TARANTINO_BENCHMARK_BOOKS`      | Sizes to run, e.g. `100,300` (default `100,300,1000`)          |
| `TARANTINO_BENCHMARK_QUICK`      | `true`: 1 process, 1 warm-up and 1 measured iteration, to check the setup |
| `TARANTINO_BENCHMARK_SKIP_MONGO` | `true`: skip the `mongo` index and metadata structures        |

```bash
TARANTINO_BENCHMARK_QUICK=true TARANTINO_BENCHMARK_BOOKS=20 mvn verify -Pbenchmark -DskipTests
```

### Results

`benchmarks/results/java-<service>.csv` holds the results shared with the other languages, in the format
of [SPEC.md](SPEC.md#11-benchmarks); `benchmarks/<service>/jmh-results-pass-<1|2>.csv` keep the raw JMH output of
each pass.

| Comparison               | Structures                 | Metrics                                                                  | Benchmarks                                                          |
|--------------------------|----------------------------|--------------------------------------------------------------------------|---------------------------------------------------------------------|
| Datalake (PDF 3.1)       | `time`, `book`, `batch`    | write throughput, lookup, new books detection, recovery and leftover files, files, disk | crawler `DatalakeWriteBenchmark`, `DatalakeLookupBenchmark`, `NewBooksDetectionBenchmark`, `RecoveryScenario` |
| Inverted index (PDF 4.2) | `json`, `folders`, `mongo` | full build, incremental update book by book and in batch, index open, query, build and index memory, allocations, terms, disk | indexer `FullIndexBuildBenchmark`, `IncrementalUpdateBenchmark`, query `IndexOpenBenchmark`, `QueryTimeBenchmark` |
| Metadata (PDF 4.1)       | `sqlite`, `mongo`          | bulk insertion, book by id, books by author                              | indexer `MetadataInsertionBenchmark`, query `MetadataQueryBenchmark` |

How every metric is measured, and the rules that keep results comparable across languages, are defined
in [SPEC.md](SPEC.md#11-benchmarks).

When comparing languages, note that the Java tokenizer scans code points with `Character.isLetter` instead of
matching `\p{L}+` with a regular expression. It finds exactly the same terms (`TokenizerEquivalenceTest`
checks it against the regular expression on thousands of texts, and the SPEC conformance cases pass), but it
is about 1.6 times faster and allocates half the memory (200 books: 1.15 s and 1.2 GB instead of 1.9 s and
2.6 GB), which shortens Java's build and update times and lowers its `memory_allocated`.

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

## Future improvements

**Ranked search (Stage 2).** The inverted index stores only which books contain each term: the tokenizer
counts term frequencies, but every structure discards them when it stores postings (SPEC §8.1). That is
enough for boolean search, the AND queries this stage benchmarks, but not for ordering results by relevance
(TF-IDF, BM25) as a real search engine does. Ranking would need:

- **Index:** postings of book id and term frequency in the three structures (for example
  `{"island": {"5": 3, "1342": 1}}` in `json`, `5 3` per line in `folders`, `{id, tf}` documents in `mongo`),
  and re-indexing a book must replace its frequencies instead of adding them (no more `$addToSet`).
- **Metadata:** the length of each book in terms, which BM25 normalizes by.
- **Search:** results ordered by score instead of by id, usually matching any term (OR) instead of all of
  them, which returns many more candidates.

Expected costs: postings about 1.5 to 2 times larger, more memory for `json` (a map per term instead of a
set), slower queries (scoring, above all for frequent terms) and somewhat slower updates; the real disk
taken by `folders` barely changes, since one block per file already dominates it. It changes the SPEC for
every language and every benchmark result, so it belongs to Stage 2, before its benchmarks are run.
