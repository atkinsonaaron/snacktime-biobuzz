# Snack Time Robotics — Team 34672 — Robot Code (Java)

A generation-first FTC codebase. **Read `CLAUDE.md` first** — it is the operating charter, and every
file here exists to serve one of its rules. This README just maps the tree to that charter.
`STATUS.md` is where this project actually is right now (read it first if setup is underway).
`SETUP.md` walks you from zero to a working robot the first time.
`WORKFLOW.md` is the day-to-day playbook (how to actually work against the charter), and
`CHANGELOG.md` is the plain-language history of changes; per CLAUDE.md §12 the AI adds an entry for
every change it makes, so undo and rollback stay easy.

## Stack
**Java** · FTC SDK (Control Hub) · **SolversLib** (commands/subsystems; maintained FTCLib fork) ·
**Pedro Pathing** (navigation, always latest) · **Limelight 3A** (perception coprocessor) ·
**Panels** (dashboard) · **Sloth** (sub-second hot reload).

## How this drops in
This is a **skeleton to layer onto a base FTC workspace**, not a standalone Gradle project. Recommended:
start from the **SolversLib Quickstart** (ships SolversLib + Pedro), then:
1. copy the `org/firstinspires/ftc/teamcode/**` packages into your `TeamCode` module,
2. apply the changes in `build.gradle.additions` (Sloth, latest Pedro, git-hash BuildConfig),
3. do one full install, then deploy with Sloth from then on.

> API note: SolversLib / Panels / Pedro class names shift between versions. This skeleton follows
> their documented patterns; **confirm exact signatures against the SolversLib javadocs**
> (repo.dairy.foundation/javadoc/releases/org/solverslib/core/latest) and adjust. The *structure* is
> what matters and is stable. TODOs mark the game-specific gaps.

## The tree (four layers, CLAUDE.md §3)
```
teamcode/
├── config/     TuningConfig.java     Live configurables — every tunable number (§6 Tier 1)
├── hardware/   BuildInfo.java        Git hash + build time for snapshot traceability (§7/§12)
├── logic/      IntakeLogic.java      PURE mode→power mapping, no hardware — unit-testable (§9)
├── subsystems/ Intake.java           FLAGSHIP: single-motor active intake; enum + configurable + commands
│               Drivetrain.java       Mecanum + per-wheel health telemetry (§5)
├── util/       BulkReads.java        MANUAL bulk caching — ours to own; biggest loop-time lever (§0/§4)
│               LoopTimer.java        Measures loop time; every OpMode telemeters it (§0/§4 rule 7)
│               Persistence.java      JSON snapshot: auto-export + guarded load, git hash (§7)
│               Datalogger.java       Buffered CSV time-series for debugging (§14)
└── opmodes/    AutonomousExample.java Command tree (not a switch) + alliance/pose in one place (§3/§9)
                TeleOpExample.java     Gamepad -> commands, minimal driver telemetry (§3/§8)
                SystemsCheck.java      Pre-match diagnostic, plain LinearOpMode (§5)

test/logic/     IntakeLogicTest.java  Off-robot unit tests (`./gradlew :TeamCode:test`) (§9)
```

## Start here (Phase 0, CLAUDE.md §13)
1. Wire the config names in `TuningConfig` / subsystems to your Robot Controller configuration (§10).
2. Get `Drivetrain` driving with Panels open (field view + per-wheel telemetry).
3. Prove the loop works end to end: **Sloth hot-reload**, a **snapshot** writing on stop, and the
   **SystemsCheck** passing — all before any game-specific code.

## The rule that keeps this safe
Nothing goes on the competition robot unless Kieran or Elijah can explain what it does (CLAUDE.md §1).
