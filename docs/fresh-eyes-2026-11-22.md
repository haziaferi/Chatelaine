# Code review, 2026-11-22

Scope: `tools/*.py` (the leak check and the four mockup scorers) and `.github/workflows/publish-check.yml`, read against SPEC.md after the move to rule ids R01 to R108.

Method: the necessity checker (slopcheck) and a by-hand read for duplication, dead code and untrue comments; an independent necessity review; a code review; a fresh-eyes run (sonnet reviewer, opus verifier, 11 chunks, each with the SPEC rules its tool serves: 28 findings, 3 narrowed, 1 refuted).

The four scorers stay local (listed in `.gitignore`): their task set is read off the owner's photos.

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

Owner's decision: none of these may be committed: the photos, the harvest ids, file paths in other projects, and the `MID-` ids.
- No photo is tracked; the photos stay outside the repository, as `docs/evidence/stage0-recognition.md` already says.
- The scorers no longer cite harvest ids, other projects' file paths or `MID-` ids. Each docstring now names its axis in plain words. The mapping from each function to the id it ports moved to the private folder outside the repository, next to the harvest catalogue. Afterwards the scorers' outputs are unchanged, apart from two report lines that dropped an id.

Owner's decision: the R96 checks move to match R96, which is in the owner's own words.
- Two classification faults fixed first. `purpose()` put the back arrow (40 dp) and the photo thumbnail (44 dp) in one "header" kind. A word chip, whose word sits in a child element, was sized as if it had no label, so its width was compared.
- *Same size.* The check used to pass at a score of 0.95 over the flow. It now tests each element: every instance within 1 px of its purpose's median (the tolerance the layout skill uses for pinned controls), on every screen of the flow, the mid-correction screen included. D's plain text box is left out, because its height is the room the keyboard leaves. On the newest renders every variant passes.
- *Look-alikes.* The check runs on every screen of the flow, the PC screen included, on every purpose, and skips the phone's own selection bar. A, B and C still fail, with two more label pairs off the lines: the "+" for one more against "+ Add", and "+ Add" against the control that adds a word. D still passes.
- The two fixed faults also move the old diagnostics on axis 8 (consistency) slightly. Place's old-measures total for "picture" goes from 0.427 to 0.490, and the ranking does not change. The new axes' scores are unchanged.

## Needs a decision

These wait for the design pass, where design decisions are made (owner). The mockups' failing R96 look-alike pairs (A, B and C) are design work for the same pass.

1. **A neutral task set.** The task set in `score_taps.py` is read off the owner's photos, so it counts as the photos (owner). The four scorers are now listed in `.gitignore` and stay local. They could be published only with a neutral task set, which means re-rendering the mockups, because `task_model.py` looks the words up in the renders.
2. **R91's acceptance** reads a control scoring well as proof that the axes are wrong, so no outcome can fail the purpose map (found by the fresh-eyes run on R91's text, not the code).
3. **`phone_pc_parity`** returns 0 wrapped lines when phone and PC have different line counts, which reads as "no difference". This changes the score, so it is not fixed.
4. **`read_look`** drops malformed rating lines without a word, so a typo shows the look as "pending". The options are a warning on stderr, or leaving it as it is.

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

**Resolved (2026-11-22).** The items above, except the last, were fixed in the shared source, each with a test that fails on the earlier version. `tools/publish_check.py` here is the fixed copy. The independent review of that fix found more, and these were fixed too:
- skip-worktree and assume-unchanged files, symlinks and UTF-16 text went unscanned;
- `--install-hook` could overwrite a short hook of the owner's.

Still open: commit and tag messages are not scanned, and the copy here can still drift from the shared one (the last item above).

The workflow had no findings: its action is pinned by hash, the token is read-only and credentials are not kept.

## Withdrawn

- "R22 says the check runs before each commit and in CI, but only a pre-push hook exists". Refuted: `.git/hooks/pre-commit` and the workflow both run it. The reviewer's chunk had neither file.
- "R80's 'adjusting a line is quicker than deleting wrong ones' contradicts a one-tap untick". R80 compares editing one lumped line with deleting the wrong lines of a split list; an untick drops one wrong line. Those are not the same comparison.
- "No rule records the scorers' axes or the old ten-axis diagnostics". The screen-axis harvest skill records them in its catalogue and recommendations, which are kept privately with the workshop notes. A SPEC rule is not needed (owner).
- One empty finding ("not a finding; skip"), refuted by the verifier.

## Not reviewed

- `render_mockups.py` and the renders, which are outside the repository. The probe fields were checked against the code that reads them, not against the code that writes them.
- `--look` scoring: no ratings file was found, so the look axis ran as "pending".
- `edge_density`'s PIL ring mode is kept for the self-test's comparison with the source, although the scorer uses only the inner mode.
