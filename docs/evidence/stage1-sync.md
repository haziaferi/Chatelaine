# Stage 1 sync on real devices

Evidence for `SPEC.md`, not a source of rules. The rules that rely on it are R51, R54, R55, R57 and R100 (stage 1's gate). Run on 2026-11-26 between the owner's Windows 11 PC and the owner's phone (API 34), with the stage 1 debug harness on both.

## Verdict

Two-device sync with conflicts works through a real folder-sync tool. Both devices ended with the same 20 items, every conflicting name settled the same way on both, and each overwritten name was kept in history on both devices (R54). The automated round trips of R100 (`StageGateTest`: export → import, two-device sync with conflicts, a corrupted database kept) pass on the same code.

## Set-up

- PC: Syncthing; phone: Syncthing-Fork. One shared folder, empty at the start apart from Syncthing's own `.stfolder` marker.
- PC side: `./gradlew :desktopApp:harness --args="--data <dir> add|edit|list|sync <folder>"`. Phone side: the debug-only "Chatelaine harness" activity, with the shared folder picked through the Storage Access Framework (`SafSyncFolder`). Release builds contain neither.

## Steps and results

| Step | Result |
|---|---|
| PC adds 10 items, syncs | 1 log segment written, 6,502 bytes |
| Phone syncs, adds 10 items, syncs | phone lists all 20 |
| PC syncs | synced in 2 rounds; PC lists all 20 |
| Phone taken offline (sync tool paused); the same 5 items renamed on the PC and on the phone, differently | each device shows its own names |
| Phone back online, both sync | PC synced in 2 rounds; both devices list the same 20 items and the same 5 names |
| History on each device | each of the 5 items holds the losing name in `sync_lost`: on the phone, one name the PC overwrote and four the phone's own edits lost to later PC edits; the PC holds the matching names |

Which side won each name followed the edits' hybrid clock stamps: the renames on the two devices overlapped in time, and the later edit of each item won on both. The two clocks agreed within 3 seconds.

## Change-log size (R55)

After the run the folder held 40,241 bytes: four log segments of 15,915, 5,100, 15,915 and 3,189 bytes, 63 lines in all, each line one whole item row with the stamps of every field group.

| Measure | Value |
|---|---|
| One item line (6 field groups with their stamps) | about 640 bytes |
| Lines per device for 10 adds, 10 items received, 5 renames and their merges | 30 to 33 |
| Segment roll | the phone's log rolled to a second segment past 16 KiB, as R58's folder layer sets |

So one group per field costs about 640 bytes per changed item per change at this item size, whatever the number of fields changed, because a change writes the whole row. A device also writes back the rows it receives the first time, which roughly doubles a fresh device's log; the 64-segment snapshot (R58) bounds the growth.

## Not checked

- A second folder-sync tool (R57 asks for one before release).
- An API 33 device (R102).
- Photos in the real-device run: the harness adds items only; photo blobs are covered by the automated `BlobSyncTest`.
