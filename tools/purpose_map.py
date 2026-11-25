#!/usr/bin/env python3
"""Score Chatelaine's purpose map (SPEC R90, R91), check it, and write docs/purpose-map.md.

    python tools/purpose_map.py docs/purpose-map    score every purpose set and map in a folder (WORK), print the tables
    python tools/purpose_map.py WORK --check        also fail (exit 1) on any placement fault
    python tools/purpose_map.py --render        write docs/purpose-map.md from docs/purpose-map.json
    python tools/purpose_map.py --selftest

WORK holds:
  facets.json         {"facets": {id: description}}
  refs/<app>.json     {"app", "features": [{"id", "name", "facets": [main, ...], "match": full|partial|none}]}
  chatelaine.json     {"features": [{"id", "name", "facets": [...], "rules": [...]}]}
  scenarios.json      {"scenarios": [{"id", "rule", "name", "facets": [...]}]}     steps of R05-R11, R114, R115
  purpose_sets.json   {"sets": {id: {"name", "purposes": {P: {"name", "job", "facets": [...]}}, "outside": [...]}}}
  maps/<id>.json      a candidate map or a control, shape in MAP below; a control serves its app's register count

MAP: {"name", "control": bool, "purpose_set": id or null,
      "screens": [{"id", "name", "purpose", "features": [ids], "surface": bool, "homepage": bool}],
      "where": [{"thing", "home", "repeats": [[screen, why]]}],
      "runs": {scenario rule: [{"screen", "do", "recall": why or null}] or null (cannot be done)},
      "shared": [{"feature", "screens": [...], "why"}], "not_placed": [{"id", "reason"}]}
Standard library only.
"""
import glob
import itertools
import json
import math
import os
import sys

ROOT = os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
SET_AXES = ("coverage", "exclusivity", "balance", "fit", "potential")
MAP_AXES = ("screen count", "one home per thing", "steps per scenario", "recall load")
# R91's constants; the plateau test moves each by a third
CONST = {"per_screen_lo": 2, "per_screen_hi": 8, "repeat_k": 4}


def load(path):
    with open(path, encoding="utf-8") as fh:
        return json.load(fh)


def clip(x):
    return max(0.0, min(1.0, x))


# Purpose-set axes

def purpose_of(facet, pset):
    for p, d in pset["purposes"].items():
        if facet in d["facets"]:
            return p
    if facet in pset.get("outside", []):
        return None
    raise ValueError(f"facet {facet!r} placed by no purpose and not outside")


def set_axes(features, pset):
    """Coverage, exclusivity and balance over facet-tagged features (first facet = main)."""
    n = len(features)
    main = [purpose_of(f["facets"][0], pset) for f in features]
    cov = sum(m is not None for m in main) / n
    excl = sum(m is not None and len({purpose_of(x, pset) for x in f["facets"]}) == 1
               for f, m in zip(features, main)) / n
    counts = {}
    for m in main:
        if m is not None:
            counts[m] = counts.get(m, 0) + 1
    k, tot = len(pset["purposes"]), sum(counts.values())
    ent = -sum(c / tot * math.log(c / tot) for c in counts.values()) if tot else 0.0
    bal = ent / math.log(k) if k > 1 else 1.0
    return {"coverage": cov, "exclusivity": excl, "balance": bal, "counts": counts}


def score_set(pset, refs, own, scenarios):
    per_app = {a: set_axes(fs + scenarios, pset) for a, fs in refs.items()}
    mean = {ax: sum(v[ax] for v in per_app.values()) / len(per_app) for ax in ("coverage", "exclusivity", "balance")}
    o = set_axes(own, pset)
    gaps = [f for fs in refs.values() for f in fs if f.get("match") in ("none", "partial")]
    named = [f for f in gaps if purpose_of(f["facets"][0], pset) is not None]
    return {**mean, "fit": (o["coverage"] + o["exclusivity"]) / 2,
            "potential": len(named) / len(gaps) if gaps else 1.0,
            "own_counts": o["counts"], "gaps": len(gaps), "gaps_named": len(named)}


# Screens-and-homes axes

def functions_of(m, ref_counts):
    """Functions a map serves: the features placed on its screens, or a control's count from the register."""
    placed = {f for sc in m["screens"] for f in sc.get("features", [])}
    return len(placed) or ref_counts.get(m["name"], 0)


def fewest_taps(maps, scenario_ids):
    """For each scenario, the fewest taps any map needs."""
    out = {}
    for sid in scenario_ids:
        done = [len(m["runs"][sid]) for m in maps.values() if m["runs"].get(sid)]
        out[sid] = min(done) if done else None
    return out


