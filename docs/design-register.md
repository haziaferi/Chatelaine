# Design register

Open questions found while building, each waiting for the stage or design pass that decides it (the owner decides;
reviews only log them). Remove an entry once it is decided, and record the decision where it belongs: in SPEC.md for a
rule, in the code for anything else.

## Stage 2 (first screens)

- **Recovery notice.** A database made anew after a corrupt one waits for its own log before backing up again. The
  notice should key on the set-aside file being there, not on a flag. On a device with no sync folder it should offer
  restoring a backup, as that is what lets backups go on. It also needs a way out when the old log never arrives (the
  device never synced, the sync folder changed, or the old device folder was deleted): for example "Go on without the
  old log", which ends the wait so backups go on.
- **Daily backup.** `BackupWorker` is not scheduled yet. It must build its folder from a stored URI.
- **Roll back.** A roll back leaves one history entry per value, with no single undo.

## Stage 3 (places, photos and fields)

- **Blocker: photos in memory.** Backup and restore hold every photo in memory (entries map, zip, SAF read-back).
  Stream them to a temp file before the camera arrives.
- **Fields.** `Fields.set` does not check that the field applies to its owner, or that link ids exist. Places have no
  `fieldsFor(place)`.

## Stage 4 (photos and the logging form)

- **Which edits mark a photo outdated (R31).** Rename, notes, quantity, away, split and field values raise the flag
  today; tags do not. Under one reading, any change to the item record counts, so tags should too. Under the other,
  only changes a photo would show count (quantity, split, a place's contents), so rename, notes and fields should not.
- **Regions on item photos.** Whether an item's photo may carry regions, as a place's can.

## Sync (design pass)

- **Cycle reset.** A reset that breaks a parent cycle is stamped "now", so it can beat a later deliberate move that has
  not arrived yet. The move can be got back from history.
- **Delete during a move.** A delete and a move made at the same time leave the item unplaced, or the sub-tag at the
  top level, on both devices, instead of under the deleted place's parent.
- **Cycle repairs.** A repair's losing "parent: none" adds a history entry.
- **Local restores.** A local restore that closes a cycle stays until the next import.
- **Photo temp files.** A `blobs/.tmp-<hash>` with no whole file beside it is never cleared: it may be another device's
  write in progress. It can be left by a crash during the first upload of a photo that was deleted before the retry.
- **History and backups.** History is not in backups.
- **Retired device logs.** A recovered device takes a new id (R59); its old id's log stays in the folder, read as a
  peer's that no longer writes, and nothing clears it yet.

## Before release

- **Second sync tool (R57).** Test a second folder-sync tool.
- **Oldest Android (R102).** Check on an API 33 device.
