# Purpose map

Which purpose each screen serves, decided before any screen is built (R90) and scored on R91's axes against the seven walked reference apps. The Inventory screens, under purpose set S5, approved by the owner on 2026-11-25; the Wardrobe, Pantry and Shopping screens are provisional until each module is designed (R97, stage 10), and the homepage is designed last (R84). Replaces the unscored draft of 2026-10-28 (in git history); its purposes, scored as set SD, came last of five.

Generated from `docs/purpose-map.json` by `tools/purpose_map.py --render`; edit the JSON, not this page.

## Purposes

| | Purpose | Its job | Screens |
|---|---|---|---|
| P1 | **Log** | get things into the app, by hand or from a photo, and fix what was logged | 4 |
| P2 | **Find** | find a thing and see where it is | 4 |
| P3 | **Arrange** | keep places and tags in order and things where they belong | 2 |
| P4 | **Use up and buy** | keep track of what runs out, wears or needs buying, and buy it | 4 |
| P5 | **Look after the app** | keep the data safe and the app set up | 2 |

## P1 Log

| Screen | Status | Holds | Reference apps with this kind of function |
|---|---|---|---|
| **Item page** | approved 2026-11-25 | edit an item's name, notes and fields; record an item's quantity; split identical things into separate items; mark an item away and back; move an item to another place; tag items and places; add a brand, model or purchase date; set expiry and opening dates; record last worn and laundry state; restore an overwritten value from history | boxorg, flindex, hot-stuff, personal-stuff, prepper, StuffList, whohasmystuff |
| **Quick add** | approved 2026-11-25 | add an item by name only; dictate a list of things to log | boxorg, flindex, hot-stuff, personal-stuff, prepper, StuffList |
| **Capture and proposals** | approved 2026-11-25 | log an item photo first, name later; take photos of items and places; log things from a photo as proposals; correct proposed lines in a few taps; split a photo into items with counts; accept or delete suggested tags; clear all suggested tags at once; get tag and place suggestions; group logged things by similarity; read expiry dates from label photos; recognise phone photos on the pc | boxorg, hot-stuff, personal-stuff, prepper, StuffList |
| **Fields** | approved 2026-11-25 | define custom fields; attach a field to tags or items; rename, hide or edit module preset fields | boxorg, prepper |

## P2 Find

| Screen | Status | Holds | Reference apps with this kind of function |
|---|---|---|---|
| **Homepage** | designed last, after every module (R84) | open places from the homepage; see an object of the day; choose the homepage cards | boxorg, flindex, hot-stuff, prepper |
| **Search and results** | approved 2026-11-25 | search items by name or tag; filter search by house or floor; see items not yet placed; move several items at once; add or remove tags in bulk; bulk edit fields on several items; see things expiring soon | boxorg, flindex, hot-stuff, personal-stuff, prepper, StuffList |
| **Place map** | approved 2026-11-25 | browse a visual map of places; rearrange place blocks on the map; draw the place map freely | boxorg, flindex, hot-stuff, prepper |
| **Photos** | approved 2026-11-25 | see which photos are outdated; browse places by their photos | boxorg, flindex, hot-stuff, personal-stuff, prepper, StuffList |

## P3 Arrange

| Screen | Status | Holds | Reference apps with this kind of function |
|---|---|---|---|
| **Place page** | approved 2026-11-25 | build a place tree across several sites; give a place tags, notes, photo; see what is inside a place; see a place's path up the tree; move a place with its contents; mark items on a settled-area photo; print photo labels with a qr code; open a place from its label qr; open a place from an nfc tag | boxorg, flindex, hot-stuff, personal-stuff, prepper, StuffList |
| **Tags** | approved 2026-11-25 | nest tags under a parent tag; give a kept-together tag a home place; see a tag's items away from home; set a tag's kind and edit kinds; browse top-level tags | boxorg, flindex, hot-stuff, personal-stuff, prepper, StuffList, whohasmystuff |

## P4 Use up and buy