def map_axes(m, scenario_ids, fewest, functions, c=CONST):
    screens = [s for s in m["screens"] if not s.get("surface")]
    n = len(screens)
    per = functions / n if n else 0.0
    sc = clip((per - c["per_screen_lo"]) / (c["per_screen_hi"] - c["per_screen_lo"]))
    where = m.get("where", [])
    reps = sum(len(w.get("repeats", [])) for w in where)
    r = reps / len(where) if where else 0.0
    home = max(0.0, 1 - c["repeat_k"] * r)
    per_scen, recall, steps, undone = [], 0, 0, 0
    for sid in scenario_ids:
        run = m["runs"].get(sid)
        if not run:   # cannot be done in this map
            per_scen.append(0.0)
            undone += 1
            continue
        per_scen.append(fewest[sid] / len(run))
        steps += len(run)
        recall += sum(1 for st in run if st.get("recall"))
    stp = sum(per_scen) / len(per_scen) if per_scen else 0.0
    rec = 1 - (recall + undone) / (steps + undone) if steps + undone else 1.0
    taps = [len(m["runs"][s]) for s in scenario_ids if m["runs"].get(s)]
    return {"screen count": sc, "one home per thing": home, "steps per scenario": stp, "recall load": rec,
            "raw": {"screens": n, "functions": functions, "per screen": round(per, 2), "repeats": reps,
                    "things": len(where), "taps mean": round(sum(taps) / len(taps), 2) if taps else None,
                    "recall steps": recall, "undone": undone}}


def total(scores, axes):
    return sum(scores[a] for a in axes) / len(axes)


def ranks(totals):
    """Ties share a rank."""
    order = sorted(set(round(v, 4) for v in totals.values()), reverse=True)
    return {k: order.index(round(v, 4)) + 1 for k, v in totals.items()}


def plateau(maps, scenario_ids, fewest, funcs):
    """How often each map comes first with every constant at 2/3, 1 and 4/3 of its value."""
    wins, runs = {k: 0 for k in maps}, 0
    for f in itertools.product((2 / 3, 1, 4 / 3), repeat=len(CONST)):
        c = {k: v * x for (k, v), x in zip(CONST.items(), f)}
        if c["per_screen_hi"] <= c["per_screen_lo"]:
            continue
        tot = {k: total(map_axes(m, scenario_ids, fewest, funcs[k], c), MAP_AXES) for k, m in maps.items()}
        best = max(tot.values())
        for k, v in tot.items():
            wins[k] += abs(v - best) < 1e-9
        runs += 1
    return {k: v / runs for k, v in wins.items()}


# Checks

def check(work, facets, refs, own, scenarios, sets, maps):
    faults = []
    allf = [f for fs in refs.values() for f in fs] + own + scenarios
    for f in allf:
        if not f.get("facets"):
            faults.append(f"{f['id']}: no facets")
        for x in f.get("facets", []):
            if x not in facets:
                faults.append(f"{f['id']}: unknown facet {x!r}")
    ids = [f["id"] for f in allf]
    faults += [f"duplicate id {i}" for i in sorted({i for i in ids if ids.count(i) > 1})]
    for sid, s in sets.items():
        placed = [x for d in s["purposes"].values() for x in d["facets"]] + s.get("outside", [])
        faults += [f"set {sid}: facet {x} in two places" for x in sorted({x for x in placed if placed.count(x) > 1})]
        faults += [f"set {sid}: facet {x} not placed" for x in sorted(set(facets) - set(placed))]
        faults += [f"set {sid}: purpose {p} has no job" for p, d in s["purposes"].items() if not d.get("job")]
    own_ids = {f["id"] for f in own}
    scen = {s["rule"] for s in scenarios}
    for mid, m in maps.items():
        if m.get("control"):
            continue
        sids = {s["id"] for s in m["screens"]}
        on = {}
        for s in m["screens"]:
            if m.get("purpose_set") and s.get("purpose") not in sets[m["purpose_set"]]["purposes"] and not s.get("surface"):
                faults.append(f"map {mid}: screen {s['id']} has unknown purpose {s.get('purpose')!r}")
            for f in s.get("features", []):
                on.setdefault(f, []).append(s["id"])
        shared = {d["feature"]: d for d in m.get("shared", [])}
        unplaced = {d["id"]: d for d in m.get("not_placed", [])}
        for f in sorted(own_ids):
            where = on.get(f, [])
            if not where and f not in unplaced:
                faults.append(f"map {mid}: {f} not placed")
            if where and f in unplaced:
                faults.append(f"map {mid}: {f} placed and not placed")
            if len(where) > 1 and (f not in shared or set(shared[f]["screens"]) != set(where) or not shared[f].get("why")):
                faults.append(f"map {mid}: {f} on {', '.join(where)} without a shared entry naming each, with a reason")
        faults += [f"map {mid}: {d['id']} not placed without a reason" for d in unplaced.values() if not d.get("reason")]
        faults += [f"map {mid}: unknown feature {f}" for f in sorted(set(on) - own_ids)]
        for w in m.get("where", []):
            if w["home"] not in sids:
                faults.append(f"map {mid}: home {w['home']} of {w['thing']!r} is no screen")
            for rp in w.get("repeats", []):
                if rp[0] not in sids or rp[0] == w["home"] or not rp[1:] or not rp[1]:
                    faults.append(f"map {mid}: repeat {rp[0]} of {w['thing']!r} unknown, the home itself, or without why")
        missing = scen - set(m.get("runs", {}))
        faults += [f"map {mid}: no run for scenario {s}" for s in sorted(missing)]
        for sid, run in m.get("runs", {}).items():
            for st in run or []:
                if st["screen"] not in sids:
                    faults.append(f"map {mid}: scenario {sid} step on unknown screen {st['screen']}")
    return faults


