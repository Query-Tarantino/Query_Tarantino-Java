import unittest

from comparison.report.displayed_value import DisplayedValue
from tests.results import result


class DisplayedValueTest(unittest.TestCase):

    def test_shows_bytes_as_megabytes(self):
        value = DisplayedValue(result("json", "disk_usage", value=822_357, unit="bytes"))

        self.assertEqual(("0.8", "MB"), (value.text(), value.unit()))

    def test_rounds_large_values_and_keeps_two_decimals_for_small_ones(self):
        self.assertEqual("27,992", DisplayedValue(result("folders", value=27_992.357)).text())
        self.assertEqual("0.45", DisplayedValue(result("book", value=0.449)).text())