| Screen | Status | Holds | Reference apps with this kind of function |
|---|---|---|---|
| **Expiration timeline** | approved 2026-11-25 | see every expiry date, closest first | flindex, hot-stuff, personal-stuff, prepper, StuffList |
| **Running out and wear** | provisional: confirmed when its module is designed (R97, stage 10) | see an item's usage rate; count stock with − and +; set a minimum stock; see an estimated run-out day; put together outfits; swap clothes at the change of season; see clothes not worn in a year | boxorg, flindex, hot-stuff, personal-stuff, prepper, StuffList |
| **Lists and store lists** | provisional: confirmed when its module is designed (R97, stage 10) | send finished consumables to the shopping list; keep a shopping list; keep a wish list; add low-stock items to the list; see what is already owned and where; edit shopping lists on the pc; make a store list; pin a store list for repeated runs; tick items bought with price paid; tick products sold by weight; add a bag when ticking; mark a discount when ticking; untick before the receipt closes; close a receipt at the till; discount a whole receipt; get warned about other store lists; send receipts to the savings app | boxorg, flindex, hot-stuff, personal-stuff, prepper, StuffList, whohasmystuff |
| **Prices** | provisional: confirmed when its module is designed (R97, stage 10) | compare packs by price per unit; compare one product across stores; keep shop prices and promotions; add, rename and delete stores; mark products a store carries | boxorg, personal-stuff, prepper |

## P5 Look after the app

| Screen | Status | Holds | Reference apps with this kind of function |
|---|---|---|---|
| **Backup and restore** | approved 2026-11-25 | export and import data with photos; restore a backup by merging; roll back to a backup | boxorg, flindex, hot-stuff, personal-stuff, prepper, whohasmystuff |
| **Settings** | approved 2026-11-25 | download larger optional models; use an ai-service key for recognition; choose approximate or exact counts; turn modules on or off; give items and wishes a savings category; turn on the savings export; sync phone and pc through a folder; back up automatically to a chosen folder; switch the app's language; get expiry notifications; get low-stock notifications; get add-to-list notifications; get usage-rate notifications; get run-out notifications for chosen items; get laundry notifications; get a seasonal swap notification | boxorg, flindex, hot-stuff, personal-stuff, prepper, StuffList, whohasmystuff |

## Surfaces outside the screens

| Surface | Status | Holds |
|---|---|---|
| **Widget, lock screen and prompts** | approved 2026-11-25 | keep a place or tag in view; show stored things on the lock screen; get an opt-in rediscover prompt |

## One home per thing

| Thing | Home | Repeats |
|---|---|---|
| an item's details | Item page | none |
| a place and what it holds | Place page | none |
| the tag tree | Tags | none |
| custom fields | Fields | none |
| an item's quantity | Item page | Running out and wear (stock counted with − and + in the running-out view) |
| an item's expiry date | Item page | none |
| last worn and laundry | Item page | Running out and wear (laundry marked from the clothes in the running-out view) |
| the shopping list | Lists and store lists | none |
| a store list | Lists and store lists | none |
| shop prices | Prices | none |
| settings | Settings | none |
| notification switches | Settings | none |
| backups | Backup and restore | none |

## Overlap candidates

| Entries | How the map settles it |
|---|---|
| search bar, tag grid, tag results | tag results are Search and results filtered by the tag; the Tags screen holds only the tag tree |
| place map, Places entry, place filter | the Places card opens the map; the filter stays on Search and results; open: whether the map itself filters, at stage 3 |
| place contents, photo browsing | two screens, Place page and Photos; open: one place screen showing its photo first, at stage 3 |
| photo add, settled area | Capture and the Place page; open: one camera flow with two starting points, at stages 3 and 4 |
| proposal review, bulk tag edit, tag suggestions | proposals on Capture, bulk edits on Search and results; open: one selection toolbar, at stage 2 |
| object of the day, rediscover prompt | two: one passive on the homepage, one opt-in outside the app (R86) |
| backup, sync | the sync folder in Settings, backups on their own screen; open: one settings entry for both |

## Scores (R91)

- Coverage and exclusivity: means over the seven walked apps' 122 functions, each with the 23 scenario steps of R05 to R11, R114 and R115. Fit: over Chatelaine's 107 functions. Potential: the share of the 76 functions Chatelaine lacks or only partly has whose main facet lands in a purpose.
- Screen count: functions per screen, 2 or fewer scores 0, 8 or more scores 1. Steps: per scenario, the fewest taps any map needs over this map's taps, 0 when it cannot be done. Recall: 1 minus the share of steps that need something the screen does not show.
- Plateau test: how often a map comes first with every constant moved down and up by a third.
- Reference apps are scored as controls on the screens-and-homes axes; every one must score below the chosen map.

### Purpose sets

| | coverage | exclusivity | balance | fit | potential | total | rank |
|---|---|---|---|---|---|---|---|
| S5 five, by what the user is doing | 0.993 | 0.558 | 0.919 | 0.874 | 0.974 | 0.864 | 1 |
| SM one purpose per module | 0.993 | 0.834 | 0.553 | 0.911 | 0.974 | 0.853 | 2 |
| S6 six, by job | 0.993 | 0.463 | 0.918 | 0.864 | 0.974 | 0.842 | 3 |
| S7 seven, by job | 0.993 | 0.460 | 0.922 | 0.855 | 0.974 | 0.841 | 4 |
| SD the 2026-10-28 draft, its modules section as one purpose | 0.993 | 0.444 | 0.883 | 0.827 | 0.974 | 0.824 | 5 |

