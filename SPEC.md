# Query Tarantino — Stage 1 Specification

This document is the contract shared by every language implementation (Java, Python, C#).
Two implementations are **conformant** when, given the same `workload/` and the same configuration,
they produce equivalent datalakes, datamarts, control files and search results, so that benchmark
differences come only from the language and the storage structure.

"Equivalent" means equal content, not byte-identical files: JSON whitespace, key order in documents
and SQLite page layout are free. Everything else in this document is normative.

## 1. General rules

- Book ids are positive integers.
- Every text file is read and written as **UTF-8 without BOM**, with **LF** (`\n`) line endings,
  on every operating system.
- Paths stored as data (metadata `path`) use `/` as separator and are relative to the working
  directory, exactly as built from the configured root (e.g. `datalake/20250925/14/1342.body.txt`).
- `strip` means removing leading and trailing Unicode whitespace
  (Java `String.strip`, Python `str.strip`, C# `string.Trim`).
- Every service runs from the project root; relative roots are resolved against it.

## 2. Configuration

| Variable                    | Default                     | Values                     |
|-----------------------------|-----------------------------|----------------------------|
| `TARANTINO_DATALAKE`        | `datalake`                  | path                       |
| `TARANTINO_DATAMARTS`       | `datamarts`                 | path                       |
| `TARANTINO_CONTROL`         | `control`                   | path                       |
| `TARANTINO_BENCHMARKS`      | `benchmarks`                | path                       |
| `TARANTINO_WORKLOAD`        | `workload`                  | path                       |
| `TARANTINO_DATALAKE_LAYOUT` | `time`                      | `time`, `book`, `batch`    |
| `TARANTINO_INDEX`           | `json`                      | `json`, `folders`, `mongo` |
| `TARANTINO_METADATA`        | `sqlite`                    | `sqlite`, `mongo`          |
| `TARANTINO_MONGO_URI`       | `mongodb://localhost:27017` | connection string          |
| `TARANTINO_INDEX_BATCH`     | `100`                       | positive integer           |

An unknown value is a configuration error and must stop the service with a message naming the value.

## 3. Workload

All files live in `TARANTINO_WORKLOAD`, one entry per line; lines are stripped and empty lines ignored.

| File             | Content                                                              |
|------------------|----------------------------------------------------------------------|
| `book_ids.txt`   | Candidate ids for the benchmark cache, in order (1 to 4000).         |
| `sample_ids.txt` | Small sample dataset; default candidates of the control service.     |
| `stopwords.txt`  | Stopwords; each entry is stripped and lowercased (see §7) on load.   |
| `queries.txt`    | One query per line for the search benchmark.                         |

## 4. Download

- URL: `https://mirror.cs.odu.edu/gutenberg-epub/<id>/pg<id>.txt`, following redirects. This is the official
  high-speed mirror of Project Gutenberg at Old Dominion University (listed in
  `https://www.gutenberg.org/MIRRORS.ALL`); it serves the same generated files as
  `www.gutenberg.org/cache/epub`, which must not be used for bulk downloads.
- The response body is decoded as UTF-8.
- HTTP 200 returns the text. HTTP 404 fails with `NOT_FOUND`. Any other status, timeout or I/O error
  fails with `NETWORK_ERROR`. A 30 second timeout is recommended.
- A failed download stores nothing.

## 5. Header and body split

1. Replace every `\r\n` with `\n`.
2. Find the first match of the start marker:
   `\*\*\* ?START OF (THE|THIS) PROJECT GUTENBERG EBOOK[^\n]*\n`
3. Find the first match of the end marker **after the end of the start marker**:
   `\*\*\* ?END OF (THE|THIS) PROJECT GUTENBERG EBOOK`
4. If either marker is missing, fail with `MISSING_MARKERS` and store nothing.
5. `header` = text before the start marker, stripped.
   `body` = text between the end of the start marker (its whole line included) and the start of the
   end marker, stripped. The footer is discarded.

Failure reasons are exactly: `NOT_FOUND`, `NETWORK_ERROR`, `MISSING_MARKERS`, `STORAGE_ERROR`.

## 6. Datalake

Every layout stores exactly two files per book, `header` and `body`, with the content of §5.

| Layout  | Header file                              | Body file                              |
|---------|------------------------------------------|----------------------------------------|
| `time`  | `<root>/<YYYYMMDD>/<HH>/<id>.header.txt` | `<root>/<YYYYMMDD>/<HH>/<id>.body.txt` |
| `book`  | `<root>/<id>/header.txt`                 | `<root>/<id>/body.txt`                 |
| `batch` | `<root>/<id div 1000>/<id>.header.txt`   | `<root>/<id div 1000>/<id>.body.txt`   |

- `time`: date and hour of the moment the book is saved, in **UTC**, zero-padded
  (`20250925`, `14`). Books saved in different hours end up in different directories.
- `batch`: the directory is the integer division of the id by 1000, without padding
  (`1342` → `1/`, `64317` → `64/`).

Writing and lookup:

1. Each file is written to `<file>.tmp` and then renamed over the final name (atomic move).
2. The header is written before the body. **A book exists in the datalake if and only if its body
   file exists**, so an interrupted write never produces a book without header.
3. Saving a book that is already stored does not create a second copy (see §9, ingestion).
4. Lookup by id: `book` and `batch` compute the path directly; `time` searches the body file name
   under `<root>` up to depth 3.
5. Before ingesting anything, the crawler and the control layer remove what an interrupted run left
   behind: every `.tmp` file, every header whose body does not exist next to it, and every directory
   under `<root>` left empty. Without it `time` would keep, in the old hour directory, the header and
   the `.tmp` body of a book that the resumed run stores again in a new one. It runs once at start,
   with no other process writing the datalake.

New books detection lists the ids of the books stored since an instant:

- `time`: every book in the hour directories from the hour that contains the instant (UTC) onwards,
  decided by the directory names alone.
- `book` and `batch`: every book whose body file was last modified at or after the instant.

## 7. Metadata extraction and tokenization

**Header fields.** For each of `Title`, `Author` and `Language`, take the first match of
`^<Field>:[ \t]*(.+)$` in multiline mode over the header, and strip it. A missing field is `null`.
The value is stored as written (e.g. `English`, not `en`).

**Tokenizer.** Applied to the body when indexing and to the query when searching:

1. Lowercase the whole text with the locale-independent Unicode mapping
   (Java `toLowerCase(Locale.ROOT)`, Python `str.lower`, C# `ToLowerInvariant`).
2. A term is a maximal run of Unicode letters, i.e. characters of category `L`
   (Java/C# `\p{L}+`; Python `regex` package `\p{L}+`, since the standard `re` module has no `\p{L}`).
   Digits, apostrophes, hyphens and any other character split terms:
   `don't` → `don`, `t`; `1984year` → `year`.
3. Discard terms shorter than **2 code points** and terms in the stopword list.
4. No stemming and no Unicode normalization.

Indexing counts the frequency of each term per book; the storage structures below keep only book ids.

## 8. Datamarts

### 8.1 Inverted index

Adding a book adds its id to the postings of each of its terms. Postings never contain duplicates,
so indexing the same book twice leaves the index unchanged. Postings are ordered ascending wherever
the structure has an order.

| Structure | Location                                         | Format |
|-----------|--------------------------------------------------|--------|
| `json`    | `<datamarts>/inverted_index.json`                | One JSON object: term → array of ids, e.g. `{"island": [5, 1342]}` |
| `folders` | `<datamarts>/inverted_index/<c>/<name>.txt`      | One id per line; `<name>` encodes the term and `<c>` its first code point (below) |
| `mongo`   | database `tarantino`*, collection `inverted_index` | One document per term: `{"term": "island", "postings": [5, 1342]}`, unique index on `term` |

- `json` is rewritten completely, and atomically (`.tmp` + rename), each time the index is flushed.
- `folders` rewrites, on each flush, every affected term file with the union of its stored ids and the
  new ones, sorted and atomically (`.tmp` + rename). Term files that gain no new id are not touched.
- `folders` file names: in the UTF-8 bytes of the term, ASCII `a`–`z` are kept and every other byte is
  written as `%` and two uppercase hexadecimal digits (`island` → `island`, `écume` → `%C3%A9cume`). If
  the result is longer than 200 characters, the name is `#` followed by the lowercase hexadecimal SHA-256
  of the term's UTF-8 bytes. `<c>` is the first code point of the term encoded the same way
  (`%C3%A9/%C3%A9cume.txt`). Case-insensitive and normalization-insensitive file systems (APFS, NTFS)
  would otherwise store distinct terms such as `shape` and `ſhape`, or `café` in NFC and NFD, in the same
  file, and names would exceed the 255-byte limit for long terms.
- `mongo` upserts one document per affected term, adding the ids with `$addToSet`; readers must treat
  `postings` as a set.

\* Or the database named in the path of `TARANTINO_MONGO_URI` (`mongodb://host:27017/<database>`).

### 8.2 Metadata

Fields: `book_id`, `title`, `author`, `language`, `path` (the body file of §6, see §1 for the format).
Saving a book that already exists replaces its row or document. Each backend opens its connection once
and reuses it for every save and query; each save is committed before it returns, because §9 marks a
book as indexed right after.

| Backend  | Location                            | Schema |
|----------|-------------------------------------|--------|
| `sqlite` | `<datamarts>/metadata.db`, table `books` | `book_id INTEGER PRIMARY KEY, title TEXT, author TEXT, language TEXT, path TEXT NOT NULL` |
| `mongo`  | database `tarantino`*, collection `books` | Same fields, unique index on `book_id`, missing fields stored as `null` |

## 9. Control layer

**State files** in `TARANTINO_CONTROL`: `downloaded_books.txt` and `indexed_books.txt`, one id per line.
They are append-only. On read, lines that are not a whole number after stripping are ignored,
so a partially written last line is harmless. File order is preserved. They are read once when a run
starts; during the run the control layer keeps the state in memory and only appends to the files, so a
step costs the same however many books are already done.

**Ingestion** of a book is idempotent: if the datalake already contains it (§6), it succeeds with the
existing paths without downloading again.

**Books to index** are the ids of `downloaded_books.txt`, in file order, that are not in
`indexed_books.txt` and have not failed during this run. They are indexed in batches of K books,
K = `TARANTINO_INDEX_BATCH` (100 by default).

**Next step**, evaluated before every step:

1. `INDEX` the first K books to index, if there are at least K, or all of them if there is at least one
   and no candidate is left to download.
2. Otherwise, `DOWNLOAD` the first candidate, in candidates-file order, that is not in
   `downloaded_books.txt` and has not failed during this run.
3. Otherwise `IDLE`: the run ends.

**After each step**, each id is appended to the matching state file **only if it succeeded**.
A failed id is remembered in memory for the current run and retried on the next run.
Indexing a batch writes the metadata of each book, adds every book to the inverted index and flushes
it **once**; only then are the books that succeeded marked as indexed. An interrupted batch marks none
of its books, so the next run indexes it again, which is harmless because indexing a book twice leaves
the index unchanged (§8.1) and metadata is replaced (§8.2).

Batching only changes when the index is written, never its content: after the same books, the index is
the same for any K, and so are search results. A book becomes searchable when its batch is flushed.
Flushing once per batch instead of once per book is what makes it cheaper: `json` rewrites the whole
file on every flush and `folders` every term file of the flushed books (see `incremental_update_time`
and `batch_update_time`, §11). K = 1 indexes book by book.

Candidates come from `TARANTINO_WORKLOAD/<file>`, where `<file>` is the first argument of the control
service, or `sample_ids.txt` by default.

## 10. Search

1. Turn the query into terms with the tokenizer of §7, removing duplicates.
2. If no term remains, the result is empty.
3. The result is the set of books whose postings contain **every** term (AND).
4. Each id is resolved through the metadata backend; ids without metadata are dropped.
5. Results are ordered by `book_id` ascending.

Author lookup is a case-insensitive substring match (ASCII case folding is enough).

## 11. Benchmarks

**Dataset.** `scripts/fill_cache.sh` downloads raw texts once from the official mirror (§4) to
`<benchmarks>/cache/<id>.txt`, unchanged, following `book_ids.txt`, until the cache holds 2 800 books.
Only books with both markers (§5) are cached; ids answered with 404 or without markers are listed in
`<benchmarks>/cache/skipped.txt` and never retried, while network errors are retried on the next run.
The cached ids, in `book_ids.txt` order, are the **cache order**. Every implementation reads from the
same cache, so the network is never measured.

**Sizes.** N ∈ {100, 500, 1000, 2000}. The dataset of size N is the first N ids in cache order. The
**new books** are always the same 100: positions 2001 to 2100 in cache order. They never belong to a
dataset and are the same for every N, so results at different N differ only by N.

**Simulated download time.** The datalake benchmarks save the books of a dataset as a crawl
downloading 100 books per hour would: the book at position i (0-based) is saved at T₀ + ⌊i / 100⌋ hours,
so `time` spreads N books over ⌈N / 100⌉ hour directories. In new books detection that instant is also
set as the modification time of each body file, which `book` and `batch` read (§6); the other
benchmarks leave it unchanged, so writing costs the same for every layout.

**Execution.**

- Same machine for all languages, with nothing else running. The files that benchmarks write are kept
  out of file indexing: on macOS, under `<benchmarks>/tmp.noindex/`, since Spotlight skips directories
  whose name ends in `.noindex`.
- Same MongoDB for all languages: **MongoDB 7.0 running natively** on the benchmark machine, with the
  WiredTiger cache fixed at 1 GB (`storage.wiredTiger.engineConfig.cacheSizeGB: 1`), using one database
  per benchmark. It must not run in a container: on macOS and Windows Docker runs a virtual machine that
  reserves its own memory and adds a network round trip to every operation, so `mongo` would be measured
  at a disadvantage. `docker-compose.yml` is only for development.
- Record CPU, RAM, OS, runtime versions and the MongoDB version.
- Each benchmark runs in **3 separate processes**. Each process runs warm-up iterations, which are
  discarded, and then measured iterations. Every measured iteration is one **sample**:

| Kind of metric                                                                        | Warm-up per process | Measured per process | Samples |
|---------------------------------------------------------------------------------------|---------------------|----------------------|--------:|
| One whole run: write, full build, incremental and batch update, insertion, index open | 1 run               | 3 runs               |       9 |
| One operation: lookup, detection, query, metadata queries                             | 3 × 1 second        | 5 × 1 second         |      15 |

  A sample of the first kind is the time of one run. A sample of the second kind is the mean time per
  operation during one second.
- Before every run of the first kind, warm-up included, storage is reset without timing it: emptied,
  except for the incremental and batch updates, where the index is restored to exactly the dataset of
  size N, and index open, which only reads.
- The **value** of a metric is the mean of its samples. Its **error** is the half-width of the 99.9%
  confidence interval of that mean, t₀.₉₉₉₅,ₙ₋₁ · s / √n over the n samples (what JMH reports as
  `Score Error`). Two structures are **tied** when their intervals overlap: |a − b| ≤ error(a) + error(b).

**Validation.** Before measuring, each benchmark checks that the structure gives the correct result,
and the benchmark run fails without writing results otherwise:

- Lookup: every book of the dataset is found, and both of its files exist.
- New books detection: exactly the ids of the 100 new books.
- Query: for every query of `queries.txt`, the same ids as a reference computed in memory from the
  tokenized books of the dataset (§7, §10).
- Metadata queries: every id returns the book with that id, and every author returns at least every
  book of the dataset with exactly that author.

**Results.** Each implementation writes `<benchmarks>/results/<language>-<service>.csv` with the header
`language,structure,metric,n_books,value,error,unit`, e.g. `java,time,write_throughput,1000,812.4,35.2,books/s`.
`error` is in the unit of the metric. It is `0` for exact metrics (counts, sizes and `recovery_ok`) and
empty when it cannot be computed (fewer than 2 samples). For a value derived from a time, such as
`write_throughput` = N / time, the error is the value times the relative error of the time.
With the results of every language in that directory, `scripts/compare_results.py` builds the
comparison report in `<benchmarks>/report/`, where tied structures share the first place.

| Group    | Structures                   | Metric                     | Unit    |
|----------|------------------------------|----------------------------|---------|
| Datalake | `time`, `book`, `batch`      | `write_throughput`         | books/s |
|          |                              | `lookup_time`              | µs/op   |
|          |                              | `new_books_detection_time` | ms      |
|          |                              | `recovery_ok`              | 0 or 1  |
|          |                              | `recovery_leftover_files`  | files   |
|          |                              | `file_count`               | files   |
|          |                              | `directory_count`          | dirs    |
|          |                              | `disk_usage`               | bytes   |
| Index    | `json`, `folders`, `mongo`   | `full_build_time`          | ms      |
|          |                              | `incremental_update_time`  | ms/book |
|          |                              | `batch_update_time`        | ms/book |
|          |                              | `index_open_time`          | ms      |
|          |                              | `query_time`               | µs/query|
|          |                              | `build_memory`             | bytes   |
|          |                              | `index_memory`             | bytes   |
|          |                              | `memory_allocated`         | bytes   |
|          |                              | `term_count`               | terms   |
|          |                              | `disk_usage`               | bytes   |
| Metadata | `sqlite`, `mongo`            | `bulk_insertion_time`      | ms      |
|          |                              | `book_by_id_time`          | µs/op   |
|          |                              | `books_by_author_time`     | µs/op   |

- `write_throughput`: N divided by the time to ingest the N cached books as the crawler does (§9,
  ingestion): look the book up in the datalake (§6, lookup), then read, split (§5) and store it. The
  lookup is part of real ingestion, and for `time` it searches the whole datalake.
- `lookup_time`: time to find the header and body of a random book of the dataset.
- `new_books_detection_time`: time to list the 100 new books (§6, new books detection). The N books of
  the dataset are saved first, ending one day before the new ones, which are then saved at the current time.
- `recovery_ok`: measured with 100 books. Half of them are ingested; the next one is interrupted after
  its header is written, leaving its body as `.tmp`; one hour later a new run removes incomplete writes
  (§6) and ingests all 100 again. It is 1 if every book ends with exactly one body file, 0 otherwise.
- `recovery_leftover_files`: after the same scenario, the number of files in the datalake that are
  neither the header nor the body of a stored book (`.tmp` files and orphaned headers).
- `file_count`, `directory_count`, `disk_usage`: the datalake after writing N books; directories do not
  count the root, and `disk_usage` is the sum of file sizes in bytes.
- `full_build_time` and `memory_allocated`: time and bytes allocated to read, split, tokenize and index
  N books into an empty index, flushing once at the end. `memory_allocated` is counted by every thread
  around the build alone, never around emptying the storage, one sample per measured run. It counts
  garbage too: it measures the pressure on the garbage collector, not the memory required.
- `build_memory`: memory retained while building, i.e. heap in use after a full garbage collection once
  the N books are added and before the flush, minus the same measure before the build.
- `incremental_update_time`: mean time per book to index the first 10 new books into an index of exactly
  the N books of the dataset book by book, as the control layer does with K = 1 (§9): the index is
  opened from storage once, as a new process would, and flushed after every book. Only 10 books, because
  flushing after each one is far slower (`folders` rewrites every term file of every book).
- `batch_update_time`: mean time per book to index the 100 new books into an index of exactly the N
  books of the dataset with a single flush at the end, opening the index from storage first, as the
  control layer does with the default K = 100 (§9). Compared with `incremental_update_time`, it shows
  what batching saves.
- `index_open_time`: time to open an index of N books from storage with a new reader, holding nothing
  in memory from previous runs, as a new process would, and answer the first query of `queries.txt`.
  The operating system cache and an open MongoDB connection may be reused, so it measures loading the
  index, not starting a process.
- `query_time`: time of a random query of `queries.txt` against an open index of N books; metadata is
  not read, so only the index is measured.
- `index_memory`: memory retained by an index of N books open for querying, i.e. heap in use after a
  full garbage collection with the index open and every query of `queries.txt` answered once, minus the
  same measure before opening it. Each process opens the index once without measuring it and then
  measures 5 openings, so there are 15 samples. For `mongo` only the client side is measured, with the
  client already connected.
- `term_count`: distinct terms in the index of N books.
- `bulk_insertion_time`: time to save the metadata of N books one by one, through a single open backend,
  into an empty backend.
- `book_by_id_time`, `books_by_author_time`: a random id, or the author of a random book, of the dataset.
- For `mongo`, `disk_usage` is the `storageSize` + `totalIndexSize` of its collections after an `fsync`.
- `build_memory` is measured once per process, so it has 3 samples (building again is expensive and its
  variation is small); `term_count` is exact.

## 12. Known limitations

- Terms are used as file names by `folders`. On Windows the reserved names (`con`, `nul`, `aux`, `prn`…)
  cannot be created, so benchmarks for that structure run on macOS or Linux.
- Case mapping may differ between runtimes for a few rare characters; this is accepted.
- Java `strip` does not remove U+00A0, U+2007 and U+202F, while Python and C# do. Header and body
  files may differ by those characters at their edges; terms are not affected.
- `disk_usage` of files is their logical size, not the blocks allocated by the file system, so
  structures with many small files (`folders`, `book`) take more disk space than reported.
- The datalake is not synced to disk after each write, so `write_throughput` measures writes to the
  operating system cache.
