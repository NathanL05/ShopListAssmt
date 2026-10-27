# Refactor: shared double-tap navigation — success criteria and baseline

Baseline recorded 2026-10-27 at commit `830d73a` (main), before any code change.
Device: `emulator-5554`. Gradle run with `sh gradlew` (the wrapper is not executable in this checkout).

## Success criteria

The refactor succeeds only if every criterion holds.

| # | Criterion | How measured | Baseline | Target |
|---|-----------|--------------|----------|--------|
| C1 | Behaviour unchanged | `connectedDebugAndroidTest` | 7/7 pass (6 `CategoryNavigationTest` + 1 example) | 7/7 pass |
| C2 | Unit tests and build still pass | `assembleDebug testDebugUnitTest` | build OK, 1/1 pass | build OK, 1/1 pass |
| C3 | Tests not weakened | `git diff --stat app/src/androidTest app/src/test` | — | no changes |
| C4 | Gesture logic lives in exactly one place | `grep -rlF` for `SimpleOnGestureListener`, `onDoubleTapEvent`, `findChildViewUnder`, `firstTapPosition`, `findViewById<ViewPager2>` in `app/src/main` | 2 files (both fragments) | 1 file (the new helper); 0 in either fragment |
| C5 | Identical duplicated lines removed | non-blank lines shared by the two fragments' gesture blocks | 29 | 0 |
| C6 | Fragments shrink | `wc -l` | Plan 164, Shop 87 | each at least 25 lines shorter (Plan ≤ 139, Shop ≤ 62) |
| C7 | Net code decreases | `cat app/src/main/**/*.kt \| wc -l` | 1358 lines in 11 files | < 1358 lines (12 files) |
| C8 | No new compiler warnings | count of `w:` lines in the build log | 12 total; 4 are `GestureDetectorCompat` deprecations in the two fragments | ≤ 12 total; `GestureDetectorCompat` warnings ≤ 2 |
| C9 | No new lint problems | `lintDebug` report | 5 errors (all `UseAppTint` in layout XML, pre-existing), 93 warnings; 4 `ClickableViewAccessibility` in the two fragments | same 5 errors; ≤ 93 warnings; `ClickableViewAccessibility` ≤ 2 |
| C10 | Scope contained | `git diff --stat` | — | only the two fragments plus one new helper file |

Behaviour-specific nuance to preserve (covered by C1): Plan only navigates when the row is a category heading (not the "Add…" placeholders); Shop navigates on any row. The first tap and the second tap must land on the same row.

## Baseline detail

### Size

| File | Total lines | Non-blank, non-comment lines | Gesture block |
|------|-------------|------------------------------|---------------|
| `PlanCategoriesFragment.kt` | 164 | 140 | lines 81–118 (38 lines) |
| `ShopCategoriesFragment.kt` | 87 | 68 | lines 39–74 (36 lines) |

Gesture blocks compared line by line (whitespace trimmed): 29 identical non-blank lines, 8 differing lines.

### Duplication markers (occurrences in `app/src/main`)

| Pattern | Occurrences | Files |
|---------|-------------|-------|
| `GestureDetectorCompat` | 4 | both fragments (import + use) |
| `SimpleOnGestureListener` | 2 | both fragments |
| `onDoubleTapEvent` | 4 | both fragments (comment + override) |
| `findChildViewUnder` | 2 | both fragments |
| `firstTapPosition` | 6 | both fragments |
| `findViewById<ViewPager2>` | 2 | both fragments |

### Tests

`connectedDebugAndroidTest` on emulator-5554 — BUILD SUCCESSFUL, 7 tests, 0 failures, 0 errors, 0 skipped:

- `shopPages_afterRecreationInPlanMode_areShopPages`
- `summaryDoubleTap_afterCategoryCountChanges_opensTappedCategory`
- `summary_tapsOnTwoDifferentRows_doNotNavigate`
- `planCategories_tapsOnTwoDifferentRows_doNotNavigate`
- `summary_doubleTapOnOneRow_opensThatCategory`
- `planCategories_doubleTapOnOneRow_opensThatCategory`
- `ExampleInstrumentedTest.useAppContext`

`testDebugUnitTest` — 1 test, 0 failures. `assembleDebug` — succeeded.

### Static analysis

- Kotlin compiler: 12 warnings. 4 are `GestureDetectorCompat` deprecations (2 per fragment) and 2 are `adapterPosition` deprecations in `PlanCategoriesFragment` (drag code, out of scope).
- Lint: `lintDebug` **fails at baseline** with 5 errors, all `UseAppTint` in `item_add.xml` / `item_shopping.xml` (unrelated to this refactor), plus 93 warnings. 4 warnings are `ClickableViewAccessibility` on the fragments' `setOnTouchListener` calls (Plan line 115, Shop line 71).

## Commands to re-measure

```bash
sh gradlew --console=plain clean assembleDebug testDebugUnitTest lintDebug
ANDROID_SERIAL=emulator-5554 sh gradlew --console=plain connectedDebugAndroidTest
wc -l app/src/main/java/com/mmgpt/shoplistgpt/{Plan,Shop}CategoriesFragment.kt
grep -rlF onDoubleTapEvent app/src/main
```
