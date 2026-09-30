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

- URL: `https://gutenberg.pglaf.org/cache/epub/<id>/pg<id>.txt`, following redirects. This is the official
  high-speed mirror of Project Gutenberg (listed in `https://www.gutenberg.org/MIRRORS.ALL`) and serves the
  same files as `www.gutenberg.org`, which must not be used for bulk downloads.
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
| `folders` | `<datamarts>/inverted_index/<c>/<term>.txt`      | One id per line; `<c>` is the first code point of the term (`é/écume.txt`) |
| `mongo`   | database `tarantino`*, collection `inverted_index` | One document per term: `{"term": "island", "postings": [5, 1342]}`, unique index on `term` |

- `json` is rewritten completely, and atomically (`.tmp` + rename), each time the index is flushed.
- `folders` rewrites, on each flush, every affected term file with the union of its stored ids and the
  new ones, sorted and atomically (`.tmp` + rename). Term files that gain no new id are not touched.
- `mongo` upserts one document per affected term, adding the ids with `$addToSet`; readers must treat
  `postings` as a set.

\* Or the database named in the path of `TARANTINO_MONGO_URI` (`mongodb://host:27017/<database>`).

### 8.2 Metadata

Fields: `book_id`, `title`, `author`, `language`, `path` (the body file of §6, see §1 for the format).
Saving a book that already exists replaces its row or document.

| Backend  | Location                            | Schema |
|----------|-------------------------------------|--------|
| `sqlite` | `<datamarts>/metadata.db`, table `books` | `book_id INTEGER PRIMARY KEY, title TEXT, author TEXT, language TEXT, path TEXT NOT NULL` |
| `mongo`  | database `tarantino`*, collection `books` | Same fields, unique index on `book_id`, missing fields stored as `null` |

## 9. Control layer

**State files** in `TARANTINO_CONTROL`: `downloaded_books.txt` and `indexed_books.txt`, one id per line.
They are append-only. On read, lines that are not a whole number after stripping are ignored,
so a partially written last line is harmless. File order is preserved.

**Ingestion** of a book is idempotent: if the datalake already contains it (§6), it succeeds with the
existing paths without downloading again.

**Next step**, evaluated before every step:

1. `INDEX` the first id of `downloaded_books.txt`, in file order, that is not in
   `indexed_books.txt` and has not failed during this run.
2. Otherwise, `DOWNLOAD` the first candidate, in candidates-file order, that is not in
   `downloaded_books.txt` and has not failed during this run.
3. Otherwise `IDLE`: the run ends.

**After each step**, the id is appended to the matching state file **only if the step succeeded**.
A failed id is remembered in memory for the current run and retried on the next run.
Indexing a book writes its metadata and flushes the inverted index before it is marked as indexed.

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
The dataset of size N is the first N ids of `book_ids.txt` present in the cache. Every implementation
reads from the same cache, so the network is never measured.

**Sizes.** N ∈ {100, 500, 1000, 2000}. Each iteration of the incremental update, warm-up included, adds
the next 100 cached books after the first N, so 3 + 5 iterations need N + 800 books: the cache holds
2 800. New books detection stores N books as one day old and then 100 new ones.

**Execution.** Same machine for all languages, nothing else running, and the same MongoDB server
(`mongo:7.0` from `docker-compose.yml`), using one database per benchmark. Each data point is the
mean of the measured iterations:

| Kind of metric                                                    | Warm-up       | Measured      |
|-------------------------------------------------------------------|---------------|---------------|
| One whole run: write, full build, incremental update, insertion   | 3 runs        | 5 runs        |
| One operation: lookup, detection, query, metadata queries         | 3 × 1 second  | 5 × 1 second  |

Storage is emptied before every run of the first kind. Record CPU, RAM, OS and runtime versions.

**Results.** Each implementation writes `<benchmarks>/results/<language>-<service>.csv` with the header
`language,structure,metric,n_books,value,unit`, e.g. `java,time,write_throughput,1000,812.4,books/s`.
With the results of every language in that directory, `scripts/compare_results.py` builds the
comparison report in `<benchmarks>/report/`.

| Group    | Structures                   | Metric                     | Unit    |
|----------|------------------------------|----------------------------|---------|
| Datalake | `time`, `book`, `batch`      | `write_throughput`         | books/s |
|          |                              | `lookup_time`              | µs/op   |
|          |                              | `new_books_detection_time` | ms      |
|          |                              | `recovery_ok`              | 0 or 1  |
|          |                              | `file_count`               | files   |
|          |                              | `directory_count`          | dirs    |
|          |                              | `disk_usage`               | bytes   |
| Index    | `json`, `folders`, `mongo`   | `full_build_time`          | ms      |
|          |                              | `incremental_update_time`  | ms      |
|          |                              | `query_time`               | µs/query|
|          |                              | `memory_allocated`         | bytes   |
|          |                              | `disk_usage`               | bytes   |
| Metadata | `sqlite`, `mongo`            | `bulk_insertion_time`      | ms      |
|          |                              | `book_by_id_time`          | µs/op   |
|          |                              | `books_by_author_time`     | µs/op   |

- `write_throughput`: N divided by the time to read, split (§5) and store the N cached books.
- `lookup_time`: time to find the header and body of a random stored book.
- `new_books_detection_time`: time to list the 100 new books (§6, new books detection).
- `recovery_ok`: measured with 100 books. Half of them are ingested; the next one is interrupted after
  its header is written, leaving its body as `.tmp`; ingestion of all 100 is then run again one hour
  later. It is 1 if every book ends with exactly one body file, 0 otherwise.
- `file_count`, `directory_count`, `disk_usage`: the datalake after writing N books; directories do not
  count the root, and `disk_usage` is the sum of file sizes in bytes.
- `full_build_time` and `memory_allocated`: time and bytes allocated to read, split, tokenize and index
  N books into an empty index, flushing once at the end.
- `incremental_update_time`: time to index 100 new books into an index of N books, opening the index
  from storage as a new process would.
- `query_time`: time of a random query of `queries.txt` against an index of N books; metadata is not
  read, so only the index is measured.
- `bulk_insertion_time`: time to save the metadata of N books into an empty backend.
- `book_by_id_time`, `books_by_author_time`: a random id, or the author of a random book, among N books.
- For `mongo`, `disk_usage` is the `storageSize` + `totalIndexSize` of its collections after an `fsync`.

## 12. Known limitations

- Terms are used as file names by `folders`. On Windows the reserved names (`con`, `nul`, `aux`, `prn`…)
  cannot be created, so benchmarks for that structure run on macOS or Linux.
- Case mapping may differ between runtimes for a few rare characters; this is accepted.
- Java `strip` does not remove U+00A0, U+2007 and U+202F, while Python and C# do. Header and body
  files may differ by those characters at their edges; terms are not affected.
