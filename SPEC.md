# Chatelaine spec

Scope: Chatelaine, a home inventory for one user on an Android phone and a Windows PC, which keeps stored things in view.

History: `SPEC-history.md`, one line per change. It is never a source of current rules.

Kinds: O owner rule · D decided · I inherited · A assumed · Q open.

## Rule index

| ID | Name | Section |
|---|---|---|
| D-R01 | Users and devices | 1. Purpose and jobs |
| D-R02 | Goals | 1. Purpose and jobs |
| D-R03 | Anti-goals | 1. Purpose and jobs |
| Q-R04 | Jobs and how often | 1. Purpose and jobs |
| D-R05 | Project from a tag | 2. Acceptance scenarios |
| D-R06 | Photo with suggestions | 2. Acceptance scenarios |
| D-R07 | Bulk clear | 2. Acceptance scenarios |
| D-R08 | Box split into items | 2. Acceptance scenarios |
| D-R09 | Before buying | 2. Acceptance scenarios |
| D-R10 | Visual place map and filtered search | 2. Acceptance scenarios |
| D-R11 | Seasonal swap | 2. Acceptance scenarios |
| D-R12 | Name, package, licence and repository | 3. Platforms and build |
| D-R13 | Framework and targets | 3. Platforms and build |
| D-R14 | Shared code | 3. Platforms and build |
| D-R15 | Offline | 3. Platforms and build |
| D-R16 | No Google Play services | 3. Platforms and build |
| D-R17 | F-Droid build | 3. Platforms and build |
| D-R18 | Languages | 3. Platforms and build |
| D-R19 | Phone download size | 3. Platforms and build |
| I-R20 | Owner's PC | 3. Platforms and build |
| D-R21 | Desk companion | 3. Platforms and build |
| D-R22 | Publishing rules | 3. Platforms and build |
| D-R23 | Borrowing code | 3. Platforms and build |
| D-R24 | Sources to borrow from | 3. Platforms and build |
| D-R25 | Item | 4. Items, places and tags |
| O-R26 | Quantity and counts | 4. Items, places and tags |
| Q-R27 | Consumables and low stock | 4. Items, places and tags |
| D-R28 | Identical things | 4. Items, places and tags |
| D-R29 | Order of an item's facts | 4. Items, places and tags |
| D-R30 | Item states | 4. Items, places and tags |
| D-R31 | Photo outdated flag | 4. Items, places and tags |
| D-R32 | Place tree | 4. Items, places and tags |
| D-R33 | Place details | 4. Items, places and tags |
| D-R34 | Tags | 4. Items, places and tags |
| D-R35 | Tag tree | 4. Items, places and tags |
| D-R36 | Kept together | 4. Items, places and tags |
| D-R37 | Tag kinds | 4. Items, places and tags |
| Q-R38 | Default tag kinds | 4. Items, places and tags |
| D-R39 | Suggested tags | 4. Items, places and tags |
| D-R40 | Editing suggestions | 4. Items, places and tags |
| D-R41 | Photos | 4. Items, places and tags |
| D-R42 | Proposals from a photo | 4. Items, places and tags |
| D-R43 | Custom fields | 4. Items, places and tags |
| D-R44 | Field types | 4. Items, places and tags |
| Q-R45 | Final field types and presets | 4. Items, places and tags |
| D-R46 | Where a field attaches | 4. Items, places and tags |
| D-R47 | User-editable lists | 4. Items, places and tags |
| D-R48 | Local database | 5. Storage, sync and backup |
| Q-R49 | Database library | 5. Storage, sync and backup |
| D-R50 | A database that fails to open | 5. Storage, sync and backup |
| D-R51 | Folder sync | 5. Storage, sync and backup |
| D-R52 | Record ids and history | 5. Storage, sync and backup |
| D-R53 | No shared database file | 5. Storage, sync and backup |
| D-R54 | Conflicting edits | 5. Storage, sync and backup |
| D-R55 | Sync groups | 5. Storage, sync and backup |
| D-R56 | Photos in the sync folder | 5. Storage, sync and backup |
| D-R57 | Any folder-sync tool | 5. Storage, sync and backup |
| D-R58 | Sync code from the earlier app | 5. Storage, sync and backup |
| D-R59 | Sync lessons from the earlier app | 5. Storage, sync and backup |
| D-R60 | Backup and export | 5. Storage, sync and backup |
| D-R61 | Restoring a backup | 5. Storage, sync and backup |
| D-R62 | Image recognition | 6. Models and recognition |
| D-R63 | Model roles | 6. Models and recognition |
| D-R64 | Models on device | 6. Models and recognition |
| D-R65 | Recognition pass bar | 6. Models and recognition |
| O-R66 | Scoring crowded answers | 6. Models and recognition |
| D-R67 | Optional language model | 6. Models and recognition |
| D-R68 | Open-vocabulary recogniser | 6. Models and recognition |
| D-R69 | Recognition as a hint | 6. Models and recognition |
| D-R70 | Model cooperation | 6. Models and recognition |
| D-R71 | Personal learning | 6. Models and recognition |
| D-R72 | Model licences | 6. Models and recognition |
| O-R73 | Recognition instructions | 6. Models and recognition |
| D-R74 | Repeated lines | 6. Models and recognition |
| D-R75 | Segmentation stage | 6. Models and recognition |
| Q-R76 | Smaller recogniser model | 6. Models and recognition |
| Q-R77 | Recogniser as a first-use download | 6. Models and recognition |
| Q-R78 | AI-service key | 6. Models and recognition |
| O-R79 | Correction-first logging form | 7. Logging and search |
| O-R80 | Lump, then refine | 7. Logging and search |
| D-R81 | Search through child tags | 7. Logging and search |
| D-R82 | Search across languages | 7. Logging and search |
| D-R83 | Visual place map | 8. Place map, homepage and clear-box surfaces |
| Q-R84 | Homepage layout | 8. Place map, homepage and clear-box surfaces |
| D-R85 | Tag card | 8. Place map, homepage and clear-box surfaces |
| D-R86 | No push by default | 8. Place map, homepage and clear-box surfaces |
| D-R87 | Clear-box surfaces | 8. Place map, homepage and clear-box surfaces |
| D-R88 | Labels | 8. Place map, homepage and clear-box surfaces |
| Q-R89 | Windows widget | 8. Place map, homepage and clear-box surfaces |
| D-R90 | Purpose map | 9. Purpose map and look |
| D-R91 | Scoring axes | 9. Purpose map and look |
| D-R92 | Homepage shortcuts in scoring | 9. Purpose map and look |
| D-R93 | Screens before building | 9. Purpose map and look |
| D-R94 | Look and feel | 9. Purpose map and look |
| D-R95 | Reach and density | 9. Purpose map and look |
| D-R96 | Consistent elements | 9. Purpose map and look |
| D-R97 | Modules | 10. Modules and the shopping list |
| O-R98 | Savings app and the shopping list | 10. Modules and the shopping list |
| Q-R99 | Reminders | 10. Modules and the shopping list |
| D-R100 | Stage plan | 11. Stages |
| D-R101 | Order of stages 4 to 6 | 11. Stages |
| O-R102 | Stage 0 spikes | 11. Stages |
| D-R103 | Insurance records | Out of scope |
| D-R104 | App lock | Out of scope |
| D-R105 | Lending | Out of scope |
| D-R106 | Inventory import | Out of scope |
| D-R107 | Stock and expiry outside Pantry | Out of scope |

## Glossary

