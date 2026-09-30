from dataclasses import dataclass

from comparison.model.result import Result

BYTES_PER_MEGABYTE = 1_000_000


@dataclass(frozen=True)
class DisplayedValue:
    result: Result

    def text(self) -> str:
        if self.result.unit == "bytes":
            return f"{self.result.value / BYTES_PER_MEGABYTE:,.1f}"
        return f"{self.result.value:,.0f}" if self.result.value >= 100 else f"{self.result.value:,.2f}"

    def unit(self) -> str:
        return "MB" if self.result.unit == "bytes" else self.result.unit
