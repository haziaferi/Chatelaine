# Chatelaine spec

Scope: Chatelaine, a home inventory for one user on an Android phone and a Windows PC, which keeps stored things in view.

History: `SPEC-history.md`, one line per change. It is never a source of current rules.

Kinds: O owner rule · D decided · I inherited · A assumed · Q open.

## Rule index

| ID | Name | Section |
|---|---|---|
| O-R01 | Users and devices | 1. Purpose and jobs |
| D-R02 | Goals | 1. Purpose and jobs |
| D-R03 | Anti-goals | 1. Purpose and jobs |
| D-R04 | Jobs and how often | 1. Purpose and jobs |
| D-R05 | Project from a tag | 2. Acceptance scenarios |
| D-R06 | Photo with suggestions | 2. Acceptance scenarios |
| D-R07 | Bulk clear | 2. Acceptance scenarios |
| D-R08 | Box split into items | 2. Acceptance scenarios |
| D-R09 | Before buying | 2. Acceptance scenarios |
| D-R10 | Visual place map and filtered search | 2. Acceptance scenarios |
| D-R11 | Seasonal swap | 2. Acceptance scenarios |
| D-R114 | Cheapest pack under a tag | 2. Acceptance scenarios |
| D-R115 | Cheaper than the saved price | 2. Acceptance scenarios |
| D-R12 | Name, package, licence and repository | 3. Platforms and build |
| O-R13 | Framework and targets | 3. Platforms and build |
| D-R14 | Shared code | 3. Platforms and build |
| D-R15 | Offline first | 3. Platforms and build |
| Q-R108 | Household network | 3. Platforms and build |
| D-R16 | No Google Play services | 3. Platforms and build |
| D-R17 | F-Droid build | 3. Platforms and build |
| D-R18 | Languages | 3. Platforms and build |
| D-R19 | Phone download size | 3. Platforms and build |
| I-R20 | Owner's PC | 3. Platforms and build |
| O-R21 | Desk companion | 3. Platforms and build |
| O-R22 | Publishing rules | 3. Platforms and build |
| D-R23 | Borrowing code | 3. Platforms and build |
| D-R24 | Sources to borrow from | 3. Platforms and build |
| D-R25 | Item | 4. Items, places and tags |
| O-R26 | Quantity and counts | 4. Items, places and tags |
| D-R27 | Consumables and low stock | 4. Items, places and tags |
| D-R28 | Identical things | 4. Items, places and tags |
| D-R29 | Order of an item's facts | 4. Items, places and tags |
| D-R30 | Item states | 4. Items, places and tags |
| D-R31 | Photo outdated flag | 4. Items, places and tags |
| D-R32 | Place tree | 4. Items, places and tags |
| D-R111 | Items and places | 4. Items, places and tags |
| D-R33 | Place details | 4. Items, places and tags |
| D-R34 | Tags | 4. Items, places and tags |
| D-R35 | Tag tree | 4. Items, places and tags |
| D-R36 | Kept together | 4. Items, places and tags |
| D-R37 | Tag kinds | 4. Items, places and tags |
| D-R38 | Default tag kinds | 4. Items, places and tags |
| D-R39 | Suggested tags | 4. Items, places and tags |
| O-R40 | Editing suggestions | 4. Items, places and tags |
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
| O-R51 | Folder sync | 5. Storage, sync and backup |
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
| O-R62 | Image recognition | 6. Models and recognition |
| D-R63 | Model roles | 6. Models and recognition |
| D-R64 | Models on device | 6. Models and recognition |
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
| D-R78 | AI-service key | 6. Models and recognition |
| O-R79 | Correction-first logging form | 7. Logging and search |
| O-R80 | Lump, then refine | 7. Logging and search |
| D-R81 | Search through sub-tags | 7. Logging and search |
| D-R82 | Search across languages | 7. Logging and search |
| D-R83 | Visual place map | 8. Place map, homepage and clear-box surfaces |
| Q-R84 | Homepage layout | 8. Place map, homepage and clear-box surfaces |
| D-R85 | Tag card | 8. Place map, homepage and clear-box surfaces |
| O-R86 | No push by default | 8. Place map, homepage and clear-box surfaces |
| D-R87 | Clear-box surfaces | 8. Place map, homepage and clear-box surfaces |
| D-R88 | Labels | 8. Place map, homepage and clear-box surfaces |
| Q-R89 | Windows widget | 8. Place map, homepage and clear-box surfaces |
| O-R90 | Purpose map | 9. Purpose map and look |
| D-R91 | Scoring axes | 9. Purpose map and look |
| D-R92 | Homepage shortcuts in scoring | 9. Purpose map and look |
| O-R93 | Screens before building | 9. Purpose map and look |
| O-R94 | Look and feel | 9. Purpose map and look |
| D-R95 | Reach and density | 9. Purpose map and look |
| D-R96 | Consistent elements | 9. Purpose map and look |
| O-R97 | Modules | 10. Modules and the shopping list |
| D-R112 | Shopping module | 10. Modules and the shopping list |
| D-R113 | Export per receipt | 10. Modules and the shopping list |
| D-R122 | Savings categories | 10. Modules and the shopping list |
| D-R123 | Receipt file | 10. Modules and the shopping list |
| D-R124 | What makes a receipt | 10. Modules and the shopping list |
| D-R125 | Item category | 10. Modules and the shopping list |
| Q-R126 | Lines with no category | 10. Modules and the shopping list |
| D-R117 | Wishes | 10. Modules and the shopping list |
| D-R118 | Shopping records in sync and backup | 10. Modules and the shopping list |
| D-R119 | Price-comparison library | 10. Modules and the shopping list |
| O-R116 | Price paid in the savings export | 10. Modules and the shopping list |
| D-R121 | Showing the difference | 10. Modules and the shopping list |
| D-R109 | Expiry dates | 10. Modules and the shopping list |
| D-R110 | Usage rate | 10. Modules and the shopping list |
| O-R98 | Savings app and the shopping list | 10. Modules and the shopping list |
| D-R99 | Reminders | 10. Modules and the shopping list |
| D-R100 | Stage plan | 11. Stages |
| D-R101 | Order of stages 4 to 6 | 11. Stages |
| O-R102 | Stage 0 spikes | 11. Stages |
| O-R65 | Recognition pass bar | 12. Test plan |
| O-R66 | Scoring crowded answers | 12. Test plan |
| O-R103 | Insurance records | Out of scope |
| O-R104 | App lock | Out of scope |
| O-R105 | Lending | Out of scope |
| O-R106 | Inventory import | Out of scope |
| D-R107 | Low stock outside Pantry | Out of scope |
| O-R120 | Shopping notifications | Out of scope |

## Glossary

