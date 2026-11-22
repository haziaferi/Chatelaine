# Stage 0 recognition spike

Evidence for `SPEC.md`, not a source of rules. The rules that rely on it are R19, R65, R66, R68, R73, R74, R75, R76, R77, R78 and R79 in `SPEC.md`. Measured 2026-10-28 to 2026-10-30; errors sorted by cause on 2026-11-03.

## Method

Measured on the owner's 20 photos: 10 single objects and 10 crowded drawers or boxes. The photos and the per-photo results are kept outside the repository.

Scores are the owner's blind marks: each answer line marked right, close or wrong without knowing which recogniser gave it. 8 of the 20 photos were made hard on purpose (odd angles, untidied things overlapping by more than half), all well lit and in focus. The offline pair scores the same on clear and hard photos (clear singles 4/8), so its weakness is the model, not the photos. Rows not in the blind review keep the earlier answer-sheet scores, which proved more lenient.

Rows marked tightened use instructions that ask for the most specific label name, counts one by one ("about N" when things overlap), and nothing that can't be seen. For drawers, each answer is scored on as many lines as the shortest answer for that photo, since extra lines are extra reading (owner, 2026-10-29; SPEC R66). The offline Qwen row compares it with Claude and Gemini on the same lines (N from those answers), so its Claude figure differs from the row above it. The two tuned Qwen rows add their answers to that comparison, which shortens some drawers' N slightly.

## Results

The bars are those of SPEC R65: singles 8 of 10 (the right name among the first three suggestions in at least 4 of 5 photos), crowded at least a third of the items found.

| Recogniser | Where | Singles (bar 8/10) | Crowded (bar 1/3) | Verdict |
|---|---|---|---|---|
| SmolVLM2-500M, Gemma 4 E2B (small multimodal models) | phone | 7/10 | 22–29% | fail: too slow (15–38 s) and inaccurate |
| YOLO-World v2-S alone (open-vocabulary detector) | phone | 1/10 | 18% | fail on names; boxes good |
| YOLO-World v2-S boxes + SigLIP 2 base naming each box | phone | 6/10 (8/10 counting close) | 26% (53% counting close); 60% of lines wrong | fails by the owner's marks; the best offline result on the phone |
| Gemini 3.5 Flash (large, online) | cloud | 10/10 | 88% | ceiling for comparison; not offline |
| Qwen3-VL-8B (large, online) | cloud | 10/10 (answer sheet) | not run | open-weight ceiling |
| Claude (large, online) | cloud | 10/10 | 65% (97% counting close) | names often broader than exact; not offline |
| Claude, tightened instructions (photos 1–16) | cloud | 10/10, counts 10/10 | 91% of lines right, none wrong | clears SPEC R65 widely; not offline |
| Gemini 3.5 Flash, tightened instructions (photos 1–16) | cloud | 10/10, counts 10/10 | 93% of lines right, none wrong | clears SPEC R65 widely; not offline |
| Gemma 4 E2B, tightened instructions | phone | about 3/10 | worse | fail: copies the instructions' examples; 18–49 s |
| YOLO-World + SigLIP 2, naming only confident boxes (cut 0.14) | phone | 6 right, 0 wrong, 4 "not sure" | 19% of lines wrong; names 37 of 125 boxes | never wrong on singles, but the user names most drawer items; cut tuned on these photos |
| Qwen3-VL-8B, tightened instructions, offline (photos 1–20) | PC, 5.75 GB | 9/10 (10/10 counting close), counts 9/10 | 75% of lines right, 5% wrong (tightened Claude on the same lines: 78%, 4%); 2.3 extra lines per drawer; 1 drawer of 10 failed (repeat loop) | near-cloud naming offline; fails SPEC R65 on time (about 48 s per photo on a desktop PC) and SPEC R19 on size |
| Same, with instructions tuned to its faults and a repeat check across lines | PC, 5.75 GB | 9/10, counts 9/10 | 63% right, 29% close, 8% wrong; 0.9 extra lines per drawer; no drawer failed (on the same lines: the row above 76% right, 2.7 extra; Claude 77% right, 1.0 extra) | shorter, no loops, but vaguer names; the earlier instructions name more exactly |
| Same, round 3 instructions plus only the anti-repeat rules | PC, 5.75 GB | 9/10, counts 9/10 | 58% right, 30% close, 12% wrong; 1.7 extra lines per drawer; no drawer failed | worse than both; every change to the instructions lost accuracy somewhere (owner: stop tuning, SPEC R73) |

