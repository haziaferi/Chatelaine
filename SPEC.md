# Chatelaine — specification

Revision: SPEC v0.2 (draft), 2026-10-30. No code yet.
Package: `com.hazia.chatelaine`. Licence: GPL-3.0. Repository: github.com/haziaferi/Chatelaine.

Companion document: [docs/purpose-map.md](docs/purpose-map.md), which decides which purpose each screen serves before any screen is built.

Entries marked *(builder's call)* were settled during drafting without the owner and can be overturned.

## 0. Intent

A clear plastic box keeps its contents in mind with no effort: every glance is a reminder. A cardboard box, even labelled, needs a deliberate lookup and a mental link from label to object. That costs effort at the very moment a task starts and puts load on working memory. This "out of sight, out of mind" effect is strong for many people.

Chatelaine is a home inventory that does what the clear box does. It records what is stored and where, and it puts stored things back in front of the user without being asked. It also helps with the first big tidy: things can be logged before they have a place, grouped by what they are for, gathered, placed, and photographed once they are where they belong.

The name: a chatelaine ran a household and carried its keys and small tools on a chain at her belt, always in view.

## 1. Owner decisions (2026-10-27/28)

| # | Decision |
|---|---|
| D-01 | One user. Two devices: an Android phone and a Windows PC. |
| D-02 | Android and Windows: Kotlin Multiplatform with Compose Multiplatform. |
| D-03 | Android minSdk 30 (Android 11). |
| D-04 | Interface and dictation in Italian and English from the start; the user can switch language independently of the phone. |
| D-05 | Start from scratch; borrow code file by file from the GPL-3.0 / MIT / Apache projects listed in §12, with licence headers kept and listed in `THIRD_PARTY.md`. |
| D-06 | Places nest freely, to any depth: drawer partition › drawer › dresser › room › floor › house. Several houses (sites) are allowed. |
| D-07 | Optional quantity per item; consumables and low-stock handling to be discussed later. Suggested counts are approximate by default ("about 5") and always editable; a setting, "accurate amounts", asks for exact counts instead. Exact counts matter mainly where items link to shopping; an estimate first keeps logging quick. *(owner, 2026-10-29)* |
| D-08 | User-defined custom fields. Full or near-full customisation is a main focus of the app. |
| D-09 | An item can be "in use / away": taken out of its place and not yet put back. |
| D-10 | Lending ("who has it") comes in a later stage. |
| D-11 | Value, receipts and serial numbers (insurance use) are out of scope for v1. |
| D-12 | One organising concept: **tags**. Items and places can carry several tags. A tag can sit inside a parent tag ("red skirt" inside "sewing"). A tag can be marked **kept together**, meaning its things share a home place. The homepage's tag card shows top-level tags only. |
| D-13 | Item, place and tag information that a model suggests is always fully editable, one at a time or in bulk. |
| D-14 | Image recognition is core: a small model ships with the app, a larger, more accurate one is an optional download. Recognition runs on both devices. |
| D-15 | An optional on-device language model for tag suggestions. Target phones as in D-29 (to be measured, §9 stage 0). |
| D-16 | Fully offline. The network is used only for opt-in model downloads. F-Droid-compatible build is optional. |
| D-17 | Phone and PC share one inventory through **two-way sync over a folder** kept in step by a file-sync tool such as Syncthing. Each device writes its changes as separate files and merges the other device's. |
| D-18 | Backup: automatic backup to a folder the user picks, plus manual export (JSON and photos). |
| D-19 | App lock: biometric, PIN or password; not in v1. |
| D-20 | The app never pushes by default. Proactive prompts (notifications) are opt-in; the clear-box effect comes from passive surfaces. |
| D-21 | Labels: printable A4 sticker sheets (PDF) with a photo collage and a QR code. NFC tags later. |
| D-22 | Clear-box surfaces, by owner rank (3 = most helpful): home-screen widget 3, in-app photo browsing 3, printed photo labels 2, lock screen 1, "rediscover" prompt 1. |
| D-23 | Visual map of storage: the app draws stylised places automatically from the place hierarchy, as blocks the user can rearrange. A free-drawing editor may come later. |
| D-24 | Look and feel: a layout specific to the app's purpose, not framework defaults. Calm, friendly, low visual noise, relaxing to look at even in greyscale; shapes guide the user. Density and the hierarchy of visual function are the leading axes. Workshopped later; the owner's other apps are conceptual references, not templates. |
| D-25 | Reach and density follow the model of the owner's earlier app, not that of another of the owner's apps: not everything visible at once. |
| D-26 | No screens at stage 0; screens start at stage 2 or later, each discussed with the owner before it is built. |
| D-27 | The purpose map aims to minimise the number of screens and maximise their quality. Other apps serve as baselines to improve upon or as controls that must lose when scored. Scoring axes are agreed with the owner before scoring. |
| D-28 | The Windows PC is a **desk companion**: everything except taking photos and dictation. It runs recognition on photos taken with the phone and handles bulk editing, the place map, label printing and backup. The owner's PC: a 6-core desktop processor, 16 GB of memory, a 4 GB graphics card. |
| D-29 | Recognition pass bar, measured on a fixed set of about 20 photos taken for the purpose (kept out of the repository). On both target phones, a Snapdragon 888 flagship and a Snapdragon 695 budget phone (the slower one sets the bar): at most about 10 s per photo; no crash or overheating across the set; single objects (S2): the right name among the first three suggestions in at least 4 of 5 photos; crowded boxes (S4): at least a third of the items found, and correction quick enough for the rest. *(Amended 2026-10-29: the Snapdragon 845 phone, the first target, is too old for this project.)* |
| D-30 | Search matches across languages: "cucito" finds "sewing". *(builder's call, from the owner's answer: tags are stored once, as written; model suggestions are offered in the interface language; built-in names such as place and tag kinds switch with the language.)* |
| D-31 | The phone app's download stays under about 150 MB *(owner, 2026-10-29: download size, compressed, not installed size)*; the speech model is downloaded on first use of dictation. The small recogniser and the text-similarity model ship with the app. Limits to be refined in later stages. |
| D-32 | No existing inventory data to import; no importer in stage 1. |
| D-33 | One app on one shared core. **Wardrobe** and **Pantry** are optional modules, off by default. A module = a preset set of custom fields on its tags + a few views of its own + its own purpose-map entries. Users can build their own packs with the same tools. |
| D-34 | Purpose-map scoring axes (O-01), equal weights, ties shown as ties. **Choosing the purpose set:** A1 coverage (every reference-app function and every S1–S7 step lands in a purpose), A2 exclusivity, A3 balance, A4 fit to Chatelaine's intent, A5 potential improvement over the reference apps. **Choosing screens and homes:** B1 screen count (fewer is better), B2 one home per thing (repeats count against), B3 steps per scenario (taps from the homepage through S1–S7), B4 recall load (steps that ask the user to remember what the screen doesn't show). Reference apps are controls: a control that scores well means the axes are wrong. Layout axes are set per screen from stage 2. |
| D-35 | The homepage is a master board: shortcuts on it (the two cards, object of the day) are not counted as repeats under B2. |
| D-36 | Stage 0's change-file-sync spike is replaced by a review of the sync of the owner's earlier app (done 2026-10-28, §5.6). The KMP spike **stays**: the review found the earlier app has no Compose and no runnable Windows app, so a Compose Multiplatform window running on Android and Windows must be proven. The model spike also stays (on the D-29 phones). |
| D-37 | *(owner, 2026-10-30)* When recognition is approximate, the logging form carries the load: the form for logging objects and clusters from a photo is designed around quickly editing the recogniser's answer (a list of "about N x name" lines, some right, some close, some wrong). The stage 0 scores show most misses are *close* names rather than wrong ones, so turning a close line into the right one, fixing a count, and dropping a wrong line should each be quick. The screen itself is designed with the owner (purpose map, D-34) before it is built. **Lump, then refine** *(owner, same day)*: a good generic name with an approximate count beats a split list, because adjusting a line is quicker than deleting wrong ones. "3 x umbrella" is better than "2 x umbrella" plus "1 x purple umbrella"; "about 20 x notebook" is easy to judge true at a glance. Each line is editable in a few taps: change the number, drop or add a word, remove "about" (owner's example: "about 3 x wool scarf" → +1, +1, drop "about", drop "wool" → "5 x scarf", save). When a split is worth making, it follows what matters for the things' use, not colour (notebooks: "about 9 x small notebook", "about 10 x notebook", since size matters for note-taking). |