| Term | Meaning |
|---|---|
| Clear-box effect | What a clear plastic box does for its contents: every glance is a reminder, with no effort. |
| Earlier app | The owner's earlier app (unpublished): the owner's own Kotlin Multiplatform code, whose folder sync is the starting point for Chatelaine's sync. |
| Item | A thing the user owns, as recorded in the app. |
| Place | A storage location in the place tree. |
| Site | A house, at the top of a place tree. |
| Tag | A label that says what kind of thing something is. Items and places carry tags. |
| Parent tag | A tag that other tags sit inside; those are its child tags. A top-level tag has no parent. |
| Kept together | A mark on a tag: its things share a home place. |
| Field | A value that differs from item to item, such as size M or expires March 2027. A custom field is one the user defines. |
| Module | A preset set of custom fields on its tags, plus a few views of its own and its own purpose-map entries. |
| Pack | A module a user builds with the same tools as the built-in modules. |
| Settled area | An area whose things are where they belong, photographed as a whole. |
| Place photo | A photo that belongs to a place. |
| Suggestion | Information a model proposes, such as a name, a count or a tag. |
| Proposal | An item a recogniser proposes from a photo. |
| Line | One entry in a recogniser's answer, in the form "about N x name". |
| Right, close, wrong | The owner's marks for an answer line in a blind review, given without knowing which recogniser wrote it. |
| Tightened instructions | The instructions sent with each photo that ask for the most specific label name, counts one by one ("about N" when things overlap), and nothing that cannot be seen. |
| Object of the day | A random stored thing shown on the homepage, chosen from the least searched-for first. |
| Purpose map | The companion document `docs/purpose-map.md`. |
| Reference app | Another app scored beside Chatelaine's plans, as a baseline to improve upon or as a control that must lose. |
| Stage | One step of the stage plan. |
| Done-when | The check that ends a stage. |

## 1. Purpose and jobs

A clear plastic box keeps its contents in mind with no effort: every glance is a reminder. A cardboard box, even labelled, needs a deliberate lookup and a mental link from label to object. That costs effort at the very moment a task starts and puts load on working memory. The effect known as out of sight, out of mind is strong for many people.

Chatelaine is a home inventory that does what the clear box does. It records what is stored and where, and it puts stored things back in front of the user without being asked. It also helps with the first big tidy: things can be logged before they have a place, grouped by what they are for, gathered, placed, and photographed once they are where they belong.

The name: a chatelaine ran a household and carried its keys and small tools on a chain at her belt, always in view.

### D-R01 · Users and devices

**Rule:** Chatelaine has one user, with two devices: an Android phone and a Windows PC. Several users or shared households are not in v1.

**Acceptance:** none: a scope boundary.

**Since:** 2026-10-28 · owner · migrated

### D-R02 · Goals

**Rule:** The app pursues the goals below.

**Values:**

| Goal | Detail |
|---|---|
| Stored things stay present in mind without effort | through passive surfaces (R86, R87) |
| Logging costs at most a word, a dictated phrase or a photo | everything else can come later |
| Finding a thing takes one search or one walk through the place map | — |
| The first big tidy is supported end to end | log, group, gather, place, photograph |
| The user can shape the app to their own logic | tags, fields, place structure, what the homepage shows |

**Acceptance:** none: a statement of aims; the scenarios R05 to R11 carry the checks.

**Since:** 2026-10-28 · unrecorded · migrated

### D-R03 · Anti-goals

**Rule:** Each outcome below is a failure, even if the app technically works.

**Values:**

| Anti-goal |
|---|
| Streaks, guilt or nagging wording, or reminders the user did not ask for |
| A busy, everything-at-once interface |
| A stock Material look |
| Any model guess that cannot be corrected, or that overrides a user correction |
| Data loss from sync, backup or a corrupted database |

**Acceptance:** none:

**Since:** 2026-10-28 · unrecorded · migrated

### Q-R04 · Jobs and how often

**Rule:** Which jobs does the app serve, and how often is each done?

**Default:** The source's only proposal: R101 calls the jobs of R06, R07, R08 and R11 daily-use loops. For the other jobs none is proposed, because the source records no frequency for them. An answer can change the order of stages 4 to 6, which R101 bases on those loops.

**Why:** The source states the jobs as aims (R02) and scenarios (R05 to R11), but not how often each is done.

**Since:** 2026-11-22 · builder · own words

## 2. Acceptance scenarios

The app is working when all the scenarios below succeed. The stage plan (R100) names the stage that unlocks each.

### D-R05 · Project from a tag

**Rule:** Search the tag "sewing" and get the items with that tag; select the ones needed for a new project and add the tag "red skirt" to them in one action. Later, find them quickly by "red skirt".

**Acceptance:** The steps succeed end to end, as stage 2's gate requires (R100).

**Since:** 2026-10-28 · owner · migrated

### D-R06 · Photo with suggestions

**Rule:** Photograph three headphones. The app recognises headphones and the count of three, and suggests tags such as "technology", "music", "phone", "PC". The user deletes "PC" and keeps the rest, which are saved with the item.

**Acceptance:** The steps succeed end to end, as stage 4's gate requires (R100).

**Since:** 2026-10-28 · owner · migrated

### D-R07 · Bulk clear

**Rule:** Photograph a full drawer of mixed objects; the app recognises some or all and suggests tags. The user clears all suggested tags in one action and adds their own, "junk drawer".

**Acceptance:** The steps succeed end to end, as stage 4's gate requires (R100).

**Since:** 2026-10-28 · owner · migrated

### D-R08 · Box split into items

**Rule:** Photograph a box of many items, some overlapping or partly hidden; the app recognises some and gets others wrong. The user corrects the wrong ones and saves each as a separate item, not one box, so they can be repositioned and rephotographed later.

**Acceptance:** The steps succeed end to end, as stage 4's gate requires (R100).

**Since:** 2026-10-28 · owner · migrated

### D-R09 · Before buying

**Rule:** Open the bathroom cabinet under the sink in the app and see which cleaning supplies are there, so the scale remover is not bought twice.

**Acceptance:** The steps succeed end to end, as stage 3's gate requires (R100).

**Since:** 2026-10-28 · owner · migrated

### D-R10 · Visual place map and filtered search

**Rule:** Build a small visual map from drawer partitions up through dresser, shelf, closet, cabinet, room, floor and house, with several houses. Select a house or floor and search "shoes" to filter results by place. If there is no result there, a short list follows with the same search's results from other places.

**Acceptance:** The steps succeed end to end, as stage 6's gate requires (R100).

**Since:** 2026-10-28 · owner · migrated

### D-R11 · Seasonal swap

**Rule:** Having grouped the winter clothes with the app, swap the closet at the change of season, then group and organise the summer clothes being put away in the same way.

**Acceptance:** The move part succeeds at stage 3 and the whole scenario at stage 5, as those stages' gates require (R100).

**Since:** 2026-10-28 · owner · migrated

## 3. Platforms and build

### D-R12 · Name, package, licence and repository

**Rule:** The app's name, package, licence and repository are those below.

**Values:**

| Subject | Value |
|---|---|
| Name | Chatelaine |
| Package | `com.hazia.chatelaine` |
| Licence | GPL-3.0 |
| Repository | `github.com/haziaferi/Chatelaine`, public |

**Acceptance:** none: a naming and licensing choice.

**Since:** 2026-10-28 · unrecorded · migrated

### D-R13 · Framework and targets

**Rule:** The app is built with Kotlin Multiplatform and Compose Multiplatform. It targets Android, with minSdk 30 (Android 11), and Windows desktop on the JVM.

