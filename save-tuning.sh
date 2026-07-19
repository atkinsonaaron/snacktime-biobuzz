#!/bin/bash
#
# save-tuning.sh — pull a robot's tuning file off its hub into the repo, ready to commit.
#
# WHY: tuned values are written to a file ON THE HUB (/sdcard/FIRST/settings/), not to this repo.
# A plain `git commit` won't back them up until that file is pulled into tuning/. This script does
# the pull in one step so you don't have to remember the path or filename.
# Background: tuning/README.md and WORKFLOW.md §11.
#
# USAGE:
#   ./save-tuning.sh comp     # competition robot (hub named 34672-C-RC)
#   ./save-tuning.sh test     # test bot          (hub named 34672-T-RC)
#
# PREREQS:
#   - adb installed and on your PATH (Android platform-tools). Comes with Android Studio.
#   - the robot's hub connected to this computer (USB, or adb-over-Wi-Fi).
#
# After it runs, review the change and commit:
#   git add tuning/ && git commit -m "Tune <robot>: <what changed>" && git push

set -e

# Always operate from the repo root, wherever the script is called from.
cd "$(dirname "$0")"

robot="$1"
case "$robot" in
  comp) file="comp_tuning.json" ;;
  test) file="testbot_tuning.json" ;;
  *)    echo "Usage: ./save-tuning.sh comp|test"; exit 1 ;;
esac

if ! command -v adb >/dev/null 2>&1; then
  echo "ERROR: adb not found on PATH. Install Android platform-tools (ships with Android Studio)."
  exit 1
fi

if ! adb get-state >/dev/null 2>&1; then
  echo "ERROR: no hub connected via adb. Plug in the '$robot' robot's hub (USB or Wi-Fi adb)."
  exit 1
fi

hub_path="/sdcard/FIRST/settings/$file"
echo "Pulling $hub_path  ->  tuning/$file"
if ! adb pull "$hub_path" "tuning/$file"; then
  echo "ERROR: could not pull $file."
  echo "       Has the '$robot' robot been run and stopped at least once (which writes the file),"
  echo "       and is the correct hub connected?"
  exit 1
fi

echo
echo "Changes to tuning/ (this is what will be committed):"
git status --short tuning/ 2>/dev/null || true
echo
echo "Next:  git add tuning/ && git commit -m \"Tune $robot: <what changed>\" && git push"