## 2. Goals, non-goals, anti-goals

**Goals**
- Stored things stay present in mind without effort (passive surfaces, §7).
- Logging costs at most a word, a dictated phrase or a photo. Everything else can come later.
- Finding a thing takes one search or one walk through the place map.
- The first big tidy is supported end to end: log, group, gather, place, photograph.
- The user can shape the app to their own logic: tags, fields, place structure, what the homepage shows.

**Non-goals (v1)**
- Insurance records, values, receipts (D-11).
- Low-stock alerts, shopping lists and expiry tracking outside the Pantry module (D-07, D-33).
- Several users or shared households.
- Cloud accounts or any server.

**Anti-goals** (failure even if it technically works)
- Streaks, guilt or nagging wording, or reminders the user did not ask for.
- A busy, everything-at-once interface.
- A stock Material look.
- Any model guess that cannot be corrected, or that overrides a user correction.
- Data loss from sync, backup or a corrupted database.

## 3. Acceptance scenarios (owner, 2026-10-28)

The app is working when these all succeed. Each stage in §9 names the scenarios it unlocks.

- **S1 Project from a tag.** Search the tag "sewing"; get the items with that tag; select the ones needed for a new project and add the tag "red skirt" to them in one action; later find them quickly by "red skirt".
- **S2 Photo with suggestions.** Photograph three headphones. The app recognises headphones and the count of three, and suggests tags such as "technology", "music", "phone", "PC". The user deletes "PC" and keeps the rest, which are saved with the item.
- **S3 Bulk clear.** Photograph a full drawer of mixed objects. The app recognises some or all and suggests tags. The user clears all suggested tags in one action and adds their own, "junk drawer".
- **S4 Box split into items.** Photograph a box of many items, some overlapping or partly hidden. The app recognises some and gets others wrong. The user corrects the wrong ones and saves each as a separate item, not one box, so they can be repositioned and rephotographed later.
- **S5 Before buying.** Open the bathroom cabinet under the sink in the app and see which cleaning supplies are there, so the scale remover isn't bought twice.
- **S6 Visual place map and filtered search.** Build a small visual map from drawer partitions up through dresser, shelf, closet, cabinet, room, floor and house, with several houses. Select a house or floor and search "shoes" to filter results by place. If there's no result there, a short list follows with the same search's results from other places.
- **S7 Seasonal swap.** Having grouped the winter clothes with the app, swap the closet at the change of season, then group and organise the summer clothes being put away in the same way.

