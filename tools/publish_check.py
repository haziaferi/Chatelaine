#!/usr/bin/env python3
"""publish_check.py -- what a repo would make public, checked for private and sensitive data.

    python ~/.claude/tools/publish_check.py <repo>            # scan; exit 1 on any finding
    python ~/.claude/tools/publish_check.py <repo> --history  # also every commit: blobs, messages, tags
    python ~/.claude/tools/publish_check.py --install-hook <repo>   # run it as the repo's pre-commit and pre-push hooks

Scans the files git would publish (tracked + untracked-not-ignored, as they are on disk), the staged
copy of every tracked file whose working-tree copy differs from it (the next commit takes that one),
the repo's author identity and, with --history, every blob in every commit, every author/committer
line, every commit message (labelled "commit <hash>") and every annotated tag object: its message and
tagger (labelled "tag <name>"). Messages are checked by the text rules (secret, email, path, private,
link), the tagger like an author. Exit 0 clean, 1 findings, 2 when the scan could not be completed (a
git command failed, an object could not be read): a scan that saw nothing must not pass. Rules:

  secret      private keys, GitHub/Anthropic/OpenAI/AWS/Google/Slack tokens, literal signing passwords
  email       any address except GitHub noreply (<user>@users.noreply.github.com, noreply@github.com),
              noreply@anthropic.com (the co-author line), the reserved example domains (example.com,
              .org, .net and their subdomains; the .example, .test, .invalid, .localhost TLDs) and
              the allow-list
  path        a home-folder path (C:\\Users\\<name>, /c/Users/<name>, /home/<name>, /Users/<name>)
  private     every word in the private deny-list (names, emails, phone serials ...), case-insensitive
  link        claude.ai links (artifacts and chats are private pages)
  exif        JPEG EXIF GPS position, camera or lens serial, camera owner (read from the IFDs)
  identity    the repo's user.email / commit authors, committers and taggers not a GitHub noreply address
  large       a text file over 8 MiB: only the deny-list was checked; read it, then allow it by
              rule and path

The deny-list holds the private values themselves, so it lives OUTSIDE every repo:
~/.claude/tools/publish_denylist.txt (one term per line, # comments). Per-repo exceptions go in
<repo>/.publish-allow (one regex per line, matched against "rule|path|text"); that file is public, so it
must never contain a private value - allow by rule and path, not by quoting the value. A message is
allowed by rule and label: ^path\\|commit 1a2b3c4d5e\\| or ^email\\|tag v1\\.0\\|. A pushed message
cannot be rewritten without rewriting the history, so such a line is how an old one is accepted. The
allow file is itself checked by every rule; its earlier versions in the history by the secret and
private rules only.

Stdlib only. Binary files are skipped except JPEG (EXIF) and checked as bytes for the deny-list;
UTF-16 text is decoded and scanned as text. A symlink is scanned as its link text, which is what git
stores. Commits and tags are read as raw objects, a merge commit's copied tag (mergetag) included,
and git replace stand-ins are ignored (a push sends the originals). A tag's signature block is not
scanned (it is not text anyone wrote); text after it is.
"""
import os
import re
import subprocess
import sys

HOME = os.path.expanduser("~")
DENYLIST = os.path.join(HOME, ".claude", "tools", "publish_denylist.txt")
ALLOW = ".publish-allow"

