package org.firstinspires.ftc.teamcode.config;

import com.bylazar.configurables.annotations.Configurable;

/**
 * TuningConfig — cross-cutting configurables that apply globally, not to any one subsystem.
 *
 * Subsystem-specific tunables (drive speeds, PID gains, mechanism powers) live as public static
 * fields in each subsystem file with @Configurable on the class (§6 Tier 1). Panels groups them
 * by class name so they're easy to find. Add each subsystem to TUNING_CLASSES in Persistence.java.
 *
 * Fields must be public + static + non-final for the dashboard to see and edit them.
 */
@Configurable
public class TuningConfig {

    // Verbose subsystem telemetry is a BENCH tool. Leave OFF for matches so the loop allocates no
    // telemetry strings (prime directive §0, §4 rule 8). Flip ON live to watch subsystem health.
    public static boolean verboseTelemetry = false;

    // How long a Problem stays in the DiagnosticsCenter feed before it's cleaned up.
    public static long diagnosticsProblemExpireSeconds = 10;

    // Profiler.timeIt() logs per-block avg/min/max via RobotLog. BENCH TOOL — leave OFF for
    // matches (§4 rule 8). Flip on when investigating a loop-time regression.
    public static boolean profilerEnabled = false;

    private TuningConfig() { } // static holder; never instantiated
}