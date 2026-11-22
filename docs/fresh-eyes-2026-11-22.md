# Code review, 2026-11-22

Scope: `tools/*.py` (the leak check and the four mockup scorers) and `.github/workflows/publish-check.yml`, read against SPEC.md after the move to rule ids R01 to R108.

Method: the necessity checker (slopcheck) and a by-hand read for duplication, dead code and untrue comments; an independent necessity review; a code review; a fresh-eyes run (sonnet reviewer, opus verifier, 11 chunks, each with the SPEC rules its tool serves: 28 findings, 3 narrowed, 1 refuted).

Nothing here is committed. The four scorers are still untracked.

## Fixed

Every tool's output was compared before and after the fixes, on the newest renders: the self-test, `score_taps.py` and `task_model.py` print the same output, and so does `score_screens.py`. In its JSON, only the two changes listed at the end of the score_screens part differ.

borrowed_axes.py
- Docstring: the SPEC rule (R96) is named; the probe fields listed now include the ones the code reads (viewport, ctx, lh, words); "each function names its axis id" now says "each axis function"; a private folder's name is gone.
- `png_read` rejects interlaced PNGs, which used to decode into wrong pixels without an error; files are closed after reading.
- The gap from a leaf to the leaf below was computed in three places; it is now `gaps_below`. sRGB linearisation was written three times; it is now `_lin`.
- Removed code nothing called: `token_count_flat`, `clutter_score`. `row_targets` returns only the mean, the one value its caller reads.
- Comments made true: the edge ring adds 2(W+H)-4 pixels, not 2(W+H); "kinds without a text label" means labels of one glyph or none; a label pair needs only one of the two to be a control; the contrast background includes the text's own element; a self-test comment said two chromes were the same where the test shows they differ.
- Banner comments made plain; date stamps replaced by the rule (R96) or dropped.

score_taps.py
- "SPEC D-37" (gone in the migration) now reads R79, R80. "Each with or without suggestions" now says A, B and C, which is what the code does.
- Two copied path blocks are shared (`TEXT_EDITS` for A and D, `WORD_EDITS` for B and C).
- `typical_mix` returns task ids. task_model used to find tasks by list identity (`s is steps`).

task_model.py
- "SPEC D-37" now reads R79. The docstring says a tap on another line ends the attempts only when it changes that line, as the code does.
- `BANNER_CANCEL_W` was 74 for a 70 dp button plus a 4 dp gap; it is now 70, with the gap as its own constant. The banner uses the probe's viewport width, not a hard-coded 412.
- Removed code nothing used: the `"sum"` target kind, `play`'s `start` and `ret_at`, `variant`'s `theme`, and the `GEO` table, which mapped each letter to `identify-<letter>`.

score_screens.py
- Bug: "consistency across phases" was computed over two different screen lists, one in the scored measure and one in the pass/fail check (the check left out identify-C for Adding a word). There is now one `flow()`. Effect on the newest renders: Adding a word's component sizes go from 0.98 and 0.99 to 1.00. Both variants passed before and still pass.
- Docstring: the look tolerance is 0.05, not 0.01. Date stamps dropped. "My call, for the owner to check" now says the ladder is not yet confirmed with the owner.
- The contrast message says "every app text", because the phone's own overlay is skipped. A comment explains why the 40 dp floor is tested at 39.5.
- `report_new` gave tied totals different ranks; both reports now share `ranks()`, which shows ties as ties. The current totals have no ties, so the output is unchanged.
- One copy each of "C's lines that fit in a list" (was three) and the hidden-gestures list (was two). `SYSTEM` no longer tests for the keyboard, which `Screens.probe` already removes. `score_new`'s unused `sigma` parameter is gone. Files are closed after use.

Repository
- `.gitignore` now ignores `__pycache__/`; `git add tools/` would have published `.pyc` files.

## Needs a decision

1. **Publishing the scorers (R22).** These files are untracked; committing them makes them public. Before that:
   - `score_taps.py` task 1 uses an intimate clothing item, of the kind R80's example replaced. Rewording it means re-rendering the mockups, because `task_model.py` looks the word up in the renders.
   - The docstrings cite harvest ids (short prefixes naming the source projects), and `borrowed_axes.py` cites file paths in another project. The rule is to decide before committing. Options: keep them, replace them with neutral axis names, or move the mapping into the private catalogue.
   - `task_model.py` and `score_screens.py` cite `MID-` ids that no document in the repository defines.
2. **R96's scope.** R96 asks for no look-alike pairs "on every screen's render". The check runs on each variant's own screens (at rest and mid-correction), and only on a line's parts (keep, count, name, line). Either widen the check or narrow R96's acceptance.
3. **R96's "same size".** The consistency check passes at 0.95 of 1, but R96 sets no tolerance. Record 0.95 in R96, or tighten the check.
4. **Which rule the scorers' axes serve.** Time, clean drawers, lines in view, look and the five checks match R79's typical drawer and R96. No rule records the axes or the old ten-axis path that `score_screens.py` keeps as diagnostics. R91's axes are the purpose map's. Record them in SPEC, or delete the old path.
5. **R91's acceptance** reads a control scoring well as proof that the axes are wrong, so no outcome can fail the purpose map (found by the fresh-eyes run on R91's text, not the code).
6. **`phone_pc_parity`** returns 0 wrapped lines when phone and PC have different line counts, which reads as "no difference". This changes the score, so it is not fixed.
7. **`read_look`** drops malformed rating lines without a word, so a typo shows the look as "pending". The options are a warning on stderr, or leaving it as it is.

## publish_check.py (shared copy: reported, not changed)

`tools/publish_check.py` is identical to the shared copy that the hooks run. A change belongs in the shared source.
- The email rule exempts every `@anthropic.com` and every `noreply@` address, and "example.*" covers only .com, .org and .net. The identity check uses the same list, so an example.com author passes.
- `--history` skips a blob whose hash is still in the index and scans the working-tree file instead. A committed blob that was changed or deleted in the working tree, but not staged, is never scanned.
- The current `.publish-allow` is skipped by every rule, the private-word check included.
- The EXIF check looks for a tag's two bytes anywhere in the block instead of walking the IFD, so any matching bytes are reported.
- Text files over 8 MiB are skipped without a note, but still counted as scanned.
- `git()` ignores git's exit status. If git fails, the file list is empty and the check passes. A short `cat-file` header ends the history scan without a word.
- `scan_file` has an unused `repo` parameter; `--install-hook` installs only the pre-push hook (this repo's pre-commit hook was added by hand).
- The hooks run the shared copy, while R22 and CI name `tools/publish_check.py`. The two are identical today but can drift apart.

The workflow had no findings: its action is pinned by hash, the token is read-only and credentials are not kept.

## Withdrawn

- "R22 says the check runs before each commit and in CI, but only a pre-push hook exists". Refuted: `.git/hooks/pre-commit` and the workflow both run it. The reviewer's chunk had neither file.
- "R80's 'adjusting a line is quicker than deleting wrong ones' contradicts a one-tap untick". R80 compares editing one lumped line with deleting the wrong lines of a split list; an untick drops one wrong line. Those are not the same comparison.
- One empty finding ("not a finding; skip"), refuted by the verifier.

## Not reviewed

- `render_mockups.py` and the renders, which are outside the repository. The probe fields were checked against the code that reads them, not against the code that writes them.
- `--look` scoring: no ratings file was found, so the look axis ran as "pending".
- `edge_density`'s PIL ring mode is kept for the self-test's comparison with the source, although the scorer uses only the inner mode.