# (label, pattern). A finding reports the label only, never the matched text.
TOKEN_PATTERNS = [
    ("private key", re.compile(r"-----BEGIN (?:RSA |EC |OPENSSH |DSA |PGP )?PRIVATE KEY")),
    ("github token", re.compile(r"\b(?:ghp|gho|ghu|ghs|ghr)_[A-Za-z0-9]{30,}|\bgithub_pat_[A-Za-z0-9_]{40,}")),
    ("anthropic key", re.compile(r"\bsk-ant-[A-Za-z0-9_-]{20,}")),
    ("openai key", re.compile(r"\bsk-(?:proj-)?[A-Za-z0-9]{32,}")),
    ("aws key", re.compile(r"\b(?:AKIA|ASIA)[0-9A-Z]{16}\b")),
    ("google key", re.compile(r"\bAIza[0-9A-Za-z_-]{35}\b")),
    ("slack token", re.compile(r"\bxox[abprs]-[A-Za-z0-9-]{10,}")),
    ("signing password", re.compile(r"(?i)\b(?:store|key)Password\s*[=:]?\s*['\"][^'\"$]{3,}['\"]")),
]
EMAIL = re.compile(r"[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}")
GITHUB_NOREPLY = r"[^@\s]+@users\.noreply\.github\.com|noreply@github\.com"
# RFC 2606 / 6761 reserved names: nobody receives mail there
RESERVED = r"[^@\s]+@(?:[a-z0-9-]+\.)*(?:example\.(?:com|org|net)|example|test|invalid|localhost)"
IDENTITY_OK = re.compile(r"(?i)^(?:%s)$" % GITHUB_NOREPLY)
EMAIL_OK = re.compile(r"(?i)^(?:%s|noreply@anthropic\.com|%s)$" % (GITHUB_NOREPLY, RESERVED))
PATH = re.compile(r"(?i)(?:\b[A-Z]:[\\/]+Users[\\/]+|/[a-z]/Users/|/home/|/Users/)(?!Public\b|Default\b|<|\{|\$|\*)([A-Za-z0-9._ -]{2,40})")
LINK = re.compile(r"https?://(?:www\.)?claude\.ai/[^\s)\"'>]+")
TEXT_LIMIT = 8 * 1024 * 1024


class GitError(Exception):
    """A git command failed, or answered in a shape this script does not understand."""


# git replace makes git show a replacement object, but a push sends the original: read the originals.
GIT = ("git", "--no-replace-objects")


def git(repo, *args, ok=(0,), input=None, raw=False):
    """stdout of `git -C repo args` (bytes if raw); GitError unless the exit status is in ok."""
    text = {} if raw else dict(text=True, encoding="utf-8", errors="replace")
    try:
        p = subprocess.run([*GIT, "-C", repo, *args], capture_output=True, input=input, **text)
    except OSError as e:
        raise GitError("cannot run git: %s" % e)
    if p.returncode not in ok:
        err = p.stderr.decode("utf-8", "replace") if raw else p.stderr
        raise GitError("git %s failed (exit %d): %s" % (" ".join(args), p.returncode,
                                                        err.strip()[:500] or "no message"))
    return p.stdout


def git_z(repo, *args):
    """The NUL-separated entries of a -z listing, decoded the way the file system names them
    (os.fsdecode), so a name that is not UTF-8 still opens and still matches across listings."""
    return [os.fsdecode(e) for e in git(repo, *args, raw=True).split(b"\0") if e]


def shown(s):
    """s printable on any console: names that are not UTF-8 come out with replacement characters."""
    return s.encode("utf-8", "surrogateescape").decode("utf-8", "replace")


def read_objects(repo, oids, kind=b"blob"):
    """{oid: bytes} for the objects of type kind among oids, from one `git cat-file --batch` stream (not
    one process per object). GitError if git fails, an object cannot be read, or the stream is malformed."""
    if not oids:
        return {}
    try:
        p = subprocess.run([*GIT, "-C", repo, "cat-file", "--batch"], capture_output=True,
                           input="\n".join(oids).encode() + b"\n")
    except OSError as e:
        raise GitError("cannot run git: %s" % e)
    if p.returncode != 0:
        raise GitError("git cat-file --batch failed (exit %d): %s" % (
            p.returncode, (p.stderr or b"").decode("utf-8", "replace").strip()[:500] or "no message"))
    out, blobs, unreadable, i = p.stdout, {}, [], 0
    while i < len(out):
        nl = out.find(b"\n", i)
        if nl < 0:
            raise GitError("git cat-file: output ends inside a header")
        head = out[i:nl].split()
        if len(head) == 2 and head[1] in (b"missing", b"ambiguous"):
            unreadable.append(head[0].decode("utf-8", "replace"))
            i = nl + 1
            continue
        if len(head) != 3 or not head[2].isdigit():
            raise GitError("git cat-file: unexpected header %r" % out[i:nl][:80])
        size = int(head[2])
        if nl + 1 + size > len(out):
            raise GitError("git cat-file: output ends inside object %s" % head[0].decode()[:10])
        if head[1] == kind:
            blobs[head[0].decode()] = out[nl + 1:nl + 1 + size]
        i = nl + 1 + size + 1
    if unreadable:
        raise GitError("git cat-file: %d object(s) could not be read: %s" % (
            len(unreadable), ", ".join(u[:10] for u in unreadable[:5])))
    return blobs


