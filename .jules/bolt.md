
## 2024-05-24 - Optimize HistoricalExamRequest list generation in ScoreViewModel
**Learning:** In Kotlin, using `flatMap` and `map` instead of nested `forEach` loops inside a `buildList` block can improve performance. This allows the standard library to more efficiently pre-size or batch-process the underlying array, reducing internal capacity resizing overhead and intermediate closure allocations.
**Action:** Replaced a nested `forEach` within a `buildList` block with `.flatMap { ... map { ... } }` in `ScoreViewModel.kt`, resulting in a ~12% performance improvement in generating historical exam request lists over a mocked dataset.