### Maps and controls

| | screen count | one home per thing | steps per scenario | recall load | total | rank |
|---|---|---|---|---|---|---|
| by purpose | 0.781 | 0.385 | 0.926 | 0.973 | 0.766 | 1 |
| hybrid | 0.657 | 0.143 | 0.926 | 1.000 | 0.682 | 2 |
| by module | 0.410 | 0.000 | 0.889 | 1.000 | 0.575 | 3 |
| control: boxorg | 0.125 | 1.000 | 0.179 | 0.578 | 0.470 | 4 |
| control: prepper | 0.000 | 1.000 | 0.068 | 0.800 | 0.467 | 5 |
| control: hot-stuff | 0.000 | 1.000 | 0.048 | 0.707 | 0.439 | 6 |
| control: flindex | 0.000 | 1.000 | 0.069 | 0.618 | 0.422 | 7 |
| control: StuffList | 0.000 | 1.000 | 0.123 | 0.486 | 0.402 | 8 |
| control: personal-stuff | 0.000 | 0.636 | 0.035 | 0.542 | 0.303 | 9 |
| control: whohasmystuff | 0.000 | 0.000 | 0.000 | 0.000 | 0.000 | 10 |

| | screens | functions | per screen | repeats / things | taps mean | recall steps | undone | first in the plateau test |
|---|---|---|---|---|---|---|---|---|
| hybrid | 18 | 107 | 5.94 | 3 / 14 | 4.11 | 0 | 0 | 0% |
| by module | 24 | 107 | 4.46 | 4 / 14 | 4.22 | 0 | 0 | 0% |
| by purpose | 16 | 107 | 6.69 | 2 / 13 | 4.11 | 1 | 0 | 100% |
| StuffList | 9 | 14 | 1.56 | 0 / 6 | 15.0 | 12 | 7 | 0% |
| boxorg | 4 | 11 | 2.75 | 0 / 4 | 13.0 | 13 | 6 | 0% |
| flindex | 7 | 13 | 1.86 | 0 / 4 | 13.5 | 6 | 7 | 0% |
| hot-stuff | 10 | 19 | 1.9 | 0 / 4 | 17.0 | 5 | 7 | 0% |
| personal-stuff | 17 | 26 | 1.53 | 1 / 11 | 16.0 | 3 | 8 | 0% |
| prepper | 19 | 31 | 1.63 | 0 / 6 | 14.0 | 0 | 7 | 0% |
| whohasmystuff | 4 | 8 | 2.0 | 2 / 2 | None | 0 | 9 | 0% |

### Purposes of each set

| Set | Purposes |
|---|---|
| S5 five, by what the user is doing | Log · Find · Arrange · Use up and buy · Look after the app |
| S7 seven, by job | Log · Find · Places · Tags · Run out and wear · Shop · Look after the app |
| SM one purpose per module | Inventory · Wardrobe · Pantry · Shopping · App |
| S6 six, by job | Log · Find · Arrange · Run out and wear · Shop · Look after the app |
| SD the 2026-10-28 draft, its modules section as one purpose | Find · Record · Organise · Know the storage · Keep in view · Look after it · Modules |

### Screens of the other candidate maps

| Map | Screens |
|---|---|
| hybrid | Homepage, Search and results, Expiration timeline, Item page, Quick add, Capture and proposals, Fields, Place page, Place map, Tags, Photos, Backup and restore, Pantry, Wardrobe, Shopping list, Store list, Prices, Settings |
| by module | Homepage, Search and results, Expiration timeline, Item page, Quick add, Capture and proposals, Fields, Place page, Place map, Tags, Photos, Backup and restore, Wardrobe, Outfits, Seasonal swap, Pantry stock, Run-out, Shopping list, Wish list, Store list, Prices, Stores, Settings, Notifications |

## Owner's answers

- 2026-11-25: scoring defaults for R91 accepted; screen count as functions per screen and steps against the fewest taps, after the first scoring let five reference apps win.
- 2026-11-25: screens by purpose confirmed for the Inventory module only; the other modules are not yet conceptually developed.
- 2026-11-25: an expiration timeline lists every item with an expiry date, closest first (R153); expiring soon is a filter on Search and results.
- 2026-11-25: the homepage is designed last, after every module is complete (R84).
