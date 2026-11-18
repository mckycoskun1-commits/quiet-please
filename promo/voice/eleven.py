import argparse
import json
import os
import pathlib
import re
import sys
import urllib.error
import urllib.request

API = "https://api.elevenlabs.io"
here = pathlib.Path(__file__).resolve().parent


def api_key():
    key = os.environ.get("ELEVENLABS_API_KEY")
    if not key and sys.platform == "win32":
        import winreg
        try:
            with winreg.OpenKey(winreg.HKEY_CURRENT_USER, "Environment") as reg:
                key = winreg.QueryValueEx(reg, "ELEVENLABS_API_KEY")[0]
        except OSError:
            key = None
    if not key:
        sys.exit("ELEVENLABS_API_KEY is not set")
    return key.strip()


def call(path, body=None, raw=False):
    data = json.dumps(body).encode() if body is not None else None
    req = urllib.request.Request(API + path, data=data, method="POST" if body is not None else "GET")
    req.add_header("xi-api-key", api_key())
    if body is not None:
        req.add_header("Content-Type", "application/json")
    try:
        with urllib.request.urlopen(req, timeout=120) as res:
            payload = res.read()
    except urllib.error.HTTPError as err:
        sys.exit(f"{err.code} {err.read().decode(errors='replace')[:400]}")
    return payload if raw else json.loads(payload)


def lines():
    out = {}
    for row in (here / "lines-2.1.md").read_text(encoding="utf-8").splitlines():
        cells = [c.strip() for c in row.strip().strip("|").split("|")]
        if len(cells) == 4 and re.fullmatch(r"[pn]\d+", cells[0]):
            out[cells[0]] = (cells[1], cells[3])
    return out


def voices():
    found = {}
    for v in call("/v1/voices")["voices"]:
        found[v["name"].split(" - ")[0].strip().lower()] = v
        found[v["voice_id"]] = v
    return found


def cmd_voices(args):
    seen = set()
    for v in call("/v1/voices")["voices"]:
        if v["voice_id"] in seen:
            continue
        seen.add(v["voice_id"])
        labels = ", ".join(f"{k}={val}" for k, val in (v.get("labels") or {}).items())
        print(f"{v['name'][:40]:40s} {v['voice_id']} {v.get('category', '')} | {labels}")


def cmd_info(args):
    sub = call("/v1/user/subscription")
    print(f"plan {sub.get('tier')}, used {sub.get('character_count')} of {sub.get('character_limit')} credits")
    for m in call("/v1/models"):
        if m.get("can_do_text_to_speech"):
            print(m["model_id"], "|", m.get("name"))


def cmd_say(args):
    table = lines()
    known = voices()
    out = pathlib.Path(args.out)
    out.mkdir(parents=True, exist_ok=True)
    roles = {"Player": args.player, "Narrator": args.narrator}
    wanted = args.lines.split(",") if args.lines else list(table)
    for key in wanted:
        role, text = table[key]
        for name in (roles[role] or "").split(","):
            if not name:
                continue
            voice = known.get(name.lower()) or known.get(name)
            if not voice:
                sys.exit(f"voice not found: {name}")
            tag = re.sub(r"[^a-z0-9]+", "", voice["name"].split(" - ")[0].lower())
            for take in range(1, args.takes + 1):
                target = out / f"{key}-{tag}-{take}.mp3"
                if target.exists() and not args.force:
                    continue
                body = {
                    "text": text,
                    "model_id": args.model,
                    "seed": args.seed + take,
                    "voice_settings": {"stability": args.stability, "similarity_boost": 0.75, "speed": args.speed},
                }
                audio = call(f"/v1/text-to-speech/{voice['voice_id']}?output_format=mp3_44100_192", body, raw=True)
                target.write_bytes(audio)
                print(target.name, len(audio))


ap = argparse.ArgumentParser()
sp = ap.add_subparsers(required=True)
sp.add_parser("voices").set_defaults(run=cmd_voices)
sp.add_parser("info").set_defaults(run=cmd_info)
say = sp.add_parser("say")
say.add_argument("--player", default="")
say.add_argument("--narrator", default="")
say.add_argument("--lines", default="")
say.add_argument("--takes", type=int, default=1)
say.add_argument("--seed", type=int, default=100)
say.add_argument("--speed", type=float, default=1.0)
say.add_argument("--stability", type=float, default=0.5)
say.add_argument("--model", default="eleven_v3")
say.add_argument("--force", action="store_true")
say.add_argument("--out", default=str(here / "takes"))
say.set_defaults(run=cmd_say)
args = ap.parse_args()
args.run(args)