**Acceptance:** A Compose Multiplatform window runs on Android and on Windows, as stage 0 holds (R100, R102).

**Since:** 2026-10-28 · owner · migrated

### D-R14 · Shared code

**Rule:** Code is shared by default. Platform code is written only for the camera, file access, widgets, speech, model runtimes and printing.

**Acceptance:** none:

**Since:** 2026-10-28 · unrecorded · migrated

### D-R15 · Offline

**Rule:** The app is fully offline: the network serves only opt-in model downloads. There are no cloud accounts and no server.

**Acceptance:** none:

**Since:** 2026-10-28 · owner · migrated

### D-R16 · No Google Play services

**Rule:** The core uses no Google Play services.

**Acceptance:** none:

**Since:** 2026-10-28 · unrecorded · migrated

### D-R17 · F-Droid build

**Rule:** An F-Droid-compatible build is optional. The stage plan lists it for stage 11 (R100).

**Acceptance:** none: optional; stage 11 gives it its own decision first.

**Since:** 2026-10-28 · owner · migrated

### D-R18 · Languages

**Rule:** The interface is in Italian and English from the start; dictation, in both languages, arrives in stage 8 (R100). The user can switch the app's language independently of the phone's.

**Acceptance:** A dictated list of ten things becomes ten items, in Italian and in English, as stage 8's gate requires (R100).

**Since:** 2026-11-22 · owner · agreed rewording

### D-R19 · Phone download size

**Rule:** The phone app's download stays under about 150 MB, measured compressed, not as installed. The small recogniser and the text-similarity model ship with the app; the speech model is downloaded on first use of dictation. These limits are refined in later stages.

**Evidence:** Stage 0's best offline recogniser pair came to 138 MB compressed on its own (`docs/evidence/stage0-recognition.md`).

**Acceptance:** The compressed phone download measures under about 150 MB.

**Since:** 2026-10-29 · owner · migrated

### I-R20 · Owner's PC

**Rule:** The owner's PC has the parts below.

**Values:**

| Part | Value |
|---|---|
| Processor | a 6-core desktop processor |
| Memory | 16 GB |
| Graphics | a 4 GB graphics card |

**Since:** 2026-10-28 · owner · migrated

### D-R21 · Desk companion

**Rule:** The Windows PC is a desk companion: it does everything except taking photos and dictation. It runs recognition on photos taken with the phone, and handles bulk editing, the place map, label printing and backup.

**Acceptance:** none:

**Since:** 2026-10-28 · owner · migrated

### D-R22 · Publishing rules

**Rule:** The repository holds no personal data. The publish check (`tools/publish_check.py`) runs before each commit and push, and in CI.

**Acceptance:** The publish check reports no finding before each commit and push, and in CI.

**Since:** 2026-10-27 · unrecorded · migrated

### D-R23 · Borrowing code

**Rule:** The app starts from scratch. Code is borrowed file by file from the GPL-3.0, MIT and Apache projects in R24, with their licence headers kept and each file listed in `THIRD_PARTY.md`.

**Acceptance:** none:

**Since:** 2026-10-28 · owner · migrated

### D-R24 · Sources to borrow from

**Rule:** The projects below are read and borrowed from for the use each row gives. Spot It (marked spots on a photo), Nestmer (visual map of rooms) and Store & Forget (photo-first entry) are design references, not open source.

**Values:**

| Project | Licence | Language | Use |
|---|---|---|---|
| `rh-id/a-personal-stuff` | GPL-3.0 | Java | feature ideas (reminders) |
| `stovocor/whohasmystuff` | GPL-3.0 | Kotlin | lending (R105) |
| `escapedcanadian/prepper` | GPL-3.0 | Kotlin | to be read |
| `PamCatten/hot-stuff` | Apache-2.0 | Kotlin | to be read; archived |
| `asmw/flindex` | MIT | Dart | NFC and QR container flows (port, not copy) |
| `anderslum/boxorg` | MIT | TypeScript | photo-first box interface (reference) |
| `sysadminsmedia/homebox` | AGPL-3.0 | Go | data model, QR labels (reference only) |
| `lcukerd/StuffList` | none | Java | look only, never copy |
| The owner's earlier app (unpublished) | the owner's own, no licence file; code taken into Chatelaine is relicensed GPL-3.0 by the owner and recorded in `THIRD_PARTY.md` | Kotlin (KMP) | sync engine and folder layer, version catalog, `:core`/`:data` split, typed units, app lock |

**Acceptance:** none: a list of sources; R23 says how their code is taken in.

**Since:** 2026-10-28 · unrecorded · migrated

## 4. Items, places and tags

### D-R25 · Item

**Rule:** An item carries the fields below and exists as soon as it has a name.

**Values:**

| Field | Note |
|---|---|
| name | an item exists as soon as it has one |
| tags | R34 |
| notes | — |
| quantity | optional (R26) |
| custom fields | R43 |
| photos | optional (R41) |
| place | optional (R32) |
| state | R30 |

**Acceptance:** none:

**Since:** 2026-10-28 · unrecorded · migrated

### O-R26 · Quantity and counts

**Rule:** An item can carry an optional quantity. Suggested counts are approximate by default ("about 5") and always editable; a setting, "accurate amounts", asks for exact counts instead.

**Why:** Exact counts matter mainly where items link to shopping; an estimate first keeps logging quick.

**Acceptance:** none:

**Since:** 2026-10-29 · owner · migrated

### Q-R27 · Consumables and low stock

**Rule:** How are consumables and low stock handled? It is needed by stage 10.

**Default:** none proposed: the source leaves it for a later discussion and offers no answer.

**Why:** R26 leaves consumables and low-stock handling for later. The Pantry module (R97) holds minimum stock and a shopping list in stage 10, while the stage plan (R100) lists consumables in stage 11.

**Since:** 2026-10-28 · unrecorded · migrated

### D-R28 · Identical things

**Rule:** A set of identical things can be one item with a quantity ("headphones ×3"), and it can be split into separate items later.

**Acceptance:** R06 passes with the three headphones saved as one item with a count of three.

**Since:** 2026-10-28 · builder · migrated

### D-R29 · Order of an item's facts

**Rule:** An item's tags, place and photo are independent facts that can come in any order, including the paths below.

**Values:**

| Path | What happens |
|---|---|
| Name only | logged by name, with no place or photo yet: unplaced (R30) |
| Photo first | photographed first, then named |
| Settled area | placed in a settled area that was photographed as a whole (R41) |

**Acceptance:** none:

**Since:** 2026-10-28 · unrecorded · migrated

### D-R30 · Item states

**Rule:** An item is in one of the states below.

**Values:**

| State | Meaning |
|---|---|
| unplaced | no place yet |
| placed | has a place |
| in use / away | taken out of its place and not yet put back |
| lent | lent to someone; comes with lending (R105) |

**Acceptance:** none:

**Since:** 2026-10-28 · owner · migrated

### D-R31 · Photo outdated flag

**Rule:** A "photo outdated" flag, separate from an item's state, is raised when an item, or a place's contents, changed after the last photo.

**Acceptance:** none:

**Since:** 2026-10-28 · unrecorded · migrated

### D-R32 · Place tree

**Rule:** Places nest freely in a tree of any depth, such as site (house) › floor › room › furniture › compartment › partition, and the user can skip or add levels. Several sites are allowed.

**Acceptance:** R10 passes, with its map running from drawer partitions up to several houses.

**Since:** 2026-10-28 · owner · migrated

### D-R33 · Place details

