import unittest

from comparison.report.tables.metric_table import MISSING, MetricTable
from tests.results import result


class MetricTableTest(unittest.TestCase):

    def test_has_one_column_per_size_and_bolds_the_best_value(self):
        lines = MetricTable([result("time", books=100, value=74), result("book", books=100, value=0.45),
                             result("book", books=1000, value=0.8)]).lines()

        self.assertEqual("| Language | Structure | N = 100 | N = 1,000 |", lines[0])
        self.assertIn("| java | book | **0.45** | 0.80 |", lines)
        self.assertIn(f"| java | time | 74.00 | {MISSING} |", lines)
