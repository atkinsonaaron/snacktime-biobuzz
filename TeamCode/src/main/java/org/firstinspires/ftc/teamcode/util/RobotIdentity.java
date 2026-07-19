package org.firstinspires.ftc.teamcode.util;

import com.qualcomm.robotcore.util.RobotLog;

import org.firstinspires.ftc.robotcore.internal.network.DeviceNameManagerFactory;

import java.util.Locale;

/**
 * RobotIdentity — answers "which physical robot am I running on?" from the hub's network name.
 *
 * WHY THIS EXISTS: the same commit is deployed to both the Competition robot and the Test bot
 * (one codebase, never forked). But the two robots differ — most importantly in drivetrain/Pedro
 * tuning, which depends on mass and weight distribution (a bare test chassis vs. a fully-loaded
 * comp robot). So the code has to know, at runtime, which robot it landed on and load the right
 * tuning for it. Neither Android Studio nor Panels knows this — Android Studio deploys the same
 * bytes to whatever hub is on adb, and Panels reflects whatever hub is running. Identity is a
 * property of the physical hub, read here at init and shown loudly so a human can always confirm it.
 *
 * WHY THE NETWORK NAME: it's set once per hub in the REV Hardware Client, requires a deliberate
 * action + reboot to change (a student can't flip it from an OpMode), survives app reinstalls and
 * Sloth reloads, and is human-visible as the robot's Wi-Fi name. That makes it a far stronger,
 * harder-to-corrupt identity source than a file a stray commit could overwrite.
 *
 * NAMING SCHEME (set in the REV Hardware Client, one per hub):
 *   Competition robot -> 34672-C-RC   (resolves to COMPETITION)
 *   Test bot          -> 34672-T-RC   (resolves to TESTBOT)
 *   anything else     -> UNKNOWN      (fail-closed: caller must load NO tuning and say so loudly)
 *
 * The single-letter suffix (-C / -T) is required by the FTC device-name validator
 * ([a-zA-Z0-9]+(-[a-zA-Z])?-(?i)(DS|RC), verified in RobotCore 11.1.0 sources) AND by competition
 * robot inspection — words like "-COMP" are rejected.
 *
 * FAIL-CLOSED: any failure to read the name, or a name that matches neither suffix, resolves to
 * UNKNOWN. UNKNOWN must never be treated as "probably the comp robot" — the whole point is that an
 * unidentified hub can never silently load the wrong tuning (CLAUDE.md §5, deterministic + fail loud).
 *
 * Read ONCE at OpMode init and hold the result; the name cannot change without a reboot, and
 * reading it is a preferences lookup that has no business in the hot loop (§4).
 */
public final class RobotIdentity {

    public enum Robot { COMPETITION, TESTBOT, UNKNOWN }

    /** Which robot this is. Never null. */
    public final Robot robot;
    /** The raw network name we read, for display/logging. Never null. */
    public final String networkName;

    private RobotIdentity(Robot robot, String networkName) {
        this.robot = robot;
        this.networkName = networkName;
    }

    /** Read the hub's network name and resolve identity. Call once at init, not in the loop. */
    public static RobotIdentity resolve() {
        String name = null;
        try {
            name = DeviceNameManagerFactory.getInstance().getDeviceName();
        } catch (Throwable t) {
            // Off-robot, or the name manager isn't available — fail closed to UNKNOWN.
            RobotLog.ee("RobotIdentity", t, "could not read device name; robot treated as UNKNOWN");
        }

        String raw = (name == null || name.trim().isEmpty()) ? "(unavailable)" : name.trim();

        Robot resolved = Robot.UNKNOWN;
        if (name != null) {
            String upper = raw.toUpperCase(Locale.US);
            if (upper.endsWith("-C-RC")) {
                resolved = Robot.COMPETITION;
            } else if (upper.endsWith("-T-RC")) {
                resolved = Robot.TESTBOT;
            }
        }

        RobotIdentity id = new RobotIdentity(resolved, raw);

        // Loud in the RC log so the exact name is on record (and so the FIRST on-robot run tells us
        // the precise string, in case the returned format ever surprises us). Logs the literal
        // banner text too, so a post-match RC-log read shows exactly what was on the Driver Station.
        RobotLog.ii("RobotIdentity", "network name=\"%s\" resolved to %s — banner: %s",
                raw, resolved, id.banner());
        return id;
    }

    public boolean isCompetition() { return robot == Robot.COMPETITION; }
    public boolean isKnown()       { return robot != Robot.UNKNOWN; }

    /**
     * A loud, one-line banner for the Driver Hub and Panels. Build it ONCE at init and reuse the
     * string every loop — do not rebuild it per loop (§4 rule 8, no per-loop allocations).
     */
    public String banner() {
        switch (robot) {
            case COMPETITION: return "ROBOT: COMPETITION   [" + networkName + "]";
            case TESTBOT:     return "ROBOT: TEST BOT   [" + networkName + "]";
            default:          return "ROBOT: *** UNKNOWN *** name=[" + networkName
                                     + "] — expected ...-C-RC or ...-T-RC";
        }
    }

    /**
     * Same banner, sized up for the Driver Station panel via a subset of HTML tags (FTC SDK
     * {@code Telemetry.DisplayFormat.HTML} — see {@code Telemetry.java}'s own doc comment: "allows
     * use of a subset of HTML tags, enabling rich text display, e.g. color & size"). The CALLER must
     * set {@code telemetry.setDisplayFormat(Telemetry.DisplayFormat.HTML)} once at init for this to
     * render as intended — otherwise the tags show up as literal text.
     *
     * PANELS DOES NOT USE THIS — Panels' telemetry is a different channel with no HTML display-format
     * concept; keep using {@link #banner()} (plain) for {@code PanelsTelemetry…debug()}.
     *
     * Build ONCE at init and reuse, same as {@link #banner()} (§4 rule 8, no per-loop allocations).
     */
    public String bannerHtml() {
        String color;
        switch (robot) {
            case COMPETITION: color = "red";   break;
            case TESTBOT:     color = "green"; break;
            default:          color = "orange"; break;
        }
        return "<font size=\"6\" color=\"" + color + "\"><b>" + banner() + "</b></font>";
    }
}