# Report

def score_all(work):
    facets = load(os.path.join(work, "facets.json"))["facets"]
    refs = {load(p)["app"]: load(p)["features"] for p in sorted(glob.glob(os.path.join(work, "refs", "*.json")))}
    own = load(os.path.join(work, "chatelaine.json"))["features"]
    scenarios = load(os.path.join(work, "scenarios.json"))["scenarios"]
    sets = load(os.path.join(work, "purpose_sets.json"))["sets"]
    maps = {os.path.splitext(os.path.basename(p))[0]: load(p) for p in sorted(glob.glob(os.path.join(work, "maps", "*.json")))}
    set_scores = {sid: score_set(s, refs, own, scenarios) for sid, s in sets.items()}
    scen_ids = list(dict.fromkeys(s["rule"] for s in scenarios))   # one run per scenario rule
    fewest = fewest_taps(maps, scen_ids)
    funcs = {mid: functions_of(m, {a: len(fs) for a, fs in refs.items()}) for mid, m in maps.items()}
    map_scores = {mid: map_axes(m, scen_ids, fewest, funcs[mid]) for mid, m in maps.items()}
    return {"facets": facets, "refs": refs, "own": own, "scenarios": scenarios, "sets": sets, "maps": maps,
            "set_scores": set_scores, "map_scores": map_scores,
            "plateau": plateau(maps, scen_ids, fewest, funcs) if maps else {}}


def table(rows, axes, names):
    tot = {k: total(v, axes) for k, v in rows.items()}
    rk = ranks(tot)
    out = ["| | " + " | ".join(axes) + " | total | rank |", "|---" * (len(axes) + 3) + "|"]
    for k in sorted(rows, key=lambda k: (rk[k], k)):
        out.append(f"| {names.get(k, k)} | " + " | ".join(f"{rows[k][a]:.3f}" for a in axes)
                   + f" | {tot[k]:.3f} | {rk[k]} |")
    return out


def report(r):
    out = ["## Purpose sets", ""]
    out += table(r["set_scores"], SET_AXES, {k: f"{k} {v['name']}" for k, v in r["sets"].items()})
    out += ["", "## Maps and controls", ""]
    out += table(r["map_scores"], MAP_AXES,
                 {k: ("control: " if m.get("control") else "") + m["name"] for k, m in r["maps"].items()})
    out += ["", "| | screens | functions | per screen | repeats / things | taps mean | recall steps | undone"
            " | first in the plateau test |", "|---|---|---|---|---|---|---|---|---|"]
    for k, v in r["map_scores"].items():
        x = v["raw"]
        out.append(f"| {r['maps'][k]['name']} | {x['screens']} | {x['functions']} | {x['per screen']} | {x['repeats']} / "
                   f"{x['things']} | {x['taps mean']} | {x['recall steps']} | {x['undone']} | {r['plateau'].get(k, 0):.0%} |")
    return "\n".join(out)


