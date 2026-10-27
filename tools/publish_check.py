#!/usr/bin/env python3
"""publish_check.py -- what a repo would make public, checked for private and sensitive data.

    python ~/.claude/tools/publish_check.py <repo>            # scan; exit 1 on any finding
    python ~/.claude/tools/publish_check.py <repo> --history  # also every blob in every commit
    python ~/.claude/tools/publish_check.py --install-hook <repo>   # run it as the repo's pre-push hook

Scans the files git would publish (tracked + untracked-not-ignored), the repo's author identity and,
with --history, every commit's blobs and author/committer lines. Rules:

  secret      private keys, GitHub/Anthropic/OpenAI/AWS/Google/Slack tokens, literal signing passwords
  email       any address except GitHub noreply, example.*, and the allow-list
  path        a home-folder path (C:\\Users\\<name>, /c/Users/<name>, /home/<name>, /Users/<name>)
  private     every word in the private deny-list (names, emails, phone serials ...), case-insensitive
  link        claude.ai links (artifacts and chats are private pages)
  exif        JPEG EXIF GPS, camera serial or owner fields
  identity    the repo's user.email / commit authors not a noreply address

The deny-list holds the private values themselves, so it lives OUTSIDE every repo:
~/.claude/tools/publish_denylist.txt (one term per line, # comments). Per-repo exceptions go in
<repo>/.publish-allow (one regex per line, matched against "rule|path|text"); that file is public, so it
must never contain a private value - allow by rule and path, not by quoting the value.

Stdlib only. Binary files are skipped except JPEG (EXIF) and checked as bytes for the deny-list.
"""
import os
import re
import subprocess
import sys

HOME = os.path.expanduser("~")
DENYLIST = os.path.join(HOME, ".claude", "tools", "publish_denylist.txt")