**Rule:** A place can carry tags, notes, a photo and a position in the visual map (R83). Each level has a kind (house, floor, room, furniture, container, partition, other) that sets how it is drawn; kinds are user-editable (R47).

**Acceptance:** none:

**Since:** 2026-10-28 · unrecorded · migrated

### D-R34 · Tags

**Rule:** Tags are the one organising concept. Items and places can carry any number of tags, and a tag can sit inside a parent tag ("red skirt" inside "sewing").

**Acceptance:** R05 passes.

**Since:** 2026-10-28 · owner · migrated

### D-R35 · Tag tree

**Rule:** A tag has at most one parent tag, so tags form a tree; otherwise tags are flat.

**Acceptance:** none:

**Since:** 2026-10-28 · builder · migrated

### D-R36 · Kept together

**Rule:** A tag can be marked kept together. It can then have a home place, and the app can say which of its items are away from that home.

**Acceptance:** R11 passes fully, with the home and away views that stage 5 holds, as stage 5's gate requires (R100).

**Since:** 2026-10-28 · owner · migrated

### D-R37 · Tag kinds

**Rule:** A tag can carry a kind for display: function, project, season, place-like or other. Tag kinds are user-editable (R47).

**Acceptance:** none:

**Since:** 2026-10-28 · builder · migrated

### Q-R38 · Default tag kinds

**Rule:** Which tag kinds exist by default? It is needed by stage 2.

**Default:** The kinds R37 lists, a builder's call: function, project, season, place-like and other. An answer sets the kinds a new install starts with.

**Why:** R37 lists kinds for display; the source leaves open which of them exist by default.

**Since:** 2026-10-28 · unrecorded · migrated

### D-R39 · Suggested tags

**Rule:** Tags a model suggests are stored as suggestions until the user accepts them. Once accepted, they look the same as tags the user made.

**Acceptance:** R06 passes: the suggested tags the user keeps are saved with the item, and the deleted one is not.

**Since:** 2026-10-28 · unrecorded · migrated

### D-R40 · Editing suggestions

**Rule:** Item, place and tag information that a model suggests is always fully editable, one at a time or in bulk.

**Acceptance:** R07 and R08 pass: suggested tags are cleared in one action, and wrong items are corrected one at a time.

**Since:** 2026-10-28 · owner · migrated

### D-R41 · Photos

**Rule:** A photo belongs to an item or to a place. A place photo of a settled area can have regions that point to the items they show, like the marked spots of Spot It.

**Acceptance:** none:

**Since:** 2026-10-28 · unrecorded · migrated

### D-R42 · Proposals from a photo

**Rule:** Recognition can propose items from a photo (R06 to R08). Each proposal is accepted, corrected or discarded, and a photo can be split into several separate items (R08).

**Acceptance:** R06, R07 and R08 pass, as stage 4's gate requires (R100).

**Since:** 2026-10-28 · unrecorded · migrated

### D-R43 · Custom fields

**Rule:** Users define their own custom fields. Full or near-full customisation is a main focus of the app.

**Acceptance:** none: a direction; how far customisation reaches is workshopped per screen.

**Since:** 2026-10-28 · owner · migrated

### D-R44 · Field types

**Rule:** Fields can go on items and on places, with the types below, sized for both modules (R97).

**Values:**

| Type |
|---|
| text |
| number with a typed unit |
| date |
| choice list |
| checkbox |
| link to another item or place |

**Acceptance:** none:

**Since:** 2026-10-28 · unrecorded · migrated

### Q-R45 · Final field types and presets

**Rule:** What is the final list of custom field types and presets, worked out with the owner from use cases? It is needed by stage 1.

**Default:** The types in R44 and the module fields in R97, as drafted.

**Why:** R44 and R97 hold a drafted list; the source settles the final one with the owner, from use cases.

**Since:** 2026-10-28 · unrecorded · migrated

### D-R46 · Where a field attaches

**Rule:** A field can be attached to a tag, so every item carrying that tag gets the field ("clothes" → size), or added to a single item.

**Acceptance:** none:

**Since:** 2026-10-28 · builder · migrated

### D-R47 · User-editable lists

**Rule:** Place kinds, tag kinds, homepage cards and the widget's content are user-editable.

**Acceptance:** none:

**Since:** 2026-10-28 · unrecorded · migrated

## 5. Storage, sync and backup

### D-R48 · Local database

**Rule:** Each device keeps a local database.

**Acceptance:** none:

**Since:** 2026-10-28 · builder · migrated

### Q-R49 · Database library

**Rule:** Which database library does the app use, SQLDelight or Room KMP? It is needed by stage 1.

**Default:** none proposed: the source names the two candidates and leaves the choice to stage 1.

**Why:** SQLDelight and Room KMP are both named, with no reason given to prefer either.

**Since:** 2026-10-28 · builder · migrated

### D-R50 · A database that fails to open

**Rule:** A database that fails to open is kept, never deleted automatically.

**Acceptance:** In stage 1's round-trip tests, a corrupted database is kept, not deleted (R100).

**Since:** 2026-10-28 · unrecorded · migrated

### D-R51 · Folder sync

**Rule:** The phone and the PC share one inventory through two-way sync over a folder, kept in step by a file-sync tool such as Syncthing. Each device appends its changes as small files in the folder, and reads and merges the other device's.

**Acceptance:** Two-device sync with conflicts passes stage 1's round-trip tests (R100).

**Since:** 2026-10-28 · owner · migrated

### D-R52 · Record ids and history

**Rule:** Every record has a globally unique id and a modification history.

**Acceptance:** none:

**Since:** 2026-10-28 · unrecorded · migrated

### D-R53 · No shared database file

**Rule:** No database file is ever shared directly through the folder.

**Why:** File sync corrupts live databases.

**Acceptance:** none:

**Since:** 2026-10-28 · unrecorded · migrated

### D-R54 · Conflicting edits

**Rule:** When both devices edited the same field, the later edit wins. The overwritten value stays in history, so it can be restored.

**Acceptance:** In stage 1's two-device sync test with conflicts (R100), the later edit wins and the overwritten value can be restored from history.

**Since:** 2026-10-28 · builder · migrated

### D-R55 · Sync groups

**Rule:** Each user-visible field is its own sync group, and every user-visible group keeps the losing value, so that the history of R54 holds.

**Acceptance:** none:

**Not:** One group per row, and discarding the losing value except in a few groups, as the earlier app does by default: the history of R54 would not hold.

**Since:** 2026-10-28 · unrecorded · migrated

### D-R56 · Photos in the sync folder

**Rule:** Photos travel as files in the same folder, named by id.

**Acceptance:** none:

**Since:** 2026-10-28 · unrecorded · migrated

### D-R57 · Any folder-sync tool

**Rule:** Sync works with any folder-sync tool. The app does not talk to Syncthing itself.

**Acceptance:** none:

**Since:** 2026-10-28 · unrecorded · migrated

### D-R58 · Sync code from the earlier app

**Rule:** The folder sync of the earlier app, the owner's own code, is the starting point. Its parts are taken over as the rows below say.

**Values:**

| Part | Holds | Taken over |
|---|---|---|
| `:core` engine | ULID ids, hybrid logical clock stamps, field groups, merge with a permanent purge registry | almost unchanged |
| Folder layer | 16 KiB per-device log segments, snapshots every 64, atomic writes, Syncthing conflict copies merged, waiting rows for missing parents, outbox, photos stored by SHA-256 under `blobs/` | adapted: database and group sets passed in, sync tables moved into Chatelaine's schema, new row adapters for items, places, tags and photos |