def load_lines(path):
    if not os.path.exists(path):
        return []
    out = []
    for line in open(path, encoding="utf-8"):
        line = line.strip()
        if line and not line.startswith("#"):
            out.append(line)
    return out


def is_binary(data):
    return b"\0" in data[:8192]


# Tags that name the camera or its owner. Some writers put the Exif-IFD ones in IFD0, so both IFDs are
# read for all of them. A GPS IFD counts only when it holds a position.
OWNER_TAGS = {0xA430: "camera owner", 0xA431: "camera serial", 0xA435: "lens serial", 0xC62F: "camera serial"}
GPS_POSITION = (0x0002, 0x0004)   # GPSLatitude, GPSLongitude
EXIF_IFD, GPS_IFD = 0x8769, 0x8825
TYPE_SIZE = {1: 1, 2: 1, 3: 2, 4: 4, 5: 8, 6: 1, 7: 1, 8: 2, 9: 4, 10: 8, 11: 4, 12: 8, 13: 4}


def _ifd(t, off, end):
    """{tag: (type, count, value bytes)} for the IFD at offset off of TIFF block t; {} if out of range."""
    if off < 8 or off + 2 > len(t):
        return {}
    entries = {}
    for k in range(int.from_bytes(t[off:off + 2], end)):
        e = off + 2 + 12 * k
        if e + 12 > len(t):
            break
        tag, typ, count = (int.from_bytes(t[e:e + 2], end), int.from_bytes(t[e + 2:e + 4], end),
                           int.from_bytes(t[e + 4:e + 8], end))
        size = TYPE_SIZE.get(typ, 1) * count
        if size <= 4:
            value = t[e + 8:e + 8 + size]
        else:
            at = int.from_bytes(t[e + 8:e + 12], end)
            value = t[at:at + size] if at + size <= len(t) else b""
        entries[tag] = (typ, count, value)
    return entries


def tiff_findings(t):
    """GPS position and owner/serial tags in a TIFF block, found by walking IFD0, the Exif IFD and the
    GPS IFD (not by looking for a tag's two bytes anywhere, which matched inside values)."""
    if len(t) < 8 or t[:2] not in (b"II", b"MM"):
        return []
    end = "little" if t[:2] == b"II" else "big"
    if int.from_bytes(t[2:4], end) != 42:
        return []
    ifd0 = _ifd(t, int.from_bytes(t[4:8], end), end)

    def sub(tag):
        e = ifd0.get(tag)
        return _ifd(t, int.from_bytes(e[2][:4], end), end) if e and e[0] in (4, 13) and len(e[2]) >= 4 else {}

    exif, gps = sub(EXIF_IFD), sub(GPS_IFD)
    found = []
    if any(tag in gps for tag in GPS_POSITION):
        found.append("GPS")
    for ifd in (ifd0, exif):
        for tag, name in OWNER_TAGS.items():
            if tag in ifd:
                typ, _, value = ifd[tag]
                if typ != 2 or value.strip(b"\0 "):     # an empty string names nobody
                    found.append(name)
    return found


def exif_findings(data):
    """EXIF findings of the JPEG APP1 segments before the image data."""
    found = []
    if data[:2] != b"\xff\xd8":
        return found
    i = 2
    while i + 4 <= len(data) and data[i] == 0xFF:
        marker, size = data[i + 1], int.from_bytes(data[i + 2:i + 4], "big")
        if marker == 0xDA or size < 2:
            break
        seg = data[i + 4:i + 2 + size]
        if marker == 0xE1 and seg.startswith(b"Exif\0\0"):
            found += tiff_findings(seg[6:])
        i += 2 + size
    return sorted(set(found))


