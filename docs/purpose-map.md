# Purpose map (draft)

Chatelaine's purposes, and the planned screens and controls that serve each. Drafted 2026-10-28, **before any screen exists**, so layout and navigation are decided once instead of corrected later (SPEC R93, R90).

Status: candidate set, not scored. The purpose set and the screen list are chosen by scoring against reference apps once the owner has agreed the axes (SPEC R91). Reference apps serve as baselines to improve upon, or as controls that must lose.

How to read it:
- **Entry**: a planned screen, panel or control.
- **Where**: the screen it lives on.
- **Scenarios**: the acceptance scenarios it serves (SPEC R05–R11).
- **Overlaps**: entries that may be two ways to the same job. Each overlap is a candidate for merging when the map is scored.

## Purposes (candidates)

| | Purpose | Its job | Scenarios |
|---|---|---|---|
| P1 | **Find** | get to a thing, or everything for a task, by name, tag, meaning or place | R05, R09, R10 |
| P2 | **Record** | get things into the app with the least effort: a word, a phrase, a photo; correct what the models got wrong | R06, R07, R08 |
| P3 | **Organise** | shape tags and parent tags, keep things together, move them, swap seasons | R05, R07, R11 |
| P4 | **Know the storage** | the places themselves: houses, floors, rooms, furniture, containers, and how they look | R09, R10 |
| P5 | **Keep in view** | the clear-box effect: stored things come back into sight without being asked | — |
| P6 | **Look after it** | backup, sync, languages, models, customisation, lock | — |

## P1 Find

| Entry | Where | Scenarios | Overlaps |
|---|---|---|---|
| **Search bar**: by name, tag (sub-tags included) and meaning | homepage, top | R05, R10 | Tag grid, Place filter |
| **Place filter**: limit search to a house, floor or place | search results | R10 | Place map |
| **Fallback results**: the same search in other places when the chosen one has none | search results | R10 | — |
| **Place contents**: what is in a place, with its photo | place screen | R09 | Photo browsing |
| **Tag results**: items carrying a tag or its sub-tags, with multi-select | tag screen | R05 | Search bar |

## P2 Record

| Entry | Where | Scenarios | Overlaps |
|---|---|---|---|
| **Quick add**: a name and nothing else; lands in Unplaced | global action | — | Voice add |
| **Voice add**: a dictated list split into items | global action (stage 8) | — | Quick add |
| **Photo add**: photograph, get proposed items, count and tags | global action | R06, R07, R08 | Settled area |
| **Settled area**: photograph a finished place, mark the items in it | place screen | — | Photo add |
| **Proposal review**: accept, correct, discard or split proposals; clear all suggested tags at once | after Photo add | R06, R07, R08 | Bulk tag edit |
| **Unplaced inbox**: items with no place yet | homepage or Places | — | — |
| **Item editor**: name, tags, quantity, fields, notes, place, state | item screen | R06 | — |

## P3 Organise

| Entry | Where | Scenarios | Overlaps |
|---|---|---|---|
| **Tag grid**: top-level tags, opening into sub-tags | homepage card | R05, R11 | Search bar |
| **Tag editor**: name, parent, kind, kept together, home place | tag screen | R05 | — |
| **Bulk tag edit**: add or remove tags on a selection | any multi-select list | R05, R07 | Proposal review |
| **Tag suggestions**: proposed tags and parent tags for new items | item editor, proposal review | R06 | Proposal review |
| **Away view**: things of a kept-together tag not in its home | tag screen | R11 | Place contents |
| **Bulk move**: move a selection or a whole tag to another place; marks photos outdated | any multi-select list | R11 | — |

## P4 Know the storage

| Entry | Where | Scenarios | Overlaps |
|---|---|---|---|
| **Places entry**: house map, or floor cards, or house grid, depending on how many exist | homepage card | R10 | Place map |
| **Place map**: stylised blocks drawn from the place tree, rearrangeable | Places | R10 | Place filter, Places entry |
| **Place editor**: name, size, parent, tags, photo, position | place screen | R10 | — |

## P5 Keep in view

| Entry | Where | Scenarios | Overlaps |
|---|---|---|---|
| **Photo browsing**: places shown by their contents photos | Places / place screen | — | Place contents |
| **Object of the day**: least-searched things first | homepage | — | Rediscover |
| **Home-screen widget**: rotating contents of a place or tag | Android launcher | — | — |
| **Photo labels**: A4 sticker PDF with collage and QR | place screen, export | — | — |
| **Rediscover prompt**: opt-in notification | settings (stage 11) | — | Object of the day |

## P6 Look after it

| Entry | Where | Scenarios | Overlaps |
|---|---|---|---|
| **Backup and export** | settings | — | Sync |
| **Sync folder** | settings | — | Backup |
| **Language** (interface, dictation) | settings | — | — |
| **Models**: download, remove, which device | settings | — | — |
| **Customisation**: fields, tag kinds, homepage cards, widget | settings | — | Tag editor, Place editor |
| **App lock** (stage 11) | settings | — | — |

## Modules (SPEC R97, stage 10; mapped when designed)

| Entry | Module | Where | Overlaps |
|---|---|---|---|
| **Outfits**: links between clothing items | Wardrobe | module view | Tag results |
| **Seasonal swap** | Wardrobe | module view | Bulk move (R11) |
| **Not worn in a year** | Wardrobe | module view | Object of the day |
| **Expiring soon** | Inventory | module view | Rediscover prompt |
| **Shopping list**: things to buy; with Pantry on, items below minimum stock too | Shopping | module view | — |
| **Already have it?**: quick check while shopping | Shopping | module view | Search bar (R09) |
| **Wish list**: things not owned, kept for later (R117) | Shopping | module view | Shopping list |
| **Price comparison**: packs under a tag, cheapest per unit first (R114) | Shopping | module view | Tag results |

## Overlap candidates to settle when scoring

1. **Search bar / Tag grid / Tag results**: three ways into tagged items.
2. **Place map / Places entry / Place filter**: the map may *be* the place filter.
3. **Place contents / Photo browsing**: one place screen showing its photo first may serve both.
4. **Photo add / Settled area**: the same camera flow with a different starting point.
5. **Proposal review / Bulk tag edit / Tag suggestions**: one selection toolbar may serve all three.
6. **Object of the day / Rediscover prompt**: same job, passive vs opt-in push.
7. **Backup / Sync**: both write to a user-chosen folder; one settings entry may hold both.

## Reference apps

Baselines and controls (SPEC R90): inventoried in [reference-apps.md](reference-apps.md) (2026-10-28), plus Spot It, Nestmer and Store & Forget from their listings.