Of the Qwen runs, the plain round 3 instructions (shared with the cloud models) stay the most accurate. A loop is better handled by the app (cut repeated lines, cap at 15; builder's call, SPEC R74) than by more rules, which the owner judged to confuse the model and lengthen the user's work (SPEC R73).

On these photos an open model running offline names things nearly as well as the cloud models; what it lacks is speed and a size that fits the phone. Two questions follow for stage 0: whether a smaller model of the same family (Qwen3-VL 2B or 4B) keeps enough of this on the Snapdragon 888 phone within SPEC R65 (SPEC R76), and whether the recogniser may be downloaded on first use like the speech model (SPEC R77, an owner decision).

## The best offline pair, as it would ship

A stage 0 candidate, not adopted: it fails SPEC R65 by the owner's marks.

- YOLO-World v2-S (GPL-3.0), weights stored in fp16: 28 MB. SigLIP 2 base image side (Apache-2.0), 8-bit weights with int8 maths: 109 MB. Name vectors for a fixed 1,356-name vocabulary: 4 MB. A reduced ONNX Runtime 1.30 with only the operators used: 14 MB. Total 155 MB raw, 138 MB compressed (SPEC R19).
- Time per photo: Snapdragon 888 phone 1.8–6.3 s, Snapdragon 695 phone 3.1–8.9 s; none of the 20 over 10 s. Accuracy identical to full precision.
- Weak spots: counts (3/10 right), crowded drawers (26% found, and most lines wrong, so correction would cost more than it saves). SigLIP 2's text side (about 1.1 GB) is not shipped: user tags and search by meaning need the Similarity model instead, or a later decision.
- Rejected on the way: dynamic int8 (broke SigLIP), 4-bit weights (lost accuracy), MobileCLIP (Apple research-only licence), Qualcomm's ready-made 8-bit file (no smaller outside their NPU).

## Errors by cause

Sorted on 2026-11-03. Asked whether cluttered backgrounds are a big problem (the photos are of real drawers and boxes, not things on a plain backdrop), every line the owner marked close or wrong, every line naming the container, and every answer-sheet group missed were sorted by cause after looking at each photo (per-line tags kept outside the repository).

The background caused few of them: Claude (tightened) 3 of 24 problem lines and 2 of 4 misses; Gemini (tightened, photos 1–17) 0 of 7 and 1 of 1; Qwen3-VL-8B offline 8 of 53 and 1 of 6; YOLO-World + SigLIP 2 15 of 105. The rest were wrong or broader names, wrong counts, Qwen's repeated lines (20 of its 53) and names read off the picture or words printed on a package (5 lines).

- Things that blend in (dark among dark, the same colour as what is behind, see-through) were the main background cause and every background miss. Misses cost the most to fix (typing a line).
- Things outside the drawer: one line from the large models (a package standing beside the box); a few from the detector.
- The container itself named as a stored thing (bin, shelf, the drawer's knobs): Qwen 3 lines, the detector 5. True, but one line each to untick.
- So recognition gets no cut-out (segmentation) stage (SPEC R75): a crop step would have removed about one line on these photos, and low-contrast things are hard for cut-out models too; light (a torch button on Capture) fits the cause better.
- Caveats: all 20 photos were well lit, so dim light is untested; misses are judged against an answer sheet written before any model ran and not checked by the owner; the detector's boxes were not saved, so 6 of its tags are likely, not seen.