def scan_text(rel, text, deny, hits):
    for n, line in enumerate(text.splitlines(), 1):
        for label, rx in TOKEN_PATTERNS:
            if rx.search(line):
                hits.append(("secret", rel, n, label))
        for m in EMAIL.finditer(line):
            if not EMAIL_OK.search(m.group(0)):
                hits.append(("email", rel, n, m.group(0)))
        for m in PATH.finditer(line):
            hits.append(("path", rel, n, m.group(0)))
        for m in LINK.finditer(line):
            hits.append(("link", rel, n, m.group(0)))
        low = line.lower()
        for term in deny:
            if term.lower() in low:
                hits.append(("private", rel, n, "deny-list term #%d" % (deny.index(term) + 1)))


def scan_deny_bytes(rel, data, deny, hits, why):
    low = data.lower()
    for term in deny:
        if term.lower().encode("utf-8") in low:
            hits.append(("private", rel, 0, "deny-list term #%d (%s)" % (deny.index(term) + 1, why)))


def utf16_text(data):
    """data decoded as UTF-16 when it has a UTF-16 byte-order mark, or when its start is ASCII text
    stored in two bytes per character (PowerShell 5 Out-File, .reg exports); else None."""
    if data[:2] in (b"\xff\xfe", b"\xfe\xff"):
        return data.decode("utf-16", errors="replace")
    head = data[:1024]
    head = head[:len(head) // 2 * 2]
    if len(head) >= 4:
        even, odd = head[0::2], head[1::2]
        if not odd.strip(b"\0") and 0 not in even:
            return data.decode("utf-16-le", errors="replace")
        if not even.strip(b"\0") and 0 not in odd:
            return data.decode("utf-16-be", errors="replace")
    return None


def scan_file(rel, data, deny, hits):
    if rel.lower().endswith((".jpg", ".jpeg")):
        for f in exif_findings(data):
            hits.append(("exif", rel, 0, f))
    text = utf16_text(data)
    if text is None and is_binary(data):
        scan_deny_bytes(rel, data, deny, hits, "binary")
        return
    if text is not None:
        # a binary file can start like UTF-16 text; its raw bytes keep the deny-list check binaries get
        # (genuine UTF-16 text never matches here, its letters are interleaved with NULs)
        scan_deny_bytes(rel, data, deny, hits, "raw bytes")
    if len(data) > TEXT_LIMIT:
        if text is None:
            scan_deny_bytes(rel, data, deny, hits, "large file")
        else:
            low = text.lower()
            for term in deny:
                if term.lower() in low:
                    hits.append(("private", rel, 0, "deny-list term #%d (large UTF-16 file)"
                                 % (deny.index(term) + 1)))
        hits.append(("large", rel, 0, "text file over %d MiB: only the deny-list was checked"
                     % (TEXT_LIMIT // (1024 * 1024))))
        return
    scan_text(rel, text if text is not None else data.decode("utf-8", errors="replace"), deny, hits)


def blobs_to_scan(repo, history, read):
    """{oid: label} of the blobs that reading the working tree did not cover.

    A staged blob counts as covered only when its path was read from disk (read: the paths that were)
    as a regular file whose content git would hash to that same blob. Every other staged blob is
    scanned under its plain path: a file changed or deleted in the working tree but not staged (the
    next commit takes the staged copy, and it is often the committed one), a path git hides from
    diff-files (assume-unchanged, skip-worktree), a symlink (git stores the link text), a conflict
    stage. Paths listed as changed only because their stat data moved are dropped by hashing the
    working-tree copy the way git would (filters included). With history, every other blob of every
    commit follows."""
    changed = set(git_z(repo, "diff-files", "--name-only", "-z"))
    for entry in git_z(repo, "ls-files", "-v", "-z"):
        if len(entry) > 2 and (entry[0].islower() or entry[0] == "S"):   # assume-unchanged, skip-worktree
            changed.add(entry[2:])
    present = sorted(f for f in changed if f in read and not os.path.islink(os.path.join(repo, f)))
    disk = dict(zip(present, git(repo, "hash-object", "--stdin-paths", raw=True,
                                 input=b"".join(os.fsencode(f) + b"\n" for f in present)).decode().split())
                ) if present else {}
    covered, wanted = set(), {}
    for entry in git_z(repo, "ls-files", "-s", "-z"):
        meta, path = entry.split("\t", 1)
        mode, oid, stage = meta.split()[:3]
        if mode == "160000":            # a submodule's commit, not a blob of this repo
            continue
        if (stage == "0" and mode != "120000" and path in read
                and (path not in changed or disk.get(path) == oid)):
            covered.add(oid)
        else:
            wanted.setdefault(oid, path)
    if history and has_history(repo):
        for line in git(repo, "rev-list", "--all", "--objects").splitlines():
            parts = line.split(" ", 1)
            if len(parts) == 2 and parts[0] not in covered and parts[0] not in wanted:
                wanted[parts[0]] = "history:" + parts[1]
    return wanted


COMMIT_HASH = re.compile(r"^[0-9a-f]{40}(?:[0-9a-f]{24})?$")       # SHA-1 or SHA-256
EMAIL_FIELD = re.compile(rb"<([^>\n]*)>")
SIG_BEGIN = re.compile(r"^-----BEGIN (PGP SIGNATURE|SSH SIGNATURE|SIGNED MESSAGE)-----$", re.M)


def has_history(repo):
    """True when there are commits to scan: a valid HEAD, or any ref (HEAD may be an unborn branch
    while other branches, which a push can still send, hold commits)."""
    return bool(git(repo, "rev-parse", "--verify", "-q", "HEAD", ok=(0, 1)).strip()
                or git(repo, "for-each-ref", "--count=1").strip())


def without_signature(msg):
    """msg without its signature block: the last BEGIN line, as git finds it, up to its END line. Text
    after the END line is kept, since it is published like the rest."""
    starts = list(SIG_BEGIN.finditer(msg))
    if not starts:
        return msg
    begin = starts[-1]
    end = re.compile(r"^-----END %s-----$" % begin.group(1), re.M).search(msg, begin.end())
    return msg[:begin.start()] + (msg[end.end():] if end else "")


def split_object(data):
    """(header lines as [(key, value bytes)], message text) of a raw commit or tag object. Continuation
    lines (a leading space) are joined to their header with newlines. The message is decoded with the
    object's own encoding header when git would honour one, else as UTF-8."""
    head, _, msg = data.partition(b"\n\n")
    fields = []
    for line in head.split(b"\n"):
        if line.startswith(b" ") and fields:
            fields[-1] = (fields[-1][0], fields[-1][1] + b"\n" + line[1:])
        else:
            key, _, value = line.partition(b" ")
            fields.append((key, value))
    enc = dict(fields).get(b"encoding", b"utf-8").decode("ascii", "replace")
    try:
        text = msg.decode(enc, "replace")
    except LookupError:
        text = msg.decode("utf-8", "replace")
    return fields, text


def scan_tag_object(label, data, deny, hits):
    """The tagger and message of a raw tag object (also used for a merge commit's mergetag header).
    Returns its header fields."""
    fields, msg = split_object(data)
    for key, value in fields:
        if key == b"tagger":
            m = EMAIL_FIELD.search(value)
            if not IDENTITY_OK.search(m.group(1).decode("utf-8", "replace") if m else ""):
                hits.append(("identity", label, 0, "tagger email is not GitHub noreply"))
    scan_text(label, without_signature(msg), deny, hits)
    return dict(fields)


def scan_messages(repo, deny, hits):
    """--history: the author and committer emails and the message of every commit, and the tagger and
    message of every annotated tag object (those a ref points at, and the tags they point at in turn).
    Commits are read as raw objects, not through git log: a NUL byte in a message, log.showSignature
    or i18n.logOutputEncoding cannot change or cut what is scanned. Returns (commits, tag objects)."""
    oids = git(repo, "rev-list", "--all").split()
    bad = [o for o in oids if not COMMIT_HASH.match(o)]
    if bad:
        raise GitError("git rev-list: unexpected line %r" % bad[0][:80])
    objs = read_objects(repo, oids, b"commit")
    for oid in oids:
        if oid not in objs:
            raise GitError("git cat-file: %s is not a commit" % oid[:10])
        label = "commit " + oid[:10]
        fields, msg = split_object(objs[oid])
        for key, value in fields:
            if key in (b"author", b"committer"):
                m = EMAIL_FIELD.search(value)
                if not IDENTITY_OK.search(m.group(1).decode("utf-8", "replace") if m else ""):
                    hits.append(("identity", label, 0, "author/committer email is not GitHub noreply"))
            elif key == b"mergetag":        # a merged signed tag, copied into the commit
                scan_tag_object(label, value, deny, hits)
        scan_text(label, msg, deny, hits)

    todo = {}
    for line in git(repo, "for-each-ref", "--format=%(objecttype) %(objectname) %(refname)").splitlines():
        parts = line.split(" ", 2)
        if len(parts) != 3:
            raise GitError("git for-each-ref: unexpected line %r" % line[:80])
        if parts[0] == "tag":
            todo.setdefault(parts[1], "tag " + parts[2].removeprefix("refs/tags/"))
    done = set()
    while todo:
        objs = read_objects(repo, list(todo), b"tag")
        nested = {}
        for oid, label in todo.items():
            if oid not in objs:
                raise GitError("git cat-file: %s is not a tag object" % oid[:10])
            done.add(oid)
            if not label:                   # a nested tag no ref points at: its own name
                name = dict(split_object(objs[oid])[0]).get(b"tag", oid[:10].encode())
                label = "tag " + name.decode("utf-8", "replace")
            fields = scan_tag_object(label, objs[oid], deny, hits)
            inner = fields.get(b"object", b"").decode("ascii", "replace")
            # a tag a ref also points at keeps the ref's label, so it is scanned (and allowed) once
            if fields.get(b"type") == b"tag" and inner and inner not in done and inner not in todo:
                nested.setdefault(inner, None)
        todo = nested
    return len(oids), len(done)


def scan(repo, history):
    deny = load_lines(DENYLIST)
    allow = [re.compile(p) for p in load_lines(os.path.join(repo, ALLOW))]
    hits = []

    files = git_z(repo, "ls-files", "-co", "--exclude-standard", "-z")
    read = set()
    for rel in files:
        p = os.path.join(repo, rel)
        if os.path.islink(p):           # git stores and publishes the link text, not the target
            data = os.fsencode(os.readlink(p))
        elif os.path.isfile(p):
            with open(p, "rb") as fh:
                data = fh.read()
        else:
            continue
        scan_file(rel, data, deny, hits)
        read.add(rel)
    scanned = len(read)

    email = git(repo, "config", "user.email", ok=(0, 1)).strip()      # exit 1: not set
    if not IDENTITY_OK.search(email):
        hits.append(("identity", ".git/config", 0, "user.email is not a GitHub noreply address"))
    messages = (0, 0)
    if history and has_history(repo):
        messages = scan_messages(repo, deny, hits)

    wanted = blobs_to_scan(repo, history, read)
    blobs = read_objects(repo, list(wanted))
    for oid, data in blobs.items():
        scan_file(wanted[oid], data, deny, hits)

    # The current .publish-allow is checked by every rule: patterns written by rule and path (as the
    # docstring asks) trip none, and a quoted value is exactly the leak to catch. Earlier versions in
    # the history may hold patterns that match their own rules and cannot be rewritten now; only the
    # secret and private rules apply to them.
    hits = [h for h in hits if h[1] != "history:" + ALLOW or h[0] in ("secret", "private")]
    kept = []
    for h in hits:
        if h not in kept and not any(a.search("%s|%s|%s" % (h[0], h[1], h[3])) for a in allow):
            kept.append(h)
    for rule, rel, n, what in kept:
        print(shown("%-8s %s%s  %s" % (rule, rel, ":%d" % n if n else "", what)))
    print("publish_check: %d files%s%s, %d finding(s)%s" % (
        scanned, " + %d staged or earlier blobs" % len(blobs) if blobs else "",
        " + %d commit and %d tag messages" % messages if history else "", len(kept),
        "" if not deny else ", deny-list %d terms" % len(deny)))
    if not deny:
        print("note: no deny-list at %s - names, serials and private emails are only caught by pattern" % DENYLIST)
    return 1 if kept else 0


HOOK_MARK = "# written by publish_check.py --install-hook"
HOOKS = (("pre-commit", "", "commit"), ("pre-push", " --history", "push"))


def sh_quote(s):
    return "'" + s.replace("'", "'\\''") + "'"


# The exact texts this script writes, now and in its earlier version (which some repositories also
# copied by hand). Any other hook, even one that calls publish_check.py, is the owner's.
OLD_HOOK = re.compile(r'#!/bin/sh\npython "[^"\n]*publish_check\.py" "[^"\n]*"(?: --history)? \|\| \{\n'
                      r'  echo "publish_check: (?:commit|push) blocked"; exit 1; \}\n?\Z')
OUR_HOOK = re.compile(r"#!/bin/sh\n%s\ntop=\$\(git rev-parse --show-toplevel\) \|\| exit 1\n"
                      r"python '(?:[^'\n]|'\\'')*publish_check\.py' \"\$top\"(?: --history)? \|\| \{\n"
                      r'  echo "publish_check: (?:commit|push) blocked"; exit 1; \}\n\Z' % re.escape(HOOK_MARK))


def written_by_us(text):
    text = text.replace("\r\n", "\n")
    return bool(OLD_HOOK.match(text) or OUR_HOOK.match(text))


def install_hooks(repo):
    """pre-commit (the files as they are about to be committed) and pre-push (with --history), in the
    hooks folder git actually reads (core.hooksPath honoured). The hooks scan the checkout they run in
    (git rev-parse --show-toplevel), so a linked worktree is scanned as itself, with the index git hands
    the hook. If either hook exists and this script did not write it, nothing is installed and the
    lines to add to it are printed instead."""
    try:
        hooks = git(repo, "rev-parse", "--git-path", "hooks").strip()
    except GitError as e:
        print("publish_check: %s" % e)
        return 2
    hooks = os.path.normpath(os.path.join(repo, hooks))
    script = sh_quote(os.path.abspath(__file__).replace("\\", "/"))
    plan, foreign = [], []
    for name, flag, verb in HOOKS:
        path = os.path.join(hooks, name)
        line = ('top=$(git rev-parse --show-toplevel) || exit 1\npython %s "$top"%s || {\n'
                '  echo "publish_check: %s blocked"; exit 1; }\n' % (script, flag, verb))
        if os.path.exists(path):
            with open(path, encoding="utf-8", errors="replace") as f:
                if not written_by_us(f.read()):
                    foreign.append((path, line))
                    continue
        plan.append((path, "#!/bin/sh\n%s\n%s" % (HOOK_MARK, line)))
    if foreign:
        for path, line in foreign:
            print("publish_check: %s was not written by --install-hook; left alone. Add to it:\n%s" % (path, line))
        print("publish_check: no hook installed")
        return 2
    os.makedirs(hooks, exist_ok=True)
    for path, text in plan:
        with open(path, "w", newline="\n") as f:
            f.write(text)
        os.chmod(path, os.stat(path).st_mode | 0o111)    # git skips a hook that is not executable
        print("hook installed:", path)
    return 0


def main(argv):
    if len(argv) >= 3 and argv[1] == "--install-hook":
        return install_hooks(os.path.abspath(argv[2]))
    if len(argv) < 2:
        print(__doc__)
        return 2
    try:
        return scan(os.path.abspath(argv[1]), "--history" in argv[2:])
    except GitError as e:
        print("publish_check: %s" % e)
        print("publish_check: scan not completed, so it fails")
        return 2


if __name__ == "__main__":
    sys.exit(main(sys.argv))