SECRET = [
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
EMAIL_OK = re.compile(r"(?i)@users\.noreply\.github\.com$|^noreply@|@example\.(?:com|org|net)$|@anthropic\.com$")
PATH = re.compile(r"(?i)(?:\b[A-Z]:[\\/]+Users[\\/]+|/[a-z]/Users/|/home/|/Users/)(?!Public\b|Default\b|<|\{|\$|\*)([A-Za-z0-9._ -]{2,40})")
LINK = re.compile(r"https?://(?:www\.)?claude\.ai/[^\s)\"'>]+")
TEXT_LIMIT = 8 * 1024 * 1024


def git(repo, *args):
    return subprocess.run(["git", "-C", repo, *args], capture_output=True, text=True, encoding="utf-8",
                          errors="replace").stdout


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


def exif_findings(data):
    """JPEG APP1 EXIF: GPS IFD present, or serial/owner tags (0xA431 BodySerialNumber, 0xA430 owner, 0xC62F)."""
    found = []
    i = 2
    while i + 4 <= len(data) and data[i] == 0xFF:
        marker, size = data[i + 1], int.from_bytes(data[i + 2:i + 4], "big")
        seg = data[i + 4:i + 2 + size]
        if marker == 0xE1 and seg.startswith(b"Exif\0\0"):
            t = seg[6:]
            le = t[:2] == b"II"
            tags = {0x8825: "GPS", 0xA431: "camera serial", 0xA430: "camera owner", 0xC62F: "camera serial"}
            for tag, name in tags.items():
                b = tag.to_bytes(2, "little" if le else "big")
                if b in t:
                    found.append(name)
        if marker == 0xDA:
            break
        i += 2 + size
    return sorted(set(found))


def scan_text(rel, text, deny, hits):
    for n, line in enumerate(text.splitlines(), 1):
        for name, rx in SECRET:
            if rx.search(line):
                hits.append(("secret", rel, n, name))
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


def scan_file(repo, rel, data, deny, hits):
    if rel.lower().endswith((".jpg", ".jpeg")):
        for f in exif_findings(data):
            hits.append(("exif", rel, 0, f))
    if is_binary(data):
        low = data.lower()
        for term in deny:
            if term.lower().encode("utf-8") in low:
                hits.append(("private", rel, 0, "deny-list term #%d (binary)" % (deny.index(term) + 1)))
        return
    if len(data) > TEXT_LIMIT:
        return
    scan_text(rel, data.decode("utf-8", errors="replace"), deny, hits)


def main(argv):
    if len(argv) >= 3 and argv[1] == "--install-hook":
        repo = os.path.abspath(argv[2])
        hook = os.path.join(repo, ".git", "hooks", "pre-push")
        with open(hook, "w", newline="\n") as f:
            f.write('#!/bin/sh\npython "%s" "%s" --history || {\n  echo "publish_check: push blocked"; exit 1; }\n'
                    % (os.path.abspath(__file__).replace("\\", "/"), repo.replace("\\", "/")))
        print("pre-push hook installed:", hook)
        return 0
    if len(argv) < 2:
        print(__doc__)
        return 2
    repo = os.path.abspath(argv[1])
    history = "--history" in argv
    deny = load_lines(DENYLIST)
    allow = [re.compile(p) for p in load_lines(os.path.join(repo, ".publish-allow"))]
    hits = []

    files = [f for f in git(repo, "ls-files", "-co", "--exclude-standard", "-z").split("\0") if f]
    for rel in files:
        if rel == ".publish-allow":   # its patterns would match themselves
            continue
        p = os.path.join(repo, rel)
        if os.path.isfile(p):
            with open(p, "rb") as fh:
                scan_file(repo, rel, fh.read(), deny, hits)

    email = git(repo, "config", "user.email").strip()
    if not EMAIL_OK.search(email):
        hits.append(("identity", ".git/config", 0, "user.email is not a noreply address"))
    if history and git(repo, "rev-parse", "--verify", "-q", "HEAD").strip():
        for line in git(repo, "log", "--all", "--format=%H %ae %ce").splitlines():
            h, ae, ce = line.split(" ", 2)
            for e in (ae, ce):
                if not EMAIL_OK.search(e):
                    hits.append(("identity", "commit " + h[:10], 0, "author/committer email is not noreply"))
        # blobs from earlier commits only (current files were scanned above); one cat-file stream, not one per object
        current = {}
        for line in git(repo, "ls-files", "-s").splitlines():
            meta, path = line.split("\t", 1)
            current[meta.split()[1]] = path
        wanted = {}
        for line in git(repo, "rev-list", "--all", "--objects").splitlines():
            parts = line.split(" ", 1)
            if len(parts) == 2 and parts[0] not in current and parts[0] not in wanted:
                wanted[parts[0]] = parts[1]
        if wanted:
            p = subprocess.Popen(["git", "-C", repo, "cat-file", "--batch"], stdin=subprocess.PIPE, stdout=subprocess.PIPE)
            out, _ = p.communicate("\n".join(wanted).encode() + b"\n")
            i = 0
            while i < len(out):
                nl = out.index(b"\n", i)
                head = out[i:nl].split()
                if len(head) < 3:
                    break
                size = int(head[2])
                data = out[nl + 1:nl + 1 + size]
                if head[1] == b"blob":
                    scan_file(repo, "history:" + wanted[head[0].decode()], data, deny, hits)
                i = nl + 1 + size + 1

    # older .publish-allow versions hold patterns that match their own rules, as the current one does
    # (skipped above); only the private deny-list still applies to them
    hits = [h for h in hits if not (h[1] == "history:.publish-allow" and h[0] != "private")]
    kept = [h for h in hits if not any(a.search("%s|%s|%s" % (h[0], h[1], h[3])) for a in allow)]
    for rule, rel, n, what in kept:
        print("%-8s %s%s  %s" % (rule, rel, ":%d" % n if n else "", what))
    print("publish_check: %d files, %d finding(s)%s" % (len(files), len(kept),
          "" if not deny else ", deny-list %d terms" % len(deny)))
    if not deny:
        print("note: no deny-list at %s - names, serials and private emails are only caught by pattern" % DENYLIST)
    return 1 if kept else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