| Term | Meaning |
|---|---|
| Clear-box effect | What a clear plastic box does for its contents: every glance is a reminder, with no effort. |
| Landscape effect | Things always in view become part of the landscape and stop being noticed; the daily random items (R87) work against it. |
| Earlier app | The owner's earlier app (unpublished): the owner's own Kotlin Multiplatform code, whose folder sync is the starting point for Chatelaine's sync. |
| Item | A thing the user owns, as recorded in the app. An item is never a place: a box is an item only when nothing is stored in it, such as an empty gift box (R111). |
| Place | A storage location in the place tree, also called a container: a drawer, a storage box, a shelf, a room, a house. A place is never an item (R111). |
| Site | A house, at the top of a place tree. |
| Tag | A label that says what kind of thing something is. Items and places carry tags. |
| Parent tag | A tag that other tags sit inside; those are its sub-tags. A top-level tag has no parent. |
| Kept together | A mark on a tag: its things share a home place. |
| Field | A value that differs from item to item, such as size M or expires March 2027. A custom field is one the user defines. |
| Module | A preset set of custom fields on its tags or records of its own (the Shopping module's price book), plus a few views of its own and its own purpose-map entries. |
| Replica | A module a user builds with the same tools as the built-in modules. |
| Inventory module | The app's foundation: always on and cannot be turned off; the other modules build on it. |
| Pantry module | The module for consumables of every kind, from face creams to metal screws, not only food. |
| Shopping module | The module for what to buy, where and at what price: the shopping list, the price book and the price comparison. |
| Pack | One unit of a product with a given size, as sold, whatever amount it holds: a 500 g bag of cookies, a 6 × 330 ml multipack. |
| Price book | Shop prices recorded by pack, store and date, with promotions and their end dates. |
| Wish | A shopping-list entry for something not in Inventory; it becomes an item only when ticked as bought (R117). |
| Receipt | The items bought at one store in one payment; one row in the savings export (R124). |
| Category | An optional label on an item or wish that the savings app matches to one of its own categories (R125). |
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

A clear plastic box keeps its contents in view with no effort: every glance is a reminder of what's inside. A cardboard box, even labelled, needs a deliberate action: reading, opening and a mental link from label to object. That costs effort at the very moment a task starts and puts load on working memory. The out-of-sight, out-of-mind effect and the "landscape" effect can influence people's daily life.

Chatelaine is a home inventory app that turns all types of storage into a clear box. It records what is stored and where, and it gives the user the chance to find something without searching through the house. Optionally, it offers to show daily randomized items and containers to help the user remember something exists and where to find it without looking it up. The app can also be used as an aid for the first big tidy: it can log items before they're in their proper place, group them by purpose, gather different groups by similarity and suggest order of storage, then keep photographs linked to items and their containers once they are where they belong.

The name: a chatelaine was the mistress of a château, in charge of the household's internal affairs, holding the keys to the estate's valuables on her belt. In fashion history the name became associated with this last characteristic, becoming the noun for a clasp worn at the waist with suspended chains, holding items like keys, scissors, watches, and thimbles, serving as a functional and status-symbol accessory for women in the 18th and 19th centuries.

### O-R01 · Users and devices

**Rule:** Chatelaine has one user, with two devices: an Android phone and a Windows PC. Several users or shared households are not in v1.

**Why:** v1 needs no identities or permissions. The folder sync (R51) could later carry a household's devices, since each device writes its own changes and the merge works field by field (R55).

**Acceptance:** none: a scope boundary.

**Not:** One user for good: the sync does not rule out a household later.

**Since:** 2026-11-22 · owner · agreed rewording

### D-R02 · Goals

**Rule:** The app pursues the goals below.

**Values:**

| Goal | Detail |
|---|---|
| Help remind of the existence and placement of stored items without the user's active search | through passive surfaces (R86, R87) |
| Logging costs at most a word, a dictated phrase or a photo | everything else can come later |
| Finding a thing takes one search or one walk through the place map | — |
| The first big tidy is supported end to end | log, group, gather, place, photograph |
| The user can shape the app to their own logic | tags, fields, place structure, what the homepage shows |

**Acceptance:** none: a statement of aims; the scenarios R05 to R11 carry the checks.

**Since:** 2026-11-22 · owner · agreed rewording

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

**Acceptance:** Reading every screen's text and every notification finds no streak, guilt or nagging wording. A fresh install sends no reminder the user did not turn on. A later model run never overwrites a correction. Stage 1's round-trip tests pass.

**Since:** 2026-11-22 · owner · own words

### D-R04 · Jobs and how often

**Rule:** The app serves the jobs below, as often as each row says. The frequencies are the owner's first estimate.

**Values:**

| Job | Scenario | How often |
|---|---|---|
| Find where a thing is | R10 | several times a week |
| Log new things by photo | R06, R07, R08 | daily during the first big tidy, otherwise about monthly |
| Check before buying | R09 | weekly |
| Gather things for a project | R05 | monthly |
| Seasonal swap | R11 | twice a year |
| Shop from the list, comparing prices | R114, R115 | every shopping trip |
| The first big tidy | — | once, over some weeks |

**Acceptance:** none: the frequencies guide the stage order (R101); there is nothing in the app to check.

**Since:** 2026-11-23 · owner · agreed rewording

## 2. Acceptance scenarios

The app is working when all the scenarios below succeed. The stage plan (R100) names the stage that unlocks each.

### D-R05 · Project from a tag

**Rule:** Search the tag "sewing" and get the items with that tag; select the ones needed for a new project and add the tag "red skirt" to them in one action. Later, find them quickly by "red skirt".

**Acceptance:** The steps succeed end to end, as stage 2's gate requires (R100).

**Since:** 2026-11-22 · owner · own words

### D-R06 · Photo with suggestions

**Rule:** Photograph three headphones. The app recognises headphones and the count of three, and suggests tags such as "technology", "music", "phone", "PC". The user deletes "PC" and keeps the rest, which are saved with the item.

**Acceptance:** The steps succeed end to end, as stage 4's gate requires (R100).

**Since:** 2026-11-22 · owner · own words

### D-R07 · Bulk clear

**Rule:** Photograph a full drawer of mixed objects; the app recognises some or all and suggests tags. The user clears all suggested tags in one action and adds their own, "junk", "to be sorted". The container can be called "junk drawer", not the items contained.

**Acceptance:** The steps succeed end to end, as stage 4's gate requires (R100).

**Since:** 2026-11-22 · owner · own words

### D-R08 · Box split into items

**Rule:** Photograph a box of many items, some overlapping or partly hidden; the app recognises some and gets others wrong. The user corrects the wrong ones and saves each kind of thing as its own item, with its count, inside the box, which is a place (R111), not an item. Each can then be repositioned and rephotographed later.

**Acceptance:** The steps succeed end to end, as stage 4's gate requires (R100).

**Since:** 2026-11-22 · owner · agreed rewording

### D-R09 · Before buying

**Rule:** Open the bathroom cabinet under the sink in the app and see which cleaning supplies were logged there, so the scale remover is not bought twice. It is an Inventory scenario: it relies only on what was logged, never on a new photo.

**Acceptance:** The steps succeed end to end, as stage 3's gate requires (R100).

**Not:** A Pantry scenario: keeping consumables current by photo would mean photographing them more than weekly, which is not the low-effort setup the app aims for.

**Since:** 2026-11-22 · owner · agreed rewording

### D-R10 · Visual place map and filtered search

**Rule:** Build a small visual map from drawer partitions up through dresser, shelf, closet, cabinet, room, floor and house, with several houses. Select a house or floor and search "shoes" to filter results by place. If there is no result there, a short list follows with the same search's results from other places.

**Acceptance:** The steps succeed end to end, as stage 6's gate requires (R100).

**Since:** 2026-11-22 · owner · own words

### D-R11 · Seasonal swap

**Rule:** Having grouped the winter clothes with the app, swap the closet at the change of season, then group and organise the summer clothes being put away in the same way.

**Acceptance:** The move part succeeds at stage 3 and the whole scenario at stage 5, as those stages' gates require (R100).

**Since:** 2026-11-22 · owner · own words

### D-R114 · Cheapest pack under a tag

**Rule:** Select the tag "cookies", which three packs carry: brand S 1 kg at 2,99 €, brand S 500 g at 1,99 € and brand Q 600 g at 1,59 €. The list compares them per 1 kg and ranks them cheapest first: Q 600 g at 2,65 €/kg, S 1 kg at 2,99 €/kg, S 500 g at 3,98 €/kg.

**Acceptance:** The three packs appear in that order with those prices per kg, as stage 10's gate requires (R100).

**Since:** 2026-11-23 · owner · agreed rewording

### D-R115 · Cheaper than the saved price

**Rule:** Cookies Q 600 g are on the list with a saved price of 1,59 € at Supermarket A, and on the shelf there they cost 1,29 €. The user ticks them as bought and types 1,29 € over the pre-filled 1,59 €. The price book records 1,29 € at Supermarket A on that date as the current price, and the receipt's savings export counts 1,29 €, the price paid.

**Acceptance:** After the purchase the cookies rank by 1,29 € (2,15 €/kg), 1,59 € stays in the price history, and the receipt's savings export counts 1,29 €, as stage 10's gate requires (R100).

**Since:** 2026-11-23 · owner · agreed rewording

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

**Since:** 2026-11-22 · owner · own words

### O-R13 · Framework and targets

**Rule:** The app is built with Kotlin Multiplatform and Compose Multiplatform. It targets Android, with minSdk 33 (Android 13), and Windows desktop on the JVM.

**Why:** The app cannot run successfully on minSdk 30 (Android 11).

**Acceptance:** A Compose Multiplatform window runs on Android and on Windows, as stage 0 holds (R100, R102).

**Not:** minSdk 30 (Android 11), the first choice: the app cannot run successfully on it.

**Since:** 2026-11-22 · owner · agreed rewording

### D-R14 · Shared code

**Rule:** Code is shared by default. Platform code is written only for the camera, file access, widgets, speech, model runtimes and printing.

**Acceptance:** Platform source sets (for example `androidMain`, `jvmMain`) hold code only for the camera, file access, widgets, speech, model runtimes and printing. Everything else is in common code.

**Since:** 2026-11-22 · owner · own words

### D-R15 · Offline first

**Rule:** The app works offline first: every job works with no network. The network serves only the optional uses below. There are no cloud accounts and no server.

**Values:**

| Network use | Status |
|---|---|
| Model downloads | opt-in |
| AI-service key | optional, off by default (R78) |
| Household network | optional, being worked out (R108) |

**Acceptance:** With the optional network uses off, a network capture over a full run of R05 to R11 shows no request at all.

**Not:** Fully offline, with model downloads as the only network use: the owner widened it to the optional uses above.

**Since:** 2026-11-22 · owner · agreed rewording

### Q-R108 · Household network

**Rule:** What does the optional household network do (R15), and how does it fit one user in v1 (R01)?

**Default:** Later than v1, an optional household-structured sync, building on R51's change files. The owner is working out the rest.

**Why:** R15 lists it among the optional network uses; R01 keeps one user in v1 but leaves a household's devices open for later.

**Since:** 2026-11-22 · owner · agreed rewording

### D-R16 · No Google Play services

**Rule:** The core uses no Google Play services.

**Acceptance:** On every build, the core module's resolved dependencies hold no `com.google.android.gms` or `com.google.firebase` artifact.

**Since:** 2026-11-22 · owner · own words

### D-R17 · F-Droid build

**Rule:** An F-Droid-compatible build is optional. The stage plan lists it for stage 11 (R100).

**Acceptance:** none: optional; stage 11 gives it its own decision first.

**Since:** 2026-11-22 · owner · own words

### D-R18 · Languages

**Rule:** The interface is in Italian and English from the start; dictation, in both languages, arrives in stage 8 (R100). The user can switch the app's language independently of the phone's.

**Acceptance:** A dictated list of ten things becomes ten items, in Italian and in English, as stage 8's gate requires (R100).

**Since:** 2026-11-22 · owner · agreed rewording

### D-R19 · Phone download size

**Rule:** The phone app's download has no size limit. The small recogniser and the text-similarity model ship with the app; the speech model is downloaded on first use of dictation.

**Evidence:** Stage 0's best offline recogniser pair came to 138 MB compressed on its own (`docs/evidence/stage0-recognition.md`).

**Acceptance:** none: there is no limit to check.

**Not:** A limit of about 150 MB, measured compressed: dropped by the owner.

**Since:** 2026-11-22 · owner · agreed rewording

### I-R20 · Owner's PC

**Rule:** The owner's PC has the parts below.

**Values:**

| Part | Value |
|---|---|
| Processor | a 6-core desktop processor |
| Memory | 16 GB |
| Graphics | a 4 GB graphics card |

**Since:** 2026-11-22 · owner · agreed rewording

### O-R21 · Desk companion

**Rule:** The Windows PC is a desk companion: it does everything except taking photos and dictation. It runs recognition on photos taken with the phone, and handles bulk editing, the place map, label printing and backup.

**Why:** The PC has no camera at hand and no reason to dictate; bulk editing, the map and printing are easier with a keyboard and a big screen.

**Acceptance:** The Windows build has no camera capture and no dictation entry. Recognition on photos synced from the phone, bulk editing, the place map, label printing and backup all work there.

**Since:** 2026-11-22 · owner · own words

### O-R22 · Publishing rules

**Rule:** The repository holds no personal data, including the kinds below. The publish check (`tools/publish_check.py`) runs before each commit and push, and in CI.

**Why:** The repository is public.

**Values:**

| Never in the repository | Instead |
|---|---|
| health remarks that read as about the owner | general wording |
| the owner's other projects by name | "the owner's earlier app", "the owner's other apps" |
| device models | the chip or class only |
| private folder names | "a private folder outside the repository" |
| the owner's own belongings as examples | neutral examples |

**Acceptance:** The publish check reports no finding before each commit and push, and in CI, and a read of the new text finds none of the kinds above.

**Since:** 2026-11-22 · owner · agreed rewording

### D-R23 · Borrowing code

**Rule:** Chatelaine is a new codebase, not a copy of an existing app. Single source files from the projects that R24 marks for copying may be copied in where useful; each keeps its licence notice and is listed in `THIRD_PARTY.md`.

**Acceptance:** A script, run like the publish check, finds every copied file's licence notice and its line in `THIRD_PARTY.md`.

**Not:** Forking one reference app as the base: its structure and look would fight the purpose map (R90) and the look (R94).

**Since:** 2026-11-22 · owner · agreed rewording

### D-R24 · Sources to borrow from

**Rule:** The projects below are read and borrowed from for the use each row gives; code is copied only from a row whose Copy column says yes. Spot It (marked spots on a photo), Nestmer (visual map of rooms) and Store & Forget (photo-first entry) are design references, not open source.

**Values:**

| Project | Licence | Language | Copy | Use |
|---|---|---|---|---|
| `rh-id/a-personal-stuff` | GPL-3.0 | Java | yes | feature ideas (reminders) |
| `stovocor/whohasmystuff` | GPL-3.0 | Kotlin | yes | lending (R105) |
| `escapedcanadian/prepper` | GPL-3.0 | Kotlin | yes | moving a box with its contents, breadcrumb path, type-ahead picker, expiry badge, zip backup and restore, photo resize and rotation (`docs/reference-apps.md`) |
| `PamCatten/hot-stuff` | Apache-2.0 | Kotlin | yes | starter room names and categories as place names and tags; archived |
| `asmw/flindex` | MIT | Dart | no | NFC and QR container flows, ported, not copied |
| `anderslum/boxorg` | MIT | TypeScript | no | photo-first box interface, as a reference |
| `sysadminsmedia/homebox` | AGPL-3.0 | Go | no | data model and QR labels, as a reference |
| `lcukerd/StuffList` | none | Java | no | look only |
| The owner's earlier app (unpublished) | the owner's own, no licence file; code taken into Chatelaine is relicensed GPL-3.0 by the owner and recorded in `THIRD_PARTY.md` | Kotlin (KMP) | yes | sync engine and folder layer, version catalog, `:core`/`:data` split, typed units, app lock |

**Acceptance:** none: a list of sources; R23 says how their code is taken in.

**Since:** 2026-11-22 · owner · agreed rewording

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

**Acceptance:** An item saved with only a name is stored and found by search. Each field in its Values can be set and survives export and import.

**Since:** 2026-11-22 · owner · own words

### O-R26 · Quantity and counts

**Rule:** An item can carry an optional quantity. Suggested counts are approximate by default ("about 5") and always editable. Under "Estimated quantities", an "Accuracy" setting has the switches below, which ask for exact counts instead; the labels are the owner's drafts, settled with the Settings screen.

**Why:** Exact counts matter mainly where items link to shopping; an estimate first keeps logging quick.

**Values:**

| Switch | Exact counts for | Default |
|---|---|---|
| "Personal inventory (all modules)" | every item | off |
| "Pantry module" | Pantry items (R97) | off |

**Acceptance:** A suggested count shows as "about N" and can be edited. With a switch on, suggestions in its scope ask for an exact count; with both off, every suggestion is approximate.

**Since:** 2026-11-22 · owner · agreed rewording

### D-R27 · Consumables and low stock

**Rule:** Consumables of every kind live in the Pantry module (R97), with minimum stock, low-stock notifications (R99) and usage rate (R110). The details are explored further with the Pantry module.

**Acceptance:** none: the Pantry module's own scenarios carry the checks (R97).

**Since:** 2026-11-22 · owner · agreed rewording

### D-R28 · Identical things

**Rule:** A set of identical things can be one item with a quantity ("headphones ×3"), and it can be split into separate items later.

**Acceptance:** R06 passes with the three headphones saved as one item with a count of three.

**Since:** 2026-11-22 · owner · own words

### D-R29 · Order of an item's facts

**Rule:** An item's tags, place and photo are independent facts that can come in any order, including the paths below.

**Values:**

| Path | What happens |
|---|---|
| Name only | logged by name, with no place or photo yet: unplaced (R30) |
| Photo first | photographed first, then named |
| Settled area | placed in a settled area that was photographed as a whole (R41) |

**Acceptance:** Each of the three paths (name only; photo first, then name; placed in a settled area) ends in a saved item.

**Since:** 2026-11-22 · owner · own words

### D-R30 · Item states

**Rule:** An item is in one of the states below.

**Values:**

| State | Meaning |
|---|---|
| unplaced | no place yet |
| placed | has a place |
| in use / away | taken out of its place and not yet put back |
| lent | lent to someone; comes with lending (R105) |

**Acceptance:** An item can go unplaced, then placed, then in use / away, then placed again, and its state shows on its screen.

**Since:** 2026-11-22 · owner · own words

### D-R31 · Photo outdated flag

**Rule:** A "photo outdated" flag, separate from an item's state, is raised when an item, or a place's contents, changed after the last photo.

**Acceptance:** Editing an item, or moving an item into or out of a place, after the last photo raises the flag. A new photo clears it.

**Since:** 2026-11-22 · owner · own words

### D-R32 · Place tree

**Rule:** Places nest freely in a tree of any depth, such as house › floor › bedroom › chest of drawers › drawer › partition, and the user can skip or add levels. Several sites are allowed.

**Acceptance:** R10 passes, with its map running from drawer partitions up to several houses.

**Since:** 2026-11-22 · owner · agreed rewording

### D-R111 · Items and places

**Rule:** Items and places are different things. A place, also called a container, holds items and other places and is never an item; an item is never a place. An item inside a place is saved as its type of item and its quantity, noted as being in that place.

**Values:**

| Thing | Is | Why |
|---|---|---|
| A drawer | a place | it stores things |
| A storage box | a place | it stores things |
| An empty, unused gift box | an item | nothing is stored in it |
| A shelf in a shelving unit, in a garage, in a house | places, one inside the next | shelf < shelving unit < garage < house |

**Acceptance:** A drawer cannot be saved as an item, an item cannot hold other items, and an item saved inside a box is listed under that box as a place.

**Since:** 2026-11-22 · owner · agreed rewording

### D-R33 · Place details

**Rule:** A place can carry tags, notes, a photo and a position in the visual map (R83). A place is called what it is (house, bedroom, dresser, chest of drawers, drawer, shelving unit, shelf, box); places differ only in size, which sets how they are drawn.

**Acceptance:** A place takes tags, notes, a photo and a map position, and its size sets how it is drawn on the map.

**Since:** 2026-11-22 · owner · agreed rewording

### D-R34 · Tags

**Rule:** Tags are the one organising concept. Items and places can carry any number of tags, and a tag can sit inside a parent tag ("red skirt" inside "sewing").

**Acceptance:** R05 passes.

**Since:** 2026-11-22 · owner · own words

### D-R35 · Tag tree

**Rule:** A tag has at most one parent tag, so tags form a tree; otherwise tags are flat.

**Acceptance:** A second parent cannot be given to a tag, and no tag can sit inside its own sub-tag.

**Since:** 2026-11-22 · owner · agreed rewording

### D-R36 · Kept together

**Rule:** A tag can be marked kept together. It can then have a home place, and the app can say which of its items are away from that home.

**Acceptance:** R11 passes fully, with the home and away views that stage 5 holds, as stage 5's gate requires (R100).

**Since:** 2026-11-22 · owner · own words

### D-R37 · Tag kinds

**Rule:** A tag can carry a kind for display: function, project, season, place-like or other. Tag kinds are user-editable (R47).

**Acceptance:** Each of the five kinds can be set on a tag and shows on it as an icon or a group heading. The user can rename, add and remove kinds.

**Since:** 2026-11-22 · owner · own words

### D-R38 · Default tag kinds

**Rule:** A new install starts with the tag kinds R37 lists: function, project, season, place-like and other.

**Acceptance:** A fresh install offers exactly these five tag kinds.

**Since:** 2026-11-22 · owner · agreed rewording

### D-R39 · Suggested tags

**Rule:** Tags a model suggests are stored as suggestions until the user accepts them. Once accepted, they look the same as tags the user made.

**Acceptance:** R06 passes: the suggested tags the user keeps are saved with the item, and the deleted one is not.

**Since:** 2026-11-22 · owner · own words

### O-R40 · Editing suggestions

**Rule:** Item, place and tag information that a model suggests is always fully editable, one at a time or in bulk.

**Why:** The user's corrections come first: R03 rules out a later model run overwriting a correction.

**Acceptance:** R07 and R08 pass: suggested tags are cleared in one action, and wrong items are corrected one at a time.

**Since:** 2026-11-22 · owner · own words

### D-R41 · Photos

**Rule:** A photo belongs to an item or to a place. A place photo of a settled area can have regions that point to the items they show, like the marked spots of Spot It.

**Acceptance:** A photo saves on an item or on a place. A region marked on a place photo opens the item it points to.

**Since:** 2026-11-22 · owner · own words

### D-R42 · Proposals from a photo

**Rule:** Recognition can propose items from a photo (R06 to R08). Each proposal is accepted, corrected or discarded, and a photo can be split into several separate items (R08).

**Acceptance:** R06, R07 and R08 pass, as stage 4's gate requires (R100).

**Since:** 2026-11-22 · owner · own words

### D-R43 · Custom fields

**Rule:** Users define their own custom fields. Full or near-full customisation is a main focus of the app.

**Acceptance:** none: a direction; how far customisation reaches is workshopped per screen.

**Since:** 2026-11-22 · owner · own words

### D-R44 · Field types

**Rule:** Fields can go on items and on places, with the types below, sized for every module (R97).

**Values:**

| Type |
|---|
| text |
| number with a typed unit |
| date |
| choice list |
| checkbox |
| link to another item or place |

**Acceptance:** A field of each type can be created on an item and on a place, filled in, and survives export and import.

**Since:** 2026-11-22 · owner · agreed rewording

### Q-R45 · Final field types and presets

**Rule:** What is the final list of custom field types and presets, worked out with the owner from use cases? It is needed by stage 1.

**Default:** The types in R44 and the module fields in R97, as drafted.

**Why:** R44 and R97 hold a drafted list; the source settles the final one with the owner, from use cases.

**Since:** 2026-10-28 · unrecorded · migrated

### D-R46 · Where a field attaches

**Rule:** A field can be attached to a tag, so every item carrying that tag gets the field ("clothes" → size), or added to a single item.

**Acceptance:** A field attached to a tag appears on every item that carries the tag, including items tagged later. A field added to one item appears only on that item.

**Since:** 2026-11-22 · owner · own words

### D-R47 · User-editable lists

**Rule:** Tag kinds, homepage cards and the widget's content are user-editable.

**Acceptance:** The user can add, rename and remove tag kinds, homepage cards and widget content, and the change shows at once.

**Since:** 2026-11-22 · owner · agreed rewording

## 5. Storage, sync and backup

### D-R48 · Local database

**Rule:** Each device keeps a local database.

**Acceptance:** Each device opens its own database with the network off.

**Since:** 2026-11-22 · owner · own words

### Q-R49 · Database library

**Rule:** Which database library does the app use, SQLDelight or Room KMP? It is needed by stage 1.

**Default:** Whichever the earlier app's sync code (R58) uses, so reusing it stays simple; decided in stage 1.

**Why:** SQLDelight and Room KMP are both named, with no reason given to prefer either.

**Since:** 2026-11-22 · owner · agreed rewording

### D-R50 · A database that fails to open

**Rule:** A database that fails to open is kept, never deleted automatically.

**Acceptance:** In stage 1's round-trip tests, a corrupted database is kept, not deleted (R100).

**Since:** 2026-11-22 · owner · own words

### O-R51 · Folder sync

**Rule:** The phone and the PC share one inventory through two-way sync over a folder, kept in step by a file-sync tool such as Syncthing. Each device appends its changes as small files in the folder, and reads and merges the other device's.

**Why:** There is no server (R15), the earlier app's proven sync engine is reused (R58), and a file-sync tool works on the home network without an account.

**Acceptance:** Two-device sync with conflicts passes stage 1's round-trip tests (R100).

**Since:** 2026-11-22 · owner · own words

### D-R52 · Record ids and history

**Rule:** Every record has a globally unique id and a modification history.

**Acceptance:** Every record has a unique id, and every change to it adds an entry to its history.

**Since:** 2026-11-22 · owner · own words

### D-R53 · No shared database file

**Rule:** No database file is ever shared directly through the folder.

**Why:** File sync corrupts live databases.

**Acceptance:** After any stage 1 sync test, the sync folder holds no database file.

**Since:** 2026-11-22 · owner · own words

### D-R54 · Conflicting edits

**Rule:** When both devices edited the same field, the later edit wins. The overwritten value stays in history, so it can be restored.

**Acceptance:** In stage 1's two-device sync test with conflicts (R100), the later edit wins and the overwritten value can be restored from history.

**Since:** 2026-11-22 · owner · own words

### D-R55 · Sync groups

**Rule:** Each user-visible field is its own sync group, and every user-visible group keeps the losing value, so that the history of R54 holds.

**Evidence:** Not checked: the change-log size with one group per field; it is measured in stage 1's sync test.

**Acceptance:** When the two devices edit different fields of one item at the same time, both edits survive. The losing value of a same-field conflict is in history.

**Not:** One group per row, and discarding the losing value except in a few groups, as the earlier app does by default: the history of R54 would not hold.

**Since:** 2026-11-22 · owner · own words

### D-R56 · Photos in the sync folder

**Rule:** Photos travel as files in the same folder, named by a SHA-256 hash of their content, under `blobs/` (R58); each photo record points to its hash.

**Acceptance:** After a sync, each photo is in the folder as a file named by its content hash, and the other device shows it.

**Since:** 2026-11-22 · owner · agreed rewording

### D-R57 · Any folder-sync tool

**Rule:** Sync works with any folder-sync tool. The app does not talk to Syncthing itself; its only tool-specific code is the list of conflict-copy file-name patterns (R58).

**Evidence:** Not checked: the official Syncthing Android app was discontinued in late 2024, and Syncthing-Fork is the maintained one.

**Acceptance:** Stage 1's sync test passes with Syncthing (Syncthing-Fork on the phone), and the app has no code specific to one sync tool beyond the conflict-copy patterns. Before release, the test passes once with a second folder-sync tool.

**Since:** 2026-11-22 · owner · own words

### D-R58 · Sync code from the earlier app

**Rule:** The folder sync of the earlier app, the owner's own code, is the starting point. Its parts are taken over as the rows below say. The code is copied once for v1; whether it becomes a library shared with the earlier app is decided after stage 1.

**Values:**

| Part | Holds | Taken over |
|---|---|---|
| `:core` engine | ULID ids, hybrid logical clock stamps, field groups, merge with a permanent purge registry | almost unchanged |
| Folder layer | 16 KiB per-device log segments, snapshots every 64, atomic writes, conflict copies merged, found by known file-name patterns (Syncthing's first), waiting rows for missing parents, outbox, photos stored by SHA-256 under `blobs/` | adapted: database and group sets passed in, sync tables moved into Chatelaine's schema, new row adapters for items, places, tags and photos |

**Evidence:** Review of the earlier app, 2026-10-28.

**Acceptance:** none: a choice of starting code; R51, R54 and R55 say what the result must do.

**Since:** 2026-11-22 · owner · own words

### D-R59 · Sync lessons from the earlier app

**Rule:** The sync follows the lessons below, adopted from the earlier app.

**Values:**

| Subject | Lesson |
|---|---|
| Replayed versions | ignore a replayed version no newer than the base |
| Device-id file | keep it out of Android Auto Backup |
| Export | through an outbox, not a stamp cursor |
| Purge registry | permanent: purged rows are recorded for good, since time-limited tombstones let deleted rows come back |
| Recovered database | re-reads its own folder files |

**Evidence:** Review of the earlier app, 2026-10-28.

**Acceptance:** One test per lesson: a replayed old version is ignored; the device-id file is excluded from Android Auto Backup; export reads the outbox; a purged row never comes back; a recovered database re-reads its folder files.

**Since:** 2026-11-22 · owner · own words

### D-R60 · Backup and export

**Rule:** The app backs up automatically to a folder the user picks, and offers a manual export of JSON and photos.

**Acceptance:** Export → import passes stage 1's round-trip tests (R100).

**Since:** 2026-11-22 · owner · own words

### D-R61 · Restoring a backup

**Rule:** A backup carries each value's sync stamps, and restoring it merges like any other device's changes. Restoring and rolling back work as the rows below say.

**Why:** The earlier app taught that a stamp-less backup overwrites every device.

**Values:**

| Action | Effect |
|---|---|
| Restore | merges by stamps: edits newer than the backup survive on both devices |
| Roll back to a backup | a separate, deliberate action, recorded as a change; the state it replaced stays in history |

**Evidence:** The earlier app built only a settings backup, so full-backup merge and the rollback action are new (review of the earlier app, 2026-10-28).

**Acceptance:** Restoring a backup on one device keeps every edit made after the backup, on both devices. Rolling back is a separate action that adds a history entry, and the replaced state can be restored.

**Since:** 2026-11-22 · owner · own words

## 6. Models and recognition

### O-R62 · Image recognition

**Rule:** Image recognition is core: a small model ships with the app, and a larger, more accurate one is an optional download. Recognition runs on both devices.

**Why:** The PC can run larger recognisers than the phone: in stage 0, Qwen3-VL-8B ran on a desktop PC at about 48 s a photo (`docs/evidence/stage0-recognition.md`).

**Acceptance:** As stage 4's gate requires (R100), R65's bar is met on both target phones, and R06, R07 and R08 pass.

**Since:** 2026-11-22 · owner · own words

### D-R63 · Model roles

**Rule:** The app's models fill the roles below.

**Values:**

| Role | Model kind | Ships | Used for |
|---|---|---|---|
| Ears | speech-to-text, whisper.cpp class; the phone's own recogniser only as a fallback where it works without Google Play services (R16) | base set, downloaded on first use of dictation (R19) | dictated logging (stage 8) |
| Similarity | small multilingual text embedding model | base | tag suggestions, grouping, search by meaning |
| Eyes (small) | small on-device recognition model | base | R06 to R08: objects, counts, rough tags |
| Eyes (large) | larger recogniser or multimodal model, Qwen3-VL class (R76) | optional download | better recognition in crowded photos |
| Reader | the multimodal recogniser; a small open-source text reader (OCR) only if it cannot | with the recogniser | label text, such as expiry dates (R109) |
| Labeller | small LLM, Qwen3 0.6B or Gemma class | optional download | function tags for an item, names for a new parent tag, splitting a dictated list |
| Your logic | nearest neighbour over the user's examples (R71) | base, no model | personal tag and place suggestions |

**Acceptance:** Each optional model measurably improves a scenario, as stage 9's gate requires (R100).

**Since:** 2026-11-22 · owner · agreed rewording

### D-R64 · Models on device

**Rule:** Every model runs on the device. Models larger than the base set are downloaded on request, never bundled.

**Acceptance:** With the network off, every installed model runs. No model above the base set is in the app's download.

**Since:** 2026-11-22 · owner · own words

### D-R67 · Optional language model

**Rule:** An optional on-device language model makes tag suggestions. Its target phones are those of R65.

**Evidence:** Not checked: stage 0 holds timing the small language model against R65 on the target phones (R100), and the source records no result.

**Acceptance:** The model measurably improves a scenario, as stage 9's gate requires (R100).

**Since:** 2026-11-22 · owner · own words

### D-R68 · Open-vocabulary recogniser

**Rule:** The recogniser is open-vocabulary: image–text matching or a small multimodal model. The stage 0 spike measures this kind of model, not a fixed-class detector.

**Why:** R06 needs names outside the usual fixed detector classes: "headphones" is not among the 80 COCO classes.

**Evidence:** Stage 0 measured open-vocabulary detectors, image–text naming and small multimodal models (`docs/evidence/stage0-recognition.md`).

**Acceptance:** R06 passes: the recogniser names the headphones.

**Not:** A fixed-class detector: it has no name for things outside its classes.

**Since:** 2026-11-22 · owner · own words

### D-R69 · Recognition as a hint

**Rule:** Recognition is a hint, never the record. Crowded or overlapping photos are expected to be partly wrong: R08 depends on easy correction, not on accuracy.

**Acceptance:** R08 passes.

**Since:** 2026-11-22 · owner · own words

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

**Since:** 2026-11-22 · owner · own words

### D-R71 · Personal learning

**Rule:** The user's sorted items act as labelled examples: a new item's nearest neighbours carry the user's own logic. Corrections always outrank model output. Nothing is trained or uploaded.

**Acceptance:** As stage 5's gate requires (R100), suggestions are accepted or corrected in one tap, and R11 passes fully.

**Since:** 2026-11-22 · owner · own words

### D-R72 · Model licences

**Rule:** Each model's licence is checked before it is adopted. The weights' licence must allow redistribution from a GPL app's download screen.

**Acceptance:** Every adopted model has its licence recorded, and each licence allows redistribution from a GPL app's download screen.

**Since:** 2026-11-22 · owner · own words

### O-R73 · Recognition instructions

**Rule:** The recognition instructions are not tuned further.

**Why:** Every change to the instructions lost accuracy somewhere, and the owner judged that more rules confuse the model and lengthen the user's work.

**Evidence:** Of the Qwen runs in `docs/evidence/stage0-recognition.md`, the plain round 3 tightened instructions, shared with the cloud models, are the most accurate.

**Acceptance:** none: a working practice.

**Not:** Instructions tuned to a model's faults, or anti-repeat rules added to them: each lost accuracy on the stage 0 photos.

**Since:** 2026-11-22 · owner · own words

### D-R74 · Repeated lines

**Rule:** The app cuts repeated lines from a recogniser's answer and caps the answer at 15 lines.

**Why:** A loop is better handled by the app than by more instructions (R73).

**Evidence:** Qwen3-VL-8B looped on 1 drawer of 10 with the tightened instructions (`docs/evidence/stage0-recognition.md`).

**Acceptance:** An answer that repeats a line shows it once. An answer longer than the instructions' own limit of 15 lines (R73) shows 15.

**Since:** 2026-11-22 · owner · own words

### D-R75 · Segmentation stage

**Rule:** Recognition has no cut-out (segmentation) stage.

**Why:** On the stage 0 photos the background caused few errors, mostly things that blend in; a crop step would have removed about one line, and low-contrast things are hard for cut-out models too. Light, such as a torch button on Capture, fits the cause better.

**Evidence:** Errors sorted by cause on 2026-11-03 (`docs/evidence/stage0-recognition.md`). Not checked: dim light, since all 20 photos were well lit; misses are judged against an answer sheet the owner has not checked; 6 of the detector's tags are likely, not seen.

**Acceptance:** none: it rules a stage out, so nothing is built.

**Since:** 2026-11-22 · owner · own words

### Q-R76 · Smaller recogniser model

**Rule:** Does a smaller model of the Qwen3-VL family, 2B or 4B, keep enough of the 8B model's naming on the Snapdragon 888 phone within R65? It is open for stage 0.

**Default:** none proposed: it waits on a measurement the source has not made.

**Why:** Offline on a PC, Qwen3-VL-8B names nearly as well as the cloud models, but fails R65 on time (about 48 s per photo on a desktop PC) and is a 5.75 GB download. A smaller model may fit the phone; whether it keeps the naming is unmeasured.

**Since:** 2026-11-22 · unrecorded · migrated

### Q-R77 · Recogniser as a first-use download

**Rule:** May the recogniser be downloaded on first use, like the speech model (R19)? It is open for stage 0.

**Default:** none proposed: the source leaves it to the owner, with no leaning.

**Why:** R19 ships the small recogniser inside the phone download, and only the speech model waits for first use; the best offline namer in stage 0 is far larger (R76).

**Since:** 2026-11-22 · unrecorded · migrated

### D-R78 · AI-service key

**Rule:** The user can supply an optional AI-service key (Gemini, Claude, self-hosted, any provider). It is off by default; only when it is on are photos sent, each with the tightened instructions (R73), not a bare question.

**Why:** Stage 0 found that with the tightened instructions both Gemini (free key) and Claude clear R65 with a wide margin, which no offline recogniser did (`docs/evidence/stage0-recognition.md`). R15 lists the key among its optional network uses.

**Acceptance:** With the key off, no photo leaves the device. With it on, each photo is sent with the tightened instructions, and the answer comes back as proposals.

**Not:** Offline recognition only: no offline recogniser met R65 in stage 0.

**Since:** 2026-11-22 · owner · agreed rewording

## 7. Logging and search

### O-R79 · Correction-first logging form

**Rule:** The form for logging objects and clusters from a photo is designed around quickly editing the recogniser's answer: a list of "about N x name" lines, some right, some close, some wrong. Turning a close line into the right one, fixing a count and dropping a wrong line are each quick. The screen is designed with the owner, through the purpose map (R90), before it is built.

**Why:** When recognition is approximate, the logging form carries the load. The stage 0 scores show most misses are close names rather than wrong ones (`docs/evidence/stage0-recognition.md`).

**Acceptance:** On the built form, a typical drawer (10 lines: 8 right, 1.5 close, 0.5 wrong) is corrected within the time and the taps that the typical-drawer task model predicts for the chosen layout, and the owner's tap check on the phone confirms it. The limits are set when the form's layout is chosen.

**Since:** 2026-11-22 · owner · own words

### O-R80 · Lump, then refine

**Rule:** A good generic name with an approximate count beats a split list. Each line is editable in a few taps: change the number, drop or add a word, remove "about". When a split is worth making, it follows what matters for the things' use, not colour.

**Why:** Adjusting a line is quicker than deleting wrong ones, and a line such as "about 20 x notebook" is easy to judge true at a glance.

**Values:**

| Case | Lines | Note |
|---|---|---|
| Refining a line | "about 3 x wool scarf" → +1, +1, drop "about", drop "wool" → "5 x scarf", save | the owner's example |
| A split by use | "about 9 x small notebook", "about 10 x notebook" | size matters for note-taking |

**Acceptance:** On an open line, the owner's example takes four edits and a save, each one action. R79 counts the taps.

**Not:** A split list by colour, such as "2 x umbrella" plus "1 x purple umbrella" in place of "3 x umbrella": adjusting a line is quicker than deleting wrong ones.

**Since:** 2026-11-22 · owner · own words

### D-R81 · Search through sub-tags

**Rule:** Searching a parent tag also finds the items that carry any of its sub-tags.

**Acceptance:** With "red skirt" inside "sewing", a search for "sewing" finds the items tagged only "red skirt".

**Since:** 2026-11-22 · owner · agreed rewording

### D-R82 · Search across languages

**Rule:** Search matches across languages: "cucito" finds "sewing". Tags are stored once, as written; model suggestions are offered in the interface language, and built-in names such as tag kinds and starter place names switch with the language.

**Acceptance:** With the interface in English, "cucito" finds items tagged "sewing", and the reverse works in Italian. Switching the language changes the built-in kind names but not the user's tags.

**Since:** 2026-11-22 · owner · agreed rewording

## 8. Place map, homepage and clear-box surfaces

### D-R83 · Visual place map

**Rule:** The app draws a visual map of storage automatically from the place tree: stylised places, as blocks the user can rearrange. A free-drawing editor may come later, in stage 11 (R100).

**Acceptance:** R10 passes, as stage 6's gate requires (R100).

**Since:** 2026-11-22 · owner · own words

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

**Acceptance:** With "red skirt" inside "sewing", the tag card shows "sewing" and not "red skirt".

**Since:** 2026-11-22 · owner · own words

### O-R86 · No push by default

**Rule:** The app never pushes by default. Proactive prompts, such as notifications, are opt-in; the clear-box effect comes from passive surfaces (R87).

**Why:** Notifications nag, and R03 rules out nagging, streaks and guilt.

**Acceptance:** A fresh install with default settings sends no notification during a week of use.

**Since:** 2026-11-22 · owner · own words

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

**Since:** 2026-11-22 · owner · own words

### D-R88 · Labels

**Rule:** Labels are printable A4 sticker sheets (PDF) with a photo collage of the contents and a QR code that opens the place. NFC tags come later, in stage 11 (R100).

**Acceptance:** The owner uses the labels for a week, as stage 7's gate requires (R100).

**Since:** 2026-11-22 · owner · own words

### Q-R89 · Windows widget

**Rule:** What is the Windows equivalent of the home-screen widget: a tray, a startup window, or none? It is needed by stage 7.

**Default:** none proposed: the source lists the three options with no leaning.

**Why:** R87 brings the widget to Android first and leaves the Windows side open.

**Since:** 2026-10-28 · unrecorded · migrated

## 9. Purpose map and look

### O-R90 · Purpose map

**Rule:** The purpose map decides which purpose each screen serves before any screen is built. It aims to minimise the number of screens and maximise their quality, with other apps scored beside it as reference apps.

**Why:** Layout and navigation are decided once instead of corrected later.

**Acceptance:** The owner approves the spec and the purpose map, as stage 0's gate requires (R100).

**Since:** 2026-11-22 · owner · own words

### D-R91 · Scoring axes

**Rule:** The purpose map is scored on the axes below, agreed with the owner before scoring, with equal weights and ties shown as ties. Layout axes are set per screen from stage 2.

**Values:**

| Axis | Measures | Chooses |
|---|---|---|
| Coverage | every reference-app function and every step of R05 to R11, R114 and R115 lands in a purpose | the purpose set |
| Exclusivity | — | the purpose set |
| Balance | — | the purpose set |
| Fit | fit to Chatelaine's intent | the purpose set |
| Potential | potential improvement over the reference apps | the purpose set |
| Screen count | fewer is better | screens and homes |
| One home per thing | repeats count against | screens and homes |
| Steps per scenario | taps from the homepage through R05 to R11, R114 and R115 | screens and homes |
| Recall load | steps that ask the user to remember what the screen does not show | screens and homes |

**Acceptance:** Every reference app, scored as a control, loses: a control that scores well means the axes are wrong.

**Since:** 2026-11-23 · owner · own words

### D-R92 · Homepage shortcuts in scoring

**Rule:** The homepage is a master board: its shortcuts, the two cards and the object of the day, do not count as repeats under one home per thing (R91).

**Evidence:** Not checked: the homepage layout is open (R84); the shortcuts named are those of its sketch.

**Acceptance:** none: a scoring practice.

**Since:** 2026-11-22 · owner · own words

### O-R93 · Screens before building

**Rule:** No screens are built at stage 0. Screens start at stage 2 or later, each discussed with the owner before it is built.

**Why:** Layout and navigation are decided once instead of corrected later (R90).

**Acceptance:** Every screen built is owner-approved, as stage 2's gate requires (R100).

**Since:** 2026-11-22 · owner · own words

### O-R94 · Look and feel

**Rule:** The look is specific to the app's purpose, not framework defaults, and follows the direction below. It is workshopped per screen.

**Why:** Framework defaults look generic and busy; the app is opened often and should be calm.

**Values:**

| Aspect | Direction |
|---|---|
| Mood | calm, friendly, low visual noise |
| Colour | relaxing to look at in greyscale, before colour |
| Shapes | shapes guide the user |
| Leading axes | density and the hierarchy of visual function |
| Framework | not stock Material: the framework is plumbing, the look has its own identity |

**Acceptance:** none: a direction, workshopped per screen.

**Since:** 2026-11-22 · owner · own words

### D-R95 · Reach and density

**Rule:** Only the controls for the task at hand are on screen; the rest are one tap away.

**Acceptance:** none: a direction, workshopped per screen.

**Not:** Everything visible at once.

**Since:** 2026-11-22 · owner · agreed rewording

### D-R96 · Consistent elements

**Rule:** The same element reads and acts the same on every screen, and controls at the same level keep the same size and place.

**Acceptance:** Run on every screen's render, the look-alike check finds no two elements with different purposes that look alike, and the size check finds same-purpose elements the same size across phases.

**Since:** 2026-11-22 · owner · own words

## 10. Modules and the shopping list

### O-R97 · Modules

**Rule:** Chatelaine is one app built on the Inventory module, which is always on and cannot be turned off. Wardrobe, Pantry and Shopping are optional modules on top of it, off by default, each designed with the owner first; users can build their own replicas with the same tools. Expiry belongs to the Inventory module, since it covers medication and cosmetics as well as food.

**Why:** The core stays calm and small; modules add fields and screens only for those who want them.

**Values:**

| Module | Fields | Views |
|---|---|---|
| Inventory | expiry date, opened on, months after opening | expiring soon |
| Wardrobe | size, colour, material, season, occasion, last worn, laundry | outfits, seasonal swap, not worn in a year, usage rate |
| Pantry | quantity and unit, minimum stock | usage rate |
| Shopping | category, off by default (R125); prices live in the price book (R112) | shopping list, "already have it" check, price comparison |

**Acceptance:** Each module passes its own scenarios, written with the owner, as stage 10's gate requires (R100).

**Since:** 2026-11-23 · owner · agreed rewording

### D-R112 · Shopping module

**Rule:** The Shopping module holds the shopping list, the price book and the price comparison, and builds on the Inventory module rather than on Pantry, so anything can go on the list. With Pantry on too, low stock appears on the list as a suggestion the user accepts with one tap, and the list shows Pantry stock. The price comparison comes from a standalone library the owner's other apps can also use (R119).

**Why:** Pantry is about what the user has and how fast it is used; Shopping is about what to buy, where and at what price. Not everything bought is a consumable, and the money bridge (R98) sits where money is spent, so Chatelaine touches money in one module only.

**Acceptance:** With Shopping on and Pantry off, a non-consumable thing can be put on the list as a wish (R117) and ticked as bought. With Pantry on, a low-stock item shows as a suggestion and joins the list only after one tap. With Shopping off, no list, price book, comparison or savings export appears.

**Not:** The shopping list inside the Pantry module: non-consumables could not go on the list, and a list would require stock tracking.

**Since:** 2026-11-23 · owner · agreed rewording

### D-R113 · Export per receipt

**Rule:** Bought items go to the savings app as one row per receipt (R124): store, date and total paid, with the receipt's lines as detail in the receipt file (R123). No line is an expense of its own; the savings app splits a receipt across its categories (R122).

**Why:** The savings app records each expense against a category with a place and a date, and imports bank statements, where one card payment is one row.

**Acceptance:** A receipt of five items exports one row with the store, the date, the total paid and five lines, and no line is exported as an expense of its own.

**Not:** One row per item: once the bank statement is imported, the savings app would count the card payment twice. One row per category from Chatelaine: no row would match the bank's single row, so the savings app's duplicate check would miss the payment.

**Since:** 2026-11-23 · owner · agreed rewording

### D-R122 · Savings categories

**Rule:** The savings app splits each receipt across its own categories by the category each line carries (R125). The user matches each Chatelaine category to one of the savings app's once, the first time it appears, and the savings app remembers the match; Chatelaine never reads the savings app's categories.

**Why:** Chatelaine stays free of money logic (R98) and the flow stays one way; the user sets up categories in each app and links them once.

**Acceptance:** Every line with a category carries it in the receipt file, and Chatelaine reads nothing from the savings app.

**Not:** Categories worked out from tags: an item carries tags of several kinds (R37), such as a season or a place, so no tag says which category a line belongs to. Chatelaine reading the savings app's category list: a flow back against R98.

**Since:** 2026-11-23 · owner · agreed rewording

### D-R123 · Receipt file

**Rule:** The receipt file Chatelaine writes for the savings app carries the fields below. The savings app's spec cites this rule, and a change to a field's meaning raises the format version.

**Values:**

| Field | Holds |
|---|---|
| format version | a whole number, starting at 1 |
| receipt id | the receipt's unique id (R52), so a second import of the same receipt is recognised |
| store | the store's name |
| date | the day of the payment |
| currency | an ISO 4217 code, `EUR` in v1 |
| total paid | in whole cents: the sum of the lines' prices paid |
| lines | per line: item or wish name, pack, quantity, price paid with every discount applied (R116), and category (R125) when the line has one |

**Acceptance:** An exported receipt file holds every field above and no tags, its total paid equals the sum of its lines, and exporting the same receipt twice gives the same receipt id.

**Not:** Tags on each line: they no longer decide the category (R122), and they would send labels such as projects and places that the savings app has no use for.

**Since:** 2026-11-23 · builder · agreed rewording

### D-R124 · What makes a receipt

**Rule:** A receipt is one payment: the items ticked as bought at one store until the user closes the receipt at the till. Without a close, the receipt ends at the end of the day or when an item is ticked as bought at another store; a closed receipt never changes.

**Why:** One receipt matches one row of the bank statement. The user knows at the till when they pay twice, while the savings app sees payments only days later, in a statement.

**Values:**

| Case | Receipts |
|---|---|
| Items ticked at store A, receipt closed at the till | one; items ticked at A afterwards start a new receipt |
| Items ticked at store A, never closed | one, ending at the end of the day |
| Items ticked at store A, then at store B | two, one per store |

**Acceptance:** Two closes at one store on one day give two receipts with different ids; with no close, the items ticked at one store on one day make one receipt; ticking an item at a second store starts a new receipt; re-exporting a closed receipt gives the same file.

**Not:** Splitting in the savings app: it sees payments only when a statement is imported, when no one remembers which items went with which payment. One receipt per store per day with no close: two payments at one store on one day would not match the bank's rows.

**Since:** 2026-11-23 · owner · agreed rewording

### D-R125 · Item category

**Rule:** An item or a wish can carry one category, a label kept apart from its tags and chosen from a list the user edits. The category is optional and off by default: a Shopping setting turns it on.

**Why:** The savings app files each receipt line under a category (R122), and tags cannot say which one: an item carries tags of several kinds (R37).

**Acceptance:** With Shopping on and the setting off, no category field appears. With it on, an item or a wish can be given one category, and ticking a wish as bought keeps its category on the new item.

**Not:** Category as a tag kind: an item could then carry several, and tag kinds are for display (R37).

**Since:** 2026-11-23 · owner · agreed rewording

### Q-R126 · Lines with no category

**Rule:** How does the savings app file a receipt line that carries no category, because the setting is off or the item has none?

**Default:** The user picks a category for the line in the savings app's import review, and nothing is remembered, since the line carries nothing stable to remember it by. Changes: R122's import review and R123's lines.

**Why:** Picking by hand each time is simple but repeats for the same item; remembering per item would need the item's id (R52) on every line of the receipt file.

**Since:** 2026-11-23 · builder · agreed rewording

### D-R117 · Wishes

**Rule:** A list entry for something already in Inventory links to that item. Anything else put on the list is a wish, a record of its own, and becomes an Inventory item only when it is ticked as bought.

**Acceptance:** Putting a kettle that is not in Inventory on the list creates a wish and no item; search and the object of the day do not show it; ticking it as bought creates the item. Putting batteries that are in Inventory on the list links to that item, and ticking them as bought raises its quantity.

**Not:** A "to buy" item state: things not owned would show in search and as the object of the day.

**Since:** 2026-11-23 · owner · agreed rewording

### D-R118 · Shopping records in sync and backup

**Rule:** Wishes, list entries, stores and the price book sync (R51) and back up (R60) like every other record.

**Why:** They hold shop prices and what was bought, not accounts or balances, so the security bar R98 guards against does not rise.

**Acceptance:** A wish and a price added on one device appear on the other after a sync, and both survive export and import.

**Since:** 2026-11-23 · owner · agreed rewording

### D-R119 · Price-comparison library

**Rule:** In v1 the price comparison (R112) comes from a GPL-3.0 library of the owner's own, used as a dependency rather than copied in file by file (R23). It never fetches prices online, so the network uses of R15 are unchanged.

**Acceptance:** The library's licence is GPL-3.0, and with the network off every price comparison works.

**Since:** 2026-11-23 · owner · agreed rewording

### O-R116 · Price paid in the savings export

**Rule:** The savings export (R98, R113) counts the price actually paid, with every discount applied, never the saved price. A difference from the saved price is not sent as an amount of its own.

**Why:** Otherwise the money-out recorded in the savings app would not be true.

**Values:**

| Purchase | Saved price | Counted in the receipt total |
|---|---|---|
| Cookies Q 600 g, bought below the saved price (R115) | 1,59 € | 1,29 € |
| 3 packs at 1,59 € under "3 x 2" | 1,59 € each | 3,18 € |
| 1 pack at 2,00 € with 25% off | 2,00 € | 1,50 € |

**Acceptance:** Each row's purchase, ticked as bought, adds the amount in its last column to the receipt's exported total, and no export carries a difference or a saving as a separate amount.

**Since:** 2026-11-23 · owner · agreed rewording

### D-R121 · Showing the difference

**Rule:** Chatelaine does not show the difference between the price paid and the saved price (R115). Ticking an item as bought shows only the price paid.

**Acceptance:** Ticking an item as bought below or above its saved price shows no difference or saving, at the tick step or anywhere else.

**Not:** Showing the difference once at the tick step ("0,30 € less than saved").

**Since:** 2026-11-23 · owner · agreed rewording

### D-R109 · Expiry dates

**Rule:** Expiry dates are read from labels in photos, in the formats below, and fill the item's expiry date or months-after-opening field (R97); the user can also log or edit them by hand. Dates show in the interface language's format.

**Values:**

| Label | Language | Format | Fills |
|---|---|---|---|
| "Best before:" | English | dd/mm/yyyy or mm/yyyy | expiry date |
| "BBD:" | English | dd/mm/yyyy or mm/yyyy | expiry date |
| "SCAD." | Italian | dd/mm/yyyy or mm/yyyy | expiry date |
| "12M" (a number of months) | any | N months | months after opening, counted from "opened on" |

**Evidence:** Not checked: whether the multimodal recogniser reads label text well enough (R63).

**Acceptance:** A photo of a label in each format fills the right field, and each field can be typed or edited by hand. In the Italian interface a date shows as dd/mm/yyyy.

**Since:** 2026-11-22 · owner · agreed rewording

### D-R110 · Usage rate

**Rule:** Usage rate is worked out from what is already recorded, with nothing extra to log: in Pantry from quantity changes over time, in Wardrobe from "last worn" and laundry (R97).

**Acceptance:** An item's usage rate appears once its quantity (Pantry) or its last-worn date (Wardrobe) has changed, with no extra entry asked of the user.

**Since:** 2026-11-22 · owner · agreed rewording

### O-R98 · Savings app and the shopping list

**Rule:** Personal savings is a separate app, not a Chatelaine module. The two meet only through the Shopping module's list (R112), so the bridge exists only when Shopping is on: when something goes on the list, Chatelaine shows what is already owned and where ("you already have 3 of these"), pantry stock is used first when Pantry is on, and each receipt goes one way to the savings app as store, date and total paid (R113), by export or a shared file. Shop prices used for comparison stay in Chatelaine's price book (R112), while what was spent, budgets and goals stay in the savings app.

**Why:** Money is a different job (amounts, accounts, budgets) with its own rhythm and tone, and would raise the security bar for app lock, backups and folder sync. Shop prices are not personal finances: no account, balance or budget is involved. Chatelaine owns the shopping list because it knows the stock.

**Acceptance:** With Shopping on, putting an item on the shopping list shows how many are owned and where, and each receipt exports as store, date and total paid. With Shopping off, no shopping list or export appears. Chatelaine holds no budget or goal.

**Not:** Savings as a module inside Chatelaine: money is a different job, as Why says. The shopping list as a core feature: odd for someone who only wants to find where things are, such as important documents or rarely used small appliances.

**Since:** 2026-11-23 · owner · agreed rewording

### D-R99 · Reminders

**Rule:** Chatelaine owns every reminder about its things. Each notification belongs to one or more modules and can be turned on only while all of them are on; the Inventory module's are always available (R97). Every notification is opt-in (R86): all are off until the user turns them on, also when their modules are switched on.

**Values:**

| Notification | Module |
|---|---|
| expiry: best-before dates (medication, food), months after opening (cosmetics) | Inventory |
| low stock | Pantry |
| add to the shopping list | Pantry and Shopping |
| usage rate rising or falling | Pantry |
| time to do the laundry | Wardrobe |
| running out of clean items of a kind, the rest being in the laundry | Wardrobe |
| usage rate: none, low, medium or high; rising or falling | Wardrobe |
| seasonal swap | Wardrobe |

**Acceptance:** With any of its modules off, a notification can neither be turned on nor arrive. The Inventory module's notifications can be turned on at any time.

**Not:** Leaving reminders to the owner's earlier app: every reminder about Chatelaine's things belongs to Chatelaine. Reminders that are not about a thing, such as paying a bill: not Chatelaine's.

**Since:** 2026-11-23 · owner · agreed rewording

## 11. Stages

### D-R100 · Stage plan

**Rule:** The work runs in the stages below, in order of dependency. Each stage ends with its done-when check and, from stage 2 on, a walk on a real device.

**Values:**

| Stage | Holds | Done when | Unlocks |
|---|---|---|---|
| 0 Foundations | the spec; purpose map scored on the axes of R91; screen-by-purpose decisions; reference apps walked on the Snapdragon 845 phone; throwaway spike: small LLM and open-vocabulary recognition timed against R65 on the target phones; Compose Multiplatform window running on Android and Windows (R102) | owner approves the spec and the purpose map; spike timings recorded (`docs/evidence/stage0-recognition.md`); KMP spike run recorded (`docs/evidence/stage0-kmp.md`) | — |
| 1 Data and safety | data model (items, places, tags with parents and kept-together, photos, states, custom fields, history); database; backup and export; change-file sync | round-trip tests: export → import, two-device sync with conflicts, a corrupted database is kept, not deleted | — |
| 2 Logging and tags | quick add by name; unplaced inbox; item editor; tags with parents; bulk tag add and remove; search including sub-tags | owner-approved screens built; R05 passes | R05 |
| 3 Places and photos | place tree; camera (phone only, R21); item and place photos; settled-area path; place contents view; in use / away; move and bulk move | R09 passes; the move part of R11 passes | R09 |
| 4 Recognition | small open-vocabulary recognition model on phone and PC (PC works on synced phone photos); suggestions from photos; count; split a photo into items; bulk clear of suggestions; expiry dates read from labels (R109) | the bar of R65 is met on both target phones; R06, R07 and R08 pass | R06, R07, R08 |
| 5 Suggestions and grouping | embedding model; personal nearest-neighbour layer; tag suggestions for new items; kept-together tags with home and away views | suggestions accepted or corrected in one tap; R11 passes fully | R11 |
| 6 Visual map | automatic stylised place map with rearrangeable blocks; filter search by house or floor; fallback results from other places | R10 passes | R10 |
| 7 Clear-box surfaces | photo browsing; home-screen widget; label PDF with QR; object of the day | owner uses the widget and labels for a week | — |
| 8 Voice | dictated logging in Italian and English on the phone (R21); speech model downloaded on first use (R19); splitting a list into items | a dictated list of ten things becomes ten items | — |
| 9 Optional models | large recogniser; labeller LLM; download screen; licence checks | each optional model measurably improves a scenario | — |
| 10 Modules | the modules of R97, each designed with the owner first; notifications by module, expiry included (R99); the price-comparison library (R119) and the savings export (R98) | each module passes its own scenarios, to be written with the owner; R114 and R115 pass | R114, R115 |
| 11 Later | lending; app lock; NFC; lock-screen surface; rediscover prompt; free-drawing map editor; F-Droid build | each gets its own decision first | — |

**Acceptance:** Each stage closes only when its done-when holds and, from stage 2 on, after a walk on a real device.

**Since:** 2026-11-23 · owner · agreed rewording

### D-R101 · Order of stages 4 to 6

**Rule:** The visual map (stage 6) comes after recognition (stage 4) and suggestions (stage 5). The owner may reorder stages 4 to 6: none of them depends on the others.

**Why:** R06, R07 and R08 are daily during the first big tidy (R04), the app's first heavy use, while the map is reached through places that already exist from stage 3.

**Acceptance:** none: an ordering choice.

**Since:** 2026-11-22 · builder · agreed rewording

### O-R102 · Stage 0 spikes

**Rule:** Stage 0 reviews the sync of the earlier app in place of a change-file-sync spike; the review was done on 2026-10-28 (R58). The KMP spike stays and proves a Compose Multiplatform window running on Android, at minSdk 33 (R13), and on Windows. The model spike also stays, on the target phones of R65.

**Why:** The review found the earlier app has no Compose and no runnable Windows app.

**Evidence:** Stage 0 KMP spike, 2026-11-23 (`docs/evidence/stage0-kmp.md`): one shared screen ran on Windows and on Android at API 36 and API 34, from a build with minSdk 33. Not checked: an API 33 device.

**Acceptance:** A Compose Multiplatform window runs on Android and on Windows, and the spike timings are recorded, as stage 0 requires (R100).

**Not:** Replacing the KMP spike with the review as well, as first planned: the earlier app has no Compose and no runnable Windows app.

**Since:** 2026-11-23 · owner · own words

## 12. Test plan

### O-R65 · Recognition pass bar

**Rule:** Recognition passes when every row below holds on both target phones, a Snapdragon 888 flagship and a Snapdragon 695 budget phone; the slower one sets the bar. It is measured on a fixed set of about 20 photos taken for the purpose and kept out of the repository.

**Why:** About 10 s is the longest wait the owner accepts standing at a drawer, and a third of the items found is where correcting starts to beat typing. The owner may revise the numbers.

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

**Since:** 2026-11-22 · owner · own words

### O-R66 · Scoring crowded answers

**Rule:** For a crowded photo, each recogniser's answer is scored on as many lines as the shortest answer for that photo.

**Why:** Extra lines are extra reading.

**Evidence:** Used for the crowded scores in `docs/evidence/stage0-recognition.md`.

**Acceptance:** none: a scoring practice; R65 carries the check.

**Since:** 2026-11-22 · owner · own words

## Acceptance list

- R03 · Anti-goals: Reading every screen's text and every notification finds no streak, guilt or nagging wording. A fresh install sends no reminder the user did not turn on. A later model run never overwrites a correction. Stage 1's round-trip tests pass.
- R05 · Project from a tag: The steps succeed end to end, as stage 2's gate requires (R100).
- R06 · Photo with suggestions: The steps succeed end to end, as stage 4's gate requires (R100).
- R07 · Bulk clear: The steps succeed end to end, as stage 4's gate requires (R100).
- R08 · Box split into items: The steps succeed end to end, as stage 4's gate requires (R100).
- R09 · Before buying: The steps succeed end to end, as stage 3's gate requires (R100).
- R10 · Visual place map and filtered search: The steps succeed end to end, as stage 6's gate requires (R100).
- R11 · Seasonal swap: The move part succeeds at stage 3 and the whole scenario at stage 5, as those stages' gates require (R100).
- R114 · Cheapest pack under a tag: The three packs appear in that order with those prices per kg, as stage 10's gate requires (R100).
- R115 · Cheaper than the saved price: After the purchase the cookies rank by 1,29 € (2,15 €/kg), 1,59 € stays in the price history, and the receipt's savings export counts 1,29 €, as stage 10's gate requires (R100).
- R13 · Framework and targets: A Compose Multiplatform window runs on Android and on Windows, as stage 0 holds (R100, R102).
- R14 · Shared code: Platform source sets (for example `androidMain`, `jvmMain`) hold code only for the camera, file access, widgets, speech, model runtimes and printing. Everything else is in common code.
- R15 · Offline first: With the optional network uses off, a network capture over a full run of R05 to R11 shows no request at all.
- R16 · No Google Play services: On every build, the core module's resolved dependencies hold no `com.google.android.gms` or `com.google.firebase` artifact.
- R18 · Languages: A dictated list of ten things becomes ten items, in Italian and in English, as stage 8's gate requires (R100).
- R21 · Desk companion: The Windows build has no camera capture and no dictation entry. Recognition on photos synced from the phone, bulk editing, the place map, label printing and backup all work there.
- R22 · Publishing rules: The publish check reports no finding before each commit and push, and in CI, and a read of the new text finds none of the kinds above.
- R23 · Borrowing code: A script, run like the publish check, finds every copied file's licence notice and its line in `THIRD_PARTY.md`.
- R25 · Item: An item saved with only a name is stored and found by search. Each field in its Values can be set and survives export and import.
- R26 · Quantity and counts: A suggested count shows as "about N" and can be edited. With a switch on, suggestions in its scope ask for an exact count; with both off, every suggestion is approximate.
- R28 · Identical things: R06 passes with the three headphones saved as one item with a count of three.
- R29 · Order of an item's facts: Each of the three paths (name only; photo first, then name; placed in a settled area) ends in a saved item.
- R30 · Item states: An item can go unplaced, then placed, then in use / away, then placed again, and its state shows on its screen.
- R31 · Photo outdated flag: Editing an item, or moving an item into or out of a place, after the last photo raises the flag. A new photo clears it.
- R32 · Place tree: R10 passes, with its map running from drawer partitions up to several houses.
- R111 · Items and places: A drawer cannot be saved as an item, an item cannot hold other items, and an item saved inside a box is listed under that box as a place.
- R33 · Place details: A place takes tags, notes, a photo and a map position, and its size sets how it is drawn on the map.
- R34 · Tags: R05 passes.
- R35 · Tag tree: A second parent cannot be given to a tag, and no tag can sit inside its own sub-tag.
- R36 · Kept together: R11 passes fully, with the home and away views that stage 5 holds, as stage 5's gate requires (R100).
- R37 · Tag kinds: Each of the five kinds can be set on a tag and shows on it as an icon or a group heading. The user can rename, add and remove kinds.
- R38 · Default tag kinds: A fresh install offers exactly these five tag kinds.
- R39 · Suggested tags: R06 passes: the suggested tags the user keeps are saved with the item, and the deleted one is not.
- R40 · Editing suggestions: R07 and R08 pass: suggested tags are cleared in one action, and wrong items are corrected one at a time.
- R41 · Photos: A photo saves on an item or on a place. A region marked on a place photo opens the item it points to.
- R42 · Proposals from a photo: R06, R07 and R08 pass, as stage 4's gate requires (R100).
- R44 · Field types: A field of each type can be created on an item and on a place, filled in, and survives export and import.
- R46 · Where a field attaches: A field attached to a tag appears on every item that carries the tag, including items tagged later. A field added to one item appears only on that item.
- R47 · User-editable lists: The user can add, rename and remove tag kinds, homepage cards and widget content, and the change shows at once.
- R48 · Local database: Each device opens its own database with the network off.
- R50 · A database that fails to open: In stage 1's round-trip tests, a corrupted database is kept, not deleted (R100).
- R51 · Folder sync: Two-device sync with conflicts passes stage 1's round-trip tests (R100).
- R52 · Record ids and history: Every record has a unique id, and every change to it adds an entry to its history.
- R53 · No shared database file: After any stage 1 sync test, the sync folder holds no database file.
- R54 · Conflicting edits: In stage 1's two-device sync test with conflicts (R100), the later edit wins and the overwritten value can be restored from history.
- R55 · Sync groups: When the two devices edit different fields of one item at the same time, both edits survive. The losing value of a same-field conflict is in history.
- R56 · Photos in the sync folder: After a sync, each photo is in the folder as a file named by its content hash, and the other device shows it.
- R57 · Any folder-sync tool: Stage 1's sync test passes with Syncthing (Syncthing-Fork on the phone), and the app has no code specific to one sync tool beyond the conflict-copy patterns. Before release, the test passes once with a second folder-sync tool.
- R59 · Sync lessons from the earlier app: One test per lesson: a replayed old version is ignored; the device-id file is excluded from Android Auto Backup; export reads the outbox; a purged row never comes back; a recovered database re-reads its folder files.
- R60 · Backup and export: Export → import passes stage 1's round-trip tests (R100).
- R61 · Restoring a backup: Restoring a backup on one device keeps every edit made after the backup, on both devices. Rolling back is a separate action that adds a history entry, and the replaced state can be restored.
- R62 · Image recognition: As stage 4's gate requires (R100), R65's bar is met on both target phones, and R06, R07 and R08 pass.
- R63 · Model roles: Each optional model measurably improves a scenario, as stage 9's gate requires (R100).
- R64 · Models on device: With the network off, every installed model runs. No model above the base set is in the app's download.
- R67 · Optional language model: The model measurably improves a scenario, as stage 9's gate requires (R100).
- R68 · Open-vocabulary recogniser: R06 passes: the recogniser names the headphones.
- R69 · Recognition as a hint: R08 passes.
- R70 · Model cooperation: Suggestions are accepted or corrected in one tap, as stage 5's gate requires (R100).
- R71 · Personal learning: As stage 5's gate requires (R100), suggestions are accepted or corrected in one tap, and R11 passes fully.
- R72 · Model licences: Every adopted model has its licence recorded, and each licence allows redistribution from a GPL app's download screen.
- R74 · Repeated lines: An answer that repeats a line shows it once. An answer longer than the instructions' own limit of 15 lines (R73) shows 15.
- R78 · AI-service key: With the key off, no photo leaves the device. With it on, each photo is sent with the tightened instructions, and the answer comes back as proposals.
- R79 · Correction-first logging form: On the built form, a typical drawer (10 lines: 8 right, 1.5 close, 0.5 wrong) is corrected within the time and the taps that the typical-drawer task model predicts for the chosen layout, and the owner's tap check on the phone confirms it. The limits are set when the form's layout is chosen.
- R80 · Lump, then refine: On an open line, the owner's example takes four edits and a save, each one action. R79 counts the taps.
- R81 · Search through sub-tags: With "red skirt" inside "sewing", a search for "sewing" finds the items tagged only "red skirt".
- R82 · Search across languages: With the interface in English, "cucito" finds items tagged "sewing", and the reverse works in Italian. Switching the language changes the built-in kind names but not the user's tags.
- R83 · Visual place map: R10 passes, as stage 6's gate requires (R100).
- R85 · Tag card: With "red skirt" inside "sewing", the tag card shows "sewing" and not "red skirt".
- R86 · No push by default: A fresh install with default settings sends no notification during a week of use.
- R87 · Clear-box surfaces: The owner uses the widget and the labels for a week, as stage 7's gate requires (R100).
- R88 · Labels: The owner uses the labels for a week, as stage 7's gate requires (R100).
- R90 · Purpose map: The owner approves the spec and the purpose map, as stage 0's gate requires (R100).
- R91 · Scoring axes: Every reference app, scored as a control, loses: a control that scores well means the axes are wrong.
- R93 · Screens before building: Every screen built is owner-approved, as stage 2's gate requires (R100).
- R96 · Consistent elements: Run on every screen's render, the look-alike check finds no two elements with different purposes that look alike, and the size check finds same-purpose elements the same size across phases.
- R97 · Modules: Each module passes its own scenarios, written with the owner, as stage 10's gate requires (R100).
- R112 · Shopping module: With Shopping on and Pantry off, a non-consumable thing can be put on the list as a wish (R117) and ticked as bought. With Pantry on, a low-stock item shows as a suggestion and joins the list only after one tap. With Shopping off, no list, price book, comparison or savings export appears.
- R113 · Export per receipt: A receipt of five items exports one row with the store, the date, the total paid and five lines, and no line is exported as an expense of its own.
- R122 · Savings categories: Every line with a category carries it in the receipt file, and Chatelaine reads nothing from the savings app.
- R123 · Receipt file: An exported receipt file holds every field above and no tags, its total paid equals the sum of its lines, and exporting the same receipt twice gives the same receipt id.
- R124 · What makes a receipt: Two closes at one store on one day give two receipts with different ids; with no close, the items ticked at one store on one day make one receipt; ticking an item at a second store starts a new receipt; re-exporting a closed receipt gives the same file.
- R125 · Item category: With Shopping on and the setting off, no category field appears. With it on, an item or a wish can be given one category, and ticking a wish as bought keeps its category on the new item.
- R117 · Wishes: Putting a kettle that is not in Inventory on the list creates a wish and no item; search and the object of the day do not show it; ticking it as bought creates the item. Putting batteries that are in Inventory on the list links to that item, and ticking them as bought raises its quantity.
- R118 · Shopping records in sync and backup: A wish and a price added on one device appear on the other after a sync, and both survive export and import.
- R119 · Price-comparison library: The library's licence is GPL-3.0, and with the network off every price comparison works.
- R116 · Price paid in the savings export: Each row's purchase, ticked as bought, adds the amount in its last column to the receipt's exported total, and no export carries a difference or a saving as a separate amount.
- R121 · Showing the difference: Ticking an item as bought below or above its saved price shows no difference or saving, at the tick step or anywhere else.
- R109 · Expiry dates: A photo of a label in each format fills the right field, and each field can be typed or edited by hand. In the Italian interface a date shows as dd/mm/yyyy.
- R110 · Usage rate: An item's usage rate appears once its quantity (Pantry) or its last-worn date (Wardrobe) has changed, with no extra entry asked of the user.
- R98 · Savings app and the shopping list: With Shopping on, putting an item on the shopping list shows how many are owned and where, and each receipt exports as store, date and total paid. With Shopping off, no shopping list or export appears. Chatelaine holds no budget or goal.
- R99 · Reminders: With any of its modules off, a notification can neither be turned on nor arrive. The Inventory module's notifications can be turned on at any time.
- R100 · Stage plan: Each stage closes only when its done-when holds and, from stage 2 on, after a walk on a real device.
- R102 · Stage 0 spikes: A Compose Multiplatform window runs on Android and on Windows, and the spike timings are recorded, as stage 0 requires (R100).
- R65 · Recognition pass bar: The bar is met on both target phones, as stage 4's gate requires (R100).

## Out of scope

### O-R103 · Insurance records

**Rule:** Value, receipts and serial numbers for insurance use are out of scope for v1. The prices kept are shop prices in the price book and the prices of bought items (R112), for comparing and for the savings-app bridge (R98).

**Why:** Value tracking is a different job.

**Acceptance:** none: a scope boundary.

**Since:** 2026-11-23 · owner · agreed rewording

### O-R104 · App lock

**Rule:** An app lock (biometric, PIN or password) is not in v1. The stage plan lists it for stage 11 (R100).

**Why:** One user, offline first, on a personal phone: the phone's own lock protects it.

**Acceptance:** none: a scope boundary; stage 11 gives it its own decision first.

**Since:** 2026-11-22 · owner · own words

### O-R105 · Lending

**Rule:** Lending, recording who has a thing, comes in a later stage. The stage plan lists it for stage 11 (R100).

**Why:** Lending is not a daily job.

**Acceptance:** none: a scope boundary; stage 11 gives it its own decision first.

**Since:** 2026-11-22 · owner · own words

### O-R106 · Inventory import

**Rule:** Stage 1 has no importer for existing inventory data.

**Why:** There is no existing inventory data to import.

**Acceptance:** none: a scope boundary.

**Since:** 2026-11-22 · owner · agreed rewording

### D-R107 · Low stock outside Pantry

**Rule:** Low-stock alerts outside the Pantry module (R97) are not in v1. Shopping lists live in the Shopping module (R112).

**Acceptance:** none: a scope boundary.

**Since:** 2026-11-23 · owner · agreed rewording

### O-R120 · Shopping notifications

**Rule:** The Shopping module has no notifications of its own in v1, such as a promotion ending or a shop's price dropping.

**Why:** Following shop prices would need crawling or scraping shop sites, and the anti-goals (R03) keep notifications few.

**Acceptance:** none: a scope boundary.

**Since:** 2026-11-23 · owner · agreed rewording
