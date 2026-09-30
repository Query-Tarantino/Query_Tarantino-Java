#!/usr/bin/env python3
import csv
import os
import sys
from dataclasses import dataclass
from pathlib import Path

BENCHMARKS = Path(os.environ.get("TARANTINO_BENCHMARKS", "benchmarks"))
RESULTS = BENCHMARKS / "results"
REPORT = BENCHMARKS / "report"
HIGHER_IS_BETTER = {"write_throughput", "recovery_ok"}


@dataclass(frozen=True)
class Comparison:
    title: str
    structures: tuple[str, ...]
    metrics: tuple[str, ...]


@dataclass(frozen=True)
class Result:
    language: str
    structure: str
    metric: str
    books: int
    value: float
    unit: str


COMPARISONS = (
    Comparison("Datalake structures", ("time", "book", "batch"),
               ("write_throughput", "lookup_time", "new_books_detection_time", "recovery_ok",
                "file_count", "directory_count", "disk_usage")),
    Comparison("Inverted index structures", ("json", "folders", "mongo"),
               ("full_build_time", "incremental_update_time", "query_time", "memory_allocated", "disk_usage")),
    Comparison("Metadata backends", ("sqlite", "mongo"),
               ("bulk_insertion_time", "book_by_id_time", "books_by_author_time")),
)


def results() -> list[Result]:
    return [result for file in sorted(RESULTS.glob("*.csv")) for result in results_in(file)]


def results_in(file: Path) -> list[Result]:
    with file.open(newline="", encoding="utf-8") as lines:
        return [Result(row["language"], row["structure"], row["metric"], int(row["n_books"]),
                       float(row["value"]), row["unit"]) for row in csv.DictReader(lines)]


def selected(all_results: list[Result], comparison: Comparison, metric: str) -> list[Result]:
    return [result for result in all_results
            if result.metric == metric and result.structure in comparison.structures]


def best_value(candidates: list[Result]) -> float:
    choose = max if candidates[0].metric in HIGHER_IS_BETTER else min
    return choose(result.value for result in candidates)


def best(candidates: list[Result]) -> list[Result]:
    return [result for result in candidates if result.value == best_value(candidates)]


def formatted(result: Result) -> str:
    if result.unit == "bytes":
        return f"{result.value / 1_000_000:,.1f}"
    return f"{result.value:,.0f}" if result.value >= 100 else f"{result.value:,.2f}"


def display_unit(result: Result) -> str:
    return "MB" if result.unit == "bytes" else result.unit


def metric_table(metric_results: list[Result]) -> list[str]:
    sizes = sorted({result.books for result in metric_results})
    header = ["| Language | Structure | " + " | ".join(f"N = {size:,}" for size in sizes) + " |",
              "|---|---|" + "---:|" * len(sizes)]
    keys = sorted({(result.language, result.structure) for result in metric_results})
    return header + [table_row(metric_results, language, structure, sizes) for language, structure in keys]


def table_row(metric_results: list[Result], language: str, structure: str, sizes: list[int]) -> str:
    cells = [table_cell(metric_results, language, structure, size) for size in sizes]
    return f"| {language} | {structure} | " + " | ".join(cells) + " |"


def table_cell(metric_results: list[Result], language: str, structure: str, size: int) -> str:
    peers = [result for result in metric_results if result.language == language and result.books == size]
    matches = [result for result in peers if result.structure == structure]
    if not matches:
        return "–"
    text = formatted(matches[0])
    return f"**{text}**" if len(peers) > 1 and matches[0] in best(peers) else text


def winners(all_results: list[Result], comparison: Comparison) -> list[str]:
    header = ["| Metric | Language | Largest N | Best structure |", "|---|---|---:|---|"]
    metric_results = [selected(all_results, comparison, metric) for metric in comparison.metrics]
    return header + [winner_row(results_of_metric, language)
                     for results_of_metric in metric_results
                     for language in sorted({result.language for result in results_of_metric})]


def winner_row(metric_results: list[Result], language: str) -> str:
    own = [result for result in metric_results if result.language == language]
    largest = max(result.books for result in own)
    winners_at_largest = best([result for result in own if result.books == largest])
    return f"| `{own[0].metric}` | {language} | {largest:,} | {verdict(winners_at_largest)} |"


def verdict(winners_at_largest: list[Result]) -> str:
    structures = list(dict.fromkeys(result.structure for result in winners_at_largest))
    return f"tie: {', '.join(structures)}" if len(structures) > 1 else f"**{structures[0]}**"


def chart(metric_results: list[Result], comparison: Comparison, metric: str) -> str | None:
    try:
        import matplotlib
        matplotlib.use("Agg")
        import matplotlib.pyplot as plt
    except ImportError:
        return None
    languages = sorted({result.language for result in metric_results})
    figure, axes = plt.subplots(1, len(languages), figsize=(5 * len(languages), 3.5), squeeze=False)
    for axis, language in zip(axes[0], languages):
        plot_language(axis, [r for r in metric_results if r.language == language], comparison, language)
    figure.tight_layout()
    name = f"{slug(comparison.title)}-{metric}.png"
    figure.savefig(REPORT / name, dpi=120)
    plt.close(figure)
    return name


def plot_language(axis, language_results: list[Result], comparison: Comparison, language: str) -> None:
    for structure in comparison.structures:
        points = sorted((r.books, r.value) for r in language_results if r.structure == structure)
        if points:
            axis.plot(*zip(*points), marker="o", label=structure)
    axis.set_xscale("log")
    axis.set_title(f"{language}: {language_results[0].metric} ({display_unit(language_results[0])})")
    axis.set_xlabel("books")
    axis.legend()


def slug(title: str) -> str:
    return title.lower().replace(" ", "-")


def comparison_section(all_results: list[Result], comparison: Comparison) -> list[str]:
    heading = [f"## {comparison.title}: {', '.join(comparison.structures)}", "", "### Best structure", ""]
    sections = [line for metric in comparison.metrics
                for line in metric_section(selected(all_results, comparison, metric), comparison, metric)]
    return heading + winners(all_results, comparison) + [""] + sections


def metric_section(metric_results: list[Result], comparison: Comparison, metric: str) -> list[str]:
    if not metric_results:
        return []
    direction = "higher is better" if metric in HIGHER_IS_BETTER else "lower is better"
    lines = [f"### `{metric}` ({display_unit(metric_results[0])}, {direction})", ""] + metric_table(metric_results)
    image = chart(metric_results, comparison, metric)
    return lines + (["", f"![{metric}]({image})"] if image else []) + [""]


def report(all_results: list[Result]) -> str:
    heading = ["# Data structure comparison", "",
               "Generated from " + ", ".join(f"`{file.name}`" for file in sorted(RESULTS.glob("*.csv"))) + ".",
               "Best value per language and size in **bold**.", ""]
    sections = [line for comparison in COMPARISONS for line in comparison_section(all_results, comparison)]
    return "\n".join(heading + sections)


def main() -> None:
    all_results = results()
    if not all_results:
        sys.exit(f"No results in {RESULTS}; run the benchmarks first")
    REPORT.mkdir(parents=True, exist_ok=True)
    file = REPORT / "comparison.md"
    file.write_text(report(all_results), encoding="utf-8")
    print(f"Comparison written to {file}")


if __name__ == "__main__":
    main()