def render(src=os.path.join(ROOT, "docs", "purpose-map.json"), dst=os.path.join(ROOT, "docs", "purpose-map.md")):
    d = load(src)
    out = ["# Purpose map", "", d["about"], "", "Generated from `docs/purpose-map.json` by `tools/purpose_map.py --render`;"
           " edit the JSON, not this page.", "", "## Purposes", "", "| | Purpose | Its job | Screens |", "|---|---|---|---|"]
    for p, v in d["purposes"].items():
        out.append(f"| {p} | **{v['name']}** | {v['job']} | {sum(s['purpose'] == p for s in d['screens'])} |")
    for p, v in d["purposes"].items():
        out += ["", f"## {p} {v['name']}", "", "| Screen | Status | Holds | Reference apps with this kind of function |",
                "|---|---|---|---|"]
        for s in (s for s in d["screens"] if s["purpose"] == p):
            out.append(f"| **{s['name']}** | {s.get('status', '')} | {s['holds']} | {', '.join(s.get('sources', [])) or 'new'} |")
    surf = [s for s in d["screens"] if s["purpose"] not in d["purposes"]]
    if surf:
        out += ["", "## Surfaces outside the screens", "", "| Surface | Status | Holds |", "|---|---|---|"]
        out += [f"| **{s['name']}** | {s.get('status', '')} | {s['holds']} |" for s in surf]
    out += ["", "## One home per thing", "", "| Thing | Home | Repeats |", "|---|---|---|"]
    names = {s["id"]: s["name"] for s in d["screens"]}
    for w in d["where"]:
        rep = "; ".join(f"{names.get(x[0], x[0])} ({x[1]})" for x in w.get("repeats", [])) or "none"
        out.append(f"| {w['thing']} | {names.get(w['home'], w['home'])} | {rep} |")
    if d.get("overlaps"):
        out += ["", "## Overlap candidates", "", "| Entries | How the map settles it |", "|---|---|"]
        out += [f"| {o['entries']} | {o['settled']} |" for o in d["overlaps"]]
    if d.get("scores"):
        out += ["", "## Scores (R91)", ""] + [f"- {n}" for n in d.get("score_notes", [])] + ["", d["scores"]]
    if d.get("sets"):
        out += ["", "### Purposes of each set", "", "| Set | Purposes |", "|---|---|"]
        out += [f"| {k} {v['name']} | {' · '.join(v['purposes'])} |" for k, v in d["sets"].items()]
    if d.get("candidates"):
        out += ["", "### Screens of the other candidate maps", "", "| Map | Screens |", "|---|---|"]
        out += [f"| {c['name']} | {', '.join(c['screens'])} |" for c in d["candidates"]]
    if d.get("owner_answers"):
        out += ["", "## Owner's answers", ""] + [f"- {a}" for a in d["owner_answers"]]
    with open(dst, "w", encoding="utf-8", newline="\n") as fh:
        fh.write("\n".join(out) + "\n")
    return dst


def selftest():
    ok = True

    def eq(name, got, want):
        nonlocal ok
        good = abs(got - want) < 1e-6
        ok &= good
        print(("ok  " if good else "FAIL"), name, round(got, 4), "expected", want)

    ps = {"purposes": {"A": {"facets": ["a"]}, "B": {"facets": ["b"]}}, "outside": ["z"]}
    fs = [{"facets": ["a"]}, {"facets": ["a", "b"]}, {"facets": ["b"]}, {"facets": ["z"]}]
    s = set_axes(fs, ps)
    eq("coverage 3 of 4", s["coverage"], 0.75)
    eq("exclusivity: a+b spans two", s["exclusivity"], 0.5)
    eq("balance 2:1 over two", s["balance"], -(2 / 3 * math.log(2 / 3) + 1 / 3 * math.log(1 / 3)) / math.log(2))
    m = {"screens": [{"id": str(i)} for i in range(10)], "where": [{"thing": "t", "home": "0", "repeats": [["1", "x"]]},
                                                                   {"thing": "u", "home": "0"}],
         "runs": {"s1": [{"screen": "0"}] * 3, "s2": [{"screen": "0", "recall": "y"}] * 5, "s3": None}}
    a = map_axes(m, ["s1", "s2", "s3"], {"s1": 3, "s2": 4, "s3": 2}, 50)
    eq("50 functions on 10 screens: 5 per screen, between 2 and 8", a["screen count"], 0.5)
    eq("one repeat over two things, K 4", a["one home per thing"], 0.0)
    eq("taps against the fewest: 3/3, 4/5, undone 0", a["steps per scenario"], (1 + 0.8 + 0) / 3)
    eq("recall: 5 of 8 steps plus one undone", a["recall load"], 1 - 6 / 9)
    eq("ties share a rank", ranks({"x": 0.5, "y": 0.5, "z": 0.1})["y"], 1)
    print("selftest", "passed" if ok else "FAILED")
    return ok


def main(argv):
    if "--selftest" in argv:
        return 0 if selftest() else 1
    if "--render" in argv:
        print("wrote", render())
        return 0
    work = argv[1]
    r = score_all(work)
    print(report(r))
    if "--check" in argv:
        faults = check(work, r["facets"], r["refs"], r["own"], r["scenarios"], r["sets"], r["maps"])
        for f in faults:
            print("FAIL", f)
        print(f"check: {len(faults)} fault(s)")
        return 1 if faults else 0
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