**Evidence:** Review of the earlier app, 2026-10-28.

**Acceptance:** none: a choice of starting code; R51, R54 and R55 say what the result must do.

**Since:** 2026-10-28 · unrecorded · migrated

### D-R59 · Sync lessons from the earlier app

**Rule:** The sync follows the lessons below, adopted from the earlier app.

**Values:**

| Subject | Lesson |
|---|---|
| Replayed versions | ignore a replayed version no newer than the base |
| Device-id file | keep it out of Android Auto Backup |
| Export | through an outbox, not a stamp cursor |
| Purge registry | permanent: 90-day tombstones resurrected rows in another of the owner's apps |
| Recovered database | re-reads its own folder files |

**Evidence:** Review of the earlier app, 2026-10-28.

**Acceptance:** none:

**Since:** 2026-10-28 · unrecorded · migrated

### D-R60 · Backup and export

**Rule:** The app backs up automatically to a folder the user picks, and offers a manual export of JSON and photos.

**Acceptance:** Export → import passes stage 1's round-trip tests (R100).

**Since:** 2026-10-28 · owner · migrated

### D-R61 · Restoring a backup

**Rule:** A backup carries each value's sync stamps, and restoring it merges like any other device's changes. Restoring and rolling back work as the rows below say.

**Why:** The earlier app taught that a stamp-less backup overwrites every device.

**Values:**

| Action | Effect |
|---|---|
| Restore | merges by stamps: edits newer than the backup survive on both devices |
| Roll back to a backup | a separate, deliberate action, recorded as a change; the state it replaced stays in history |

**Evidence:** The earlier app built only a settings backup, so full-backup merge and the rollback action are new (review of the earlier app, 2026-10-28).

**Acceptance:** none:

**Since:** 2026-10-28 · builder · migrated

## 6. Models and recognition

### D-R62 · Image recognition

**Rule:** Image recognition is core: a small model ships with the app, and a larger, more accurate one is an optional download. Recognition runs on both devices.

**Acceptance:** As stage 4's gate requires (R100), R65's bar is met on both target phones, and R06, R07 and R08 pass.

**Since:** 2026-10-28 · owner · migrated

### D-R63 · Model roles

**Rule:** The app's models fill the roles below.

**Values:**

| Role | Model kind | Ships | Used for |
|---|---|---|---|
| Ears | speech-to-text, whisper.cpp class, or the platform recogniser offline | base set, downloaded on first use of dictation (R19) | dictated logging (stage 8) |
| Similarity | small multilingual text embedding model | base | tag suggestions, grouping, search by meaning |
| Eyes (small) | small on-device recognition model | base | R06 to R08: objects, counts, rough tags |
| Eyes (large) | larger recogniser or multimodal model, Gemma 4 E2B class | optional download | better recognition in crowded photos |
| Labeller | small LLM, Qwen3 0.6B or Gemma class | optional download | function tags for an item, names for a new parent tag, splitting a dictated list |
| Your logic | nearest neighbour over the user's examples (R71) | base, no model | personal tag and place suggestions |

**Acceptance:** Each optional model measurably improves a scenario, as stage 9's gate requires (R100).

**Since:** 2026-10-28 · unrecorded · migrated

### D-R64 · Models on device

**Rule:** Every model runs on the device. Models larger than the base set are downloaded on request, never bundled.

**Acceptance:** none:

**Since:** 2026-10-28 · unrecorded · migrated

### D-R65 · Recognition pass bar

**Rule:** Recognition passes when every row below holds on both target phones, a Snapdragon 888 flagship and a Snapdragon 695 budget phone; the slower one sets the bar. It is measured on a fixed set of about 20 photos taken for the purpose and kept out of the repository.

**Values:**

| Measure | Bar |
|---|---|
| Time | at most about 10 s per photo |
| Stability | no crash or overheating across the set |
| Single objects (R06) | the right name among the first three suggestions in at least 4 of 5 photos |
| Crowded boxes (R08) | at least a third of the items found, and correction quick enough for the rest |

**Evidence:** Stage 0 spike, 2026-10-28 to 2026-10-30 (`docs/evidence/stage0-recognition.md`): by the owner's blind marks no offline recogniser met the bar, and the cloud models with the tightened instructions cleared it widely.

**Acceptance:** The bar is met on both target phones, as stage 4's gate requires (R100).

**Not:** The Snapdragon 845 phone, the first target: too old for this project.

**Since:** 2026-10-29 · owner · migrated

### O-R66 · Scoring crowded answers

**Rule:** For a crowded photo, each recogniser's answer is scored on as many lines as the shortest answer for that photo.

**Why:** Extra lines are extra reading.

**Evidence:** Used for the crowded scores in `docs/evidence/stage0-recognition.md`.

**Acceptance:** none: a scoring practice; R65 carries the check.

**Since:** 2026-10-29 · owner · migrated

### D-R67 · Optional language model

**Rule:** An optional on-device language model makes tag suggestions. Its target phones are those of R65.

**Evidence:** Not checked: stage 0 holds timing the small language model against R65 on the target phones (R100), and the source records no result.

**Acceptance:** The model measurably improves a scenario, as stage 9's gate requires (R100).

**Since:** 2026-10-29 · owner · migrated

### D-R68 · Open-vocabulary recogniser

**Rule:** The recogniser is open-vocabulary: image–text matching or a small multimodal model. The stage 0 spike measures this kind of model, not a fixed-class detector.

**Why:** R06 needs names outside the usual fixed detector classes: "headphones" is not among the 80 COCO classes.

**Evidence:** Stage 0 measured open-vocabulary detectors, image–text naming and small multimodal models (`docs/evidence/stage0-recognition.md`).

**Acceptance:** R06 passes: the recogniser names the headphones.

**Not:** A fixed-class detector: it has no name for things outside its classes.

**Since:** 2026-10-28 · builder · migrated

### D-R69 · Recognition as a hint

**Rule:** Recognition is a hint, never the record. Crowded or overlapping photos are expected to be partly wrong: R08 depends on easy correction, not on accuracy.

**Acceptance:** R08 passes.

**Since:** 2026-10-28 · unrecorded · migrated

### D-R70 · Model cooperation

**Rule:** The models cooperate as the rows below say.

**Values:**

| Case | Behaviour |
|---|---|
| Order | cheap models run first; the LLM runs only when they disagree or are not confident |
| Sources agree | the suggestion takes one tap |
| Sources disagree | the user sees the candidates |
| The user chooses | the user's choice always wins and becomes an example (R71) |
| Heavy jobs | run in a queue, one model in memory at a time, preferably while charging |

**Acceptance:** Suggestions are accepted or corrected in one tap, as stage 5's gate requires (R100).

**Since:** 2026-10-28 · unrecorded · migrated

### D-R71 · Personal learning

**Rule:** The user's sorted items act as labelled examples: a new item's nearest neighbours carry the user's own logic. Corrections always outrank model output. Nothing is trained or uploaded.

**Acceptance:** As stage 5's gate requires (R100), suggestions are accepted or corrected in one tap, and R11 passes fully.

**Since:** 2026-10-28 · unrecorded · migrated

### D-R72 · Model licences

**Rule:** Each model's licence is checked before it is adopted. The weights' licence must allow redistribution from a GPL app's download screen.

**Acceptance:** none:

**Since:** 2026-10-28 · unrecorded · migrated

### O-R73 · Recognition instructions

**Rule:** The recognition instructions are not tuned further.

