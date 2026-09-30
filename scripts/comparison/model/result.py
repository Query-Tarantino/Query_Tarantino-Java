from dataclasses import dataclass


@dataclass(frozen=True)
class Result:
    language: str
    structure: str
    metric: str
    books: int
    value: float
    unit: str
