import tempfile
import unittest
from pathlib import Path

from comparison.adapters.csv_results_source import CsvResultsSource
from comparison.model.result import Result

CSV = "language,structure,metric,n_books,value,unit\njava,time,write_throughput,100,314.542,books/s\n"


class CsvResultsSourceTest(unittest.TestCase):

    def test_reads_every_result_file_of_the_directory(self):
        with tempfile.TemporaryDirectory() as directory:
            Path(directory, "java-crawler.csv").write_text(CSV, encoding="utf-8")
            source = CsvResultsSource(Path(directory))

            self.assertEqual([Result("java", "time", "write_throughput", 100, 314.542, "books/s")], source.results())
            self.assertEqual(["java-crawler.csv"], source.origins())