**Why:** Every change to the instructions lost accuracy somewhere, and the owner judged that more rules confuse the model and lengthen the user's work.

**Evidence:** Of the Qwen runs in `docs/evidence/stage0-recognition.md`, the plain round 3 tightened instructions, shared with the cloud models, are the most accurate.

**Acceptance:** none: a working practice.

**Not:** Instructions tuned to a model's faults, or anti-repeat rules added to them: each lost accuracy on the stage 0 photos.

**Since:** 2026-10-30 · owner · migrated

### D-R74 · Repeated lines

**Rule:** The app cuts repeated lines from a recogniser's answer and caps the answer at 15 lines.

**Why:** A loop is better handled by the app than by more instructions (R73).

**Evidence:** Qwen3-VL-8B looped on 1 drawer of 10 with the tightened instructions (`docs/evidence/stage0-recognition.md`).

**Acceptance:** none:

**Since:** 2026-10-30 · builder · migrated

### D-R75 · Segmentation stage

**Rule:** Recognition has no cut-out (segmentation) stage.

**Why:** On the stage 0 photos the background caused few errors, mostly things that blend in; a crop step would have removed about one line, and low-contrast things are hard for cut-out models too. Light, such as a torch button on Capture, fits the cause better.

**Evidence:** Errors sorted by cause on 2026-11-03 (`docs/evidence/stage0-recognition.md`). Not checked: dim light, since all 20 photos were well lit; misses are judged against an answer sheet the owner has not checked; 6 of the detector's tags are likely, not seen.

**Acceptance:** none: it rules a stage out, so nothing is built.

**Since:** 2026-11-03 · unrecorded · migrated

### Q-R76 · Smaller recogniser model

**Rule:** Does a smaller model of the Qwen3-VL family, 2B or 4B, keep enough of the 8B model's naming on the Snapdragon 888 phone within R65? It is open for stage 0.

**Default:** none proposed: it waits on a measurement the source has not made.

**Why:** Offline on a PC, Qwen3-VL-8B names nearly as well as the cloud models, but fails R65 on time (about 48 s per photo on a desktop PC) and R19 on size (5.75 GB). A smaller model may fit the phone; whether it keeps the naming is unmeasured.

**Since:** 2026-10-30 · unrecorded · migrated

### Q-R77 · Recogniser as a first-use download

**Rule:** May the recogniser be downloaded on first use, like the speech model (R19)? It is open for stage 0.

**Default:** none proposed: the source leaves it to the owner, with no leaning.

**Why:** R19 ships the small recogniser inside the phone download, under about 150 MB, and only the speech model waits for first use; the best offline namer in stage 0 is far larger (R76).

**Since:** 2026-10-30 · unrecorded · migrated

### Q-R78 · AI-service key

**Rule:** Should the user be able to supply an optional AI-service key (Gemini, Claude, self-hosted, any provider), off by default and sending photos only when turned on? It is needed by stage 4.

**Default:** none proposed: the owner is open to it for personal use but has not decided. If adopted, it changes R15, and the app would send the tightened instructions with each photo, not a bare question.

**Why:** R15 keeps the app offline. On the other side, stage 0 found that with the tightened instructions both Gemini (free key) and Claude clear R65 with a wide margin, which no offline recogniser did (`docs/evidence/stage0-recognition.md`).

**Since:** 2026-10-30 · unrecorded · migrated

## 7. Logging and search

### O-R79 · Correction-first logging form

**Rule:** The form for logging objects and clusters from a photo is designed around quickly editing the recogniser's answer: a list of "about N x name" lines, some right, some close, some wrong. Turning a close line into the right one, fixing a count and dropping a wrong line are each quick. The screen is designed with the owner, through the purpose map (R90), before it is built.

**Why:** When recognition is approximate, the logging form carries the load. The stage 0 scores show most misses are close names rather than wrong ones (`docs/evidence/stage0-recognition.md`).

**Acceptance:** none:

**Since:** 2026-10-30 · owner · migrated

### O-R80 · Lump, then refine

**Rule:** A good generic name with an approximate count beats a split list. Each line is editable in a few taps: change the number, drop or add a word, remove "about". When a split is worth making, it follows what matters for the things' use, not colour.

**Why:** Adjusting a line is quicker than deleting wrong ones, and a line such as "about 20 x notebook" is easy to judge true at a glance.

**Values:**

| Case | Lines | Note |
|---|---|---|
| Refining a line | "about 3 x wool scarf" → +1, +1, drop "about", drop "wool" → "5 x scarf", save | the owner's example |
| A split by use | "about 9 x small notebook", "about 10 x notebook" | size matters for note-taking |

**Acceptance:** none:

**Not:** A split list by colour, such as "2 x umbrella" plus "1 x purple umbrella" in place of "3 x umbrella": adjusting a line is quicker than deleting wrong ones.

**Since:** 2026-10-30 · owner · migrated

### D-R81 · Search through child tags

**Rule:** Searching a parent tag also finds the items that carry any of its child tags.

**Acceptance:** none:

**Since:** 2026-10-28 · unrecorded · migrated

### D-R82 · Search across languages

**Rule:** Search matches across languages: "cucito" finds "sewing". Tags are stored once, as written; model suggestions are offered in the interface language, and built-in names such as place and tag kinds switch with the language.

**Acceptance:** none:

**Since:** 2026-10-28 · builder · migrated

## 8. Place map, homepage and clear-box surfaces

### D-R83 · Visual place map

**Rule:** The app draws a visual map of storage automatically from the place tree: stylised places, as blocks the user can rearrange. A free-drawing editor may come later, in stage 11 (R100).

**Acceptance:** R10 passes, as stage 6's gate requires (R100).

**Since:** 2026-10-28 · owner · migrated

### Q-R84 · Homepage layout

**Rule:** What does the homepage hold, and how is it laid out? The owner's sketch below is an input to the purpose map (R90), not a decided layout.

**Default:** The owner's sketch, as the rows below give it, with reach and density following R95.

**Why:** The sketch is not decided; the purpose map (R90) and its scoring (R91) decide the screens.

**Values:**

| Position | Element | Opens |
|---|---|---|
| Top | search bar | — |
| Below the search bar | Places card | the house map; a list of floor cards if more than one floor exists; a grid of houses if more than one house exists |
| Below the search bar | Tags card | a grid of top-level (parent) tags |
| Below the cards | object of the day | — |

**Since:** 2026-10-28 · owner · migrated

### D-R85 · Tag card

**Rule:** The homepage's tag card shows top-level tags only.

**Evidence:** Not checked: the homepage layout is open (R84); this rule assumes the sketch's tag card.

**Acceptance:** none:

**Since:** 2026-10-28 · owner · migrated

### D-R86 · No push by default

**Rule:** The app never pushes by default. Proactive prompts, such as notifications, are opt-in; the clear-box effect comes from passive surfaces (R87).

**Acceptance:** none:

**Since:** 2026-10-28 · owner · migrated

### D-R87 · Clear-box surfaces

**Rule:** The surfaces below keep stored things in view. The owner ranked them from 3, most helpful, down to 1.

**Values:**

| Surface | Rank | Notes |
|---|---|---|
| Home-screen widget | 3 | rotating contents of a place or tag; Android first; the Windows equivalent is R89 |
| In-app photo browsing | 3 | places shown by their photos; open a place, see what is in it |
| Printed photo labels | 2 | R88 |
| Object of the day | not ranked | on the homepage (R84) |
| Lock screen or wallpaper | 1 | later, stage 11 |
| "Rediscover" prompt | 1 | opt-in only; later, stage 11 |

