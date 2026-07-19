# tuning/ — committed per-robot tuning (the canonical backup)

These JSON files are the **canonical, git-backed tuning** for each physical robot. The same code
runs on both robots and loads the right file by hub identity (see `CLAUDE.md` §6/§7/§10 and
`WORKFLOW.md` §11).

| Robot | Hub network name | Committed file (here) | On-hub file it mirrors |
|-------|------------------|-----------------------|------------------------|
| Competition | `34672-C-RC` | `tuning/comp_tuning.json` | `/sdcard/FIRST/settings/comp_tuning.json` |
| Test bot | `34672-T-RC` | `tuning/testbot_tuning.json` | `/sdcard/FIRST/settings/testbot_tuning.json` |

The files may not exist yet — they're created the first time you save a robot's tuning.

## Saving a robot's tuning (no transcription — just commit the file)
After live-tuning in Panels, the robot writes its file to its own hub on stop. To back it up:

```
# pull the robot's file off its hub into this folder, then commit
adb pull /sdcard/FIRST/settings/comp_tuning.json    tuning/comp_tuning.json      # competition
adb pull /sdcard/FIRST/settings/testbot_tuning.json tuning/testbot_tuning.json   # test bot
git add tuning/ && git commit -m "Tune <robot>: <what changed>"
```

That's the whole "promote" step — a whole-file commit, **never** copying individual numbers into
source. Each robot's file is independent, so committing one never touches the other's values.

## Restoring after a hub re-flash (or seeding a fresh hub)
A re-flash wipes the hub's copy; the committed copy here is the backup. Push it back:

```
adb push tuning/comp_tuning.json /sdcard/FIRST/settings/comp_tuning.json
```

If a hub has no file and none is restored, the robot runs on the in-code fallback defaults and says
so loudly on the Driver Hub — it never silently loads the wrong robot's tuning.

## What is NOT here
- **Pedro follower constants + pod offsets** live in code (`pedroPathing/Constants.java`), as
  per-robot constant sets selected by identity — not in these JSON files (decided; see `STATUS.md`).
- **Snapshots** (`snacktime_snapshot_*.json`) are records, not canonical — they're gitignored.