#!/usr/bin/env bash
set -euo pipefail

readonly MIRROR_URL_TEMPLATE="https://mirror.cs.odu.edu/gutenberg-epub/%d/pg%d.txt"
readonly START_MARKER='\*\*\* ?START OF (THE|THIS) PROJECT GUTENBERG EBOOK'
readonly END_MARKER='\*\*\* ?END OF (THE|THIS) PROJECT GUTENBERG EBOOK'

readonly workload_dir="${TARANTINO_WORKLOAD:-workload}"
readonly cache_dir="${TARANTINO_BENCHMARKS:-benchmarks}/cache"
readonly target_books="${TARANTINO_CACHE_BOOKS:-2800}"
readonly parallel_downloads="${TARANTINO_CACHE_PARALLEL:-16}"
readonly skipped_file="$cache_dir/skipped.txt"

cached_ids() {
    find "$cache_dir" -maxdepth 1 -name '*.txt' | sed -n 's|.*/\([0-9][0-9]*\)\.txt$|\1|p'
}

cached_count() {
    cached_ids | wc -l | tr -d ' '
}

known_count() {
    echo $(( $(cached_count) + $(wc -l < "$skipped_file") ))
}

pending_ids() {
    { echo "#"; cached_ids; cat "$skipped_file"; } |
        awk -v needed="$1" 'NR == FNR { known[$1]; next }
                            NF && !($1 in known) { print $1; if (++count == needed) exit }' - "$workload_dir/book_ids.txt"
}

has_markers() {
    grep -Eq "$START_MARKER" "$1" && grep -Eq "$END_MARKER" "$1"
}

download_book() {
    local id=$1 temporary="$cache_dir/$1.txt.tmp" status
    status=$(curl -sSL --retry 3 --retry-delay 2 -o "$temporary" -w '%{http_code}' \
        "$(printf "$MIRROR_URL_TEMPLATE" "$id" "$id")") || status=000
    store_or_skip "$id" "$temporary" "$status"
}

store_or_skip() {
    local id=$1 temporary=$2 status=$3
    if [[ $status == 200 ]] && has_markers "$temporary"; then
        mv "$temporary" "$cache_dir/$id.txt"
    elif [[ $status == 200 || $status == 404 ]]; then
        rm -f "$temporary"
        echo "$id" >> "$skipped_file"
    else
        rm -f "$temporary"
        echo "Book $id failed with HTTP $status; it will be retried on the next run" >&2
    fi
}

download_batch() {
    export MIRROR_URL_TEMPLATE START_MARKER END_MARKER cache_dir skipped_file
    export -f download_book store_or_skip has_markers
    echo "$1" | xargs -P "$parallel_downloads" -I {} bash -c 'download_book "$1"' _ {}
}

fail() {
    echo "$1" >&2
    exit 1
}

main() {
    cd "$(dirname "${BASH_SOURCE[0]}")/.."
    mkdir -p "$cache_dir"
    touch "$skipped_file"
    while (( $(cached_count) < target_books )); do
        local batch known_before
        batch=$(pending_ids $(( target_books - $(cached_count) )))
        [[ -n $batch ]] || fail "$workload_dir/book_ids.txt has fewer than $target_books downloadable books"
        known_before=$(known_count)
        download_batch "$batch"
        (( $(known_count) > known_before )) || fail "No progress in the last batch; check the network and run again"
        echo "Cached $(cached_count)/$target_books books ($(wc -l < "$skipped_file" | tr -d ' ') skipped)"
    done
    echo "Cache ready in $cache_dir"
}

main "$@"