**Evidence:** Not checked: the object of the day's place on the homepage follows the proposed default of R84.

**Acceptance:** The owner uses the widget and the labels for a week, as stage 7's gate requires (R100).

**Since:** 2026-10-28 · owner · migrated

### D-R88 · Labels

**Rule:** Labels are printable A4 sticker sheets (PDF) with a photo collage of the contents and a QR code that opens the place. NFC tags come later, in stage 11 (R100).

**Acceptance:** The owner uses the labels for a week, as stage 7's gate requires (R100).

**Since:** 2026-10-28 · owner · migrated

### Q-R89 · Windows widget

**Rule:** What is the Windows equivalent of the home-screen widget: a tray, a startup window, or none? It is needed by stage 7.

**Default:** none proposed: the source lists the three options with no leaning.

**Why:** R87 brings the widget to Android first and leaves the Windows side open.

**Since:** 2026-10-28 · unrecorded · migrated

## 9. Purpose map and look

### D-R90 · Purpose map

**Rule:** The purpose map decides which purpose each screen serves before any screen is built. It aims to minimise the number of screens and maximise their quality, with other apps scored beside it as reference apps.

**Acceptance:** The owner approves the spec and the purpose map, as stage 0's gate requires (R100).

**Since:** 2026-10-28 · owner · migrated

### D-R91 · Scoring axes

**Rule:** The purpose map is scored on the axes below, agreed with the owner before scoring, with equal weights and ties shown as ties. Layout axes are set per screen from stage 2.

**Values:**

| Axis | Measures | Chooses |
|---|---|---|
| Coverage | every reference-app function and every step of R05 to R11 lands in a purpose | the purpose set |
| Exclusivity | — | the purpose set |
| Balance | — | the purpose set |
| Fit | fit to Chatelaine's intent | the purpose set |
| Potential | potential improvement over the reference apps | the purpose set |
| Screen count | fewer is better | screens and homes |
| One home per thing | repeats count against | screens and homes |
| Steps per scenario | taps from the homepage through R05 to R11 | screens and homes |
| Recall load | steps that ask the user to remember what the screen does not show | screens and homes |

**Acceptance:** Every reference app, scored as a control, loses: a control that scores well means the axes are wrong.

**Since:** 2026-10-28 · owner · migrated

### D-R92 · Homepage shortcuts in scoring

**Rule:** The homepage is a master board: its shortcuts, the two cards and the object of the day, do not count as repeats under one home per thing (R91).

**Evidence:** Not checked: the homepage layout is open (R84); the shortcuts named are those of its sketch.

**Acceptance:** none: a scoring practice.

**Since:** 2026-10-28 · owner · migrated

### D-R93 · Screens before building

**Rule:** No screens are built at stage 0. Screens start at stage 2 or later, each discussed with the owner before it is built.

**Acceptance:** Every screen built is owner-approved, as stage 2's gate requires (R100).

**Since:** 2026-10-28 · owner · migrated

### D-R94 · Look and feel

**Rule:** The look is specific to the app's purpose, not framework defaults, and follows the direction below. It is workshopped per screen. The owner's other apps are conceptual references, not templates.

**Values:**

| Aspect | Direction |
|---|---|
| Mood | calm, friendly, low visual noise |
| Colour | relaxing to look at in greyscale, before colour |
| Shapes | shapes guide the user |
| Leading axes | density and the hierarchy of visual function |
| Framework | not stock Material: the framework is plumbing, the look has its own identity |

**Acceptance:** none: a direction, workshopped per screen.

**Since:** 2026-10-28 · owner · migrated

### D-R95 · Reach and density

**Rule:** Reach and density follow the model of the owner's earlier app, not that of another of the owner's apps: not everything is visible at once.

**Acceptance:** none: a direction, workshopped per screen.

**Since:** 2026-10-28 · owner · migrated

### D-R96 · Consistent elements

**Rule:** The same element reads and acts the same on every screen, and controls at the same level keep the same size and place.

**Acceptance:** none:

**Since:** 2026-10-28 · unrecorded · migrated

## 10. Modules and the shopping list

### D-R97 · Modules

**Rule:** Chatelaine is one app on one shared core. Wardrobe and Pantry are optional modules, off by default, each designed with the owner first; users can build their own packs with the same tools.

**Values:**

| Module | Fields | Views |
|---|---|---|
| Wardrobe | size, colour, material, season, occasion, last worn, laundry | outfits, seasonal swap, not worn in a year |
| Pantry | quantity and unit, expiry, opened on, minimum stock | expiring soon, shopping list, "already have it" check |

**Acceptance:** Each module passes its own scenarios, written with the owner, as stage 10's gate requires (R100).

**Since:** 2026-10-28 · owner · migrated

### O-R98 · Savings app and the shopping list

**Rule:** Personal savings is a separate app, not a Chatelaine module, and the two meet only through the shopping list, which belongs to Chatelaine's Pantry module (R97). When something goes on the list, Chatelaine shows what is already owned and where ("you already have 3 of these"), and pantry stock (R97) is used first. Each bought item goes one way to the savings app as item, price and date, by export or a shared file; budgets and goals stay in the savings app.

**Why:** Money is a different job (amounts, accounts, budgets) with its own rhythm and tone, and would raise the security bar for app lock, backups and folder sync. Chatelaine owns the shopping list because it knows the stock.

**Acceptance:** none:

**Not:** Savings as a module inside Chatelaine: ruled out for the reasons in Why.

**Since:** 2026-11-22 · owner · agreed rewording

### Q-R99 · Reminders

**Rule:** Which app owns reminders, given that the owner's earlier app has reminders and may overlap? It is needed by stage 10.

**Default:** none proposed: the source leaves it open.

**Why:** R98 settles that Chatelaine owns the shopping list and leaves reminders open; the earlier app already has them.

**Since:** 2026-11-19 · owner · migrated

## 11. Stages

### D-R100 · Stage plan

**Rule:** The work runs in the stages below, in order of dependency. Each stage ends with its done-when check and, from stage 2 on, a walk on a real device.

**Values:**

| Stage | Holds | Done when | Unlocks |
|---|---|---|---|
| 0 Foundations | the spec; purpose map scored on the axes of R91; screen-by-purpose decisions; reference apps walked on the Snapdragon 845 phone; throwaway spike: small LLM and open-vocabulary recognition timed against R65 on the target phones; Compose Multiplatform window running on Android and Windows (R102) | owner approves the spec and the purpose map; spike timings recorded (`docs/evidence/stage0-recognition.md`) | — |
| 1 Data and safety | data model (items, places, tags with parents and kept-together, photos, states, custom fields, history); database; backup and export; change-file sync | round-trip tests: export → import, two-device sync with conflicts, a corrupted database is kept, not deleted | — |
| 2 Logging and tags | quick add by name; unplaced inbox; item editor; tags with parents; bulk tag add and remove; search including child tags | owner-approved screens built; R05 passes | R05 |
| 3 Places and photos | place tree; camera (phone only, R21); item and place photos; settled-area path; place contents view; in use / away; move and bulk move | R09 passes; the move part of R11 passes | R09 |
| 4 Recognition | small open-vocabulary recognition model on phone and PC (PC works on synced phone photos); suggestions from photos; count; split a photo into items; bulk clear of suggestions | the bar of R65 is met on both target phones; R06, R07 and R08 pass | R06, R07, R08 |
| 5 Suggestions and grouping | embedding model; personal nearest-neighbour layer; tag suggestions for new items; kept-together tags with home and away views | suggestions accepted or corrected in one tap; R11 passes fully | R11 |
| 6 Visual map | automatic stylised place map with rearrangeable blocks; filter search by house or floor; fallback results from other places | R10 passes | R10 |
| 7 Clear-box surfaces | photo browsing; home-screen widget; label PDF with QR; object of the day | owner uses the widget and labels for a week | — |
| 8 Voice | dictated logging in Italian and English on the phone (R21); speech model downloaded on first use (R19); splitting a list into items | a dictated list of ten things becomes ten items | — |
| 9 Optional models | large recogniser; labeller LLM; download screen; licence checks | each optional model measurably improves a scenario | — |
| 10 Modules | the modules of R97, each designed with the owner first | each module passes its own scenarios, to be written with the owner | — |
| 11 Later | lending; app lock; consumables; NFC; lock-screen surface; rediscover prompt; free-drawing map editor; F-Droid build | each gets its own decision first | — |