## 4. Platforms and constraints

- Kotlin Multiplatform + Compose Multiplatform; targets Android (minSdk 30) and Windows desktop (JVM).
- Code shared by default; platform code only for camera, file access, widgets, speech, model runtimes and printing.
- Fully offline (D-16). No Google Play services in the core.
- Every model runs on device. Models larger than the base set are downloaded on request, never bundled.
- Storage: a local database per device *(builder's call: SQLDelight or Room KMP; to be chosen in stage 1)*. A database that fails to open is kept, never deleted automatically.
- Publishing rules: no personal data in the repository; the publish check runs before each commit and push and in CI.

## 5. Concepts and data model

### 5.1 Item
A thing the user owns. Fields: name, tags, notes, quantity (optional, D-07), custom fields (D-08), photos (optional), place (optional), state.

An item exists as soon as it has a name. Tags, place and photo are **independent facts that can come in any order**:
- logged by name only, with no place or photo yet ("unplaced");
- photographed first, then named;
- placed in a settled area that was photographed as a whole (§5.4).

Item states: unplaced · placed · in use / away (D-09) · lent (later, D-10). The "photo outdated" flag is separate from state and is raised when an item, or the place's contents, changed after the last photo.

A set of identical things can be one item with a quantity ("headphones ×3"), and it can be split into separate items later *(builder's call)*.

### 5.2 Place
A storage location in a tree of any depth (D-06): site (house) › floor › room › furniture › compartment › partition, with the user free to skip or add levels. A place can carry tags, notes, a photo and a position in the visual map (D-23). Each level has a kind (house, floor, room, furniture, container, partition, other) that sets how it's drawn; kinds are user-editable.

### 5.3 Tag
The single organising concept (D-12).
- Items and places carry any number of tags.
- A tag can have a parent tag, which forms a tree. *(builder's call: a single parent per tag; tags otherwise flat.)*
- A tag can be marked **kept together**; it can then have a home place, and the app can say which of its items are away from that home.
- A tag can carry a kind for display: function, project, season, place-like, other. *(builder's call; user-editable.)*
- Searching a parent tag finds items carrying any of its child tags too.
- Model-suggested tags are stored as suggestions until accepted; accepted and user-made tags look the same afterwards.

### 5.4 Photo
A photo belongs to an item or a place. A **place photo** of a settled area can have regions that point to the items they show (Spot It–style marked spots). Recognition can propose items from a photo (S2–S4). Each proposal is accepted, corrected or discarded, and a photo can be split into several separate items (S4).

### 5.5 Customisation
- A **tag** says what kind of thing something is; a **field** holds a value that differs from item to item (size M, expires March 2027).
- Field types, sized for both modules (D-33): text, number with a typed unit, date, choice list, checkbox, link to another item or place. Fields can go on items and on places.
- A field can be attached to a tag, so every item carrying it gets the field ("clothes" → size), or added to a single item *(builder's call: both)*. Final list settled with the owner (O-06).
- Place kinds, tag kinds, homepage cards and the widget content are user-editable.
- How far customisation reaches is workshopped per screen; D-08 sets the direction.

### 5.6 Sync (D-17)
- Every record has a globally unique id and a modification history.
- Each device appends its changes as small files in a shared folder; the other device reads and merges them. No database file is ever shared directly, because file sync corrupts live databases.
- When both devices edited the same field: the later edit wins *(builder's call)*, and the overwritten value stays in history so it can be restored.
- Photos travel as files in the same folder, named by id.
- Sync works with any folder-sync tool. The app doesn't talk to Syncthing itself.
- A backup carries each value's sync stamps, and restoring it **merges** like any other device's changes: edits newer than the backup survive on both devices. Rolling everything back to a backup is a separate, deliberate action; it is recorded as a change, and the state it replaced stays in history *(builder's call, following the lesson from the owner's earlier app that a stamp-less backup overwrites every device)*.
- The folder sync of the owner's earlier app (unpublished) is the owner's own code and the starting point (review 2026-10-28). Its `:core` engine (ULID ids, hybrid logical clock stamps, field groups, merge with a permanent purge registry) is reused almost unchanged. Its folder layer (16 KiB per-device log segments, snapshots every 64, atomic writes, Syncthing conflict copies merged, waiting rows for missing parents, outbox, photos stored by SHA-256 under `blobs/`) is adapted: database and group sets passed in, sync tables moved into Chatelaine's schema, new row adapters for items, places, tags and photos.
- Differences from the earlier app that Chatelaine needs: **one sync group per user-visible field** (the earlier app defaults to one group per row), and **every user-visible group keeps the losing value** (the earlier app discards it except for a few groups), so §5.6's history rule holds. Full-backup merge and the rollback action are new; the earlier app built only a settings backup.
- Lessons adopted from the earlier app: ignore a replayed version no newer than the base; keep the device-id file out of Android Auto Backup; export through an outbox, not a stamp cursor; the purge registry is permanent (90-day tombstones resurrected rows in another of the owner's apps); a recovered database re-reads its own folder files.

### 5.7 Personal learning
The user's sorted items act as labelled examples: a new item's nearest neighbours carry the user's own logic. Corrections always outrank model output. Nothing is trained or uploaded.

## 6. Models and recognition (D-14, D-15)

| Role | Model kind | Ships | Used for |
|---|---|---|---|
| Ears | Speech-to-text (whisper.cpp class, or the platform recogniser offline) | base | Dictated logging (stage 6) |
| Similarity | Small multilingual text embedding model | base | Tag suggestions, grouping, search by meaning |
| Eyes (small) | Small on-device recognition model | base | S2–S4: objects, counts, rough tags |
| Eyes (large) | Larger recogniser or multimodal model (Gemma 4 E2B class) | optional download | Better recognition in crowded photos |
| Labeller | Small LLM (Qwen3 0.6B / Gemma class) | optional download | Function tags for an item, names for a new parent tag, splitting a dictated list |
| Your logic | Nearest neighbour over the user's examples | base, no model | Personal tag and place suggestions |

Cooperation rules:
- Cheap models run first; the LLM runs only when they disagree or aren't confident.
- When the sources agree, the suggestion takes one tap; when they disagree, the user sees the candidates.
- The user's choice always wins and becomes an example.
- Heavy jobs run in a queue, one model in memory at a time, preferably while charging.

S2 needs names outside the usual fixed detector classes ("headphones" is not among the 80 COCO classes), so the recogniser must be **open-vocabulary** (image–text matching or a small multimodal model). The stage 0 spike measures this kind of model, not a fixed-class detector.

Recognition is a hint, never the record. Crowded or overlapping photos are expected to be partly wrong (S4 depends on easy correction, not on accuracy).

Model files: licence checked per model before adoption. The weights' licence must allow redistribution from a GPL app's download screen.

### 6.1 Stage 0 recognition spike (2026-10-28/29)

Measured on the owner's 20 photos (10 single objects, 10 crowded drawers or boxes; photos and per-photo results kept outside the repository). Scores are the owner's blind marks (each answer line marked right, close or wrong without knowing which recogniser gave it); 8 of the 20 were made hard on purpose (odd angles, untidied things overlapping by more than half), all well lit and in focus. The offline pair scores the same on clear and hard photos (clear singles 4/8), so its weakness is the model, not the photos. Rows not in the blind review keep the earlier answer-sheet scores, which proved more lenient.

| Recogniser | Where | Singles (bar 8/10) | Crowded (bar ⅓) | Verdict |
|---|---|---|---|---|
| SmolVLM2-500M, Gemma 4 E2B (small multimodal models) | phone | 7/10 | 22–29% | fail: too slow (15–38 s) and inaccurate |
| YOLO-World v2-S alone (open-vocabulary detector) | phone | 1/10 | 18% | fail on names; boxes good |
| YOLO-World v2-S boxes + SigLIP 2 base naming each box | phone | 6/10 (8/10 counting close) | 26% (53% counting close); 60% of lines wrong | **fails** by the owner's marks; best offline so far |
| Gemini 3.5 Flash (large, online) | cloud | 10/10 | 88% | ceiling for comparison; not offline |
| Qwen3-VL-8B (large, online) | cloud | 10/10 (answer sheet) | not yet run | open-weight ceiling |
| Claude (large, online) | cloud | 10/10 | 65% (97% counting close) | names often broader than exact; not offline |
| Claude, tightened instructions (photos 1–16) | cloud | 10/10, counts 10/10 | 91% of lines right, none wrong | clears D-29 widely; not offline |
| Gemini 3.5 Flash, tightened instructions (photos 1–16) | cloud | 10/10, counts 10/10 | 93% of lines right, none wrong | clears D-29 widely; not offline |
| Gemma 4 E2B, tightened instructions | phone | about 3/10 | worse | fail: copies the instructions' examples; 18–49 s |
| YOLO-World + SigLIP 2, naming only confident boxes (cut 0.14) | phone | 6 right, 0 wrong, 4 "not sure" | 19% of lines wrong; names 37 of 125 boxes | never wrong on singles, but the user names most drawer items; cut tuned on these photos |
| Qwen3-VL-8B, tightened instructions, offline (photos 1–20) | PC, 5.75 GB | 9/10 (10/10 counting close), counts 9/10 | 75% of lines right, 5% wrong (tightened Claude on the same lines: 78%, 4%); 2.3 extra lines per drawer; 1 drawer of 10 failed (repeat loop) | near-cloud naming offline; fails D-29 on time (about 48 s per photo on a desktop PC) and D-31 on size |
| Same, with instructions tuned to its faults and a repeat check across lines | PC, 5.75 GB | 9/10, counts 9/10 | 63% right, 29% close, 8% wrong; 0.9 extra lines per drawer; no drawer failed (on the same lines: the row above 76% right, 2.7 extra; Claude 77% right, 1.0 extra) | shorter, no loops, but vaguer names; the earlier instructions name more exactly |
| Same, round 3 instructions plus only the anti-repeat rules | PC, 5.75 GB | 9/10, counts 9/10 | 58% right, 30% close, 12% wrong; 1.7 extra lines per drawer; no drawer failed | worse than both; every change to the instructions lost accuracy somewhere (owner: stop tuning, D-37) |

Rows marked *tightened* use instructions that ask for the most specific label name, counts one by one ("about N" when things overlap), and nothing that can't be seen. For drawers, each answer is scored on as many lines as the shortest answer for that photo, since extra lines are extra reading (owner, 2026-10-29). The offline Qwen row compares it with Claude and Gemini on the same lines (N from those answers), so its Claude figure differs from the row above. The two tuned Qwen rows add their answers to that comparison, which shortens some drawers' N slightly. Of the Qwen runs, the plain round 3 instructions (shared with the cloud models) stay the most accurate; a loop is better handled by the app (cut repeated lines, cap at 15; *builder's call*) than by more rules, which the owner judged to confuse the model and lengthen the user's work.

An open model running offline can now name things nearly as well as the cloud models on these photos; what it lacks is speed and a size that fits the phone. Open for stage 0: whether a smaller model of the same family (Qwen3-VL 2B or 4B) keeps enough of this on the Snapdragon 888 phone within D-29, and whether the recogniser may be downloaded on first use like the speech model (D-31; owner decision).

The best offline pair, as it would ship *(stage 0 candidate, not adopted: fails D-29 by the owner's marks)*:
- YOLO-World v2-S (GPL-3.0), weights stored in fp16: 28 MB. SigLIP 2 base image side (Apache-2.0), 8-bit weights with int8 maths: 109 MB. Name vectors for a fixed 1,356-name vocabulary: 4 MB. A reduced ONNX Runtime 1.30 with only the operators used: 14 MB. Total 155 MB raw, **138 MB compressed** (D-31).
- Time per photo: Snapdragon 888 phone 1.8–6.3 s, Snapdragon 695 phone 3.1–8.9 s; none of the 20 over 10 s. Accuracy identical to full precision.
- Weak spots: counts (3/10 right), crowded drawers (26% found, and most lines wrong, so correction would cost more than it saves). SigLIP 2's text side (about 1.1 GB) is not shipped: user tags and search by meaning need the Similarity model instead, or a later decision.
- Rejected on the way: dynamic int8 (broke SigLIP), 4-bit weights (lost accuracy), MobileCLIP (Apple research-only licence), Qualcomm's ready-made 8-bit file (no smaller outside their NPU).

**Errors by cause (2026-11-03).** Asked whether cluttered backgrounds are a big problem (the photos are of real drawers and boxes, not things on a plain backdrop), every line the owner marked close or wrong, every line naming the container, and every answer-sheet group missed were sorted by cause after looking at each photo (per-line tags kept outside the repository). The background caused few of them: Claude (tightened) 3 of 24 problem lines and 2 of 4 misses; Gemini (tightened, photos 1–17) 0 of 7 and 1 of 1; Qwen3-VL-8B offline 8 of 53 and 1 of 6; YOLO-World + SigLIP 2 15 of 105. The rest were wrong or broader names, wrong counts, Qwen's repeated lines (20 of its 53) and names read off the picture or words printed on a package (5 lines).
- Things that blend in (dark among dark, the same colour as what is behind, see-through) were the main background cause and every background miss. Misses cost the most to fix (typing a line).
- Things outside the drawer: one line from the large models (a package standing beside the box); a few from the detector.
- The container itself named as a stored thing (bin, shelf, the drawer's knobs): Qwen 3 lines, the detector 5. True, but one line each to untick.
- So no cut-out (segmentation) stage for now: a crop step would have removed about one line on these photos, and low-contrast things are hard for cut-out models too; light (a torch button on Capture) fits the cause better. Caveats: all 20 photos were well lit, so dim light is untested; misses are judged against an answer sheet written before any model ran and not checked by the owner; the detector's boxes were not saved, so 6 of its tags are likely, not seen.

## 7. Clear-box surfaces (D-20, D-22)

| Surface | Rank | Notes |
|---|---|---|
| Home-screen widget | 3 | Rotating contents of a place or tag; Android first, Windows equivalent to be decided |
| In-app photo browsing | 3 | Places shown by their photos; open a place, see what's in it |
| Printed photo labels | 2 | A4 sticker-sheet PDF: contents collage plus a QR code that opens the place |
| Object of the day | — | On the homepage (owner's sketch, §8): least-searched things first |
| Lock screen / wallpaper | 1 | Later |
| "Rediscover" prompt | 1 | Opt-in only |

## 8. Homepage sketch (owner, not yet decided)

The search bar is at the top. Below it are two cards:
- **Places:** opens the house map, or a list of floor cards if more than one floor exists, or a grid of houses if more than one house exists.
- **Tags:** opens a grid of top-level (parent) tags.

Below the cards is a **random object of the day**, chosen from the least searched-for first.

This is an input to the purpose map, not a decided layout. Reach and density follow the model of the owner's earlier app (D-25).

## 9. Timeline (in order of dependency)

Each stage ends with a **done-when** check and, from stage 2 on, a walk on a real device. Screens are discussed with the owner before they are built (D-26).

| Stage | Holds | Done when | Unlocks |
|---|---|---|---|
| **0 Foundations** | SPEC; purpose map scored on the D-34 axes; screen-by-purpose decisions; reference apps walked on the Snapdragon 845 phone; throwaway spike: small LLM and open-vocabulary recognition timed against D-29 on the target phones; Compose Multiplatform window running on Android and Windows (D-36) | Owner approves SPEC and purpose map; spike timings recorded in the SPEC | — |
| **1 Data and safety** | Data model (items, places, tags with parents and kept-together, photos, states, custom fields, history); database; backup and export; change-file sync | Round-trip tests: export → import, two-device sync with conflicts, a corrupted database is kept, not deleted | — |
| **2 Logging and tags** | Quick add by name; unplaced inbox; item editor; tags with parents; bulk tag add and remove; search including child tags | Owner-approved screens built; S1 passes | S1 |
| **3 Places and photos** | Place tree; camera (phone only, D-28); item and place photos; settled-area path; place contents view; "in use / away"; move and bulk move | S5 passes; S7's move part passes | S5 |
| **4 Recognition** | Small open-vocabulary recognition model on phone and PC (PC works on synced phone photos); suggestions from photos; count; split a photo into items; bulk clear of suggestions | The D-29 pass bar is met on both target phones; S2, S3, S4 pass | S2, S3, S4 |
| **5 Suggestions and grouping** | Embedding model; personal nearest-neighbour layer; tag suggestions for new items; kept-together tags with home and away views | Suggestions accepted or corrected in one tap; S7 passes fully | S7 |
| **6 Visual map** | Automatic stylised place map with rearrangeable blocks; filter search by house or floor; fallback results from other places | S6 passes | S6 |
| **7 Clear-box surfaces** | Photo browsing; home-screen widget; label PDF with QR; object of the day | Owner uses the widget and labels for a week | — |
| **8 Voice** | Dictated logging in Italian and English on the phone (D-28); speech model downloaded on first use (D-31); splitting a list into items | A dictated list of ten things becomes ten items | — |
| **9 Optional models** | Large recogniser; labeller LLM; download screen; licence checks | Each optional model measurably improves a scenario | — |
| **10 Modules** | Wardrobe (size, colour, material, season, occasion, last worn, laundry; outfits, seasonal swap, not worn in a year) and Pantry (quantity + unit, expiry, opened on, minimum stock; expiring soon, shopping list, "already have it" check), each designed with the owner first | Each module passes its own scenarios, to be written with the owner | — |
| **11 Later** | Lending; app lock; consumables; NFC; lock-screen surface; rediscover prompt; free-drawing map editor; F-Droid build | Each gets its own decision first | — |

*(builder's call)* The visual map (6) comes after recognition (4) and suggestions (5) because S2–S4 and S7 are daily-use loops, while the map is reached through places that already exist from stage 3. The owner may reorder 4–6: none of them depends on the others.

## 10. Look and feel

Direction only (D-24, D-25); to be workshopped per screen:
- calm and friendly, low visual noise, relaxing in greyscale before colour;
- shapes guide the user;
- density and the hierarchy of visual function are the leading axes;
- not stock Material: the framework is plumbing, the look has its own identity;
- the same element reads and acts the same on every screen, and controls at the same level keep the same size and place.

## 11. Open questions

| # | Question | Needed by |
|---|---|---|
| O-02 | Windows equivalent of the home-screen widget (tray, startup window, none) | Stage 7 |
| O-03 | Consumables and low-stock handling (D-07) | Stage 10 |
| O-04 | Which tag kinds exist by default | Stage 2 |
| O-05 | Database library (SQLDelight or Room KMP) | Stage 1 |
| O-06 | Final list of custom field types and presets, from use cases (D-08, D-33) | Stage 1 |
| O-07 | Which app owns reminders and shopping lists if the earlier app overlaps (the earlier app has reminders) | Stage 10 |
| O-08 | An optional AI-service key the user supplies (Gemini, Claude, self-hosted, any provider), off by default, sending photos only when turned on. The owner is open to it for personal use; it would amend D-16. Stage 0 found the instructions sent with each photo matter as much as the service: with tightened instructions both Gemini (free key) and Claude clear D-29 with a wide margin (§6.1), so the app would ship those instructions, not a bare question. | Stage 4 |

## 12. Sources to borrow from (D-05)

| Project | Licence | Language | Use |
|---|---|---|---|
| rh-id/a-personal-stuff | GPL-3.0 | Java | Feature ideas (reminders) |
| stovocor/whohasmystuff | GPL-3.0 | Kotlin | Lending (stage 10) |
| escapedcanadian/prepper | GPL-3.0 | Kotlin | To be read |
| PamCatten/hot-stuff | Apache-2.0 | Kotlin | To be read; archived |
| asmw/flindex | MIT | Dart | NFC/QR container flows (port, not copy) |
| anderslum/boxorg | MIT | TypeScript | Photo-first box interface (reference) |
| sysadminsmedia/homebox | AGPL-3.0 | Go | Data model, QR labels (reference only) |
| lcukerd/StuffList | none | Java | Look only, never copy |
| The owner's earlier app (unpublished) | owner's own, no licence file; code taken into Chatelaine is relicensed GPL-3.0 by the owner and recorded in `THIRD_PARTY.md` | Kotlin (KMP) | Sync engine and folder layer, version catalog, `:core`/`:data` split, typed units, app lock |

Design references, not open source: Spot It (marked spots on a photo), Nestmer (visual map of rooms), Store & Forget (photo-first entry).

## 13. Decision log

| Date | Change |
|---|---|
| 2026-10-27 | Name Chatelaine; GPL-3.0; public repo; publish check in hooks and CI |
| 2026-10-28 | SPEC v0.1 drafted from the owner's answers (D-01..D-27, S1..S7) |
| 2026-10-28 | Builder check answered: D-28..D-32; restore rule (§5.6) and open-vocabulary recognition (§6) as builder's calls; O-06 opened |
| 2026-10-28 | Review of the owner's earlier app: D-36 amended (KMP spike restored), §5.6 sync reuse and differences, §12 relicensing note |
| 2026-10-28 | D-34 purpose-map axes, D-35 homepage master board, D-36 spikes replaced by a review of the owner's earlier app; O-01 closed |
| 2026-10-28 | D-33 Wardrobe and Pantry modules (stage 10); field types (§5.5); restore now merges by stamps, following the earlier app; O-07 |
| 2026-10-29 | SPEC v0.2: D-29 target phones Snapdragon 888 and Snapdragon 695 phones (Snapdragon 845 phone dropped), D-15 and D-36 follow; D-31 read as download size; §6.1 stage 0 recognition results; O-08 optional AI-service key |
| 2026-10-30 | §6.1 rescored with the owner's blind marks (offline pair fails D-29); tightened-instruction results; D-07 approximate counts with an "accurate amounts" setting; O-08 notes the instructions matter |
| 2026-10-30 | §6.1: Qwen3-VL-8B offline on a PC, owner's blind marks: near-cloud naming, fails on time and size; smaller-model and first-use-download questions opened |
| 2026-10-30 | §6.1: Qwen with tuned instructions (shorter, no loops, vaguer); D-37 logging form built for quick correction of approximate answers (owner) |
| 2026-10-30 | §6.1: Qwen with round 3 instructions plus anti-repeat rules, worse; instruction tuning stopped (owner). D-37: lump into generic names with approximate counts, refine in a few taps, split by use not colour |
| 2026-11-03 | §6.1: recognition errors sorted by cause; the background causes few (mostly things that blend in), so no segmentation stage for now |
