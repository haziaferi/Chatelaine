# Purpose map (draft)

Chatelaine's purposes, and the planned screens and controls that serve each. Drafted 2026-10-28, **before any screen exists**, so layout and navigation are decided once instead of corrected later (SPEC D-26, D-27).

Status: candidate set, not scored. The purpose set and the screen list are chosen by scoring against reference apps once the owner has agreed the axes (SPEC O-01). Reference apps serve as baselines to improve upon, or as controls that must lose.

How to read it:
- **Entry**: a planned screen, panel or control.
- **Where**: the screen it lives on.
- **Scenarios**: the acceptance scenarios it serves (SPEC §3).
- **Overlaps**: entries that may be two ways to the same job. Each overlap is a candidate for merging when the map is scored.

## Purposes (candidates)

| | Purpose | Its job | Scenarios |
|---|---|---|---|
| P1 | **Find** | get to a thing, or everything for a task, by name, tag, meaning or place | S1, S5, S6 |
| P2 | **Record** | get things into the app with the least effort: a word, a phrase, a photo; correct what the models got wrong | S2, S3, S4 |
| P3 | **Organise** | shape tags and parent tags, keep things together, move them, swap seasons | S1, S3, S7 |
| P4 | **Know the storage** | the places themselves: houses, floors, rooms, furniture, containers, and how they look | S5, S6 |
| P5 | **Keep in view** | the clear-box effect: stored things come back into sight without being asked | — |
| P6 | **Look after it** | backup, sync, languages, models, customisation, lock | — |

## P1 Find

| Entry | Where | Scenarios | Overlaps |
|---|---|---|---|
| **Search bar**: by name, tag (child tags included) and meaning | homepage, top | S1, S6 | Tag grid, Place filter |
| **Place filter**: limit search to a house, floor or place | search results | S6 | Place map |
| **Fallback results**: the same search in other places when the chosen one has none | search results | S6 | — |
| **Place contents**: what is in a place, with its photo | place screen | S5 | Photo browsing |
| **Tag results**: items carrying a tag or its children, with multi-select | tag screen | S1 | Search bar |

## P2 Record

| Entry | Where | Scenarios | Overlaps |
|---|---|---|---|
| **Quick add**: a name and nothing else; lands in Unplaced | global action | — | Voice add |
| **Voice add**: a dictated list split into items | global action (stage 8) | — | Quick add |
| **Photo add**: photograph, get proposed items, count and tags | global action | S2, S3, S4 | Settled area |
| **Settled area**: photograph a finished place, mark the items in it | place screen | — | Photo add |
| **Proposal review**: accept, correct, discard or split proposals; clear all suggested tags at once | after Photo add | S2, S3, S4 | Bulk tag edit |
| **Unplaced inbox**: items with no place yet | homepage or Places | — | — |
| **Item editor**: name, tags, quantity, fields, notes, place, state | item screen | S2 | — |

## P3 Organise

| Entry | Where | Scenarios | Overlaps |
|---|---|---|---|
| **Tag grid**: top-level tags, opening into children | homepage card | S1, S7 | Search bar |
| **Tag editor**: name, parent, kind, kept together, home place | tag screen | S1 | — |
| **Bulk tag edit**: add or remove tags on a selection | any multi-select list | S1, S3 | Proposal review |
| **Tag suggestions**: proposed tags and parent tags for new items | item editor, proposal review | S2 | Proposal review |
| **Away view**: things of a kept-together tag not in its home | tag screen | S7 | Place contents |
| **Bulk move**: move a selection or a whole tag to another place; marks photos outdated | any multi-select list | S7 | — |

## P4 Know the storage

| Entry | Where | Scenarios | Overlaps |
|---|---|---|---|
| **Places entry**: house map, or floor cards, or house grid, depending on how many exist | homepage card | S6 | Place map |
| **Place map**: stylised blocks drawn from the place tree, rearrangeable | Places | S6 | Place filter, Places entry |
| **Place editor**: name, kind, parent, tags, photo, position | place screen | S6 | — |

## P5 Keep in view

| Entry | Where | Scenarios | Overlaps |
|---|---|---|---|
| **Photo browsing**: places shown by their contents photos | Places / place screen | — | Place contents |
| **Object of the day**: least-searched things first | homepage | — | Rediscover |
| **Home-screen widget**: rotating contents of a place or tag | Android launcher | — | — |
| **Photo labels**: A4 sticker PDF with collage and QR | place screen, export | — | — |
| **Rediscover prompt**: opt-in notification | settings (stage 10) | — | Object of the day |

## P6 Look after it

| Entry | Where | Scenarios | Overlaps |
|---|---|---|---|
| **Backup and export** | settings | — | Sync |
| **Sync folder** | settings | — | Backup |
| **Language** (interface, dictation) | settings | — | — |
| **Models**: download, remove, which device | settings | — | — |
| **Customisation**: fields, place kinds, tag kinds, homepage cards, widget | settings | — | Tag editor, Place editor |
| **App lock** (stage 10) | settings | — | — |

## Modules (SPEC D-33, stage 10; mapped when designed)

| Entry | Module | Where | Overlaps |
|---|---|---|---|
| **Outfits**: links between clothing items | Wardrobe | module view | Tag results |
| **Seasonal swap** | Wardrobe | module view | Bulk move (S7) |
| **Not worn in a year** | Wardrobe | module view | Object of the day |
| **Expiring soon** | Pantry | module view | Rediscover prompt |
| **Shopping list**: items below minimum stock | Pantry | module view | — |
| **Already have it?**: quick check while shopping | Pantry | module view | Search bar (S5) |

## Overlap candidates to settle when scoring

1. **Search bar / Tag grid / Tag results**: three ways into tagged items.
2. **Place map / Places entry / Place filter**: the map may *be* the place filter.
3. **Place contents / Photo browsing**: one place screen showing its photo first may serve both.
4. **Photo add / Settled area**: the same camera flow with a different starting point.
5. **Proposal review / Bulk tag edit / Tag suggestions**: one selection toolbar may serve all three.
6. **Object of the day / Rediscover prompt**: same job, passive vs opt-in push.
7. **Backup / Sync**: both write to a user-chosen folder; one settings entry may hold both.

## Reference apps

Baselines and controls (SPEC D-27): boxorg, Flindex, a-personal-stuff, whohasmystuff, prepper, hot-stuff, StuffList, Homebox, plus Spot It, Nestmer and Store & Forget from their listings. The F-Droid ones are walked on the 6T.
