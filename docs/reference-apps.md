# Reference apps (stage 0, 2026-10-28)

The seven open-source apps from SPEC R24, inventoried as baselines and controls for the purpose map (SPEC R90, R91). Four were walked on an older Snapdragon 845 phone. Prepper and hot-stuff need Android 13 and StuffList was never published, so those three were read from source. Per-app inventories and 54 screenshots are kept in a private folder outside the repository.

## The apps in brief

| App | How it works | Places | Tags | Look |
|---|---|---|---|---|
| **boxorg** 1.1.2 (walked, partly) | Photo-first: photograph a box's contents, mark each object on the photo, label it | one level; a box is a number | free-text labels on objects, ranked by use | custom purple, very sparse |
| **Flindex** 1.1.0 (walked) | Each box is a typed or scanned code holding a flat list of names | one level | none | stock dark Material, bare |
| **Personal Stuff** 2.3.0 (walked) | Flat item list with amount, threshold, price, barcode, expiry, tags, photos; usage, purchase and maintenance logs; checklists | none | flat tags; checklists act as named groups | stock Material, loud tiles, eight equal "Add…" tiles on home |
| **Who Has My Stuff** 1.1.3 (walked) | Lent items: what, type, date, person; history of returns | none | fixed types | old Holo, dense, quiet |
| **Prepper** 0.8.3 (source; opened on the Snapdragon 888 phone) | Emergency supplies; every location, container and supply needs a scanned barcode; expiry badges | flat locations, containers nested to any depth | one free-text category per supply type | Compose |
| **hot-stuff** 1.1.1 (source) | One-off insurance list: required free-text room and category, one building | room as text | category as text | Views |
| **StuffList** (source, no licence) | Packing checklist per trip with taken / brought-back ticks; reminder an hour before leaving | none | none | — |

## Taps from the main screen (plus typing)

Walked counts are measured; source counts are estimated from the navigation code (marked ~).

| Task | boxorg | Flindex | Personal Stuff | Who Has My Stuff | Prepper | hot-stuff | StuffList |
|---|---|---|---|---|---|---|---|
| (a) add a named item, no photo | not supported | 4 | 4 | 3 (as a lent item) | ~7 + barcode scan | ~2 + 4 fields | ~3 |
| (b) find by name | not measured | 2 | 2 | not supported | ~4 | ~3 | not supported |
| (c) see inside one place | not measured | 2 (or 1 scan) | not supported | not supported | ~2 (direct contents) | ~2 + typing | ~1 (a trip) |
| (d) tag several items at once | not measured | not supported | not supported | not supported | not supported | not supported | not supported |
| (e) move an item | in code, not measured | not supported | not supported | 3 (change person) | ~7 + scan | ~6 + retyping | not supported |

**No reference app supports tagging or moving several items at once** (R05, R07, R11). Only Prepper nests places deeper than one level, and none draws them (R10), falls back to other places when a search finds nothing (R10), or searches by meaning across languages (R82).

## Functions by candidate purpose

| Purpose | What the reference apps offer |
|---|---|
| P1 Find | name/label search (boxorg, Flindex, Personal Stuff, hot-stuff); results grouped by box (Flindex); type search filtered by location (Prepper); open a box by its code (Flindex, Prepper) |
| P2 Record | photo with marked objects (boxorg); quick name entry (Flindex); full item forms (Personal Stuff, Prepper, hot-stuff); duplicate an item (Prepper) |
| P3 Organise | labels ranked by use, move an object between boxes (boxorg); flat tags and checklists (Personal Stuff); single-item moves only everywhere |
| P4 Know the storage | location › container tree with breadcrumb and a photo per place (Prepper); one level of boxes (boxorg, Flindex) |
| P5 Keep in view | weak everywhere: lent things with dates (Who Has My Stuff); "a box is its photo" (boxorg); codes stuck on boxes (Flindex); expiring soon and recent additions (Prepper) |
| P6 Look after it | JSON export/import (Flindex); automatic folder backup (Personal Stuff); zip backup and restore (Prepper); theme settings |

## Functions no candidate purpose covers

| Function | Seen in | Where it belongs for Chatelaine |
|---|---|---|
| Lending and return history | Who Has My Stuff | stage 11 (R105); a purpose then, or part of "in use / away" |
| Expiry and low-stock alerts, on by default | Personal Stuff, Prepper | Pantry module (R97), and opt-in only (R86) |
| Usage, purchase and maintenance logs; stock movements | Personal Stuff | Pantry module or out of scope |
| Scanning a code to open or register a box | Flindex, Prepper | P1 (open a place by its label's QR) — to place when scoring |
| Product barcode lookup | Personal Stuff | not planned |
| Price and total value | Personal Stuff, hot-stuff | out of scope (R103) |
| Trip packing (taken / brought back) | StuffList | close to "in use / away" and lending |
| Calendar entries, in-app help, unit conversion | Who Has My Stuff, hot-stuff, Prepper | not planned; unit handling comes with typed units (R44) |

## Code worth borrowing (SPEC R23)

From Prepper (GPL-3.0), under `app/src/main/java/io/github/escapedcanadian/prepper/`:
- `data/repository/ContainerRepository.kt`: move a box with everything inside; needs a guard against moving a box into itself.
- `ui/components/ContainerBreadcrumb.kt`: tappable "location › … › box" path.
- `ui/components/SearchableDropdown.kt`: type-ahead picker that can create a missing entry.
- `ui/components/ExpirationBadge.kt`, `util/DateUtils.kt`: expiry status (Pantry); swap `java.time` for `kotlinx-datetime`.
- `export/CsvExportManager.kt`, `export/CsvImportManager.kt`: zip backup and restore (Android/JVM).
- `util/PhotoHelper.kt`: photo resize and rotation fix (Android).

From hot-stuff (Apache-2.0): `app/src/main/res/values/arrays.xml`, 58 room names and 63 categories, as starter place kinds and tags.

StuffList has no licence: ideas only (photo grid, reminder at the moment of need, "Add more" in the add form), written from scratch if used.

## Not covered yet

- **boxorg's photo flow** was not walked: it opens the system camera, which would have photographed the room. It's the closest reference to Chatelaine's photo-first path.
- **Prepper** was opened on the Snapdragon 888 phone with the camera denied: nothing can be stored without a camera. The barcode field rejects typed text, Register stays disabled, the scanner has no manual entry, and supply types can't be deleted anywhere. Tap counts above stay estimated from code.
- **hot-stuff** was not walked: its first button accepts terms of service (with mandatory arbitration), which only the owner can agree to. Its row stays from code.
- No export, import, backup or scan flow was run in any app, since each leaves the app for a system screen or the camera.