**Acceptance:** Each stage closes only when its done-when holds and, from stage 2 on, after a walk on a real device.

**Since:** 2026-10-29 · unrecorded · migrated

### D-R101 · Order of stages 4 to 6

**Rule:** The visual map (stage 6) comes after recognition (stage 4) and suggestions (stage 5). The owner may reorder stages 4 to 6: none of them depends on the others.

**Why:** R06, R07, R08 and R11 are daily-use loops, while the map is reached through places that already exist from stage 3.

**Acceptance:** none: an ordering choice.

**Since:** 2026-10-28 · builder · migrated

### O-R102 · Stage 0 spikes

**Rule:** Stage 0 reviews the sync of the earlier app in place of a change-file-sync spike; the review was done on 2026-10-28 (R58). The KMP spike stays and proves a Compose Multiplatform window running on Android and Windows. The model spike also stays, on the target phones of R65.

**Why:** The review found the earlier app has no Compose and no runnable Windows app.

**Acceptance:** A Compose Multiplatform window runs on Android and on Windows, and the spike timings are recorded, as stage 0 requires (R100).

**Not:** Replacing the KMP spike with the review as well, as first planned: the earlier app has no Compose and no runnable Windows app.

**Since:** 2026-10-29 · owner · migrated

## Acceptance list

- R05 · Project from a tag: The steps succeed end to end, as stage 2's gate requires (R100).
- R06 · Photo with suggestions: The steps succeed end to end, as stage 4's gate requires (R100).
- R07 · Bulk clear: The steps succeed end to end, as stage 4's gate requires (R100).
- R08 · Box split into items: The steps succeed end to end, as stage 4's gate requires (R100).
- R09 · Before buying: The steps succeed end to end, as stage 3's gate requires (R100).
- R10 · Visual place map and filtered search: The steps succeed end to end, as stage 6's gate requires (R100).
- R11 · Seasonal swap: The move part succeeds at stage 3 and the whole scenario at stage 5, as those stages' gates require (R100).
- R13 · Framework and targets: A Compose Multiplatform window runs on Android and on Windows, as stage 0 holds (R100, R102).
- R18 · Languages: A dictated list of ten things becomes ten items, in Italian and in English, as stage 8's gate requires (R100).
- R19 · Phone download size: The compressed phone download measures under about 150 MB.
- R22 · Publishing rules: The publish check reports no finding before each commit and push, and in CI.
- R28 · Identical things: R06 passes with the three headphones saved as one item with a count of three.
- R32 · Place tree: R10 passes, with its map running from drawer partitions up to several houses.
- R34 · Tags: R05 passes.
- R36 · Kept together: R11 passes fully, with the home and away views that stage 5 holds, as stage 5's gate requires (R100).
- R39 · Suggested tags: R06 passes: the suggested tags the user keeps are saved with the item, and the deleted one is not.
- R40 · Editing suggestions: R07 and R08 pass: suggested tags are cleared in one action, and wrong items are corrected one at a time.
- R42 · Proposals from a photo: R06, R07 and R08 pass, as stage 4's gate requires (R100).
- R50 · A database that fails to open: In stage 1's round-trip tests, a corrupted database is kept, not deleted (R100).
- R51 · Folder sync: Two-device sync with conflicts passes stage 1's round-trip tests (R100).
- R54 · Conflicting edits: In stage 1's two-device sync test with conflicts (R100), the later edit wins and the overwritten value can be restored from history.
- R60 · Backup and export: Export → import passes stage 1's round-trip tests (R100).
- R62 · Image recognition: As stage 4's gate requires (R100), R65's bar is met on both target phones, and R06, R07 and R08 pass.
- R63 · Model roles: Each optional model measurably improves a scenario, as stage 9's gate requires (R100).
- R65 · Recognition pass bar: The bar is met on both target phones, as stage 4's gate requires (R100).
- R67 · Optional language model: The model measurably improves a scenario, as stage 9's gate requires (R100).
- R68 · Open-vocabulary recogniser: R06 passes: the recogniser names the headphones.
- R69 · Recognition as a hint: R08 passes.
- R70 · Model cooperation: Suggestions are accepted or corrected in one tap, as stage 5's gate requires (R100).
- R71 · Personal learning: As stage 5's gate requires (R100), suggestions are accepted or corrected in one tap, and R11 passes fully.
- R83 · Visual place map: R10 passes, as stage 6's gate requires (R100).
- R87 · Clear-box surfaces: The owner uses the widget and the labels for a week, as stage 7's gate requires (R100).
- R88 · Labels: The owner uses the labels for a week, as stage 7's gate requires (R100).
- R90 · Purpose map: The owner approves the spec and the purpose map, as stage 0's gate requires (R100).
- R91 · Scoring axes: Every reference app, scored as a control, loses: a control that scores well means the axes are wrong.
- R93 · Screens before building: Every screen built is owner-approved, as stage 2's gate requires (R100).
- R97 · Modules: Each module passes its own scenarios, written with the owner, as stage 10's gate requires (R100).
- R100 · Stage plan: Each stage closes only when its done-when holds and, from stage 2 on, after a walk on a real device.
- R102 · Stage 0 spikes: A Compose Multiplatform window runs on Android and on Windows, and the spike timings are recorded, as stage 0 requires (R100).

## Out of scope

### D-R103 · Insurance records

**Rule:** Value, receipts and serial numbers for insurance use are out of scope for v1. The one price kept is on bought shopping-list items, for the savings-app bridge (R98).

**Acceptance:** none: a scope boundary.

**Since:** 2026-11-19 · owner · migrated

### D-R104 · App lock

**Rule:** An app lock (biometric, PIN or password) is not in v1. The stage plan lists it for stage 11 (R100).

**Acceptance:** none: a scope boundary; stage 11 gives it its own decision first.

**Since:** 2026-10-28 · owner · migrated

### D-R105 · Lending

**Rule:** Lending, recording who has a thing, comes in a later stage. The stage plan lists it for stage 11 (R100).

**Acceptance:** none: a scope boundary; stage 11 gives it its own decision first.

**Since:** 2026-10-28 · owner · migrated

### D-R106 · Inventory import

**Rule:** There is no existing inventory data to import, so stage 1 has no importer for it.

**Acceptance:** none: a scope boundary.

**Since:** 2026-10-28 · owner · migrated

### D-R107 · Stock and expiry outside Pantry

**Rule:** Low-stock alerts, shopping lists and expiry tracking outside the Pantry module (R97) are not in v1.

**Acceptance:** none: a scope boundary.

**Since:** 2026-10-28 · unrecorded · migrated
