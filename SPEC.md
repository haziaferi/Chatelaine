# Chatelaine — specification

Revision: SPEC v0.1 (draft), 2026-10-28. No code yet.
Package: `com.hazia.chatelaine`. Licence: GPL-3.0. Repository: github.com/haziaferi/Chatelaine.

Companion document: [docs/purpose-map.md](docs/purpose-map.md), which decides which purpose each screen serves before any screen is built.

Entries marked *(builder's call)* were settled during drafting without the owner and can be overturned.

## 0. Intent

A clear plastic box keeps its contents in mind with no effort: every glance is a reminder. A cardboard box, even labelled, needs a deliberate lookup and a mental link from label to object. That costs effort at the very moment a task starts and puts load on working memory. This "out of sight, out of mind" effect is strong for people with ADHD.

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
| D-07 | Optional quantity per item; consumables and low-stock handling to be discussed later. |
| D-08 | User-defined custom fields. Full or near-full customisation is a main focus of the app. |
| D-09 | An item can be "in use / away": taken out of its place and not yet put back. |
| D-10 | Lending ("who has it") comes in a later stage. |
| D-11 | Value, receipts and serial numbers (insurance use) are out of scope for v1. |
| D-12 | One organising concept: **tags**. Items and places can carry several tags. A tag can sit inside a parent tag ("red skirt" inside "sewing"). A tag can be marked **kept together**, meaning its things share a home place. The homepage's tag card shows top-level tags only. |
| D-13 | Item, place and tag information that a model suggests is always fully editable, one at a time or in bulk. |
| D-14 | Image recognition is core: a small model ships with the app, a larger, more accurate one is an optional download. Recognition runs on both devices. |
| D-15 | An optional on-device language model for tag suggestions. Target phone: the OnePlus 6T if it runs acceptably, otherwise the 9 Pro (to be measured, §9 stage 0). |
| D-16 | Fully offline. The network is used only for opt-in model downloads. F-Droid-compatible build is optional. |
| D-17 | Phone and PC share one inventory through **two-way sync over a folder** kept in step by a file-sync tool such as Syncthing. Each device writes its changes as separate files and merges the other device's. |
| D-18 | Backup: automatic backup to a folder the user picks, plus manual export (JSON and photos). |
| D-19 | App lock: biometric, PIN or password; not in v1. |
| D-20 | The app never pushes by default. Proactive prompts (notifications) are opt-in; the clear-box effect comes from passive surfaces. |
| D-21 | Labels: printable A4 sticker sheets (PDF) with a photo collage and a QR code. NFC tags later. |
| D-22 | Clear-box surfaces, by owner rank (3 = most helpful): home-screen widget 3, in-app photo browsing 3, printed photo labels 2, lock screen 1, "rediscover" prompt 1. |
| D-23 | Visual map of storage: the app draws stylised places automatically from the place hierarchy, as blocks the user can rearrange. A free-drawing editor may come later. |
| D-24 | Look and feel: a layout specific to the app's purpose, not framework defaults. Calm, friendly, low visual noise, relaxing to look at even in greyscale; shapes guide the user. Density and the hierarchy of visual function are the leading axes. Workshopped later; Factotum and SisyphusFit are conceptual references, not templates. |
| D-25 | Reach and density follow Factotum's model, not GlassCam's: not everything visible at once. |
| D-26 | No screens at stage 0; screens start at stage 2 or later, each discussed with the owner before it is built. |
| D-27 | The purpose map aims to minimise the number of screens and maximise their quality. Other apps serve as baselines to improve upon or as controls that must lose when scored. Scoring axes are agreed with the owner before scoring. |
| D-28 | The Windows PC is a **desk companion**: everything except taking photos and dictation. It runs recognition on photos taken with the phone and handles bulk editing, the place map, label printing and backup. The owner's PC: Ryzen 5 2600X (6 cores), 16 GB RAM, Radeon RX 570 (4 GB reported). |
| D-29 | Recognition pass bar, measured on a fixed set of about 20 photos taken for the purpose (kept out of the repository). On the 6T: at most about 10 s per photo; no crash or overheating across the set; single objects (S2): the right name among the first three suggestions in at least 4 of 5 photos; crowded boxes (S4): at least a third of the items found, and correction quick enough for the rest. If the 6T misses the bar, the target moves to the 9 Pro. |
| D-30 | Search matches across languages: "cucito" finds "sewing". *(builder's call, from the owner's answer: tags are stored once, as written; model suggestions are offered in the interface language; built-in names such as place and tag kinds switch with the language.)* |
| D-31 | The phone app stays under about 150 MB; the speech model is downloaded on first use of dictation. The small recogniser and the text-similarity model ship with the app. Limits to be refined in later stages. |
| D-32 | No existing inventory data to import; no importer in stage 1. |
| D-33 | One app on one shared core. **Wardrobe** and **Pantry** are optional modules, off by default. A module = a preset set of custom fields on its tags + a few views of its own + its own purpose-map entries. Users can build their own packs with the same tools. |

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
- A backup carries each value's sync stamps, and restoring it **merges** like any other device's changes: edits newer than the backup survive on both devices. Rolling everything back to a backup is a separate, deliberate action; it is recorded as a change, and the state it replaced stays in history *(builder's call, following Factotum's lesson that a stamp-less backup overwrites every device)*.
- Factotum's folder sync (per-device log segments, snapshots, merge engine in `:core`) is the owner's own code and the starting point for this design.

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

This is an input to the purpose map, not a decided layout. Reach and density follow Factotum's model (D-25).

## 9. Timeline (in order of dependency)

Each stage ends with a **done-when** check and, from stage 2 on, a walk on a real device. Screens are discussed with the owner before they are built (D-26).

| Stage | Holds | Done when | Unlocks |
|---|---|---|---|
| **0 Foundations** | SPEC; purpose map with scoring axes agreed with the owner; screen-by-purpose decisions; throwaway spikes: (a) small LLM and recognition model timed on the 6T, then the 9 Pro if needed; (b) KMP project builds and runs on Android and Windows; (c) change-file sync between two folders | Owner approves SPEC and purpose map; spike timings recorded in the SPEC | — |
| **1 Data and safety** | Data model (items, places, tags with parents and kept-together, photos, states, custom fields, history); database; backup and export; change-file sync | Round-trip tests: export → import, two-device sync with conflicts, a corrupted database is kept, not deleted | — |
| **2 Logging and tags** | Quick add by name; unplaced inbox; item editor; tags with parents; bulk tag add and remove; search including child tags | Owner-approved screens built; S1 passes | S1 |
| **3 Places and photos** | Place tree; camera (phone only, D-28); item and place photos; settled-area path; place contents view; "in use / away"; move and bulk move | S5 passes; S7's move part passes | S5 |
| **4 Recognition** | Small open-vocabulary recognition model on phone and PC (PC works on synced phone photos); suggestions from photos; count; split a photo into items; bulk clear of suggestions | The D-29 pass bar is met on the 6T (or the 9 Pro); S2, S3, S4 pass | S2, S3, S4 |
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
| O-01 | Scoring axes for the purpose map (from GlassCam, Factotum, SisyphusFit, or new) | Stage 0 |
| O-02 | Windows equivalent of the home-screen widget (tray, startup window, none) | Stage 7 |
| O-03 | Consumables and low-stock handling (D-07) | Stage 10 |
| O-04 | Which tag kinds exist by default | Stage 2 |
| O-05 | Database library (SQLDelight or Room KMP) | Stage 1 |
| O-06 | Final list of custom field types and presets, from use cases (D-08, D-33) | Stage 1 |
| O-07 | Which app owns reminders and shopping lists if Factotum overlaps (Factotum has reminders) | Stage 10 |

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
| Factotum | owner's own | Kotlin (KMP) | Folder sync engine, typed units, app lock, KMP layer split |

Design references, not open source: Spot It (marked spots on a photo), Nestmer (visual map of rooms), Store & Forget (photo-first entry).

## 13. Decision log

| Date | Change |
|---|---|
| 2026-10-27 | Name Chatelaine; GPL-3.0; public repo; publish check in hooks and CI |
| 2026-10-28 | SPEC v0.1 drafted from the owner's answers (D-01..D-27, S1..S7) |
| 2026-10-28 | Builder check answered: D-28..D-32; restore rule (§5.6) and open-vocabulary recognition (§6) as builder's calls; O-06 opened |
| 2026-10-28 | D-33 Wardrobe and Pantry modules (stage 10); field types (§5.5); restore now merges by stamps, following Factotum; O-07 |
